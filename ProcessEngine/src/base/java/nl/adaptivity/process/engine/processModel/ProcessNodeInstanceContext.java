package nl.adaptivity.process.engine.processModel;

import java.util.Collections;
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
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.util.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ProcessNodeInstanceContext extends AbstractDataContext {

  private final ProcessNodeInstance mProcessNodeInstance;
  private final Document mDocument;
  private List<ProcessData> mDefines;
  private boolean mProvideResults;

  public ProcessNodeInstanceContext(ProcessNodeInstance pProcessNodeInstance, List<ProcessData> pDefines, boolean pProvideResults) {
    mProcessNodeInstance = pProcessNodeInstance;
    mDefines = pDefines;
    mProvideResults = pProvideResults;
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
    for(ProcessData define: mDefines) {
      if (pValueName.equals(define.getName())) {
        return define;
      }
    }
    if (mProvideResults) {
      for(ProcessData result: mProcessNodeInstance.getResults()) {
        if (pValueName.equals(result.getName())) {
          return result;
        }
      }
    }
    // allow for missing values in the database. If they were "defined" treat is as an empty value.
    for(XmlDefineType resultDef: mProcessNodeInstance.getNode().getDefines()) {
      if (pValueName.equals(resultDef.getName())) {
        return new ProcessData(pValueName, Collections.<Node>emptyList());
      }
    }
    if (mProvideResults) {
      // allow for missing values in the database. If they were "defined" treat is as an empty value.
      for(IXmlResultType resultDef: mProcessNodeInstance.getNode().getResults()) {
        if (pValueName.equals(resultDef.getName())) {
          return new ProcessData(pValueName, Collections.<Node>emptyList());
        }
      }
    }
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