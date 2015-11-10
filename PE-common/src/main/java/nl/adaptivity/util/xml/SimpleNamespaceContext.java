package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    @Override
    public Namespace next() {
      final SimpleNamespace result = new SimpleNamespace(pos);
      pos+=2;
      return result;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Read only iterator");
    }
  }

  private class SimpleNamespace implements Namespace {

    private final int pos;

    public SimpleNamespace(final int pos) {
      this.pos = pos;
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

  public SimpleNamespaceContext(@NotNull final Map<String, String> prefixMap) {
    aStrings = new String[prefixMap.size()*2];
    int i=0;
    for(final Entry<String, String> entry: prefixMap.entrySet()) {
      aStrings[(i*2)] = entry.getKey();
      aStrings[(i*2+1)] = entry.getValue();
      ++i;
    }
  }

  public SimpleNamespaceContext(@NotNull final String[] prefixes, @NotNull final String[] namespaces) {
    assert prefixes.length==namespaces.length;
    aStrings = new String[prefixes.length*2];
    for(int i=0; i<prefixes.length; ++i) {
      aStrings[(i*2)] = prefixes[i];
      aStrings[(i*2+1)] = namespaces[i];
    }
  }

  private SimpleNamespaceContext(final String[] strings) {
    aStrings = strings;
  }

  public SimpleNamespaceContext(final Iterable<Namespace> namespaces) {
    if (namespaces instanceof Collection) {
      final int len = ((Collection) namespaces).size();
      aStrings = new String[len*2];
      int i=0;
      for(final Namespace ns:namespaces) {
        aStrings[i++] = ns.getPrefix();
        aStrings[i++] = ns.getNamespaceURI();
      }
    } else {
      final ArrayList<String> intermediate = new ArrayList<>();
      for(final Namespace ns: namespaces) {
        intermediate.add(ns.getPrefix());
        intermediate.add(ns.getNamespaceURI());
      }
      aStrings = intermediate.toArray(new String[intermediate.size()]);
    }
  }

  public int size() {
    return aStrings.length/2;
  }

  @NotNull
  public SimpleNamespaceContext combine(@NotNull final SimpleNamespaceContext other) {
    final Map<String, String> result = new TreeMap<>();
    for(int i=(aStrings.length/2)-1; i>=0; --i) { result.put(aStrings[i*2], aStrings[i*2+1]); }
    for(int i=(other.aStrings.length/2)-1; i>=0; --i) { result.put(other.aStrings[i*2], other.aStrings[i*2+1]); }
    return new SimpleNamespaceContext(result);
  }

  /**
   * Combine this context with the additional context. The prefixes already in this context prevail over the added ones.
   * @param other The namespaces to add
   * @return the new context
   */
  @Nullable
  public SimpleNamespaceContext combine(@Nullable final Iterable<Namespace> other) {
    if (aStrings.length==0) {
      return from(other);
    }
    if (other==null || ! (other.iterator().hasNext())) {
      return this;
    }
    final Map<String, String> result = new TreeMap<>();
    for(int i=(aStrings.length/2)-1; i>=0; --i) { result.put(aStrings[i*2], aStrings[i*2+1]); }
    if (other instanceof SimpleNamespaceContext) {
      final SimpleNamespaceContext snother = (SimpleNamespaceContext) other;
      for (int i = (snother.aStrings.length / 2) - 1; i >= 0; --i) {
        result.put(snother.aStrings[i * 2], snother.aStrings[i * 2 + 1]);
      }
    } else {
      for(final Namespace ns: other) {
        result.put(ns.getPrefix(), ns.getNamespaceURI());
      }
    }
    return new SimpleNamespaceContext(result);

  }

  @Nullable
  public static SimpleNamespaceContext from(final Iterable<Namespace> originalNSContext) {
    if (originalNSContext instanceof SimpleNamespaceContext) {
      return (SimpleNamespaceContext) originalNSContext;
    } else if (originalNSContext==null) {
      return null;
    } else {
      return new SimpleNamespaceContext(originalNSContext);
    }
  }

  @Override
  public String getNamespaceURI(@Nullable final String prefix) {
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

  @Nullable
  @Override
  public String getPrefix(@Nullable final String namespaceURI) {
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
  public Iterator<String> getPrefixes(@Nullable final String namespaceURI) {
    if (namespaceURI==null) { throw new IllegalArgumentException(); }
    switch(namespaceURI) {
      case XMLConstants.XML_NS_URI:
        return Collections.singleton(XMLConstants.XML_NS_PREFIX).iterator();
      case XMLConstants.XMLNS_ATTRIBUTE_NS_URI:
        return Collections.singleton(XMLConstants.XMLNS_ATTRIBUTE).iterator();
      default:
        final List<String> result = new ArrayList<>(aStrings.length/2);
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

  public String getPrefix(final int index) {
    try {
      return aStrings[index * 2];
    } catch (@NotNull final ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
  }

  public String getNamespaceURI(final int index) {
    try {
      return aStrings[index * 2 + 1];
    } catch (@NotNull final ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
  }

  @NotNull
  @Override
  public Iterator<Namespace> iterator() {
    return new SimpleIterator();
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final SimpleNamespaceContext that = (SimpleNamespaceContext) o;

    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    return Arrays.equals(aStrings, that.aStrings);

  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(aStrings);
  }
}
