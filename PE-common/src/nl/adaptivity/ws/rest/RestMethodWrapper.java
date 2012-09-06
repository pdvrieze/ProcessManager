package nl.adaptivity.ws.rest;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.servlet.http.HttpServletRequest;
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

import nl.adaptivity.process.engine.MyMessagingException;
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

  public RestMethodWrapper(Object pOwner, Method pMethod) {
    aOwner = pOwner;
    aMethod = pMethod;
  }

  public void setPathParams(Map<String, String> pPathParams) {
    aPathParams = pPathParams;
  }

  public void unmarshalParams(HttpMessage pHttpMessage, Map<String, DataHandler> pAttachments) {
    if (aParams!=null) {
      throw new IllegalStateException("Parameters have already been unmarshalled");
    }
    Class<?>[] parameterTypes = aMethod.getParameterTypes();
    Annotation[][] parameterAnnotations = aMethod.getParameterAnnotations();
    int argCnt = 0;
    aParams = new Object[parameterTypes.length];

    for(int i =0; i<parameterTypes.length; ++i) {
      RestParam annotation = Annotations.getAnnotation(parameterAnnotations[i], RestParam.class);
      String name;
      ParamType type;
      String xpath;
      if (annotation==null) {
        name = "arg"+Integer.toString(argCnt);
        type = ParamType.QUERY;
        xpath = null;
      } else {
        name = annotation.name();
        type = annotation.type();
        xpath = annotation.xpath();
      }

      aParams[i] = getParam(parameterTypes[i], name, type, xpath, pHttpMessage, pAttachments);

    }
  }

  private Object getParam(Class<?> pClass, String pName, ParamType pType, String pXpath, HttpMessage pMessage, Map<String, DataHandler> pAttachments) {
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
        if (result==null) {
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
        result = getAttachment(pClass, pName, pAttachments);
    }
    if (result != null && (! pClass.isInstance(result))) {
      if (Types.isPrimitive(pClass)||(Types.isPrimitiveWrapper(pClass)) && result instanceof String) {
        result = Types.parsePrimitive(pClass, ((String) result));
      } else {
        result = JAXB.unmarshal(new CharArrayReader(result.toString().toCharArray()), pClass);
      }
    }

    return result;
  }

  private Object getAttachment(Class<?> pClass, String pName, Map<String, DataHandler> pAttachments) {
    DataHandler handler = pAttachments.get(pName);
    if (handler != null) {
      if (DataHandler.class.isAssignableFrom(pClass)) {
        return handler;
      }
      if (InputStream.class.isAssignableFrom(pClass)) {
        try {
          return handler.getInputStream();
        } catch (IOException e) {
          throw new MyMessagingException(e);
        }
      }
      if (DataSource.class.isAssignableFrom(pClass)) {
        return handler.getDataSource();
      }
      try {
        return handler.getContent();
      } catch (IOException e) {
        throw new MyMessagingException(e);
      }

    }
    return null;
  }

  private Object getParamGet(String pName, HttpMessage pMessage) {
    return pMessage.getQuery(pName);
  }

  private Object getParamPost(String pName, HttpMessage pMessage) {
    return pMessage.getPost(pName);
  }

  private <T> T getParamXPath(Class<T> pClass, String pXpath, Body pBody) {
    boolean jaxb;
    if (CharSequence.class.isAssignableFrom(pClass)) {
      jaxb = false;
    } else {
      jaxb = true;
    }
    Node match;
    for (Node n: pBody.getElements()) {
      match = xpathMatch(n, pXpath);
      if (match !=null) {
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

  private String nodeToString(Node pNode) {
    return pNode.getTextContent();
  }

  private Node xpathMatch(Node pN, String pXpath) {
    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();
    NodeList result;
    try {
      result = (NodeList) xpath.evaluate(pXpath, new DOMSource(pN), XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      return null;
    }
    if (result==null || result.getLength()==0) {
      return null;
    }
    return result.item(0);
  }

  public void exec() {
    if (aParams==null) {
      throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
    }
    try {
      aResult = aMethod.invoke(aOwner, aParams);
    } catch (IllegalArgumentException e) {
      throw new MyMessagingException(e);
    } catch (IllegalAccessException e) {
      throw new MyMessagingException(e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      throw new MyMessagingException(cause!=null ? cause : e);
    }
  }

  public void marshalResult(HttpMessage pRequest, HttpServletResponse pResponse) throws TransformerException, IOException {
    XmlRootElement xmlRootElement = aResult==null ? null : aResult.getClass().getAnnotation(XmlRootElement.class);
    if (xmlRootElement!=null) {
      try {
        JAXBContext jaxbContext = JAXBContext.newInstance(aMethod.getReturnType());
        final JAXBSource jaxbSource = new JAXBSource(jaxbContext, aResult);
        setContentType(pResponse, "text/xml");
        Sources.writeToStream(jaxbSource, pResponse.getOutputStream());
      } catch (JAXBException e) {
        throw new MyMessagingException(e);
      }
    } else if (aResult instanceof Source) {
      setContentType(pResponse, "application/binary");// Unknown content type
      Sources.writeToStream((Source) aResult, pResponse.getOutputStream());
    } else if (aResult instanceof Node){
      pResponse.setContentType("text/xml");
      Sources.writeToStream(new DOMSource((Node) aResult), pResponse.getOutputStream());
    } else if (aResult instanceof Collection) {
      XmlElementWrapper annotation = aMethod.getAnnotation(XmlElementWrapper.class);
      if (annotation!=null) {
        setContentType(pResponse, "text/xml");
        Sources.writeToStream(collectionToSource(aMethod.getGenericReturnType(),(Collection<?>) aResult, getQName(annotation)), pResponse.getOutputStream());
//
//
//        Collection<?> value = (Collection<?>) aResult;
//        Collection<JAXBElement<String>> value2 = new ArrayDeque<JAXBElement<String>>();
//        value2.add(new JAXBElement<String>(new QName("test"), String.class, "value1"));
//        value2.add(new JAXBElement<String>(new QName("test"), String.class, "value2"));
//        @SuppressWarnings("unchecked") Class<Collection<?>> declaredType = ((Class) aResult.getClass());
//        QName name = getQName(annotation);
//
//
//        JAXBElement<?> element = new JAXBElement<Collection<?>>(name, declaredType, value2);
//
//        element = (new JAXBCollectionWrapper((Collection<?>) aResult)).getJAXBElement(name);
//
//        try {
//          JAXBContext jaxbContext = newJAXBContext(JAXBCollectionWrapper.class, aResult.getClass());
////          jaxbContext.createMarshaller().marshal(element, System.err);
//          pReply.setContent(new JAXBSource(jaxbContext, element));
//        } catch (JAXBException e) {
//          throw new MessagingException(e);
//        }

      }
    } else if (aResult instanceof CharSequence) {
      setContentType(pResponse, "text/plain");
      pResponse.getWriter().append((CharSequence) aResult);
    } else {
      if (aResult !=null) {
        try {
          JAXBContext jaxbContext = JAXBContext.newInstance(aMethod.getReturnType());
          setContentType(pResponse, "text/xml");

          JAXBSource jaxbSource = new JAXBSource(jaxbContext, aResult);
          Sources.writeToStream(jaxbSource, pResponse.getOutputStream());

        } catch (JAXBException e) {
          throw new MyMessagingException(e);
        }
      }
    }
  }

  private void setContentType(HttpServletResponse pResponse, final String pDefault) {
    RestMethod methodAnnotation = aMethod.getAnnotation(RestMethod.class);
    if (methodAnnotation==null || methodAnnotation.contentType().length()==0) {
      pResponse.setContentType(pDefault);
    } else {
      pResponse.setContentType(methodAnnotation.contentType());
    }
  }

  private Source collectionToSource(Type pReturnType, Collection<?> pResult, QName pName) {
    final Class<?> rawType;
    if (pReturnType instanceof ParameterizedType) {
      ParameterizedType returnType = (ParameterizedType) pReturnType;
      rawType = (Class<?>) returnType.getRawType();
    } else if (pReturnType instanceof Class<?>) {
      rawType = (Class<?>) pReturnType;
    } else if (pReturnType instanceof WildcardType) {
      final Type[] UpperBounds = ((WildcardType) pReturnType).getUpperBounds();
      if (UpperBounds.length>0) {
        rawType = (Class<?>) UpperBounds[0];
      } else {
        rawType = Object.class;
      }
    } else if (pReturnType instanceof TypeVariable) {
      final Type[] UpperBounds = ((TypeVariable<?>) pReturnType).getBounds();
      if (UpperBounds.length>0) {
        rawType = (Class<?>) UpperBounds[0];
      } else {
        rawType = Object.class;
      }
    } else {
      throw new IllegalArgumentException("Unsupported type variable");
    }
    Class<?> elementType = null;
    if (Collection.class.isAssignableFrom(rawType)) {
      Type[] paramTypes = Types.getTypeParametersFor(Collection.class, pReturnType);
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
      if (elementType==null) {
        context = newJAXBContext(JAXBCollectionWrapper.class);
      } else {
        context = newJAXBContext(JAXBCollectionWrapper.class, elementType);
      }
      return new JAXBSource(context, new JAXBCollectionWrapper(pResult, elementType).getJAXBElement(pName));
    } catch (JAXBException e) {
      throw new MyMessagingException(e);
    }
  }

  private JAXBContext newJAXBContext(Class<?>...pClasses) throws JAXBException {
    Class<?>[] classList;
    Class<?> clazz = aMethod.getDeclaringClass();
    XmlSeeAlso seeAlso = clazz.getAnnotation(XmlSeeAlso.class);
    if (seeAlso!=null && seeAlso.value().length>0) {
      final Class<?>[] seeAlsoClasses = seeAlso.value();
      classList = new Class<?>[seeAlsoClasses.length+pClasses.length];
      System.arraycopy(seeAlsoClasses, 0, classList, 0, seeAlsoClasses.length);
      System.arraycopy(pClasses, 0, classList, seeAlsoClasses.length, pClasses.length);
    } else {
      classList = pClasses;
    }
    return JAXBContext.newInstance(classList);
  }

  private static QName getQName(XmlElementWrapper pAnnotation) {
    String nameSpace = pAnnotation.namespace();
    if ("##default".equals(nameSpace)) {
      nameSpace = XMLConstants.NULL_NS_URI;
    }
    String localName = pAnnotation.name();
    return new QName(nameSpace, localName, XMLConstants.DEFAULT_NS_PREFIX);
  }

}
