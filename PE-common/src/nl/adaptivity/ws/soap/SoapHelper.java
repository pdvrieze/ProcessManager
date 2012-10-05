package nl.adaptivity.ws.soap;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3.soapEnvelope.Envelope;
import org.w3.soapEnvelope.Header;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import net.devrieze.util.Tripple;
import net.devrieze.util.Types;
import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.util.XmlUtil;


/**
 * Static helper method that helps with handling soap requests and responses.
 * @author Paul de Vrieze
 *
 */
public class SoapHelper {
  public static final String SOAP_ENVELOPE_NS = "http://www.w3.org/2003/05/soap-envelope";
  public static final QName SOAP_RPC_RESULT = new QName("http://www.w3.org/2003/05/soap-rpc", "result");
  public static final String RESULT = "!@#$Result_MARKER::";

  private SoapHelper() {}

  public static <T> Source createMessage(QName pOperationName, Tripple<String, Class<?>, Object>... pParams) throws JAXBException {
    return createMessage(pOperationName, null, pParams);
  }

  /**
   * Create a Source encapsulating a soap message for the given operation name and parameters.
   * @param pOperationName The name of the soap operation (name of the first child of the soap body)
   * @param pHeaders A list of optional headers to add to the message.
   * @param pParams The parameters of the message
   * @return a Source that encapsulates the message.
   * @throws JAXBException
   */
  public static Source createMessage(QName pOperationName, List<?> pHeaders, Tripple<String, Class<?>, Object>... pParams) throws JAXBException {
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

    Element envelope = createSoapEnvelope(resultDoc);
    if (pHeaders!=null && pHeaders.size()>0) {
      createSoapHeader(envelope, pHeaders);
    }
    Element body = createSoapBody(envelope);
    Element message = createBodyMessage(body, pOperationName);
    for(Tripple<String, Class<?>, Object> param:pParams) {
      addParam(message, param);
    }
    return new DOMSource(resultDoc);
  }

  /**
   * Create a SOAP envelope in the document and return the body element.
   * @param pDoc The document that needs to contain the envelope.
   * @return The body element.
   */
  private static Element createSoapEnvelope(Document pDoc) {
    Element envelope = pDoc.createElementNS(SOAP_ENVELOPE_NS, "soap:Envelope");
    envelope.setAttribute("encodingStyle", SoapMethodWrapper.SOAP_ENCODING.toString());
    pDoc.appendChild(envelope);
    return envelope;
  }

  private static Element createSoapHeader(Element pEnvelope, List<?> pHeaders) {
    Document ownerDoc = pEnvelope.getOwnerDocument();
    Element header = ownerDoc.createElementNS(SOAP_ENVELOPE_NS, "soap:Header");
    pEnvelope.appendChild(header);
    for(Object headerElem:pHeaders) {
      if (headerElem instanceof Node) {
        Node node = ownerDoc.importNode(((Node) headerElem),true);
        header.appendChild(node);
      } else {
        try {
          Marshaller marshaller;
          {
            final JAXBContext context;
            if (headerElem instanceof JAXBElement) {
              context = JAXBContext.newInstance();
            } else {
              context = JAXBContext.newInstance(headerElem.getClass());
            }
            marshaller = context.createMarshaller();
          }
          marshaller.marshal(headerElem, header);

        } catch (JAXBException e) {
          throw new MyMessagingException(e);
        }
      }
    }

    return header;
  }

  private static Element createSoapBody(Element pEnvelope) {
    Element body = pEnvelope.getOwnerDocument().createElementNS(SOAP_ENVELOPE_NS, "soap:Body");
    pEnvelope.appendChild(body);
    return body;
  }

  /**
   * Create the actual body of the SOAP message.
   * @param pBody The body element in which the body needs to be embedded.
   * @param pOperationName The name of the wrapping name (the operation name).
   * @return
   */
  private static Element createBodyMessage(Element pBody, QName pOperationName) {
    Document pResultDoc = pBody.getOwnerDocument();

    Element message = pResultDoc.createElementNS(pOperationName.getNamespaceURI(), XmlUtil.getQualifiedName(pOperationName));

    pBody.appendChild(message);
    return message;
  }

