package nl.adaptivity.process.engine.processModel;

import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.process.engine.PETransformer.AbstractDataContext;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.util.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ProcessNodeInstanceContext extends AbstractDataContext {

  private final ProcessNodeInstance mProcessNodeInstance;
  private final Document mDocument;

  public ProcessNodeInstanceContext(ProcessNodeInstance pProcessNodeInstance) {
    mProcessNodeInstance = pProcessNodeInstance;
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      mDocument = dbf.newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ProcessData getData(String pValueName) {
    switch (pValueName) {
    case "handle": return new ProcessData(pValueName, mDocument.createTextNode(Long.toString(mProcessNodeInstance.getHandle())));
    case "instancehandle": return new ProcessData(pValueName, mDocument.createTextNode(Long.toString(mProcessNodeInstance.getProcessInstance().getHandle())));
    case "endpoint": return new ProcessData(pValueName, createEndpoint());
    case "owner": return new ProcessData(pValueName, mDocument.createTextNode(mProcessNodeInstance.getProcessInstance().getOwner().getName()));
    }
    // none of the default names. Now try to resolve defines
    // TODO actually get the defines and look them up.
//    defines = mProcessNodeInstance.getDefines();
    return null;
  }

  private Node createEndpoint() {
    Element endPoint = mDocument.createElementNS(Constants.MY_JBI_NS, "endpointDescriptor");
    endPoint.setPrefix("");
    EndpointDescriptor localEndpoint = mProcessNodeInstance.getProcessInstance().getEngine().getLocalEndpoint();

    endPoint.setAttributeNS(XMLConstants.NULL_NS_URI, "endpointLocation", localEndpoint.getEndpointLocation().toString());
    endPoint.setAttributeNS(XMLConstants.NULL_NS_URI, "endpointName", localEndpoint.getEndpointName());
    endPoint.setAttributeNS(XMLConstants.NULL_NS_URI, "serviceLocalName", localEndpoint.getServiceName().getLocalPart());
    endPoint.setAttributeNS(XMLConstants.NULL_NS_URI, "serviceNS", localEndpoint.getServiceName().getNamespaceURI());
    return endPoint;
  }

  @Override
  public List<XMLEvent> resolveDefaultValue(XMLEventFactory pXef) throws XMLStreamException {
    throw new UnsupportedOperationException("There is no default in this context");
  }

}