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

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.HashSet;
import java.util.Iterator;


/**
 * Created by pdvrieze on 28/08/15.
 */
public class CombiningNamespaceContext implements NamespaceContext {

  private final NamespaceContext mPrimary;
  private final NamespaceContext mSecondary;

  public CombiningNamespaceContext(final NamespaceContext primary, final NamespaceContext secondary) {
    mPrimary = primary;
    mSecondary = secondary;
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    String namespaceURI = mPrimary.getNamespaceURI(prefix);
    if (namespaceURI==null || XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
      return mSecondary.getNamespaceURI(prefix);
    }
    return namespaceURI;
  }

  @Override
  public String getPrefix(final String namespaceURI) {
    String prefix = mPrimary.getPrefix(namespaceURI);
    if (prefix == null || (XMLConstants.NULL_NS_URI.equals(namespaceURI)&& XMLConstants.DEFAULT_NS_PREFIX.equals(prefix))) {
      return mSecondary.getPrefix(namespaceURI);
    }
    return prefix;
  }

  @Override
  public Iterator getPrefixes(final String namespaceURI) {
    Iterator<String> prefixes1 = mPrimary.getPrefixes(namespaceURI);
    Iterator<String> prefixes2 = mSecondary.getPrefixes(namespaceURI);
    HashSet prefixes = new HashSet();
    while (prefixes1.hasNext()) {
      prefixes.add(prefixes1.next());
    }
    while (prefixes2.hasNext()) {
      prefixes.add(prefixes2.next());
    }
    return prefixes.iterator();
  }
}
