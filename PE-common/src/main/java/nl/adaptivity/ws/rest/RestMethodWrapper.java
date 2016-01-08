/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.ws.rest;

import net.devrieze.util.Annotations;
import net.devrieze.util.JAXBCollectionWrapper;
import net.devrieze.util.ReaderInputStream;
import net.devrieze.util.Types;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.StAXWriter;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlStreaming;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;


public abstract class RestMethodWrapper extends nl.adaptivity.ws.WsMethodWrapper {



  private static class Java6RestMethodWrapper extends RestMethodWrapper {

    public Java6RestMethodWrapper(Object pOwner, Method pMethod) {
      super(pOwner, pMethod);
    }

    @Override
    protected Class<?>[] getParameterTypes() {
      return mMethod.getParameterTypes();
    }

    @Override
    protected Annotation[][] getParameterAnnotations() {
      return mMethod.getParameterAnnotations();
    }

    @Override
    protected XmlElementWrapper getElementWrapper() {
      return mMethod.getAnnotation(XmlElementWrapper.class);
    }


    @Override
    protected Class<?> getReturnType() {
      return mMethod.getReturnType();
    }

    @Override
    protected Type getGenericReturnType() {
      return mMethod.getGenericReturnType();
    }

    @Override
    protected RestMethod getRestMethod() {
      return mMethod.getAnnotation(RestMethod.class);
    }

    @Override
    protected Class<?> getDeclaringClass() {
      return mMethod.getDeclaringClass();
    }

    @Override
    public void exec() {
      if (mParams == null) {
        throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
      }
      try {
        mResult = mMethod.invoke(mOwner, mParams);
      } catch (final IllegalArgumentException| IllegalAccessException e) {
        throw new MessagingException(e);
      } catch (final InvocationTargetException e) {
        final Throwable cause = e.getCause();
        throw new MessagingException(cause != null ? cause : e);
      }
    }

  }

  private static class Java8RestMethodWrapper extends RestMethodWrapper {

    private final MethodHandle mMethodHandle;
    private final Annotation[][] mParameterAnnotations;
    private final XmlElementWrapper mElementWrapper;
    private final Type mGenericReturnType;
    private final Class<?> mReturnType;
    private final RestMethod mRestMethod;
    private final Class<?> mDeclaringClass;

