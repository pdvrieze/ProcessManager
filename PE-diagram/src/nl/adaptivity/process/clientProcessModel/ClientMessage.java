package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.process.processModel.BaseMessage;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.util.xml.XmlUtil;
import org.w3c.dom.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;

public class ClientMessage extends BaseMessage {


  public static final QName ELEMENTNAME = new QName(NS_PM, "message", "pm");

  public ClientMessage() {
    super();
  }

  public ClientMessage(QName pService, String pEndpoint, String pOperation, String pUrl, String pMethod, String pContentType,
                       Node pMessageBody) throws XMLStreamException {
    super(pService, pEndpoint, pOperation, pUrl, pMethod, pContentType, pMessageBody);
  }

  public ClientMessage(IXmlMessage pMessage) throws XMLStreamException {
    super(pMessage);
  }

  public static ClientMessage from(IXmlMessage pMessage) {
    if (pMessage==null) { return null; }
    if (pMessage instanceof ClientMessage) { return (ClientMessage) pMessage; }
    try {
      return new ClientMessage(pMessage);
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public EndpointDescriptor getEndpointDescriptor() {
    throw new UnsupportedOperationException("Not supported for clientmessages yet");
  }

  @Override
  protected void serializeStartElement(final XMLStreamWriter pOut) throws XMLStreamException {
    XmlUtil.writeStartElement(pOut, getElementName());
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  public void serialize(SerializerAdapter pOut) {
    pOut.startTag(NS_PM, "message", false);
    if (getServiceNS()!=null) { pOut.addAttribute(null, "serviceNS", getServiceNS()); }
    if (getServiceName()!=null) { pOut.addAttribute(null, "serviceName", getServiceName()); }
    if (getEndpoint()!=null) { pOut.addAttribute(null, "endpoint", getEndpoint()); }
    if (getOperation()!=null) { pOut.addAttribute(null, "operation", getOperation()); }
    if (getUrl()!=null) { pOut.addAttribute(null, "url", getUrl()); }
    if (getContentType()!=null) { pOut.addAttribute(null, "type", getContentType()); }
    if (getMethod()!=null) { pOut.addAttribute(null, "method", getMethod()); }

    // TODO don't do this through DOM
    serialize(pOut, getMessageBodyNode());

    pOut.endTag(NS_PM, "message", true);
  }

  private void serialize(SerializerAdapter pOut, Node pNode) {
    switch (pNode.getNodeType()) {
    case Node.ELEMENT_NODE:
      serializeElement(pOut, (Element)pNode);
      break;
    case Node.CDATA_SECTION_NODE:
      pOut.cdata(((CDATASection)pNode).getData());
      break;
    case Node.COMMENT_NODE:
      pOut.comment(((Comment)pNode).getData());
      break;
    case Node.ENTITY_REFERENCE_NODE:
      pOut.entityReference(((EntityReference)pNode).getLocalName());
      break;
    case Node.TEXT_NODE:
      pOut.text(((Text)pNode).getData());
    }

  }

  private void serializeElement(SerializerAdapter pOut, Element pNode) {
    pOut.addNamespace(pNode.getPrefix(), pNode.getNamespaceURI());
    pOut.startTag(pNode.getNamespaceURI(), pNode.getLocalName(), false);
    NamedNodeMap attrs = pNode.getAttributes();
    for(int i=0; i<attrs.getLength(); ++i) {
      Attr attr = (Attr) attrs.item(i);
      pOut.addAttribute(attr.getNamespaceURI(), attr.getLocalName(), attr.getValue());
    }
    for(Node node = pNode.getFirstChild(); node!=null; node = node.getNextSibling()) {
      serialize(pOut, node);
    }
    pOut.endTag(pNode.getNamespaceURI(), pNode.getLocalName(), false);
  }

}
