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

import net.devrieze.util.StringUtil;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.Source;

import java.io.InputStream;
import java.io.Reader;


/**
 * An implementation of {@link XmlReader} based upon the JDK StAX implementation.
 * @author Created by pdvrieze on 16/11/15.
 */
public class StAXReader extends AbstractXmlReader {

  private static final EventType[] DELEGATE_TO_LOCAL;

  private static final int[] LOCAL_TO_DELEGATE;
  private boolean mFixWhitespace = false;

  static {
    DELEGATE_TO_LOCAL = new EventType[16];
    DELEGATE_TO_LOCAL[XMLStreamConstants.CDATA] = XmlStreaming.CDSECT;
    DELEGATE_TO_LOCAL[XMLStreamConstants.COMMENT] = XmlStreaming.COMMENT;
    DELEGATE_TO_LOCAL[XMLStreamConstants.DTD] = XmlStreaming.DOCDECL;
    DELEGATE_TO_LOCAL[XMLStreamConstants.END_DOCUMENT] = XmlStreaming.END_DOCUMENT;
    DELEGATE_TO_LOCAL[XMLStreamConstants.END_ELEMENT] = XmlStreaming.END_ELEMENT;
    DELEGATE_TO_LOCAL[XMLStreamConstants.ENTITY_REFERENCE] = XmlStreaming.ENTITY_REF;
    DELEGATE_TO_LOCAL[XMLStreamConstants.SPACE] = XmlStreaming.IGNORABLE_WHITESPACE;
    DELEGATE_TO_LOCAL[XMLStreamConstants.PROCESSING_INSTRUCTION] = XmlStreaming.PROCESSING_INSTRUCTION;
    DELEGATE_TO_LOCAL[XMLStreamConstants.START_DOCUMENT] = XmlStreaming.START_DOCUMENT;
    DELEGATE_TO_LOCAL[XMLStreamConstants.START_ELEMENT] = XmlStreaming.START_ELEMENT;
    DELEGATE_TO_LOCAL[XMLStreamConstants.CHARACTERS] = XmlStreaming.TEXT;
    DELEGATE_TO_LOCAL[XMLStreamConstants.ATTRIBUTE] = XmlStreaming.ATTRIBUTE;

    LOCAL_TO_DELEGATE = new int[12];
    LOCAL_TO_DELEGATE[XmlStreaming.CDSECT.ordinal()] = XMLStreamConstants.CDATA;
    LOCAL_TO_DELEGATE[XmlStreaming.COMMENT.ordinal()] = XMLStreamConstants.COMMENT;
    LOCAL_TO_DELEGATE[XmlStreaming.DOCDECL.ordinal()] = XMLStreamConstants.DTD;
    LOCAL_TO_DELEGATE[XmlStreaming.END_DOCUMENT.ordinal()] = XMLStreamConstants.END_DOCUMENT;
    LOCAL_TO_DELEGATE[XmlStreaming.END_ELEMENT.ordinal()] = XMLStreamConstants.END_ELEMENT;
    LOCAL_TO_DELEGATE[XmlStreaming.ENTITY_REF.ordinal()] = XMLStreamConstants.ENTITY_REFERENCE;
    LOCAL_TO_DELEGATE[XmlStreaming.IGNORABLE_WHITESPACE.ordinal()] = XMLStreamConstants.SPACE;
    LOCAL_TO_DELEGATE[XmlStreaming.PROCESSING_INSTRUCTION.ordinal()] = XMLStreamConstants.PROCESSING_INSTRUCTION;
    LOCAL_TO_DELEGATE[XmlStreaming.START_DOCUMENT.ordinal()] = XMLStreamConstants.START_DOCUMENT;
    LOCAL_TO_DELEGATE[XmlStreaming.START_ELEMENT.ordinal()] = XMLStreamConstants.START_ELEMENT;
    LOCAL_TO_DELEGATE[XmlStreaming.TEXT.ordinal()] = XMLStreamConstants.CHARACTERS;
    LOCAL_TO_DELEGATE[XmlStreaming.ATTRIBUTE.ordinal()] = XMLStreamConstants.ATTRIBUTE;
  }

  private final XMLStreamReader mDelegate;
  private int mDepth=0;


  public StAXReader(final Reader reader) throws XMLStreamException {
    this(XMLInputFactory.newFactory().createXMLStreamReader(reader));
  }

  public StAXReader(final InputStream inputStream, final String encoding) throws XMLStreamException {
    this(XMLInputFactory.newFactory().createXMLStreamReader(inputStream, encoding));
  }

  public StAXReader(final Source source) throws XMLStreamException {
    this(XMLInputFactory.newFactory().createXMLStreamReader(source));
  }

  public StAXReader(final XMLStreamReader streamReader) {
    mDelegate = streamReader;
  }

