package nl.adaptivity.jbi.soap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;

import org.w3.soapEnvelope.Envelope;
import org.w3.soapEnvelope.Header;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.devrieze.util.Annotations;
import net.devrieze.util.JAXBCollectionWrapper;
import net.devrieze.util.StringDataSource;
import net.devrieze.util.Types;


public class SoapMethodWrapper {

  public static final URI SOAP_ENCODING = URI.create("http://www.w3.org/2003/05/soap-encoding");
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

  private Object getParam(Class<?> pClass, Node pAttrWrapper) throws MessagingException {
    Node value = pAttrWrapper ==null ? null : pAttrWrapper.getFirstChild();
    Object result;
    if (value != null && (! pClass.isInstance(value))) {
      if (Types.isPrimitive(pClass)||(Types.isPrimitiveWrapper(pClass))) {
        result = Types.parsePrimitive(pClass, value.getTextContent());
      } else {
        if (value.getNextSibling()!=null) {
          throw new UnsupportedOperationException("Collection parameters not yet supported");
        }
        try {
          JAXBContext context;

          if (pClass.isInterface()) {
            context = newJAXBContext(Collections.<Class<?>>emptyList());
          } else {
            context = newJAXBContext(Collections.<Class<?>>singletonList(pClass));
          }
          Unmarshaller um = context.createUnmarshaller();
          if (pClass.isInterface()) {
            result = um.unmarshal(value);
            if (result instanceof JAXBElement) {
              result = ((JAXBElement<?>)result).getValue();
            }
          } else {
            final JAXBElement<?> umresult;
            umresult = um.unmarshal(value, pClass);
            result = umresult.getValue();
          }

        } catch (JAXBException e) {
          throw new MessagingException(e);
        }
      }
    } else {
      result = value;
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
    Envelope envelope = new Envelope();
    org.w3.soapEnvelope.Body body = new org.w3.soapEnvelope.Body();
    envelope.setBody(body);
    List<Class<?>> contextList = new ArrayList<Class<?>>();
    contextList.add(Envelope.class);

    boolean contentSet = false;
    XmlRootElement xmlRootElement = aResult==null ? null : aResult.getClass().getAnnotation(XmlRootElement.class);
    if (xmlRootElement!=null) {
      body.getAny().add(aResult);
      contextList.add(aResult.getClass());
      try {
        JAXBContext jaxbContext = JAXBContext.newInstance(aMethod.getReturnType());
        pReply.setContent(new JAXBSource(jaxbContext, aResult));
      } catch (JAXBException e) {
        throw new MessagingException(e);
      }
    } else if (aResult instanceof Source) {
      pReply.setContent((Source) aResult);
      contentSet = true;
    } else if (aResult instanceof Node){
      body.getAny().add(aResult);
    } else if (aResult instanceof Collection) {
      XmlElementWrapper annotation = aMethod.getAnnotation(XmlElementWrapper.class);
      if (annotation!=null) {
        JAXBCollectionWrapper collectionWrapper = wrapCollection(aMethod.getGenericReturnType(), (Collection<?>) aResult);
        JAXBElement<JAXBCollectionWrapper> jaxbElement = collectionWrapper.getJAXBElement(getQName(annotation));
        contextList.add(JAXBCollectionWrapper.class);
        final Class<?> elementType = collectionWrapper.getElementType();
        if (elementType!=null && elementType!=Object.class) {
          contextList.add(elementType);
        }
        body.getAny().add(jaxbElement);
      }
    } else if (aResult instanceof CharSequence) {
      DocumentBuilder db;
      try {
        db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
        throw new MessagingException(e);
      }
      Document doc = db.newDocument();

      body.getAny().add(doc.createTextNode(aResult.toString()));
      pReply.addAttachment("text", new DataHandler(new StringDataSource("text", "text/plain", aResult.toString())));
    }
    if (! contentSet) {
      try {
        JAXBContext context = newJAXBContext(contextList);
        Marshaller m = context.createMarshaller();
        JAXBSource source = new JAXBSource(m, envelope);
        pReply.setContent(source);
      } catch (JAXBException e) {
        throw new MessagingException(e);
      }
    }

  }

  private JAXBCollectionWrapper wrapCollection(Type pType, Collection<?> pCollection) {
    final JAXBCollectionWrapper collectionWrapper;
    {
      final Class<?> rawType;
      if (pType instanceof ParameterizedType) {
        ParameterizedType returnType = (ParameterizedType) pType;
        rawType = (Class<?>) returnType.getRawType();
      } else if (pType instanceof Class<?>) {
        rawType = (Class<?>) pType;
      } else if (pType instanceof WildcardType) {
        final Type[] UpperBounds = ((WildcardType) pType).getUpperBounds();
        if (UpperBounds.length>0) {
          rawType = (Class<?>) UpperBounds[0];
        } else {
          rawType = Object.class;
        }
      } else if (pType instanceof TypeVariable) {
        final Type[] UpperBounds = ((TypeVariable<?>) pType).getBounds();
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
        Type[] paramTypes = Types.getTypeParametersFor(Collection.class, pType);
        elementType = Types.toRawType(paramTypes[0]);
        if (elementType.isInterface()) {
          // interfaces not supported by jaxb
          elementType = Types.commonAncestor(pCollection);
        }
      } else {
        elementType = Types.commonAncestor(pCollection);
      }
      collectionWrapper = new JAXBCollectionWrapper(pCollection, elementType);
    }
    return collectionWrapper;
  }

  private JAXBContext newJAXBContext(List<Class<?>> pClasses) throws JAXBException {
    Class<?>[] classList;
    Class<?> clazz = aMethod.getDeclaringClass();
    XmlSeeAlso seeAlso = clazz.getAnnotation(XmlSeeAlso.class);
    if (seeAlso!=null && seeAlso.value().length>0) {
      final Class<?>[] seeAlsoClasses = seeAlso.value();
      final int seeAlsoLength = seeAlsoClasses.length;
      classList = new Class<?>[seeAlsoLength+pClasses.size()];
      System.arraycopy(seeAlsoClasses, 0, classList, 0, seeAlsoLength);
      for(int i=0; i<pClasses.size();++i) {
        classList[seeAlsoLength+i] = pClasses.get(i);
      }
    } else {
      classList = pClasses.toArray(new Class[pClasses.size()]);
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
