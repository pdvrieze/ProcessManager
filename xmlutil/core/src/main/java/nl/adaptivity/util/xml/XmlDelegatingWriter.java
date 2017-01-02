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

package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.NamespaceContext;


/**
 * <p>Simple delegating writer that passes all calls on to the delegate. This class is abstract for the only reason that any
 * direct instances of this class make little sense.
 * </p><p>
 * Created by pdvrieze on 17/11/15.
 * </p>
 */
public abstract class XmlDelegatingWriter implements XmlWriter{

  private final XmlWriter mDelegate;

  public XmlDelegatingWriter(final XmlWriter delegate) {
    mDelegate = delegate;
  }

  public void setPrefix(final CharSequence prefix, final CharSequence namespaceUri) throws XmlException {
    mDelegate.setPrefix(prefix, namespaceUri);
  }

  public void startDocument(@Nullable final CharSequence version, @Nullable final CharSequence encoding, @Nullable final Boolean standalone) throws XmlException {
    mDelegate.startDocument(version, encoding, standalone);
  }

  public void attribute(@Nullable final CharSequence namespace, @NotNull final CharSequence name, @Nullable final CharSequence prefix, @NotNull final CharSequence value) {
    try {mDelegate.attribute(namespace, name, prefix, value);} catch (XmlException e) {
      throw new RuntimeException(e);
    }
  }

  public void text(final CharSequence text) throws XmlException {
    mDelegate.text(text);
  }

  public NamespaceContext getNamespaceContext() {
    return mDelegate.getNamespaceContext();
  }

  public void close() throws XmlException {
    mDelegate.close();
  }

  public void namespaceAttr(final CharSequence namespacePrefix, final CharSequence namespaceUri) throws XmlException {
    mDelegate.namespaceAttr(namespacePrefix, namespaceUri);
  }

  public void endTag(@Nullable final CharSequence namespace, @NotNull final CharSequence localName, @Nullable final CharSequence prefix) throws XmlException {
    mDelegate.endTag(namespace, localName, prefix);
  }

  public int getDepth() {
    return mDelegate.getDepth();
  }

  public void processingInstruction(final CharSequence text) throws XmlException {
    mDelegate.processingInstruction(text);
  }

  public void docdecl(final CharSequence text) throws XmlException {
    mDelegate.docdecl(text);
  }

  public void comment(final CharSequence text) throws XmlException {
    mDelegate.comment(text);
  }

  public void flush() throws XmlException {
    mDelegate.flush();
  }

  public void entityRef(final CharSequence text) throws XmlException {
    mDelegate.entityRef(text);
  }

  public void cdsect(final CharSequence text) throws XmlException {
    mDelegate.cdsect(text);
  }

  public void ignorableWhitespace(final CharSequence text) throws XmlException {
    mDelegate.ignorableWhitespace(text);
  }

  public void startTag(@Nullable final CharSequence namespace, @NotNull final CharSequence localName, @Nullable final CharSequence prefix) {
    try {mDelegate.startTag(namespace, localName, prefix);} catch (XmlException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public CharSequence getNamespaceUri(@NotNull final CharSequence prefix) throws XmlException {
    return mDelegate.getNamespaceUri(prefix);
  }

  public void endDocument() throws XmlException {
    mDelegate.endDocument();
  }

  @Nullable
  public CharSequence getPrefix(@Nullable final CharSequence namespaceUri) throws XmlException {
    return mDelegate.getPrefix(namespaceUri);
  }
}