  @Override
  public void close() throws XmlException {
    try {
      mDelegate.close();
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public boolean isEndElement() {
    return mDelegate.isEndElement();
  }

  @Deprecated
  public boolean isStandalone() {
    return getStandalone()==null ? false : getStandalone().booleanValue();
  }

  @Override
  public boolean isCharacters() {
    return mDelegate.isCharacters();
  }

  @Override
  public boolean isStartElement() {
    return mDelegate.isStartElement();
  }

  @Override
  public boolean isWhitespace() throws XmlException {
    return mDelegate.isWhiteSpace();
  }

  @Deprecated
  public boolean isWhiteSpace() throws XmlException {
    return isWhitespace();
  }

  @Deprecated
  public String getNamespaceURI() {
    return getNamespaceUri();
  }

  @Override
  public String getNamespaceUri() {
    return mDelegate.getNamespaceURI();
  }

  @Deprecated
  public boolean hasText() {
    return mDelegate.hasText();
  }

  @Override
  public void require(final EventType type, final CharSequence namespace, final CharSequence name) throws XmlException {
    try {
      mDelegate.require(LOCAL_TO_DELEGATE[type.ordinal()], StringUtil.toString(namespace), StringUtil.toString(name));
    } catch (XMLStreamException e) {

      throw new XmlException(e);
    }

  }

  @Deprecated
  public int getNamespaceCount() throws XmlException {
    try {
      return getNamespaceEnd()-getNamespaceStart();
    } catch (XmlException e) {
      throw new XmlException(e);
    }
  }

  @Deprecated
  public char[] getTextCharacters() {
    return getText().toCharArray();
  }

  @Deprecated
  public String getCharacterEncodingScheme() {
    return mDelegate.getCharacterEncodingScheme();
  }

  @Deprecated
  public QName getAttributeName(final int index) {
    return new QName(getAttributeNamespace(index),getAttributeLocalName(index), getAttributePrefix(index));
  }

  @Override
  public String getNamespaceUri(final CharSequence prefix) {
    return mDelegate.getNamespaceURI(StringUtil.toString(prefix));
  }

  public String getNamespaceURI(final String prefix) {
    return getNamespaceUri(prefix);
  }

  @Override
  public String getNamespacePrefix(final CharSequence namespaceUri) throws XmlException {
    return mDelegate.getNamespaceContext().getPrefix(StringUtil.toString(namespaceUri));
  }

  @Override
  public String getLocationInfo() {
    Location location = mDelegate.getLocation();
    return location==null ? null : location.toString();
  }

  @Deprecated
  public Location getLocation() {
    return mDelegate.getLocation();
  }

  @Override
  public String getAttributeValue(final CharSequence namespaceURI, final CharSequence localName) {
    return mDelegate.getAttributeValue(StringUtil.toString(namespaceURI), StringUtil.toString(localName));
  }

  @Deprecated
  public String getVersion() {
    return mDelegate.getVersion();
  }

  @Deprecated
  public QName getName() {
    return new QName(getNamespaceUri(), getLocalName(), getPrefix());
  }

  @Override
  @Nullable
  public EventType next() throws XmlException {
    try {
      if (mDelegate.hasNext()) {
        return updateDepth(fixWhitespace(DELEGATE_TO_LOCAL[mDelegate.next()]));
      } else {
        return null;
      }
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public EventType nextTag() throws XmlException {
    try {
      return updateDepth(fixWhitespace(DELEGATE_TO_LOCAL[mDelegate.nextTag()]));
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  private EventType fixWhitespace(final EventType eventType) {
    if (eventType==EventType.TEXT) {
      if (XmlUtil.isXmlWhitespace(mDelegate.getText())) {
        mFixWhitespace = true;
        return EventType.IGNORABLE_WHITESPACE;
      }
    }
    mFixWhitespace = false;
    return eventType;
  }

  private EventType updateDepth(final EventType eventType) {
    switch (eventType) {
      case START_ELEMENT:
        ++mDepth;
        break;
      case END_ELEMENT:
        --mDepth;
        break;
    }
    return eventType;
  }

  @Override
  public boolean hasNext() throws XmlException {
    try {
      return mDelegate.hasNext();
    } catch (XMLStreamException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public int getAttributeCount() {
    return mDelegate.getAttributeCount();
  }

  @Override
  public String getAttributeNamespace(final int index) {
    return mDelegate.getAttributeNamespace(index);
  }

  @Override
  public String getAttributeLocalName(final int index) {
    return mDelegate.getAttributeLocalName(index);
  }

  @Override
  public String getAttributePrefix(final int index) {
    return mDelegate.getAttributePrefix(index);
  }

  @Override
  public String getAttributeValue(final int index) {
    return mDelegate.getAttributeValue(index);
  }

  @Override
  public int getNamespaceStart() throws XmlException {
    return 0;
  }

  @Override
  public int getNamespaceEnd() throws XmlException {
    return mDelegate.getNamespaceCount();
  }

  @Deprecated
  public String getNamespaceURI(final int index) {
    return getNamespaceUri(index);
  }

  @Override
  public String getNamespaceUri(final int index) {
    return mDelegate.getNamespaceURI(index);
  }

  @Override
  public String getNamespacePrefix(final int index) {
    return mDelegate.getNamespacePrefix(index);
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return mDelegate.getNamespaceContext();
  }

  @Override
  public EventType getEventType() {
    return mFixWhitespace ? EventType.IGNORABLE_WHITESPACE : DELEGATE_TO_LOCAL[mDelegate.getEventType()];
  }

  @Override
  public String getText() {
    return mDelegate.getText();
  }

  @Override
  public String getEncoding() {
    return mDelegate.getEncoding();
  }

  @Override
  public String getLocalName() {
    return mDelegate.getLocalName();
  }

  @Override
  public String getPrefix() {
    return mDelegate.getPrefix();
  }

  @Deprecated
  public String getPIData() {
    String text = getText();
    int index = text.indexOf(' ');
    if (index<0) {
      return null;
    } else {
      return text.substring(index+1);
    }
  }

  @Deprecated
  public String getPITarget() {
    String text = getText();
    int index = text.indexOf(' ');
    if (index<0) {
      return text;
    } else {
      return text.substring(0, index);
    }
  }

  @Override
  public Boolean getStandalone() {
    return mDelegate.standaloneSet() ? mDelegate.isStandalone() : null;
  }

  @Override
  public int getDepth() throws XmlException {
    return mDepth;
  }
}
