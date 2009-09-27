package nl.adaptivity.jbi.soap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.devrieze.util.Tripple;
import net.devrieze.util.Types;



public class SoapHelper {
  public static final String SOAP_ENVELOPE_NS = "http://www.w3.org/2003/05/soap-envelope";

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
    Element wrapper = pResultDoc.createElementNS(pMessage.getNamespaceURI(), pParam.getElem1());
    wrapper.setPrefix(pMessage.getPrefix());
    pMessage.appendChild(wrapper);

    final Class<?> paramType = pParam.getElem2();
    if (Types.isPrimitive(paramType)|| Types.isPrimitiveWrapper(paramType)) {
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

}
