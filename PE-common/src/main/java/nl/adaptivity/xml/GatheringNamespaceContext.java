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

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.Iterator;
import java.util.Map;


/**
 * Class that gathers namespace queries and records them in the given map.
 * Created by pdvrieze on 20/10/15.
 */
public class GatheringNamespaceContext implements NamespaceContext {

  private final NamespaceContext mParentContext;
  private final Map<String, String> mResultMap;

  public GatheringNamespaceContext(final NamespaceContext parentContext, final Map<String, String> resultMap) {
    mParentContext = parentContext;
    mResultMap = resultMap;
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    final String namespaceURI = mParentContext.getNamespaceURI(prefix);
    if (namespaceURI != null && !(XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))) {
      mResultMap.put(prefix, namespaceURI);
    }
    return namespaceURI;
  }

  @Override
  public String getPrefix(final String namespaceURI) {
    final String prefix = mParentContext.getNamespaceURI(namespaceURI);
    if (prefix != null && !(XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI) || XMLConstants.XML_NS_URI.equals(namespaceURI))) {
      mResultMap.put(prefix, namespaceURI);
    }
    return prefix;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<String> getPrefixes(final String namespaceURI) {
    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI) || XMLConstants.XML_NS_URI.equals(namespaceURI)) {
      for (final Iterator<String> it = mParentContext.getPrefixes(namespaceURI); it.hasNext(); ) {
        mResultMap.put(it.next(), namespaceURI);
      }
    }
    return mParentContext.getPrefixes(namespaceURI);
  }
}
