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

import nl.adaptivity.xml.*;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import java.util.*;


/**
 * Utility class that contains a lot of functionality to handle xml.
 */
public final class XmlUtil {

  private static class NamespaceInfo {

    final String mPrefix;
    final String mUrl;

    public NamespaceInfo(final String prefix, final String url) {
      this.mPrefix = prefix;
      this.mUrl = url;

    }
  }


  private XmlUtil() { /* Utility class is not constructible. */ }


  @NotNull
  public static <T extends XmlDeserializable> T deserializeHelper(@NotNull final T result, @NotNull final XmlReader in) throws
          XmlException {
    XmlReaderUtil.skipPreamble(in);
    final QName elementName = result.getElementName();
    assert XmlReaderUtil.isElement(in, elementName) : "Expected " + elementName + " but found " + in.getLocalName();
    for(int i=in.getAttributeCount()-1; i>=0; --i) {
      result.deserializeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
    }
    result.onBeforeDeserializeChildren(in);
    EventType event = null;
    if (result instanceof SimpleXmlDeserializable) {
      loop: while (in.hasNext() && event != XmlStreamingKt.END_ELEMENT) {
        switch ((event = in.next())) {
          case START_ELEMENT:
            if (((SimpleXmlDeserializable)result).deserializeChild(in)) {
              continue loop;
            }
            XmlReaderUtil.unhandledEvent(in);
            break;
          case TEXT:
          case CDSECT:
            if (((SimpleXmlDeserializable)result).deserializeChildText(in.getText())) {
              continue loop;
            }
            // If the text was not deserialized, then just fall through
          default:
            XmlReaderUtil.unhandledEvent(in);
        }
      }
    } else if (result instanceof ExtXmlDeserializable){
      ((ExtXmlDeserializable)result).deserializeChildren(in);
      if (XmlUtil.class.desiredAssertionStatus()) {
        in.require(XmlStreamingKt.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());
      }
    } else {// Neither, means ignore children
      if(!nl.adaptivity.xml.XmlUtil.isXmlWhitespace(XmlReaderUtil.siblingsToFragment(in).getContent())) {
        throw new XmlException("Unexpected child content in element");
      }
    }
    return result;
  }

  /* XXX These can't work because they don't allow for attributes
  public static void writeEmptyElement(@NotNull final StAXWriter out, @NotNull final QName qName) throws XMLStreamException {
    String namespace = qName.getNamespaceURI();
    String prefix;
    if (namespace==null) {
      namespace = out.getNamespaceContext().getNamespaceURI(qName.getPrefix());
      prefix = qName.getPrefix();
    } else {
      prefix = out.getPrefix(namespace);
      if (prefix==null) { prefix = qName.getPrefix(); }
    }
    out.writeEmptyElement(prefix, qName.getLocalPart(), namespace);
  }
*/


  static void configure(@NotNull final Transformer transformer, final int flags) {
    if ((flags & XmlStreamingKt.FLAG_OMIT_XMLDECL) != 0) {
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
  }


  public static void cannonicallize(final Source in, final Result out) throws XmlException {
    // TODO add wrapper methods that get stream readers and writers analogous to the event writers and readers
    XmlReader xsr = XmlStreaming.newReader(in);
    final XmlWriter xsw = XmlStreaming.newWriter(out, true);
    final Map<String, NamespaceInfo> collectedNS = new HashMap<>();

    while (xsr.hasNext()) {
      final EventType type=xsr.next();
      switch (type) {
        case START_ELEMENT:
//          if (xsr.getNamespaceCount()>0) {
//            for(int i=0; i<xsr.getNamespaceCount(); ++i) {
//              addNamespace(collectedNS, xsr.getNamespacePrefix(i), xsr.getNamespaceURI(i));
//            }
//          }
          addNamespace(collectedNS, xsr.getPrefix().toString(), xsr.getNamespaceUri().toString());
          for(int i=xsr.getAttributeCount()-1; i>=0; --i) {
            addNamespace(collectedNS, xsr.getAttributePrefix(i).toString(), xsr.getAttributeNamespace(i).toString());
          }
        default:
          // ignore
      }
    }

    xsr = XmlStreaming.newReader(in);

    boolean first = true;
    while (xsr.hasNext()) {
      final EventType type = xsr.next();
      switch (type) { // TODO extract the default elements to a separate method that is also used to copy StreamReader to StreamWriter without events.
        case START_ELEMENT:
          {
            if (first) {
              NamespaceInfo namespaceInfo = collectedNS.get(xsr.getNamespaceUri());
              if (namespaceInfo != null) {
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(xsr.getPrefix())) {
                  namespaceInfo = new NamespaceInfo("", namespaceInfo.mUrl);
                }
                xsw.setPrefix(namespaceInfo.mPrefix, namespaceInfo.mUrl);
                xsw.startTag(namespaceInfo.mPrefix, xsr.getLocalName().toString(), namespaceInfo.mUrl);
              } else { // no namespace info (probably no namespace at all)
                xsw.startTag(xsr.getPrefix(), xsr.getLocalName(), xsr.getNamespaceUri());
              }
              first = false;
              for (final NamespaceInfo ns : collectedNS.values()) {
                xsw.setPrefix(ns.mPrefix, ns.mUrl);
                xsw.namespaceAttr(ns.mPrefix, ns.mUrl);
              }
            } else {
              xsw.startTag(xsr.getNamespaceUri(), xsr.getLocalName(), null);
            }
            final int ac = xsr.getAttributeCount();
            for (int i = 0; i<ac; ++i) {
              xsw.attribute(xsr.getAttributeNamespace(i),xsr.getAttributeLocalName(i), null, xsr.getAttributeValue(i));
            }
            break;
          }
        case ATTRIBUTE:
          xsw.attribute(xsr.getNamespaceUri(),xsr.getLocalName(), null, xsr.getText());
          break;
        case END_ELEMENT:
          xsw.endTag(null, null, null);
          break;
        case TEXT:
          xsw.text(xsr.getText());
          break;
        case IGNORABLE_WHITESPACE:
          xsw.ignorableWhitespace(xsr.getText());
          break;
        case CDSECT:
          xsw.cdsect(xsr.getText());
          break;
        case COMMENT:
          xsw.comment(xsr.getText());
          break;
        case START_DOCUMENT:
          xsw.startDocument(xsr.getEncoding(), xsr.getVersion(), xsr.getStandalone());
          break;
        case END_DOCUMENT:
          xsw.endDocument();
          break;
        case PROCESSING_INSTRUCTION:
          xsw.processingInstruction(xsr.getText());
          break;
        case ENTITY_REF:
          xsw.entityRef(xsr.getText());
          break;
        case DOCDECL:
          xsw.docdecl(xsr.getText());
          break;
      }
    }
    xsw.close();
    xsr.close();
  }

  private static void addNamespace(@NotNull final Map<String, NamespaceInfo> collectedNS, final String prefix, @Nullable final String namespaceURI) {
    if (! (namespaceURI==null || XMLConstants.NULL_NS_URI.equals(namespaceURI))) {
      NamespaceInfo nsInfo = collectedNS.get(namespaceURI);
      if (nsInfo==null) {
        collectedNS.put(namespaceURI, new NamespaceInfo(prefix, namespaceURI));
      } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(nsInfo.mPrefix)) {
        nsInfo=new NamespaceInfo(prefix, nsInfo.mUrl);
      }
    }
  }

}