    public Java8RestMethodWrapper(Object pOwner, Method pMethod) {
      super(pOwner, pMethod);
      try {
        mMethodHandle = MethodHandles.lookup().unreflect(pMethod).bindTo(pOwner);
        mParameterAnnotations = pMethod.getParameterAnnotations();
        mElementWrapper = pMethod.getAnnotation(XmlElementWrapper.class);
        mGenericReturnType = pMethod.getGenericReturnType();
        mReturnType = pMethod.getReturnType();
        mRestMethod = pMethod.getAnnotation(RestMethod.class);
        mDeclaringClass = pMethod.getDeclaringClass();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected Class<?>[] getParameterTypes() {
      return mMethodHandle.type().parameterArray();
    }

    @Override
    protected Annotation[][] getParameterAnnotations() {
      return mParameterAnnotations;
    }

    @Override
    protected XmlElementWrapper getElementWrapper() {
      return mElementWrapper;
    }

    @Override
    protected RestMethod getRestMethod() {
      return mRestMethod;
    }

    @Override
    protected Class<?> getReturnType() {
      return mReturnType;
    }

    @Override
    protected Type getGenericReturnType() {
      return mGenericReturnType;
    }

    @Override
    protected Class<?> getDeclaringClass() {
      return mDeclaringClass;
    }

    @Override
    public void exec() {
      if (mParams == null) {
        throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
      }
      try {
        mResult = mMethodHandle.invokeWithArguments(mParams);
      } catch (final InvocationTargetException e) {
        final Throwable cause = e.getCause();
        throw new MessagingException(cause != null ? cause : e);
      } catch (final Throwable e) {
        throw new MessagingException(e);
      }
    }

  }

  private Map<String, String> mPathParams;

  private boolean mContentTypeSet = false;

  private static class HasMethodHandleHelper {
    private static final boolean HASHANDLES;

    static {
      boolean hashandles;
      try {
        hashandles = MethodHandle.class.getName()!=null;
      } catch (RuntimeException e) {
        hashandles = false;
      }
      HASHANDLES = hashandles;
    }
  }

  protected RestMethodWrapper(final Object owner, final Method method) {
    super(owner, method);
  }

  public static RestMethodWrapper get(final Object pOwner, final Method pMethod) {
    // Make it work with private methods and
    pMethod.setAccessible(true);
    if (HasMethodHandleHelper.HASHANDLES && (!"1.7".equals("java.specification.version"))) {
      return new Java8RestMethodWrapper(pOwner, pMethod);
    } else {
      return new Java6RestMethodWrapper(pOwner, pMethod);
    }
  }

  public void setPathParams(final Map<String, String> pPathParams) {
    mPathParams = pPathParams;
  }

  public void unmarshalParams(final HttpMessage pHttpMessage) throws XmlException {
    if (mParams != null) {
      throw new IllegalStateException("Parameters have already been unmarshalled");
    }
    final Class<?>[] parameterTypes = getParameterTypes();
    final Annotation[][] parameterAnnotations = getParameterAnnotations();
    final int argCnt = 0;
    mParams = new Object[parameterTypes.length];

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

      switch (type) {
        case ATTACHMENT:
          if (pHttpMessage.getAttachments().isEmpty()) {
            // No attachments, are we the only one, then take the body
            int attachmentCount = 0;
            for(int j=0; j<parameterAnnotations.length; ++j) {
              if (Annotations.getAnnotation(parameterAnnotations[j], RestParam.class).type()==ParamType.ATTACHMENT) {
                ++attachmentCount;
              }
            }
            if (attachmentCount==1) {
              if (pHttpMessage.getBody()!=null) {
                mParams[i] = coerceBody(parameterTypes[i], name, pHttpMessage.getBody());
              } else if (pHttpMessage.getByteContent().size()==1){
                mParams[i] = coerceSource(parameterTypes[i], pHttpMessage.getByteContent().get(0));
              }
              break;
            }
          } // explicit fallthrough if the special case does not apply. getBody mangles the outer value though.
        default:
          mParams[i] = getParam(parameterTypes[i], name, type, xpath, pHttpMessage);
      }


    }
  }

