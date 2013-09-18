package nl.adaptivity.ws.rest;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.devrieze.util.Annotations;
import net.devrieze.util.JAXBCollectionWrapper;
import net.devrieze.util.Types;

import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.util.HttpMessage.Body;
import nl.adaptivity.util.activation.Sources;


public class RestMethodWrapper {

  private Map<String, String> aPathParams;

  private final Object aOwner;

  private final Method aMethod;

  private Object[] aParams;

  private Object aResult;

  public RestMethodWrapper(final Object pOwner, final Method pMethod) {
    aOwner = pOwner;
    aMethod = pMethod;
  }

  public void setPathParams(final Map<String, String> pPathParams) {
    aPathParams = pPathParams;
  }

  public void unmarshalParams(final HttpMessage pHttpMessage) {
    if (aParams != null) {
      throw new IllegalStateException("Parameters have already been unmarshalled");
    }
    final Class<?>[] parameterTypes = aMethod.getParameterTypes();
    final Annotation[][] parameterAnnotations = aMethod.getParameterAnnotations();
    final int argCnt = 0;
    aParams = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; ++i) {
      final RestParam annotation = Annotations.getAnnotation(parameterAnnotations[i], RestParam.class);
      String name;
      ParamType type;
      String xpath;
      if (annotation == null) {
        name = "arg" + Integer.toString(argCnt);
        type = ParamType.QUERY;
        xpath = null;
      } else {
        name = annotation.name();
        type = annotation.type();
        xpath = annotation.xpath();
      }

      aParams[i] = getParam(parameterTypes[i], name, type, xpath, pHttpMessage);

    }
  }

  private Object getParam(final Class<?> pClass, final String pName, final ParamType pType, final String pXpath, final HttpMessage pMessage) {
    Object result = null;
    switch (pType) {
      case GET:
        result = getParamGet(pName, pMessage);
        break;
      case POST:
        result = getParamPost(pName, pMessage);
        break;
      case QUERY:
        result = getParamGet(pName, pMessage);
        if (result == null) {
          result = getParamPost(pName, pMessage);
        }
        break;
      case VAR:
        result = aPathParams.get(pName);
        break;
      case XPATH:
        result = getParamXPath(pClass, pXpath, pMessage.getBody());
        break;
      case ATTACHMENT:
        result = getAttachment(pClass, pName, pMessage);
        break;
      case PRINCIPAL: {
        final Principal principal = pMessage.getUserPrincipal();
        if (pClass.isAssignableFrom(String.class)) {
          result = principal.getName();
        } else {
          result = principal;
        }
        break;
      }

    }
    if ((result != null) && (!pClass.isInstance(result))) {
      if (Types.isPrimitive(pClass) || ((Types.isPrimitiveWrapper(pClass)) && (result instanceof String))) {
        result = Types.parsePrimitive(pClass, ((String) result));
      } else if (Enum.class.isAssignableFrom(pClass)) {
        @SuppressWarnings({ "rawtypes" })
        final Class clazz = pClass;
        @SuppressWarnings("unchecked")
        final Enum<?> tmpResult = Enum.valueOf(clazz, result.toString());
        result = tmpResult;
      } else {
        final String s = result.toString();
        // Only wrap when we don't start with <
        final char[] requestBody = (s.startsWith("<") ? s : "<wrapper>" + s + "</wrapper>").toCharArray();
        if (requestBody.length > 0) {
          result = JAXB.unmarshal(new CharArrayReader(requestBody), pClass);
        } else {
          result = null;
        }
      }
    }

    return result;
  }

  private static Object getAttachment(final Class<?> pClass, final String pName, final HttpMessage pMessage) {
    final DataSource source = pMessage.getAttachment(pName);
    if (source != null) {
      if (DataHandler.class.isAssignableFrom(pClass)) {
        return new DataHandler(source);
      }
      if (DataSource.class.isAssignableFrom(pClass)) {
        return source;
      }
      if (InputStream.class.isAssignableFrom(pClass)) {
        try {
          return source.getInputStream();
        } catch (final IOException e) {
          throw new MessagingException(e);
        }
      }
      try {
        // This will try to do magic to handle the data
        return new DataHandler(source).getContent();
      } catch (final IOException e) {
        throw new MessagingException(e);
      }

    }
    return null;
  }

  private static Object getParamGet(final String pName, final HttpMessage pMessage) {
    return pMessage.getQuery(pName);
  }

  private static Object getParamPost(final String pName, final HttpMessage pMessage) {
    return pMessage.getPost(pName);
  }

  private static <T> T getParamXPath(final Class<T> pClass, final String pXpath, final Body pBody) {
    boolean jaxb;
    if (CharSequence.class.isAssignableFrom(pClass)) {
      jaxb = false;
    } else {
      jaxb = true;
    }
    Node match;
    for (final Node n : pBody.getElements()) {
      match = xpathMatch(n, pXpath);
      if (match != null) {
        if (jaxb) {
          return JAXB.unmarshal(new DOMSource(match), pClass);
        } else {
          return pClass.cast(nodeToString(match));
        }
      }
    }
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");

  }

  private static String nodeToString(final Node pNode) {
    return pNode.getTextContent();
  }

  private static Node xpathMatch(final Node pN, final String pXpath) {
    final XPathFactory factory = XPathFactory.newInstance();
    final XPath xpath = factory.newXPath();
    NodeList result;
    try {
      result = (NodeList) xpath.evaluate(pXpath, new DOMSource(pN), XPathConstants.NODESET);
    } catch (final XPathExpressionException e) {
      return null;
    }
    if ((result == null) || (result.getLength() == 0)) {
      return null;
    }
    return result.item(0);
  }

  public void exec() {
    if (aParams == null) {
      throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
    }
    try {
      aResult = aMethod.invoke(aOwner, aParams);
    } catch (final IllegalArgumentException e) {
      throw new MessagingException(e);
    } catch (final IllegalAccessException e) {
      throw new MessagingException(e);
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      throw new MessagingException(cause != null ? cause : e);
    }
  }

  /**
   * @deprecated use {@link #marshalResult(HttpServletResponse)}, the pRequest parameter is ignored
   */
  @Deprecated
  public void marshalResult(@SuppressWarnings("unused") final HttpMessage pRequest, final HttpServletResponse pResponse) throws TransformerException, IOException {
    marshalResult(pResponse);
  }

  public void marshalResult(final HttpServletResponse pResponse) throws TransformerException, IOException {
    final XmlRootElement xmlRootElement = aResult == null ? null : aResult.getClass().getAnnotation(XmlRootElement.class);
    if (xmlRootElement != null) {
      try {
        final JAXBContext jaxbContext = JAXBContext.newInstance(aMethod.getReturnType());
        final JAXBSource jaxbSource = new JAXBSource(jaxbContext, aResult);
        setContentType(pResponse, "text/xml");
        Sources.writeToStream(jaxbSource, pResponse.getOutputStream());
      } catch (final JAXBException e) {
        throw new MessagingException(e);
      }
    } else if (aResult instanceof Source) {
      setContentType(pResponse, "application/binary");// Unknown content type
      Sources.writeToStream((Source) aResult, pResponse.getOutputStream());
    } else if (aResult instanceof Node) {
      pResponse.setContentType("text/xml");
      Sources.writeToStream(new DOMSource((Node) aResult), pResponse.getOutputStream());
    } else if (aResult instanceof Collection) {
      final XmlElementWrapper annotation = aMethod.getAnnotation(XmlElementWrapper.class);
      if (annotation != null) {
        setContentType(pResponse, "text/xml");
        Sources.writeToStream(collectionToSource(aMethod.getGenericReturnType(), (Collection<?>) aResult, getQName(annotation)), pResponse.getOutputStream());
      }
    } else if (aResult instanceof CharSequence) {
      setContentType(pResponse, "text/plain");
      pResponse.getWriter().append((CharSequence) aResult);
    } else {
      if (aResult != null) {
        try {
          final JAXBContext jaxbContext = JAXBContext.newInstance(aMethod.getReturnType());
          setContentType(pResponse, "text/xml");

          final JAXBSource jaxbSource = new JAXBSource(jaxbContext, aResult);
          Sources.writeToStream(jaxbSource, pResponse.getOutputStream());

        } catch (final JAXBException e) {
          throw new MessagingException(e);
        }
      }
    }
  }

  private void setContentType(final HttpServletResponse pResponse, final String pDefault) {
    final RestMethod methodAnnotation = aMethod.getAnnotation(RestMethod.class);
    if ((methodAnnotation == null) || (methodAnnotation.contentType().length() == 0)) {
      pResponse.setContentType(pDefault);
    } else {
      pResponse.setContentType(methodAnnotation.contentType());
    }
  }

  private Source collectionToSource(final Type pReturnType, final Collection<?> pResult, final QName pName) {
    final Class<?> rawType;
    if (pReturnType instanceof ParameterizedType) {
      final ParameterizedType returnType = (ParameterizedType) pReturnType;
      rawType = (Class<?>) returnType.getRawType();
    } else if (pReturnType instanceof Class<?>) {
      rawType = (Class<?>) pReturnType;
    } else if (pReturnType instanceof WildcardType) {
      final Type[] UpperBounds = ((WildcardType) pReturnType).getUpperBounds();
      if (UpperBounds.length > 0) {
        rawType = (Class<?>) UpperBounds[0];
      } else {
        rawType = Object.class;
      }
    } else if (pReturnType instanceof TypeVariable) {
      final Type[] UpperBounds = ((TypeVariable<?>) pReturnType).getBounds();
      if (UpperBounds.length > 0) {
        rawType = (Class<?>) UpperBounds[0];
      } else {
        rawType = Object.class;
      }
    } else {
      throw new IllegalArgumentException("Unsupported type variable");
    }
    Class<?> elementType = null;
    if (Collection.class.isAssignableFrom(rawType)) {
      final Type[] paramTypes = Types.getTypeParametersFor(Collection.class, pReturnType);
      elementType = Types.toRawType(paramTypes[0]);
      if (elementType.isInterface()) {
        // interfaces not supported by jaxb
        elementType = Types.commonAncestor(pResult);
      }
    } else {
      elementType = Types.commonAncestor(pResult);
    }
    try {
      JAXBContext context;
      if (elementType == null) {
        context = newJAXBContext(JAXBCollectionWrapper.class);
      } else {
        context = newJAXBContext(JAXBCollectionWrapper.class, elementType);
      }
      return new JAXBSource(context, new JAXBCollectionWrapper(pResult, elementType).getJAXBElement(pName));
    } catch (final JAXBException e) {
      throw new MessagingException(e);
    }
  }

  private JAXBContext newJAXBContext(final Class<?>... pClasses) throws JAXBException {
    Class<?>[] classList;
    final Class<?> clazz = aMethod.getDeclaringClass();
    final XmlSeeAlso seeAlso = clazz.getAnnotation(XmlSeeAlso.class);
    if ((seeAlso != null) && (seeAlso.value().length > 0)) {
      final Class<?>[] seeAlsoClasses = seeAlso.value();
      classList = new Class<?>[seeAlsoClasses.length + pClasses.length];
      System.arraycopy(seeAlsoClasses, 0, classList, 0, seeAlsoClasses.length);
      System.arraycopy(pClasses, 0, classList, seeAlsoClasses.length, pClasses.length);
    } else {
      classList = pClasses;
    }
    return JAXBContext.newInstance(classList);
  }

  private static QName getQName(final XmlElementWrapper pAnnotation) {
    String nameSpace = pAnnotation.namespace();
    if ("##default".equals(nameSpace)) {
      nameSpace = XMLConstants.NULL_NS_URI;
    }
    final String localName = pAnnotation.name();
    return new QName(nameSpace, localName, XMLConstants.DEFAULT_NS_PREFIX);
  }

}
