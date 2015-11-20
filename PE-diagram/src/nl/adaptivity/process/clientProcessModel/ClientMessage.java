package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.process.processModel.BaseMessage;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import org.w3c.dom.*;

import javax.xml.namespace.QName;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;

public class ClientMessage extends BaseMessage {


  public static final QName ELEMENTNAME = new QName(NS_PM, "message", "pm");

  public ClientMessage() {
    super();
  }

  public ClientMessage(QName service, String endpoint, String operation, String url, String method, String contentType,
                       Node messageBody) throws XmlException {
    super(service, endpoint, operation, url, method, contentType, messageBody);
  }

  public ClientMessage(IXmlMessage message) throws XmlException {
    super(message);
  }

  public static ClientMessage from(IXmlMessage message) {
    if (message==null) { return null; }
    if (message instanceof ClientMessage) { return (ClientMessage) message; }
    try {
      return new ClientMessage(message);
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public EndpointDescriptor getEndpointDescriptor() {
    throw new UnsupportedOperationException("Not supported for clientmessages yet");
  }

  @Override
  protected void serializeStartElement(final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, getElementName());
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  public void serialize(SerializerAdapter out) {
    out.startTag(NS_PM, "message", false);
    if (getServiceNS()!=null) { out.addAttribute(null, "serviceNS", getServiceNS()); }
    if (getServiceName()!=null) { out.addAttribute(null, "serviceName", getServiceName()); }
    if (getEndpoint()!=null) { out.addAttribute(null, "endpoint", getEndpoint()); }
    if (getOperation()!=null) { out.addAttribute(null, "operation", getOperation()); }
    if (getUrl()!=null) { out.addAttribute(null, "url", getUrl()); }
    if (getContentType()!=null) { out.addAttribute(null, "type", getContentType()); }
    if (getMethod()!=null) { out.addAttribute(null, "method", getMethod()); }

    // TODO don't do this through DOM
    serialize(out, getMessageBodyNode());

    out.endTag(NS_PM, "message", true);
  }

  private void serialize(SerializerAdapter out, Node node) {
    switch (node.getNodeType()) {
    case Node.ELEMENT_NODE:
      serializeElement(out, (Element)node);
      break;
    case Node.CDATA_SECTION_NODE:
      out.cdata(((CDATASection)node).getData());
      break;
    case Node.COMMENT_NODE:
      out.comment(((Comment)node).getData());
      break;
    case Node.ENTITY_REFERENCE_NODE:
      out.entityReference(((EntityReference)node).getLocalName());
      break;
    case Node.TEXT_NODE:
      out.text(((Text)node).getData());
    }

  }

  private void serializeElement(SerializerAdapter out, Element node) {
    out.addNamespace(node.getPrefix(), node.getNamespaceURI());
    out.startTag(node.getNamespaceURI(), node.getLocalName(), false);
    NamedNodeMap attrs = node.getAttributes();
    for(int i=0; i<attrs.getLength(); ++i) {
      Attr attr = (Attr) attrs.item(i);
      out.addAttribute(attr.getNamespaceURI(), attr.getLocalName(), attr.getValue());
    }
    for(Node child = node.getFirstChild(); child!=null; child = child.getNextSibling()) {
      serialize(out, node);
    }
    out.endTag(node.getNamespaceURI(), node.getLocalName(), false);
  }

}
