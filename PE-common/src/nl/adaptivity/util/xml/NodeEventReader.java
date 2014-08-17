package nl.adaptivity.util.xml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

import org.w3c.dom.*;


public class NodeEventReader extends AbstractBufferedEventReader {

  private NodeList mNodes;
  private int mNodesPos;
  private Node mCurrent;
  private XMLEventFactory mXef = XMLEventFactory.newInstance();
  private Node mGuardParent;
  private Deque<List<Namespace>> mNamespaceContext = new ArrayDeque<>();

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
      Node node;
      if (mCurrent==null) {
        node = mNodes.item(mNodesPos);
        // This guard will allow nodes that have parents to be looped over
        // without the loop spilling into the parent nodes.
        mGuardParent = node.getParentNode();
      } else {
        node = mCurrent.getFirstChild();
      }
      if (node==null) { node = mCurrent.getNextSibling(); }
      if (node==null) {
        if (mCurrent.getNodeType()==Node.ELEMENT_NODE || mCurrent.getNodeType()==Node.DOCUMENT_NODE || mCurrent.getNodeType()==Node.DOCUMENT_FRAGMENT_NODE) {
          // node without content
          add(createEndEvent(mCurrent));
          if (mCurrent.getNodeType()==Node.ELEMENT_NODE) {
            mNamespaceContext.pop();
          }
        }
        Node parent = mCurrent.getParentNode();
        while (parent!=mGuardParent && parent!=null && node==null) {
          if (parent.getNodeType()!=Node.DOCUMENT_FRAGMENT_NODE) {
            add(createEndEvent(parent));
          }
          node = parent.getNextSibling();
          parent = parent.getParentNode();
        }
      }

      if (node!=null) {
        mCurrent = node;
        createEvents(node);
        XMLEvent elem = peekFirst();
        if (elem!=null) {
          return elem;
        } else {
          continue;
          // retry the loop.
        }
      } else {
        mCurrent = null;
      }
      ++mNodesPos;
      mNamespaceContext.clear();
    }
    return peekFirst(); // end of stream, but still may have put some end nodes into the peek buffer
  }

  private XMLEvent createEndEvent(Node pNode) {
    switch (pNode.getNodeType()) {
      case Node.ELEMENT_NODE:
        return mXef.createEndElement(getPrefix(pNode), pNode.getNamespaceURI(), pNode.getLocalName());
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
      case Node.DOCUMENT_FRAGMENT_NODE: // ignore as event. The children get processed by themselves.
        return;
      default:
        throw new IllegalArgumentException("Nodes of type "+pNode.getNodeType()+" ("+pNode.getClass().getSimpleName()+") are not supported");
    }
  }

  private void createElement(Element pNode) {
    final ArrayList<Namespace> namespaces = new ArrayList<>();
    mNamespaceContext.push(namespaces);
    final String prefix = getPrefix(pNode);
    add(mXef.createStartElement(prefix, pNode.getNamespaceURI(), pNode.getLocalName()));
    NamedNodeMap attrs1 = pNode.getAttributes();
    List<Attr> attrs2 = new ArrayList<>();
    for(int i=0; i<attrs1.getLength(); ++i) {
      Attr attr = (Attr) attrs1.item(i);
      final Namespace ns;
      if ("xmlns".equals(attr.getPrefix())) {
        ns = mXef.createNamespace(attr.getLocalName(), attr.getValue());
      } else if ("".equals(attr.getPrefix())&& "xmlns".equals(attr.getLocalName())) {
        ns = mXef.createNamespace(attr.getValue());
      } else {
        attrs2.add(attr);
        continue;
      }
      namespaces.add(ns);
      add(ns);
    }
    if (! isKnownNs(prefix, pNode.getNamespaceURI())) {
      Namespace ns = mXef.createNamespace(prefix, pNode.getNamespaceURI());
      namespaces.add(ns);
      add(ns);
    }
    for(Attr attr: attrs2) {
      createAttribute(attr);
    }
  }

  private boolean isKnownNs(String pPrefix, String pNamespaceURI) {
    if (pNamespaceURI==null) { return true; /* no need */ }
    for(Iterator<List<Namespace>> it = mNamespaceContext.descendingIterator(); it.hasNext();) {
      final List<Namespace> namespaces = it.next();
      for(Namespace namespace:namespaces) {
        if (pPrefix.equals(namespace.getPrefix())) {
          // If we have the prefix, we can't go look back
          return pNamespaceURI.equals(namespace.getNamespaceURI());
        }
      }
    }
    return false;
  }

  private void createAttribute(Attr attr) {
    final String prefix = getPrefix(attr);
    if (! isKnownNs(prefix, attr.getNamespaceURI())) {
      Namespace ns = mXef.createNamespace(prefix, attr.getNamespaceURI());
      add(ns);
      mNamespaceContext.peekLast().add(ns);
    }
    add(mXef.createAttribute(prefix, attr.getNamespaceURI(), attr.getLocalName(), attr.getValue()));
  }

  @Override
  public Object getProperty(String pName) throws IllegalArgumentException {
    throw new IllegalArgumentException("Not supported");
  }

  private static String getPrefix(Node pNode) {
    final String prefix = pNode.getPrefix();
    return prefix==null ? XMLConstants.DEFAULT_NS_PREFIX: prefix;
  }

}
