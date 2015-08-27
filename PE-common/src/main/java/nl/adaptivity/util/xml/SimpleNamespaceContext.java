package nl.adaptivity.util.xml;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.*;
import java.util.Map.Entry;


/**
 * Created by pdvrieze on 24/08/15.
 */
public class SimpleNamespaceContext implements NamespaceContext {

  private final String[] aPrefixes;
  private final String[] aNamespaces;

  public SimpleNamespaceContext(final Map<String, String> pPrefixMap) {
    aPrefixes = new String[pPrefixMap.size()];
    aNamespaces = new String[aPrefixes.length];
    int i=0;
    for(Entry<String, String> entry: pPrefixMap.entrySet()) {
      aPrefixes[i] = entry.getKey();
      aNamespaces[i] = entry.getValue();
      ++i;
    }
  }

  public SimpleNamespaceContext(final String[] pPrefixes, final String[] pNamespaces) {
    aPrefixes = pPrefixes.clone();
    aNamespaces = pNamespaces.clone();
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    if (prefix==null) { throw new IllegalArgumentException(); }
    switch (prefix) {
      case XMLConstants.XML_NS_PREFIX:
        return XMLConstants.XML_NS_URI;
      case XMLConstants.XMLNS_ATTRIBUTE:
        return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
    }
    for(int i=aPrefixes.length-1; i>=0; --i) {
      if (prefix.equals(aPrefixes[i])) {
        return aNamespaces[i];
      }
    }

    return XMLConstants.NULL_NS_URI;
  }

  @Override
  public String getPrefix(final String namespaceURI) {
    if (namespaceURI==null) { throw new IllegalArgumentException(); }

    switch(namespaceURI) {
      case XMLConstants.XML_NS_URI:
        return XMLConstants.XML_NS_PREFIX;
      case XMLConstants.NULL_NS_URI:
        return XMLConstants.DEFAULT_NS_PREFIX;
      case XMLConstants.XMLNS_ATTRIBUTE_NS_URI:
        return XMLConstants.XMLNS_ATTRIBUTE;
      default:
        for(int i=aPrefixes.length-1; i>=0; --i) {
          if (namespaceURI.equals(aNamespaces[i])) {
            return aPrefixes[i];
          }
        }
    }
    return null;
  }

  @Override
  public Iterator<String> getPrefixes(final String namespaceURI) {
    if (namespaceURI==null) { throw new IllegalArgumentException(); }
    switch(namespaceURI) {
      case XMLConstants.XML_NS_URI:
        return Collections.singleton(XMLConstants.XML_NS_PREFIX).iterator();
      case XMLConstants.XMLNS_ATTRIBUTE_NS_URI:
        return Collections.singleton(XMLConstants.XMLNS_ATTRIBUTE).iterator();
      default:
        List<String> result = new ArrayList<>(aPrefixes.length);
        for(int i=aPrefixes.length-1; i>=0; --i) {
          if (namespaceURI.equals(aNamespaces[i])) {
            result.add(aPrefixes[i]);
          }
        }
        if (result.size()==0) {
          return Collections.emptyIterator();
        }
        return Collections.unmodifiableList(result).iterator();
    }
  }
}
