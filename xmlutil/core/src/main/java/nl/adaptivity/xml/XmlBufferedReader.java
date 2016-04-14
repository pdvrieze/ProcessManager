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

package nl.adaptivity.xml;

import net.devrieze.util.kotlin.CharSequenceUtil;
import nl.adaptivity.xml.XmlEvent.*;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.NamespaceContext;

import java.util.*;


public class XmlBufferedReader extends AbstractXmlReader {

  @NotNull private final XmlReader mDelegate;
  @NotNull private final ArrayDeque<XmlEvent> mPeekBuffer = new ArrayDeque<>();
  @NotNull private final NamespaceHolder mNamespaceHolder = new NamespaceHolder();

  private XmlEvent mCurrent;

  public XmlBufferedReader(final @NotNull XmlReader delegate) {
    mDelegate = delegate;
  }

  public XmlEvent nextEvent() throws XmlException {
    if (! mPeekBuffer.isEmpty()) {
      return  removeFirstToCurrent();
    }
    if (! hasNext()) {
      throw new NoSuchElementException();
    }
    peek();
    return removeFirstToCurrent();
  }

  private XmlEvent removeFirstToCurrent() {
    mCurrent = mPeekBuffer.removeFirst();
    switch (mCurrent.getEventType()) {
      case START_ELEMENT: {
        mNamespaceHolder.incDepth();
        StartElementEvent start = (StartElementEvent) mCurrent;
        for(Namespace ns: start.getNamespaceDecls()) {
          mNamespaceHolder.addPrefixToContext(ns);
        }
      } break;
      case END_ELEMENT: mNamespaceHolder.decDepth(); break;
    }
    return mCurrent;
  }

  XmlEvent peek() throws XmlException {
    if (! mPeekBuffer.isEmpty()) {
      return mPeekBuffer.peek();
    }
    addAll(doPeek());
    return mPeekBuffer.peek();
  }

  /**
   *  Get the next event to add to the queue. Children can override this to customize the events that are added to the
   *  peek buffer. Normally this method is only called when the peek buffer is empty.
   */
  @NotNull
  protected List<XmlEvent> doPeek() throws XmlException {
    if (mDelegate.hasNext()) {
      mDelegate.next(); // Don't forget to actually read the next element
      XmlEvent event = XmlEvent.Companion.from(mDelegate);
      ArrayList<XmlEvent> result = new ArrayList<>(1);
      result.add(event);
      return result;
    }
    return Collections.emptyList();
  }

  @Override
  public boolean hasNext() {
    if (! mPeekBuffer.isEmpty()) { return true; }
    try {
      return peek()!=null;
    } catch (final XmlException e) {
      throw new RuntimeException(e);
    }
  }

