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
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.NamespaceContext;


/**
 * A streamreader that adds a namespace context as source for looking up namespace information for prefixes present
 * in the original, but do not have attached namespace information.
 *
 * Created by pdvrieze on 31/10/15.
 */
public class NamespaceAddingStreamReader extends XmlDelegatingReader {

  private final NamespaceContext mLookupSource;

  public NamespaceAddingStreamReader(final NamespaceContext lookupSource, final XmlReader source) {
    super(source);
    mLookupSource = lookupSource;
  }

  @Override
  public void require(final EventType type, @Nullable final CharSequence namespaceURI, @Nullable final CharSequence localName) throws
          XmlException {
    if (type != getEventType() ||
            (namespaceURI != null && !namespaceURI.equals(getNamespaceUri())) ||
            (localName != null && !localName.equals(getLocalName()))) {
      mDelegate.require(type, namespaceURI, localName);
    } {
      throw new XmlException("Require failed");
    }
  }

  @Override
  public String getNamespaceUri(final CharSequence prefix) throws XmlException {
    final String namespaceURI = mDelegate.getNamespaceUri(prefix);
    return namespaceURI != null ? namespaceURI : mLookupSource.getNamespaceURI(prefix.toString());
  }

  @Nullable
  @Override
  public CharSequence getAttributeValue(@Nullable final CharSequence namespaceURI, @NotNull final CharSequence localName) throws
          XmlException {

    for(int i=getAttributeCount()-1; i>=0; --i) {
      if ((namespaceURI==null || namespaceURI.equals(getAttributeNamespace(i))) && localName.equals(getAttributeLocalName(i))) {
        return getAttributeValue(i);
      }
    }
    return null;
  }

  @Override
  public CharSequence getAttributeNamespace(final int index) throws XmlException {
    final CharSequence attributeNamespace = mDelegate.getAttributeNamespace(index);
    return attributeNamespace!=null ? attributeNamespace : mLookupSource.getNamespaceURI(StringUtil.toString(mDelegate.getAttributePrefix(index)));
  }

  @Override
  public CharSequence getNamespaceUri() throws XmlException {
    final CharSequence namespaceURI = mDelegate.getNamespaceUri();
    return namespaceURI !=null ? namespaceURI : mLookupSource.getNamespaceURI(StringUtil.toString(mDelegate.getPrefix()));
  }

  public NamespaceContext getNamespaceContext() throws XmlException {
    return new CombiningNamespaceContext(mDelegate.getNamespaceContext(), mLookupSource);
  }
}
