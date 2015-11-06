package nl.adaptivity.util.xml;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.*;
import java.util.Map.Entry;


/**
 * Created by pdvrieze on 24/08/15.
 */
public class SimpleNamespaceContext implements NamespaceContext, Iterable<Namespace> {

  private class SimpleIterator implements Iterator<Namespace> {
    private int pos=0;

    @Override
    public boolean hasNext() {
      return pos<aStrings.length;
    }

    @Override
    public Namespace next() {
      SimpleNamespace result = new SimpleNamespace(pos);
      pos+=2;
      return result;
    }


  }

  private class SimpleNamespace implements Namespace {

    private int pos;

    public SimpleNamespace(final int pPos) {
      pos = pPos;
    }

    @Override
    public String getPrefix() {
      return aStrings[pos];
    }

    @Override
    public String getNamespaceURI() {
      return aStrings[pos+1];
    }
  }

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

  private SimpleNamespaceContext(final String[] pStrings) {
    aStrings = pStrings;
  }

  private SimpleNamespaceContext(final Iterable<Namespace> pNamespaces) {
    if (pNamespaces instanceof Collection) {
      int len = ((Collection) pNamespaces).size();
      aStrings = new String[len*2];
      int i=0;
      for(Namespace ns:pNamespaces) {
        aStrings[i++] = ns.getPrefix();
        aStrings[i++] = ns.getNamespaceURI();
      }
    } else {
      ArrayList<String> intermediate = new ArrayList<>();
      for(Namespace ns: pNamespaces) {
        intermediate.add(ns.getPrefix());
        intermediate.add(ns.getNamespaceURI());
      }
      aStrings = intermediate.toArray(new String[intermediate.size()]);
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

  /**
   * Combine this context with the additional context. The prefixes already in this context prevail over the added ones.
   * @param other The namespaces to add
   * @return the new context
   */
  public SimpleNamespaceContext combine(final Iterable<Namespace> other) {
    if (aStrings.length==0) {
      return from(other);
    }
    if (other==null || ! (other.iterator().hasNext())) {
      return this;
    }
    Map<String, String> result = new TreeMap<>();
    for(int i=(aStrings.length/2)-1; i>=0; --i) { result.put(aStrings[i*2], aStrings[i*2+1]); }
    if (other instanceof SimpleNamespaceContext) {
      SimpleNamespaceContext snother = (SimpleNamespaceContext) other;
      for (int i = (snother.aStrings.length / 2) - 1; i >= 0; --i) {
        result.put(snother.aStrings[i * 2], snother.aStrings[i * 2 + 1]);
      }
    } else {
      for(Namespace ns: other) {
        result.put(ns.getPrefix(), ns.getNamespaceURI());
      }
    }
    return new SimpleNamespaceContext(result);

  }

  public static SimpleNamespaceContext from(final Iterable<Namespace> pOriginalNSContext) {
    if (pOriginalNSContext instanceof SimpleNamespaceContext) {
      return (SimpleNamespaceContext) pOriginalNSContext;
    } else {
      return new SimpleNamespaceContext(pOriginalNSContext);
    }
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

  @Override
  public Iterator<Namespace> iterator() {
    return new SimpleIterator();
  }
}