  protected void stripWhiteSpaceFromPeekBuffer() {
    XmlEvent peekLast;
    while(mPeekBuffer.size()>0 && (peekLast = mPeekBuffer.peekLast()) instanceof TextEvent && XmlUtilKt.isXmlWhitespace(((TextEvent) peekLast)
                                                                                                                                .getText())) {
      mPeekBuffer.removeLast();
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  protected boolean isPeekBufferEmpty() {
    return mPeekBuffer.isEmpty();
  }

  protected XmlEvent peekFirst() {
    return mPeekBuffer.peekFirst();
  }

  private void add(final XmlEvent event) {
    mPeekBuffer.addLast(event);
  }

  private void addAll(final Collection<? extends XmlEvent> events) {
    mPeekBuffer.addAll(events);
  }

  @Override
  public void close() throws XmlException {
    mDelegate.close();
    mPeekBuffer.clear();
  }

  protected XmlEvent getCurrent() {
    return mCurrent;
  }

  @Override
  public EventType nextTag() throws XmlException {
    return nextTagEvent().getEventType();
  }

  public XmlEvent nextTagEvent() throws XmlException {
    XmlEvent current = nextEvent();
    EventType type = current.getEventType();
    switch (type) {
      case TEXT:
      if (XmlUtilKt.isXmlWhitespace(((TextEvent) current).getText())) {
        return nextTagEvent();
      }
      case COMMENT: // ignore
      case IGNORABLE_WHITESPACE:
      case PROCESSING_INSTRUCTION:
        return nextTagEvent();
      case START_ELEMENT:
      case END_ELEMENT:
        return current;
    }
    throw new XmlException("Unexpected element found when looking for tags: "+current);
  }

  @Override
  public EventType next() throws XmlException {
    return nextEvent().getEventType();
  }

  @Override
  public CharSequence getNamespaceUri() throws XmlException {
    switch (mCurrent.getEventType()) {
      case ATTRIBUTE:
        return ((Attribute) mCurrent).getNamespaceUri();
      case START_ELEMENT:
        return ((StartElementEvent) mCurrent).getNamespaceUri();
      case END_ELEMENT:
        return ((EndElementEvent) mCurrent).getNamespaceUri();
      default:
        throw new XmlException("Attribute not defined here: namespaceUri");
    }
  }

  @Override
  public CharSequence getLocalName() throws XmlException {
    switch (mCurrent.getEventType()) {
      case ATTRIBUTE:
        return ((Attribute) mCurrent).getLocalName();
      case START_ELEMENT:
        return ((StartElementEvent) mCurrent).getLocalName();
      case END_ELEMENT:
        return ((EndElementEvent) mCurrent).getLocalName();
      default:
        throw new XmlException("Attribute not defined here: namespaceUri");
    }
  }

  @Override
  public CharSequence getPrefix() throws XmlException {
    switch (mCurrent.getEventType()) {
      case ATTRIBUTE:
        return ((Attribute) mCurrent).getPrefix();
      case START_ELEMENT:
        return ((StartElementEvent) mCurrent).getPrefix();
      case END_ELEMENT:
        return ((EndElementEvent) mCurrent).getPrefix();
      default:
        throw new XmlException("Attribute not defined here: namespaceUri");
    }
  }

  @Override
  public int getDepth() {
    return mNamespaceHolder.getDepth();
  }

  @Override
  public CharSequence getText() {
    if (mCurrent.getEventType()==EventType.ATTRIBUTE) {
        return ((Attribute) mCurrent).getValue();
    }
    return ((TextEvent) mCurrent).getText();
  }

  @Override
  public int getAttributeCount() {
    return ((StartElementEvent) mCurrent).getAttributes().length;
  }

  @Override
  public CharSequence getAttributeNamespace(final int i) throws XmlException {
    return ((StartElementEvent) mCurrent).getAttributes()[i].getNamespaceUri();
  }

  @Override
  public CharSequence getAttributePrefix(final int i) throws XmlException {
    return ((StartElementEvent) mCurrent).getAttributes()[i].getPrefix();
  }

  @Override
  public CharSequence getAttributeLocalName(final int i) throws XmlException {
    return ((StartElementEvent) mCurrent).getAttributes()[i].getLocalName();
  }

  @Override
  public CharSequence getAttributeValue(final int i) throws XmlException {
    return ((StartElementEvent) mCurrent).getAttributes()[i].getValue();
  }

  @Override
  public EventType getEventType() throws XmlException {
    return mCurrent==null ? null : mCurrent.getEventType();
  }

  @Override
  public CharSequence getAttributeValue(final CharSequence nsUri, final CharSequence localName) throws XmlException {
    StartElementEvent current = (StartElementEvent) mCurrent;
    for(Attribute attr: current.getAttributes()) {
      if ((nsUri==null || nsUri.toString().equals(attr.getNamespaceUri())) &&
          CharSequenceUtil.matches(localName, attr.getLocalName())) {
        return attr.getValue();
      }
    }
    return null;
  }

  @Override
  public int getNamespaceStart() throws XmlException {
    return 0;
  }

  @Override
  public int getNamespaceEnd() throws XmlException {
    return ((StartElementEvent) mCurrent).getNamespaceDecls().length;
  }

  @Override
  public CharSequence getNamespacePrefix(final int i) throws XmlException {
    return ((StartElementEvent) mCurrent).getNamespaceDecls()[i].getPrefix();
  }

  @Override
  public CharSequence getNamespaceUri(final int i) throws XmlException {
    return ((StartElementEvent) mCurrent).getNamespaceDecls()[i].getNamespaceURI();
  }

  @Override
  public CharSequence getNamespacePrefix(final CharSequence namespaceUri) throws XmlException {
    return ((StartElementEvent) mCurrent).getPrefix(namespaceUri);
  }

  @Override
  public String getNamespaceUri(final CharSequence prefix) throws XmlException {
    return ((StartElementEvent) mCurrent).getNamespaceUri(prefix);
  }

  @Override
  public String getLocationInfo() { // allow for location info at the start of the document
    return mCurrent == null ? mDelegate.getLocationInfo() : mCurrent.getLocationInfo();
  }

  @Override
  public NamespaceContext getNamespaceContext() throws XmlException {
    return ((StartElementEvent) mCurrent).getNamespaceContext();
  }

  @Override
  public CharSequence getEncoding() {
    return ((StartDocumentEvent) mCurrent).getEncoding();
  }

  @Override
  public Boolean getStandalone() {
    return ((StartDocumentEvent) mCurrent).getStandalone();
  }

  @Override
  public CharSequence getVersion() {
    return ((StartDocumentEvent) mCurrent).getVersion();
  }
}
