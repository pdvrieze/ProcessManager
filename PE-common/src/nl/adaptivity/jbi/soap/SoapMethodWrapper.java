package nl.adaptivity.jbi.soap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3.soapEnvelope.Envelope;
import org.w3.soapEnvelope.Header;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.devrieze.util.Annotations;
import net.devrieze.util.JAXBCollectionWrapper;
import net.devrieze.util.StringDataSource;
import net.devrieze.util.Types;

import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.util.HttpMessage.Body;


public class SoapMethodWrapper {

  private static final String SOAP_ENCODING = "http://www.w3.org/2003/05/soap-encoding";
  private final Object aOwner;
  private final Method aMethod;
  private Object[] aParams;
  private Object aResult;

  public SoapMethodWrapper(Object pOwner, Method pMethod) {
    aOwner = pOwner;
    aMethod = pMethod;
  }

  public void unmarshalParams(Source pSource, Map<String, DataHandler> pAttachments) throws MessagingException {
    if (aParams!=null) {
      throw new IllegalStateException("Parameters have already been unmarshalled");
    }

    Envelope envelope = JAXB.unmarshal(pSource, Envelope.class);
    ensureNoUnunderstoodHeaders(envelope);
    processSoapHeader(envelope.getHeader());
    URI es = envelope.getEncodingStyle();
    if (es==null || es.equals(SOAP_ENCODING)) {
      processSoapBody(envelope.getBody(), pAttachments);
    } else {
      throw new MessagingException("Ununderstood message body");
    }

  }

  private void ensureNoUnunderstoodHeaders(Envelope pEnvelope) throws MessagingException{
    // TODO Auto-generated method stub
    //
  }

  private void processSoapHeader(Header pHeader) {
    // TODO Auto-generated method stub
    //
    /* For now just ignore headers, i.e. none understood*/
  }

  private void processSoapBody(org.w3.soapEnvelope.Body pBody, Map<String, DataHandler> pAttachments) throws MessagingException {
    if (pBody.getAny().size()!=1) {
      throw new MessagingException("Multiple body elements not expected");
    }
    Node root = (Node) pBody.getAny().get(0);
    assertRootNode(root);

    LinkedHashMap<String, Node> params = new LinkedHashMap<String, Node>();

    Node child = root.getFirstChild();
    while (child != null) {
      if (child.getNodeType()==Node.ELEMENT_NODE) {
        params.put(child.getLocalName(), child);
      }
      child = child.getNextSibling();
    }

    Class<?>[] parameterTypes = aMethod.getParameterTypes();
    Annotation[][] parameterAnnotations = aMethod.getParameterAnnotations();

    aParams = new Object[parameterTypes.length];

    for(int i =0; i<parameterTypes.length; ++i) {
      WebParam annotation = Annotations.getAnnotation(parameterAnnotations[i], WebParam.class);
      String name;
      if (annotation==null) {
        name = params.keySet().iterator().next();
      } else {
        name = annotation.name();
      }
      Node value = params.remove(name);
      if (value==null) {
        throw new MessagingException("Parameter \""+name+"\" not found");
      }
      aParams[i] = getParam(parameterTypes[i], value);

    }
    if (params.size()>0) {
      Logger.getLogger(getClass().getCanonicalName()).warning("Extra parameters in message");
    }
  }

  private void assertRootNode(Node pRoot) throws MessagingException {
    WebMethod wm = aMethod.getAnnotation(WebMethod.class);
    if (wm==null || wm.operationName().equals("")) {
      if (!pRoot.getLocalName().equals(aMethod.getName())) {
        throw new MessagingException("Root node does not correspond to operation name");
      }
    } else {
      if (!pRoot.getLocalName().equals(wm.operationName())) {
        throw new MessagingException("Root node does not correspond to operation name");
      }
    }
    WebService ws = aMethod.getDeclaringClass().getAnnotation(WebService.class);
    if (! (ws==null || ws.targetNamespace().equals(""))) {
      if (! ws.targetNamespace().equals(pRoot.getNamespaceURI())) {
        throw new MessagingException("Root node does not correspond to operation namespace");
      }
    }
  }

  private Object getParam(Class<?> pClass, Node pValue) throws MessagingException {
    final Object result;
    if (pValue != null && (! pClass.isInstance(pValue))) {
      if (Types.isPrimitive(pClass)||(Types.isPrimitiveWrapper(pClass))) {
        result = Types.parsePrimitive(pClass, pValue.getTextContent());
      } else {
        try {
          JAXBContext context = JAXBContext.newInstance(pClass);
          Unmarshaller um = context.createUnmarshaller();
          final JAXBElement<?> umresult = um.unmarshal(pValue, pClass);
          result = umresult.getValue();
        } catch (JAXBException e) {
          return null;
        }
      }
    } else {
      result = pValue;
    }

    return result;
  }

  private Object getAttachment(Class<?> pClass, String pName, Map<String, DataHandler> pAttachments) throws MessagingException {
    DataHandler handler = pAttachments.get(pName);
    if (handler != null) {
      if (DataHandler.class.isAssignableFrom(pClass)) {
        return handler;
      }
      if (InputStream.class.isAssignableFrom(pClass)) {
        try {
          return handler.getInputStream();
        } catch (IOException e) {
          throw new MessagingException(e);
        }
      }
      if (DataSource.class.isAssignableFrom(pClass)) {
        return handler.getDataSource();
      }
      try {
        return handler.getContent();
      } catch (IOException e) {
        throw new MessagingException(e);
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

  public void exec() throws MessagingException {
    if (aParams==null) {
      throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
    }
    try {
      aResult = aMethod.invoke(aOwner, aParams);
    } catch (IllegalArgumentException e) {
      throw new MessagingException(e);
    } catch (IllegalAccessException e) {
      throw new MessagingException(e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      throw new MessagingException(cause!=null ? cause : e);
    }
  }

  public void marshalResult(NormalizedMessage pReply) throws MessagingException {
    XmlRootElement xmlRootElement = aResult==null ? null : aResult.getClass().getAnnotation(XmlRootElement.class);
    if (xmlRootElement!=null) {
      try {
        JAXBContext jaxbContext = JAXBContext.newInstance(aMethod.getReturnType());
        pReply.setContent(new JAXBSource(jaxbContext, aResult));
      } catch (JAXBException e) {
        throw new MessagingException(e);
      }
    } else if (aResult instanceof Source) {
      pReply.setContent((Source) aResult);
    } else if (aResult instanceof Node){
      pReply.setContent(new DOMSource((Node) aResult));
    } else if (aResult instanceof Collection) {
      XmlElementWrapper annotation = aMethod.getAnnotation(XmlElementWrapper.class);
      if (annotation!=null) {
        pReply.setContent(collectionToSource(aMethod.getGenericReturnType(),(Collection<?>) aResult, getQName(annotation)));
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
      pReply.addAttachment("text", new DataHandler(new StringDataSource("text", "text/plain", aResult.toString())));
    } else {
      if (aResult !=null) {
        try {
          JAXBContext jaxbContext = JAXBContext.newInstance(aMethod.getReturnType());
          pReply.setContent(new JAXBSource(jaxbContext, aResult));
        } catch (JAXBException e) {
          throw new MessagingException(e);
        }
      }
    }
  }

  private Source collectionToSource(Type pReturnType, Collection<?> pResult, QName pName) throws MessagingException {
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
      JAXBContext context = newJAXBContext(JAXBCollectionWrapper.class, elementType);
      return new JAXBSource(context, new JAXBCollectionWrapper(pResult).getJAXBElement(pName));
    } catch (JAXBException e) {
      throw new MessagingException(e);
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
