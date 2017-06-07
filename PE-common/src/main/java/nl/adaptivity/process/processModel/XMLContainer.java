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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import java.io.CharArrayReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
public abstract class XMLContainer implements ExtXmlDeserializable, XmlSerializable, CompactFragment {

  private static final SimpleNamespaceContext BASE_NS_CONTEXT = new SimpleNamespaceContext(new String[]{""}, new String[]{""});

  private char[] content;
  @Nullable private SimpleNamespaceContext originalNSContext;

  public XMLContainer() {
  }

  public XMLContainer(final Iterable<Namespace> originalNSContext, final char[] content) {
    setContent(originalNSContext, content);
  }

  public XMLContainer(final CompactFragment fragment) {
    setContent(fragment);
  }

  @Deprecated
  public XMLContainer(final Source source) throws XmlException {
    XmlReader reader = XmlStreaming.newReader(source);
    if (reader.hasNext()) reader.next(); // Initialise the reader
    setContent(XmlReaderExt.siblingsToFragment(reader));
  }

  public void deserializeChildren(@NotNull final XmlReader in) throws XmlException {
    if (in.hasNext()) {
      if (in.next() != EventType.END_ELEMENT) {
        final CompactFragment content = XmlReaderExt.siblingsToFragment(in);
        setContent(content);
      }
    }
  }

  @Override
  public void onBeforeDeserializeChildren(@NotNull final XmlReader reader) throws XmlException {
    int nsEnd = reader.getNamespaceEnd();
    for(int i = reader.getNamespaceStart(); i < nsEnd; ++i) {
      visitNamespace(reader, reader.getNamespacePrefix(i));
    }
  }

  @Nullable
  public char[] getContent() {
    return content;
  }

  @Override
  public boolean isEmpty() {
    return content.length==0;
  }

  @NotNull
  @Override
  public String getContentString() {
    return new String(content);
  }

  @NotNull
  public Iterable<Namespace> getOriginalNSContext() {
    return originalNSContext !=null ? originalNSContext : Collections.<Namespace>emptyList();
  }

  public void setContent(final Iterable<Namespace> originalNSContext, final char[] content) {
    this.originalNSContext = SimpleNamespaceContext.Companion.from(originalNSContext);
    this.content = content;
  }

  public void setContent(@NotNull final CompactFragment content) {
    setContent(content.getNamespaces(), content.getContent());
  }

  protected void updateNamespaceContext(final Iterable<Namespace> additionalContext) {
    final Map<String, String> nsmap = new TreeMap<>();
    final SimpleNamespaceContext context = originalNSContext == null ? SimpleNamespaceContext.Companion.from(additionalContext) : originalNSContext.combine(additionalContext);
    try {
      final GatheringNamespaceContext gatheringNamespaceContext = new GatheringNamespaceContext(context, nsmap);
      visitNamespaces(gatheringNamespaceContext);
    } catch (@NotNull final XmlException e) {
      throw new RuntimeException(e);
    }
    originalNSContext = new SimpleNamespaceContext(nsmap);
  }

  void addNamespaceContext(@NotNull final SimpleNamespaceContext namespaceContext) {
    originalNSContext = (originalNSContext==null || originalNSContext.size()==0) ? namespaceContext: originalNSContext.combine(namespaceContext);
  }

  @Override
  public final void serialize(@NotNull final XmlWriter out) throws XmlException {
    serializeStartElement(out);
    serializeAttributes(out);
    final NamespaceContext outNs = out.getNamespaceContext();
    if (originalNSContext!=null) {
      for (final Namespace ns : originalNSContext) {
        if (!ns.getNamespaceURI().equals(outNs.getNamespaceURI(ns.getPrefix()))) {
          out.namespaceAttr(ns.getPrefix(), ns.getNamespaceURI());
        }
      }
    }
    serializeBody(out);
    serializeEndElement(out);
  }

  protected static void visitNamespace(@NotNull final XmlReader in, final CharSequence prefix) throws XmlException {
    if (prefix!=null) {
      in.getNamespaceContext().getNamespaceURI(prefix.toString());
    }
  }

  protected void visitNamesInElement(@NotNull final XmlReader source) throws XmlException {
    assert source.getEventType()==EventType.START_ELEMENT;
    visitNamespace(source, source.getPrefix());

    for(int i=source.getAttributeCount()-1; i>=0; --i ) {
      final QName attrName = source.getAttributeName(i);
      visitNamesInAttributeValue(source.getNamespaceContext(), source.getName(), attrName, source.getAttributeValue(i));
    }
  }

  protected void visitNamesInAttributeValue(final NamespaceContext referenceContext, final QName owner, final QName attributeName, final CharSequence attributeValue) {
    // By default there are no special attributes
  }

  @SuppressWarnings("UnusedReturnValue")
  @NotNull
  protected List<QName> visitNamesInTextContent(final QName parent, final CharSequence textContent) {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public SimpleNamespaceContext getNamespaces() {
    return originalNSContext;
  }

  protected void visitNamespaces(final NamespaceContext baseContext) throws XmlException {
    if (content != null) {
      final XmlReader xsr = new NamespaceAddingStreamReader(baseContext,
                                                            XMLFragmentStreamReaderKt.getXmlReader(this));

      visitNamespacesInContent(xsr, null);
    }
  }

  private void visitNamespacesInContent(@NotNull final XmlReader xsr, final QName parent) throws
          XmlException {
    while (xsr.hasNext()) {
      switch(xsr.next()) {
        case START_ELEMENT: {
          visitNamesInElement(xsr);
          visitNamespacesInContent(xsr, xsr.getName());
          break;
        }
        case TEXT: {
          visitNamesInTextContent(parent, xsr.getText());
          break;
        }

        default:
          //ignore
      }
    }
  }

  private void serializeBody(@NotNull final XmlWriter out) throws XmlException {
    if (content !=null && content.length>0) {
      final XmlReader contentReader = XmlReaderUtil.asSubstream(getBodyStreamReader());
      while(contentReader.hasNext() && contentReader.next()!=null) {
        XmlReaderUtil.writeCurrent(contentReader, out);
      }
    }

  }

  @NotNull
  public XmlReader getBodyStreamReader() throws XmlException {
    return XMLFragmentStreamReaderKt.getXmlReader(this);
  }

  protected void serializeAttributes(final XmlWriter out) throws XmlException {
    // No attributes by default
  }

  protected abstract void serializeStartElement(final XmlWriter out) throws XmlException;

  protected abstract void serializeEndElement(final XmlWriter out) throws XmlException;


}
