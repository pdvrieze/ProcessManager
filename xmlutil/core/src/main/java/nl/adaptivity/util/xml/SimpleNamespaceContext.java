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

import nl.adaptivity.xml.Namespace;
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
      return pos<mStrings.length;
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
      return mStrings[pos];
    }

    @Override
    public String getNamespaceURI() {
      return mStrings[pos+1];
    }
  }

  private final String[] mStrings;

  public SimpleNamespaceContext(@NotNull final Map<? extends CharSequence, ? extends CharSequence> prefixMap) {
    mStrings = new String[prefixMap.size()*2];
    int i=0;
    for(final Entry<? extends CharSequence, ? extends CharSequence> entry: prefixMap.entrySet()) {
      mStrings[(i*2)] = entry.getKey().toString();
      final String nsUri = entry.getValue().toString();
      if (nsUri==null || nsUri.isEmpty()) { throw new IllegalArgumentException("Null namespaces are illegal"); }
      mStrings[(i * 2 + 1)] = nsUri;
      ++i;
    }
  }

  public SimpleNamespaceContext(@NotNull final CharSequence[] prefixes, @NotNull final CharSequence[] namespaces) {
    assert prefixes.length==namespaces.length;
    mStrings = new String[prefixes.length*2];
    for(int i=0; i<prefixes.length; ++i) {
      mStrings[(i*2)] = prefixes[i].toString();
      mStrings[(i*2+1)] = namespaces[i].toString();
    }
  }

  public SimpleNamespaceContext(@NotNull final CharSequence prefix, @NotNull final CharSequence namespace) {
    mStrings=new String[2];
    mStrings[0] = prefix.toString();
    mStrings[1] = namespace.toString();
  }

  SimpleNamespaceContext(final String[] strings) {
    mStrings = strings;
  }

  public SimpleNamespaceContext(final Iterable<Namespace> namespaces) {
    if (namespaces instanceof Collection) {
      final int len = ((Collection) namespaces).size();
      mStrings = new String[len*2];
      int i=0;
      for(final Namespace ns:namespaces) {
        mStrings[i++] = ns.getPrefix().toString();
        mStrings[i++] = ns.getNamespaceURI().toString();
      }
    } else {
      final ArrayList<String> intermediate = new ArrayList<>();
      for(final Namespace ns: namespaces) {
        intermediate.add(ns.getPrefix().toString());
        intermediate.add(ns.getNamespaceURI().toString());
      }
      mStrings = intermediate.toArray(new String[intermediate.size()]);
    }
  }

  public int size() {
    return mStrings.length/2;
  }

  @NotNull
  public SimpleNamespaceContext combine(@NotNull final SimpleNamespaceContext other) {
    final Map<String, String> result = new TreeMap<>();
    for(int i=(mStrings.length/2)-1; i>=0; --i) { result.put(mStrings[i*2], mStrings[i*2+1]); }
    for(int i=(other.mStrings.length/2)-1; i>=0; --i) { result.put(other.mStrings[i*2], other.mStrings[i*2+1]); }
    return new SimpleNamespaceContext(result);
  }

  /**
   * Combine this context with the additional context. The prefixes already in this context prevail over the added ones.
   * @param other The namespaces to add
   * @return the new context
   */
  @Nullable
  public SimpleNamespaceContext combine(@Nullable final Iterable<Namespace> other) {
    if (mStrings.length==0) {
      return from(other);
    }
    if (other==null || ! (other.iterator().hasNext())) {
      return this;
    }
    final Map<String, String> result = new TreeMap<>();
    for(int i=(mStrings.length/2)-1; i>=0; --i) { result.put(mStrings[i*2], mStrings[i*2+1]); }
    if (other instanceof SimpleNamespaceContext) {
      final SimpleNamespaceContext snother = (SimpleNamespaceContext) other;
      for (int i = (snother.mStrings.length / 2) - 1; i >= 0; --i) {
        result.put(snother.mStrings[i * 2], snother.mStrings[i * 2 + 1]);
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
    for(int i=mStrings.length-2; i>=0; i-=2) { // Should be backwards to allow overrriding
      if (prefix.equals(mStrings[i])) {
        return mStrings[i+1];
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
        for(int i=mStrings.length-2; i>=0; i-=2) {// Should be backwards to allow overrriding
          if (namespaceURI.equals(mStrings[i+1])) {
            return mStrings[i];
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
        final List<String> result = new ArrayList<>(mStrings.length/2);
        for(int i=mStrings.length-2; i>=0; i-=2) {// Should be backwards to allow overrriding
          if (namespaceURI.equals(mStrings[i+1])) {
            result.add(mStrings[i]);
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
      return mStrings[index * 2];
    } catch (@NotNull final ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
  }

  public String getNamespaceURI(final int index) {
    try {
      return mStrings[index * 2 + 1];
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
    return Arrays.equals(mStrings, that.mStrings);

  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(mStrings);
  }
}