  private Object getParam(final Class<?> pClass, final String pName, final ParamType pType, final String pXpath, final HttpMessage pMessage) throws XmlException {
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
        result = mPathParams.get(pName);
        break;
      case XPATH:
        result = getParamXPath(pClass, pXpath, pMessage.getBody());
        break;
      case BODY:
        result = getBody(pClass, pMessage);
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
    // XXX generizice this and share the same approach to unmarshalling in ALL code
    // TODO support collection/list parameters
    if ((result != null) && (!pClass.isInstance(result))) {
      if ((Types.isPrimitive(pClass) || (Types.isPrimitiveWrapper(pClass))) && (result instanceof String)) {
        result = Types.parsePrimitive(pClass, ((String) result));
      } else if (Enum.class.isAssignableFrom(pClass)) {
        @SuppressWarnings({ "rawtypes" })
        final Class clazz = pClass;
        @SuppressWarnings("unchecked")
        final Enum<?> tmpResult = Enum.valueOf(clazz, result.toString());
        result = tmpResult;
      } else if (result instanceof Node) {
        XmlDeserializer factory = pClass.getAnnotation(XmlDeserializer.class);
        if (factory!=null) {
          try {
            result = factory.value().newInstance().deserialize(XmlStreaming.newReader(new DOMSource((Node) result)));
          } catch (IllegalAccessException | InstantiationException e) {
            throw new XmlException(e);
          }
        } else {
          result = JAXB.unmarshal(new DOMSource((Node) result), pClass);
        }
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

  private static Object getBody(final Class<?> pClass, final HttpMessage pMessage) throws XmlException {
    CompactFragment body = pMessage.getBody();
    if (body!=null) {
      return XmlUtil.childrenToDocumentFragment(XMLFragmentStreamReader.from(body));
    } else {
      return getAttachment(pClass, null, pMessage);
    }
  }

  private static Object getAttachment(final Class<?> pClass, final String pName, final HttpMessage pMessage) {
    final DataSource source = pMessage.getAttachment(pName);
    return coerceSource(pClass, source);
  }

  private static Object coerceBody(final Class<?> pTargetType, final String name, final CompactFragment pBody) {
    DataSource dataSource = new DataSource() {
      @Override
      public InputStream getInputStream() throws IOException {
        return new ReaderInputStream(Charset.forName("UTF-8"), new CharArrayReader(pBody.getContent()));
      }

      @Override
      public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getContentType() {
        return "application/xml";
      }

      @Override
      public String getName() {
        return name;
      }
    };
    return coerceSource(pTargetType, dataSource);
  }

  private static Object coerceSource(final Class<?> pClass, final DataSource pSource) {
    if (pSource != null) {
      if (DataHandler.class.isAssignableFrom(pClass)) {
        return new DataHandler(pSource);
      }
      if (DataSource.class.isAssignableFrom(pClass)) {
        return pSource;
      }
      if (InputStream.class.isAssignableFrom(pClass)) {
        try {
          return pSource.getInputStream();
        } catch (final IOException e) {
          throw new MessagingException(e);
        }
      }
      try {
        // This will try to do magic to handle the data
        return new DataHandler(pSource).getContent();
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
    return pMessage.getPosts(pName);
  }

  private static <T> T getParamXPath(final Class<T> pClass, final String pXpath, final CompactFragment pBody) throws XmlException {
    // TODO Avoid JAXB where possible, use XMLDeserializer instead
    final boolean string = CharSequence.class.isAssignableFrom(pClass);
    Node match;
    DocumentFragment fragment = XmlUtil.childrenToDocumentFragment(XMLFragmentStreamReader.from(pBody));
    for (Node n = fragment.getFirstChild(); n!=null; n = n.getNextSibling()) {
      match = xpathMatch(n, pXpath);
      if (match != null) {
        if (! string) {
          XmlDeserializer deserializer = pClass.getAnnotation(XmlDeserializer.class);
          if (deserializer!=null) {
            try {
              XmlDeserializerFactory<?> factory = deserializer.value().newInstance();
              factory.deserialize(XmlStreaming.newReader(new DOMSource(n)));
            } catch (InstantiationException | IllegalAccessException e) {
              throw new RuntimeException(e);
            }
          } else {
            return JAXB.unmarshal(new DOMSource(match), pClass);
          }
        } else {
          return pClass.cast(nodeToString(match));
        }
      }
    }
    return null;
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

  private void serializeValue(final HttpServletResponse pResponse, Object value) throws TransformerException, IOException, FactoryConfigurationError {
    if (value instanceof Source) {
      setContentType(pResponse, "application/binary");// Unknown content type
      Sources.writeToStream((Source) value, pResponse.getOutputStream());
    } else if (value instanceof Node) {
      pResponse.setContentType("text/xml");
      Sources.writeToStream(new DOMSource((Node) value), pResponse.getOutputStream());
    } else if (value instanceof XmlSerializable) {
      pResponse.setContentType("text/xml");
      XMLOutputFactory factory = XMLOutputFactory.newInstance();
      factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
      try {
        StAXWriter out = (StAXWriter) XmlStreaming.newWriter(pResponse.getOutputStream(), pResponse.getCharacterEncoding(), true);
        try {
          out.startDocument(null, null, null);
          ((XmlSerializable) value).serialize(out);
          out.endDocument();
        } finally {
          out.close();
        }
      } catch (XmlException e) {
        throw new TransformerException(e);
      }
    } else if (value instanceof Collection) {
      final XmlElementWrapper annotation = getElementWrapper();
      if (annotation != null) {
        setContentType(pResponse, "text/xml");
        Sources.writeToStream(collectionToSource(getGenericReturnType(), (Collection<?>) value, getQName(annotation)), pResponse.getOutputStream());
      }
    } else if (value instanceof CharSequence) {
      setContentType(pResponse, "text/plain");
      pResponse.getWriter().append((CharSequence) value);
    } else {
      if (value != null) {
        try {
          final JAXBContext jaxbContext = JAXBContext.newInstance(getReturnType());
          setContentType(pResponse, "text/xml");

          final JAXBSource jaxbSource = new JAXBSource(jaxbContext, value);
          Sources.writeToStream(jaxbSource, pResponse.getOutputStream());

        } catch (final JAXBException e) {
          throw new MessagingException(e);
        }
      }
    }
  }

  protected abstract XmlElementWrapper getElementWrapper();

  protected abstract RestMethod getRestMethod();

  protected abstract Annotation[][] getParameterAnnotations();

  protected abstract Class<?>[] getParameterTypes();

  protected abstract Class<?> getReturnType();

  protected abstract Type getGenericReturnType();

  protected abstract Class<?> getDeclaringClass();

  /**
   * @deprecated use {@link #marshalResult(HttpServletResponse)}, the pRequest parameter is ignored
   */
  @Deprecated
  public void marshalResult(@SuppressWarnings("unused") final HttpMessage pRequest, final HttpServletResponse pResponse) throws TransformerException, IOException, XmlException {
    marshalResult(pResponse);
  }

  public void marshalResult(final HttpServletResponse pResponse) throws TransformerException, IOException, XmlException {
    if (mResult instanceof XmlSerializable) {
      // By default don't use JAXB
      setContentType(pResponse, "text/xml");
      OutputStreamWriter writer = new OutputStreamWriter(pResponse.getOutputStream(), pResponse.getCharacterEncoding());
      XmlUtil.serialize((XmlSerializable) mResult, writer);
      writer.close();
      return;
    }
    final XmlRootElement xmlRootElement = mResult == null ? null : mResult.getClass().getAnnotation(XmlRootElement.class);
    if (xmlRootElement != null) {
      try {
        final JAXBContext jaxbContext = JAXBContext.newInstance(getReturnType());
        final JAXBSource jaxbSource = new JAXBSource(jaxbContext, mResult);
        setContentType(pResponse, "text/xml");
        Sources.writeToStream(jaxbSource, pResponse.getOutputStream());
      } catch (final JAXBException e) {
        throw new MessagingException(e);
      }
    } else {
      serializeValue(pResponse, this.mResult);
    }
  }

  private void setContentType(final HttpServletResponse pResponse, final String pDefault) {
    if (! mContentTypeSet) {
      final RestMethod methodAnnotation = getRestMethod();
      if ((methodAnnotation == null) || (methodAnnotation.contentType().length() == 0)) {
        pResponse.setContentType(pDefault);
      } else {
        pResponse.setContentType(methodAnnotation.contentType());
      }
      mContentTypeSet =true;
    }
  }

  private Source collectionToSource(final Type pGenericCollectionType, final Collection<?> pCollection, final QName pOutertagName) {
    final Class<?> rawType;
    if (pGenericCollectionType instanceof ParameterizedType) {
      final ParameterizedType returnType = (ParameterizedType) pGenericCollectionType;
      rawType = (Class<?>) returnType.getRawType();
    } else if (pGenericCollectionType instanceof Class<?>) {
      rawType = (Class<?>) pGenericCollectionType;
    } else if (pGenericCollectionType instanceof WildcardType) {
      final Type[] UpperBounds = ((WildcardType) pGenericCollectionType).getUpperBounds();
      if (UpperBounds.length > 0) {
        rawType = (Class<?>) UpperBounds[0];
      } else {
        rawType = Object.class;
      }
    } else if (pGenericCollectionType instanceof TypeVariable) {
      final Type[] UpperBounds = ((TypeVariable<?>) pGenericCollectionType).getBounds();
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
      final Type[] paramTypes = Types.getTypeParametersFor(Collection.class, pGenericCollectionType);
      elementType = Types.toRawType(paramTypes[0]);
      if (elementType.isInterface()) {
        // interfaces not supported by jaxb
        elementType = Types.commonAncestor(pCollection);
      }
    } else {
      elementType = Types.commonAncestor(pCollection);
    }
    try {
      JAXBContext context;
      if (elementType == null) {
        context = newJAXBContext(JAXBCollectionWrapper.class);
      } else {
        context = newJAXBContext(JAXBCollectionWrapper.class, elementType);
      }
      return new JAXBSource(context, new JAXBCollectionWrapper(pCollection, elementType).getJAXBElement(pOutertagName));
    } catch (final JAXBException e) {
      throw new MessagingException(e);
    }
  }

  private JAXBContext newJAXBContext(final Class<?>... pClasses) throws JAXBException {
    Class<?>[] classList;
    final Class<?> clazz = getDeclaringClass();
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
