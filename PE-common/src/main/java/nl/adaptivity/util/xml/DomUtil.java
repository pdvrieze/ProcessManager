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

package nl.adaptivity.util.xml;

import net.devrieze.util.StringUtil;
import nl.adaptivity.util.CombiningReader;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.util.List;


/**
 * Utility class for methods involving DOM manipulation.
 */
public final class DomUtil {

  private static final int DEFAULT_FLAGS = XmlUtil.FLAG_OMIT_XMLDECL;

  private DomUtil() {}

  /**
   * Create an {@link Element} with the given name. Depending on the prefix, and namespace it uses the "correct"
   * approach, with null namespace or prefix, or specified namespace and prefix.
   *
   * @param document  The owning document.
   * @param qName The name of the element.
   */
  public static Element createElement(@NotNull final Document document, @NotNull final QName qName) {
    final Element root;
    if (XMLConstants.NULL_NS_URI.equals(qName.getNamespaceURI()) || null == qName.getNamespaceURI()) {
      root = document.createElement(qName.getLocalPart());
    } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(qName.getPrefix())) {
      root = document.createElementNS(qName.getNamespaceURI(), qName.getLocalPart());
    } else {
      root = document.createElementNS(qName.getNamespaceURI(), qName.getPrefix() + ':' + qName.getLocalPart());
    }
    return root;
  }

  /**
   * XPath processing does require either a document or a fragment to actually work. This method will
   * make this work. If the node is either that will be returned, otherwise, if it is the root node of the owner document,
   * the owner document is returned. Otherwise, a fragment will be created with a clone of the node.
   * @param node The node to attach if needed.
   * @return A document or documentfragment representing the given node (it may be a clone though)
   */
  public static Node ensureAttached(final Node node) {
    if (node==null) { return null; }
    if (node instanceof Document || node instanceof DocumentFragment) {
      return node;
    }
    if (node.isSameNode(node.getOwnerDocument().getDocumentElement())) {
      return node.getOwnerDocument();
    }
    final DocumentFragment frag = node.getOwnerDocument().createDocumentFragment();
    frag.appendChild(node.cloneNode(true));
    return frag;
  }

  public static boolean isAttached(final Node node) {
    if (node instanceof Document || node instanceof DocumentFragment) {
      return true;
    }
    final Node docElem = node.getOwnerDocument().getDocumentElement();
    if (docElem!=null) {
      for (Node curNode = node; curNode != null; curNode = curNode.getParentNode()) {
        if (docElem.isSameNode(curNode)) { return true; }
      }
    }
    return false;
  }

  @Nullable
  public static Document tryParseXml(final InputStream inputStream) throws IOException {
    return tryParseXml(new InputSource(inputStream));
  }

  @Nullable
  public static Document tryParseXml(final Reader reader) throws IOException {
    return tryParseXml(new InputSource(reader));
  }

  @Nullable
  public static Document tryParseXml(@NotNull final String xmlString) throws IOException {
    return tryParseXml(new StringReader(xmlString));
  }

  public static Document tryParseXml(final InputSource xmlSource) throws IOException {
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();

      return db.parse(xmlSource);
    } catch (@NotNull final SAXException e) {
      return null;
    } catch (@NotNull final ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static DocumentFragment tryParseXmlFragment(final Reader reader) throws IOException {
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document doc = db.parse(new InputSource(new CombiningReader(new StringReader("<elem>"), reader, new StringReader("</elem>"))));
      final DocumentFragment frag = doc.createDocumentFragment();
      final Element docelem = doc.getDocumentElement();
      for (Node child = docelem.getFirstChild(); child != null; child = docelem.getFirstChild()) {
        frag.appendChild(child);
      }
      doc.removeChild(docelem);
      return frag;
    } catch (@NotNull ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }

  }

  public static String toString(final Node value) {
    return toString(value, DEFAULT_FLAGS);
  }

  public static String toString(final Node value, final int flags) {
    final StringWriter out =new StringWriter();
    try {
      final Transformer t = TransformerFactory
        .newInstance()
        .newTransformer();
      XmlUtil.configure(t, flags);
      t.transform(new DOMSource(value), new StreamResult(out));
    } catch (@NotNull final TransformerException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  public static String toString(@NotNull final NodeList nodeList) {
    return toString(nodeList, DEFAULT_FLAGS);
  }

  public static String toString(@NotNull final NodeList nodeList, final int flags) {
    final StringWriter out =new StringWriter();
    try {
      final Transformer t = TransformerFactory
        .newInstance()
        .newTransformer();
      XmlUtil.configure(t, flags);
      for(int i=0; i<nodeList.getLength(); ++i) {
        t.transform(new DOMSource(nodeList.item(i)), new StreamResult(out));
      }
    } catch (@NotNull final TransformerException e) {
      throw new RuntimeException(e);
    }
    return out.toString();
  }

  @Nullable
  public static DocumentFragment toDocFragment(@Nullable final NodeList value) {
    if (value==null || value.getLength()==0) { return null; }
    final Document document = value.item(0).getOwnerDocument();
    final DocumentFragment fragment = document.createDocumentFragment();
    for(int i=0; i<value.getLength(); ++i) {
      final Node n = value.item(i);
      if (n.getOwnerDocument()!=document) {
        fragment.appendChild(document.adoptNode(n.cloneNode(true)));
      } else {
        fragment.appendChild(n.cloneNode(true));
      }
    }
    return fragment;
  }

  public static DocumentFragment childrenToDocumentFragment(final XmlReader in) throws XmlException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final Document doc;
    try {
      doc = dbf.newDocumentBuilder().newDocument();
    } catch (@NotNull final ParserConfigurationException e) {
      throw new XmlException(e);
    }
    final DocumentFragment documentFragment = doc.createDocumentFragment();
    final XmlWriter        out              = XmlStreaming.newWriter(new DOMResult(documentFragment), true);
    while (in.hasNext() && (in.next() != EventType.END_ELEMENT)) {
      XmlUtil.writeCurrentEvent(in, out);
      if (in.getEventType()== EventType.START_ELEMENT) {
        XmlUtil.writeElementContent(null, in, out);
      }
    }
    return documentFragment;
  }

  public static Node childToNode(final XmlReader in) throws XmlException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final Document doc;
    try {
      doc = dbf.newDocumentBuilder().newDocument();
    } catch (@NotNull final ParserConfigurationException e) {
      throw new XmlException(e);
    }
    final DocumentFragment documentFragment = doc.createDocumentFragment();
    final XmlWriter out = XmlStreaming.newWriter(new DOMResult(documentFragment), true);
    XmlUtil.writeCurrentEvent(in, out);
    if (in.getEventType()== EventType.START_ELEMENT) {
      XmlUtil.writeElementContent(null, in, out);
    }
    return documentFragment.getFirstChild();
  }

  @NotNull
  public static CompactFragment nodeListToFragment(@NotNull final NodeList nodeList) throws XmlException {
    switch(nodeList.getLength()) {
      case 0:
        return new CompactFragment("");
      case 1:
        final Node node = nodeList.item(0);
        return nodeToFragment(node);
      default:
        return nodeToFragment(toDocFragment(nodeList));
    }
  }

  @NotNull
  public static CompactFragment nodeToFragment(final Node node) throws XmlException {
    if (node instanceof Text) {
      return new CompactFragment(((Text) node).getData());
    }
    return XmlReaderUtil.siblingsToFragment(XmlStreaming.newReader(new DOMSource(node)));
  }

  @Nullable
  public static DocumentFragment toDocFragment(@Nullable final List<Node> value) {
    if (value==null || value.size()==0) { return null; }
    final Document document = value.get(0).getOwnerDocument();
    final DocumentFragment fragment = document.createDocumentFragment();
    for(final Node n: value) {
      if (n.getOwnerDocument()!=document) {
        fragment.appendChild(document.adoptNode(n.cloneNode(true)));
      } else {
        fragment.appendChild(n.cloneNode(true));
      }
    }
    return fragment;
  }

  /**
   * Make a QName for the given parameters.
   * @param reference The node to use to look up the namespace that corresponds to the prefix.
   * @param name This is the full name of the element. That includes the prefix (or if no colon present) the default prefix.
   * @return The QName.
   */
  @NotNull
  public static QName asQName(@NotNull final Node reference, @NotNull final String name) {
    final int colPos = name.indexOf(':');
    if (colPos >= 0) {
      final String prefix = name.substring(0, colPos);
      return new QName(reference.lookupNamespaceURI(prefix), name.substring(colPos + 1), prefix);
    } else {
      return new QName(reference.lookupNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), name, XMLConstants.NULL_NS_URI);
    }

  }

  @Nullable
  public static Element getChild(@NotNull final Element parent, @NotNull final QName name) {
    return getFirstChild(parent, name.getNamespaceURI(), name.getLocalPart());
  }

  public static Element getFirstChild(@NotNull final Element parent, @Nullable final String namespaceURI, final String localName) {
    for (Element child = getFirstChildElement(parent); child != null; child = getNextSiblingElement(child)) {
      if ((namespaceURI == null) || (namespaceURI.length() == 0)) {
        if (((child.getNamespaceURI() == null) || (child.getNamespaceURI().length() == 0))
            && StringUtil.isEqual(localName, child.getLocalName())) {
          return child;
        }
      } else {
        if (StringUtil.isEqual(namespaceURI, child.getNamespaceURI()) && StringUtil.isEqual(localName, child.getLocalName())) {
          return child;
        }
      }
    }
    return null;
  }

  @Nullable
  public static Element getNextSibling(@NotNull final Element sibling, @NotNull final QName name) {
    return getNextSibling(sibling, name.getNamespaceURI(), name.getLocalPart());
  }

  public static Element getNextSibling(@NotNull final Element sibling, final String namespaceURI, final String localName) {
    for (Element child = getNextSiblingElement(sibling); child != null; child = getNextSiblingElement(child)) {
      if (StringUtil.isEqual(namespaceURI, child.getNamespaceURI()) && StringUtil.isEqual(localName, child.getLocalName())) {
        return child;
      }
    }
    return null;
  }

  /**
   * Return the first child that is an element.
   *
   * @param parent The parent element.
   * @return The first element child, or <code>null</code> if there is none.
   */
  public static Element getFirstChildElement(@NotNull final Element parent) {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) {
        return (Element) child;
      }
    }
    return null;
  }

  /**
   * Return the next sibling that is an element.
   *
   * @param sibling The reference element.
   * @return The next element sibling, or <code>null</code> if there is none.
   */
  public static Element getNextSiblingElement(@NotNull final Element sibling) {
    for (Node child = sibling.getNextSibling(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) {
        return (Element) child;
      }
    }
    return null;
  }

  public static void setAttribute(@NotNull final Element element, @NotNull final QName name, final String value) {
    if (name.getNamespaceURI()==null || XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI())) {
      element.setAttribute(name.getLocalPart(), value);
    } else if (name.getPrefix()==null || XMLConstants.DEFAULT_NS_PREFIX.equals(name.getPrefix())) {
      element.setAttributeNS(name.getNamespaceURI(),name.getLocalPart(), value);
    } else {
      element.setAttributeNS(name.getNamespaceURI(),name.getPrefix()+':'+name.getLocalPart(), value);
    }
  }

  public static Node cannonicallize(final Node content) throws ParserConfigurationException,
                                                               XmlException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();

    if (content instanceof DocumentFragment) {
      final DocumentFragment df = (DocumentFragment) content;
      final DocumentFragment result = db.newDocument().createDocumentFragment();
      final DOMResult dr = new DOMResult(result);
      for(Node child=df.getFirstChild(); child!=null; child=child.getNextSibling()) {
        XmlUtil.cannonicallize(new DOMSource(child), dr);
      }
      return result;
    } else {
      final Document result = db.newDocument();
      XmlUtil.cannonicallize(new DOMSource(content), new DOMResult(result));
      return result.getDocumentElement();
    }
  }

  public static String getPrefix(final Node node, final String namespaceURI) {
    if (node==null) { return null; }
    if (node instanceof Element) {
      final NamedNodeMap attrs = node.getAttributes();
      for (int i=0; i<attrs.getLength(); ++i) {
        final Attr attr = (Attr) attrs.item(i);
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI()) && attr.getValue().equals(namespaceURI)) {
          return attr.getName();
        }
      }
    }
    final String prefix = getPrefix(node.getParentNode(), namespaceURI);
    if (node.hasAttributes()&& prefix!=null) {
      if (prefix.isEmpty()) {
        if (node.getAttributes().getNamedItem(XMLConstants.XMLNS_ATTRIBUTE) != null) {
          return null;
        }
      } else {
        if (node.getAttributes().getNamedItemNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix) != null) {
          return null;
        }
      }
    }
    return prefix;
  }
}
