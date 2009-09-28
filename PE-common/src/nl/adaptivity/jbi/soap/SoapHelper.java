package nl.adaptivity.jbi.soap;

import java.lang.reflect.Method;
import java.util.*;

import javax.jbi.messaging.MessagingException;
import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3.soapEnvelope.Envelope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.devrieze.util.Tripple;
import net.devrieze.util.Types;



public class SoapHelper {
  public static final String SOAP_ENVELOPE_NS = "http://www.w3.org/2003/05/soap-envelope";
  public static final String RESULT = "!@#$Result_MARKER::";

  private SoapHelper() {}

  public static <T> Source createMessage(QName pOperationName, Tripple<String, Class<?>, Object>... pParams) throws JAXBException {
    DocumentBuilder db;
    {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      try {
        db = dbf.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
    Document resultDoc = db.newDocument();

    Element body = createSoapEnvelope(resultDoc);
    Element message = createBodyMessage(resultDoc, body, pOperationName);
    for(Tripple<String, Class<?>, Object> param:pParams) {
      addParam(resultDoc, message, param);
    }
    return new DOMSource(resultDoc);
  }

  private static Element createSoapEnvelope(Document pDoc) {
    Element envelope = pDoc.createElementNS(SOAP_ENVELOPE_NS, "Envelope");
    envelope.setPrefix("soap");
    envelope.setAttribute("encodingStyle", SoapMethodWrapper.SOAP_ENCODING.toString());
    pDoc.appendChild(envelope);
    Element body = pDoc.createElementNS(SOAP_ENVELOPE_NS, "Body");
    body.setPrefix("soap");
    envelope.appendChild(body);
    return body;
  }

  private static Element createBodyMessage(Document pResultDoc, Element pBody, QName pOperationName) {
    Element message = pResultDoc.createElementNS(pOperationName.getNamespaceURI(), pOperationName.getLocalPart());
    if (pOperationName.getPrefix()!=null) {
      message.setPrefix(pOperationName.getPrefix());
    } else {
      message.setPrefix("");
    }
    pBody.appendChild(message);
    return message;
  }

  private static Element addParam(Document pResultDoc, Element pMessage, Tripple<String, Class<?>, Object> pParam) throws JAXBException {
    if (pParam.getElem1()==RESULT) {
      Element wrapper = pResultDoc.createElementNS("http://www.w3.org/2003/05/soap-rpc", "result");
      wrapper.setPrefix("rpc");
      pMessage.appendChild(wrapper);
      if (pMessage.getPrefix()==null || pMessage.getPrefix().length()==0) {
        wrapper.appendChild(pResultDoc.createTextNode(((CharSequence)pParam.getElem3()).toString()));
      } else {
        wrapper.appendChild(pResultDoc.createTextNode(pMessage.getPrefix()+":"+((CharSequence)pParam.getElem3()).toString()));
      }
      return wrapper;
    }
    Element wrapper = pResultDoc.createElementNS(pMessage.getNamespaceURI(), pParam.getElem1());
    wrapper.setPrefix(pMessage.getPrefix());
    pMessage.appendChild(wrapper);

    final Class<?> paramType = pParam.getElem2();
    if (pParam.getElem3()==null) {
      // don't add anything
    } else if (Types.isPrimitive(paramType)|| Types.isPrimitiveWrapper(paramType)) {
      wrapper.appendChild(pResultDoc.createTextNode(pParam.getElem3().toString()));
    } else if (Collection.class.isAssignableFrom(paramType)){
      Collection<?> params = (Collection<?>) pParam.getElem3();
      Set<Class<?>> paramTypes = new HashSet<Class<?>>();
      {
        for(Object elem:params) {
          paramTypes.add(elem.getClass());
        }
      }
      Marshaller marshaller;
      {
        JAXBContext context = JAXBContext.newInstance(paramTypes.toArray(new Class<?>[paramTypes.size()]));
        marshaller = context.createMarshaller();
      }
      for(Object elem:params) {
        marshaller.marshal(elem, wrapper);
      }
    } else if (Node.class.isAssignableFrom(paramType)) {
      Node param = (Node) pParam.getElem3();
      param = param.cloneNode(true);
      param = pResultDoc.adoptNode(param);
      wrapper.appendChild(param);
    } else {
      Marshaller marshaller;
      {
        JAXBContext context = JAXBContext.newInstance(paramType);
        marshaller = context.createMarshaller();
      }
      marshaller.marshal(pParam.getElem3(), wrapper);
    }
    return wrapper;
  }

  public static <T> T processResponse(Class<T> pClass, Source pContent) throws MessagingException {
    Envelope env = JAXB.unmarshal(pContent, Envelope.class);
    List<Object> elements = env.getBody().getAny();
    if (elements.size()!=1) {
      return null;
    }
    Element wrapper = (Element) elements.get(0);
    if (wrapper.getFirstChild()==null) {
      return null; // Must be void method
    }
    LinkedHashMap<String, Node> results = getParamMap(wrapper);
    return pClass.cast(unMarshalNode(null, pClass, results.get(RESULT)));
  }

  static LinkedHashMap<String, Node> getParamMap(Node root) {
    LinkedHashMap<String, Node> params;
    {
      params = new LinkedHashMap<String, Node>();

      Node child = root.getFirstChild();
      String returnName = null;
      while (child != null) {
        if (child.getNodeType()==Node.ELEMENT_NODE) {
          if ("http://www.w3.org/2003/05/soap-rpc".equals(child.getNamespaceURI()) && "result".equals(child.getLocalName())) {
            returnName = child.getTextContent();
            int i =returnName.indexOf(':');
            if (i>=0) {
              returnName = returnName.substring(i+1);
            }
            if (params.containsKey(returnName)) {
              Node val = params.remove(returnName);
              params.put(RESULT, val);
            }

          } else if (returnName!=null && child.getLocalName().equals(returnName)) {
            params.put(RESULT, child);
          } else {

            params.put(child.getLocalName(), child);
          }
        }
        child = child.getNextSibling();
      }
    }
    return params;
  }

  static <T> T unMarshalNode(Method pMethod, Class<T> pClass, Node pAttrWrapper) throws MessagingException {
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
            context = newJAXBContext(pMethod, Collections.<Class<?>>emptyList());
          } else {
            context = newJAXBContext(pMethod, Collections.<Class<?>>singletonList(pClass));
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
    if (Types.isPrimitive(pClass)) {
      @SuppressWarnings("unchecked") T r2 = (T) result;
      return r2;
    }

    return result==null ? null : pClass.cast(result);
  }

  private static JAXBContext newJAXBContext(Method pMethod, List<Class<?>> pClasses) throws JAXBException {
    Class<?>[] classList;
    XmlSeeAlso seeAlso;
    if (pMethod!=null) {
      Class<?> clazz = pMethod.getDeclaringClass();
      seeAlso = clazz.getAnnotation(XmlSeeAlso.class);
    } else {
      seeAlso = null;
    }
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

}