  private static Element addParam(Element pMessage, Tripple<String, Class<?>, Object> pParam) throws JAXBException {
    Document ownerDoc = pMessage.getOwnerDocument();
    String prefix;
    if (pMessage.getPrefix()!=null && pMessage.getPrefix().length()>0) {
      prefix= pMessage.getPrefix()+':';
    } else {
      prefix="";
    }
    if (pParam.getElem1()==RESULT) { // We need to create the wrapper that refers to the actual result.
      Element wrapper = ownerDoc.createElementNS(SOAP_RPC_RESULT.getNamespaceURI(), "rpc:"+SOAP_RPC_RESULT.getLocalPart());
      pMessage.appendChild(wrapper);

      wrapper.appendChild(ownerDoc.createTextNode(prefix+((CharSequence)pParam.getElem3()).toString()));
      return wrapper;
    }

    Element wrapper = ownerDoc.createElementNS(pMessage.getNamespaceURI(), prefix+pParam.getElem1());
    wrapper.setPrefix(pMessage.getPrefix());
    pMessage.appendChild(wrapper);

    final Class<?> paramType = pParam.getElem2();
    if (pParam.getElem3()==null) {
      // don't add anything
    } else if (Types.isPrimitive(paramType)|| Types.isPrimitiveWrapper(paramType)) {
      wrapper.appendChild(ownerDoc.createTextNode(pParam.getElem3().toString()));
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
      param = ownerDoc.importNode(param, true);
      wrapper.appendChild(param);
    } else if (Principal.class.isAssignableFrom(paramType)) {
      wrapper.appendChild(ownerDoc.createTextNode(((Principal)pParam.getElem3()).getName()));
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

  public static <T> T processResponse(Class<T> pClass, Source pContent) {
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

  static LinkedHashMap<String, Node> getParamMap(Node bodyParamRoot) {
    LinkedHashMap<String, Node> params;
    {
      params = new LinkedHashMap<String, Node>();

      Node child = bodyParamRoot.getFirstChild();
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

  public static Map<String, Node> getHeaderMap(Header pHeader) {
    if (pHeader==null) { return Collections.emptyMap(); }
    LinkedHashMap<String, Node> result = new LinkedHashMap<String, Node>();
    for(Object o:pHeader.getAny()) {
      if (o instanceof Node) {
        Node n=(Node) o;
        result.put(n.getLocalName(), n);
      }

    }
    return result;
  }

  static <T> T unMarshalNode(Method pMethod, Class<T> pClass, Node pAttrWrapper) {
    Node value = pAttrWrapper ==null ? null : pAttrWrapper.getFirstChild();
    Object result;
    if (value != null && (! pClass.isInstance(value))) {
      if (Types.isPrimitive(pClass)||(Types.isPrimitiveWrapper(pClass))) {
        result = Types.parsePrimitive(pClass, value.getTextContent());
      } else if (Enum.class.isAssignableFrom(pClass)) {
        String val = value.getTextContent();
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Object tmpResult = Enum.valueOf((Class) pClass, val);
        result = tmpResult;
      } else if (pClass.isAssignableFrom(Principal.class) && (value instanceof Text)) {
        result = new SimplePrincipal(((Text)value).getData());
      } else if (CharSequence.class.isAssignableFrom(pClass) && (value instanceof Text)) {
        if (pClass.isAssignableFrom(String.class)) {
          result = ((Text) value).getData();
        } else if (pClass.isAssignableFrom(StringBuilder.class)) {
          String val = ((Text)value).getData();
          result = new StringBuilder(val.length());
          ((StringBuilder) result).append(val);
        } else {
          throw new UnsupportedOperationException("Can not unmarshal other strings than to string or stringbuilder");
        }
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
            if (value instanceof Text) {
              result = ((Text) value).getData();
            } else {
              result = um.unmarshal(value);

              if (result instanceof JAXBElement) {
                result = ((JAXBElement<?>)result).getValue();
              }
            }
          } else {
            final JAXBElement<?> umresult;
            umresult = um.unmarshal(value, pClass);
            result = umresult.getValue();
          }

        } catch (JAXBException e) {
          throw new MyMessagingException(e);
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
