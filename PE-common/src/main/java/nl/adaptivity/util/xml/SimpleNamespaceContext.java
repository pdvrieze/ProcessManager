package nl.adaptivity.util.xml;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.*;
import java.util.Map.Entry;


/**
 * Created by pdvrieze on 24/08/15.
 */
public class SimpleNamespaceContext implements NamespaceContext {

  private final String[] aStrings;

  public SimpleNamespaceContext(final Map<String, String> pPrefixMap) {
    aStrings = new String[pPrefixMap.size()*2];
    int i=0;
    for(Entry<String, String> entry: pPrefixMap.entrySet()) {
      aStrings[(i*2)] = entry.getKey();
      aStrings[(i*2+1)] = entry.getValue();
      ++i;
    }
  }

  public SimpleNamespaceContext(final String[] pPrefixes, final String[] pNamespaces) {
    assert pPrefixes.length==pNamespaces.length;
    aStrings = new String[pPrefixes.length*2];
    for(int i=0; i<pPrefixes.length; ++i) {
      aStrings[(i*2)] = pPrefixes[i];
      aStrings[(i*2+1)] = pNamespaces[i];
    }
  }

  public int size() {
    return aStrings.length/2;
  }

  public SimpleNamespaceContext combine(final SimpleNamespaceContext other) {
    Map<String, String> result = new TreeMap<>();
    for(int i=(aStrings.length/2)-1; i>=0; --i) { result.put(aStrings[i*2], aStrings[i*2+1]); }
    for(int i=(other.aStrings.length/2)-1; i>=0; --i) { result.put(other.aStrings[i*2], other.aStrings[i*2+1]); }
    return new SimpleNamespaceContext(result);
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
    for(int i=aStrings.length-2; i>=0; i-=2) {
      if (prefix.equals(aStrings[i])) {
        return aStrings[i+1];
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
        for(int i=aStrings.length-2; i>=0; i-=2) {
          if (namespaceURI.equals(aStrings[i+1])) {
            return aStrings[i];
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
        List<String> result = new ArrayList<>(aStrings.length/2);
        for(int i=aStrings.length-2; i>=0; i-=2) {
          if (namespaceURI.equals(aStrings[i+1])) {
            result.add(aStrings[i]);
          }
        }
        if (result.size()==0) {
          return Collections.emptyIterator();
        }
        return Collections.unmodifiableList(result).iterator();
    }
  }

  public String getPrefix(final int pIndex) {
    try {
      return aStrings[pIndex * 2];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(pIndex);
    }
  }

  public String getNamespaceURI(final int pIndex) {
    try {
      return aStrings[pIndex * 2 + 1];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(pIndex);
    }
  }
}
