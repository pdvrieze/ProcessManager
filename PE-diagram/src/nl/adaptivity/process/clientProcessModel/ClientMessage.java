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

package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.process.processModel.BaseMessage;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.AbstractXmlWriter;
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
  protected void serializeEndElement(final XmlWriter out) throws XmlException {
    AbstractXmlWriter.endTag(out, getElementName());
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  public void serialize(XmlWriter out) throws XmlException {
    out.startTag(NS_PM, "message", null);
    if (getServiceNS()!=null) out.attribute(null, "serviceNS", null, getServiceNS());
    if (getServiceName()!=null) out.attribute(null, "serviceName", null, getServiceName());
    if (getEndpoint()!=null) out.attribute(null, "endpoint", null, getEndpoint());
    if (getOperation()!=null) out.attribute(null, "operation", null, getOperation());
    if (getUrl()!=null) out.attribute(null, "url", null, getUrl()); if (getContentType()!=null) out.attribute(null, "type", null, getContentType());
    if (getMethod()!=null) out.attribute(null, "method", null, getMethod());
    // TODO don't do this through DOM
    serialize(out, getMessageBodyNode());

    out.endTag(NS_PM, "message", null);
  }

  private void serialize(XmlWriter out, Node node) throws XmlException {
    switch (node.getNodeType()) {
    case Node.ELEMENT_NODE:
      serializeElement(out, (Element)node);
      break;
    case Node.CDATA_SECTION_NODE:
      out.cdsect(((CDATASection)node).getData());
      break;
    case Node.COMMENT_NODE:
      out.comment(((Comment)node).getData());
      break;
    case Node.ENTITY_REFERENCE_NODE:
      out.entityRef(((EntityReference)node).getLocalName());
      break;
    case Node.TEXT_NODE:
      out.text(((Text)node).getData());
    }

  }

  private void serializeElement(XmlWriter out, Element node) throws XmlException {
    out.namespaceAttr(node.getPrefix(), node.getNamespaceURI());
    out.startTag(node.getNamespaceURI(), node.getLocalName(), null);
    NamedNodeMap attrs = node.getAttributes();
    for(int i=0; i<attrs.getLength(); ++i) {
      Attr attr = (Attr) attrs.item(i);
      out.attribute(attr.getNamespaceURI(), attr.getLocalName(), null, attr.getValue());
    }
    for(Node child = node.getFirstChild(); child!=null; child = child.getNextSibling()) {
      serialize(out, node);
    }
    out.endTag(node.getNamespaceURI(), node.getLocalName(), null);
  }

}
