package nl.adaptivity.util.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.w3c.dom.*;


public class NodeEventReader extends AbstractBufferedEventReader {

  private NodeList mNodes;
  private int mNodesPos;
  private Node mCurrent;
  private XMLEventFactory mXef = XMLEventFactory.newInstance();

  public NodeEventReader(NodeList pNodeList) {
    mNodes = pNodeList;
  }

  public NodeEventReader(Node pNode) {
    mNodes = new SingletonNodeList(pNode);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public XMLEvent peek() throws XMLStreamException {
    if (!isPeekBufferEmpty()) { return peekFirst(); }
    while (mNodesPos<mNodes.getLength()) {
      Node node = mCurrent==null ? mNodes.item(mNodesPos) : mCurrent.getFirstChild();
      if (node==null) { node = mCurrent.getNextSibling(); }
      if (node==null) {
        if (mCurrent.getNodeType()==Node.ELEMENT_NODE || mCurrent.getNodeType()==Node.DOCUMENT_NODE || mCurrent.getNodeType()==Node.DOCUMENT_FRAGMENT_NODE) {
          // node without content
          add(createEndEvent(mCurrent));
        }
        Node parent = mCurrent.getParentNode();
        while (parent!=null && node==null) {
          add(createEndEvent(parent));
          node = parent.getNextSibling();
          parent = parent.getParentNode();
        }
      }

      if (node!=null) {
        createEvents(node);
        mCurrent = node;
        return peekFirst();
      } else {
        mCurrent = null;
      }
      ++mNodesPos;
    }
    return null; // end of stream
  }

  private XMLEvent createEndEvent(Node pNode) {
    switch (pNode.getNodeType()) {
      case Node.ELEMENT_NODE:
        return mXef.createEndElement(pNode.getPrefix(), pNode.getNamespaceURI(), pNode.getLocalName());
      case Node.DOCUMENT_NODE:
        return mXef.createEndDocument();
    }
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private void createEvents(Node pNode) {
    switch (pNode.getNodeType()) {
      case Node.ELEMENT_NODE:
        createElement((Element) pNode);
        return;
      case Node.ATTRIBUTE_NODE:
        createAttribute((Attr) pNode);
        return;
      case Node.CDATA_SECTION_NODE:
        add(mXef.createCData(((CDATASection) pNode).getData()));
        return;
      case Node.COMMENT_NODE:
        add(mXef.createComment(((Comment) pNode).getData()));
        return;
      case Node.DOCUMENT_NODE: {
        Document doc = (Document) pNode;
        add(mXef.createStartDocument(doc.getXmlEncoding(), doc.getXmlVersion(), doc.getXmlStandalone()));
        return;
      }
      case Node.PROCESSING_INSTRUCTION_NODE: {
        ProcessingInstruction pi = (ProcessingInstruction) pNode;
        add(mXef.createProcessingInstruction(pi.getTarget(), pi.getData()));
        return;
      }
      case Node.TEXT_NODE:
        add(mXef.createCharacters(((Text) pNode).getData()));
        return;
      case Node.DOCUMENT_FRAGMENT_NODE:
      default:
        throw new IllegalArgumentException("Nodes of type "+pNode.getNodeType()+" ("+pNode.getClass().getSimpleName()+") are not supported");
    }
  }

  private void createElement(Element pNode) {
    add(mXef.createStartElement(pNode.getPrefix(), pNode.getNamespaceURI(), pNode.getLocalName()));
    NamedNodeMap attrs1 = pNode.getAttributes();
    List<Attr> attrs2 = new ArrayList<>();
    for(int i=0; i<attrs1.getLength(); ++i) {
      Attr attr = (Attr) attrs1.item(i);
      if ("xmlns".equals(attr.getPrefix())) {
        add(mXef.createNamespace(attr.getLocalName(), attr.getValue()));
      } else if ("".equals(attr.getPrefix())&& "xmlns".equals(attr.getLocalName())) {
        add(mXef.createNamespace(attr.getValue()));
      } else {
        attrs2.add(attr);
      }
    }
    for(Attr attr: attrs2) {
      createAttribute(attr);
    }
  }

  private void createAttribute(Attr attr) {
    add(mXef.createAttribute(attr.getPrefix(), attr.getNamespaceURI(), attr.getLocalName(), attr.getValue()));
  }

  @Override
  public Object getProperty(String pName) throws IllegalArgumentException {
    throw new IllegalArgumentException("Not supported");
  }

}
