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
import nl.adaptivity.util.CombiningReader;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.io.CharArrayReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper elements, and then ignoring those on reading.
 *
 * Created by pdvrieze on 04/11/15.
 */
public class XMLFragmentStreamReader extends XmlDelegatingReader {

  private static class FragmentNamespaceContext extends SimpleNamespaceContext {

    private final FragmentNamespaceContext mParent;

    public FragmentNamespaceContext(final FragmentNamespaceContext parent, @NotNull final String[] prefixes, @NotNull final String[] namespaces) {
      super(prefixes, namespaces);
      mParent = parent;
    }

    @Override
    public String getNamespaceURI(final String prefix) {
      final String namespaceURI = super.getNamespaceURI(prefix);
      if (namespaceURI==null && mParent!=null) {
        return mParent.getNamespaceURI(prefix);
      }
      return namespaceURI;
    }

    @Nullable
    @Override
    public String getPrefix(final String namespaceURI) {

      final String prefix = super.getPrefix(namespaceURI);
      if (prefix==null && mParent!=null) { return mParent.getPrefix(namespaceURI); }
      return prefix;

    }

    @Override
    public Iterator<String> getPrefixes(final String namespaceURI) {
      if (mParent==null) { return super.getPrefixes(namespaceURI); }
      final Set<String> prefixes = new HashSet<>();

      for(final Iterator<String> it = super.getPrefixes(namespaceURI); it.hasNext();) {
        prefixes.add(it.next());
      }

      for(final Iterator<String> it = mParent.getPrefixes(namespaceURI); it.hasNext();) {
        final String prefix = it.next();
        final String localNamespaceUri = getLocalNamespaceUri(prefix);
        if (localNamespaceUri==null) {
          prefixes.add(prefix);
        }
      }

      return prefixes.iterator();
    }

    private String getLocalNamespaceUri(@NotNull final String prefix) {
      for(int i=size()-1; i>=0; --i) {
        if (prefix.equals(getPrefix(i))) {
          return getNamespaceURI(i);
        }
      }
      return null;
    }
  }

  private static final String WRAPPERPPREFIX = "SDFKLJDSF";
  private static final String WRAPPERNAMESPACE = "http://wrapperns";
  @Nullable private FragmentNamespaceContext localNamespaceContext;

  public XMLFragmentStreamReader(final Reader in, @NotNull final Iterable<Namespace> wrapperNamespaceContext) throws XmlException {
    super(getDelegate(in, wrapperNamespaceContext));
    localNamespaceContext = new FragmentNamespaceContext(null, new String[0], new String[0]);
  }

  private static XmlReader getDelegate(final Reader in, final @NotNull Iterable<Namespace> wrapperNamespaceContext) throws
          XmlException {
    final StringBuilder wrapperBuilder = new StringBuilder();
    wrapperBuilder.append("<" + WRAPPERPPREFIX + ":wrapper xmlns:" + WRAPPERPPREFIX + "=\"" + WRAPPERNAMESPACE+'"');
    for(final Namespace ns:wrapperNamespaceContext) {
      final String prefix = ns.getPrefix();
      final String uri = ns.getNamespaceURI();
      if (prefix==null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
        wrapperBuilder.append(" xmlns");
      } else {
        wrapperBuilder.append(" xmlns:").append(prefix);
      }
      wrapperBuilder.append("=\"").append(XmlUtil.xmlEncode(uri)).append('"');
    }
    wrapperBuilder.append(" >");

    final String wrapper = wrapperBuilder.toString();
    final Reader actualInput = new CombiningReader(new StringReader(wrapper), in, new StringReader("</" + WRAPPERPPREFIX + ":wrapper>"));
    return XmlStreaming.newReader(actualInput);
  }

  @NotNull
  public static XMLFragmentStreamReader from(final Reader in, @NotNull final Iterable<Namespace> namespaceContext) throws
          XmlException {
    return new XMLFragmentStreamReader(in, namespaceContext);
  }

  @NotNull
  public static XMLFragmentStreamReader from(final Reader in) throws
          XmlException {
    return new XMLFragmentStreamReader(in, Collections.<Namespace>emptyList());
  }

  @NotNull
  public static XMLFragmentStreamReader from(@NotNull final CompactFragment fragment) throws XmlException {
    return new XMLFragmentStreamReader(new CharArrayReader(fragment.getContent()), fragment.getNamespaces());
  }

  @Override
  public EventType next() throws XmlException {
    final EventType result = mDelegate.next();
    if (result==null) { return null; }
    switch (result) {
      case END_DOCUMENT:
        return result;
      case START_DOCUMENT:
      case PROCESSING_INSTRUCTION:
      case DOCDECL:
        return next();
      case START_ELEMENT:
        if (StringUtil.isEqual(WRAPPERNAMESPACE, mDelegate.getNamespaceUri())) {
          return mDelegate.next();
        }
        extendNamespace();
        break;
      case END_ELEMENT:
        if (StringUtil.isEqual(WRAPPERNAMESPACE, mDelegate.getNamespaceUri())) {
          return mDelegate.next();
        }
        localNamespaceContext = localNamespaceContext.mParent;
        break;
    }
    return result;
  }

  @Override
  public String getNamespaceUri(final CharSequence prefix) throws XmlException {
    if (StringUtil.isEqual(WRAPPERPPREFIX, prefix)) { return null; }
    return super.getNamespaceUri(prefix);
  }

  @Override
  public CharSequence getNamespacePrefix(final CharSequence namespaceUri) throws XmlException {
    if (StringUtil.isEqual(WRAPPERNAMESPACE, namespaceUri)) { return null; }
    return super.getNamespacePrefix(namespaceUri);
  }

  @Override
  public int getNamespaceStart() throws XmlException {
    return 0;
  }

  @Override
  public int getNamespaceEnd() throws XmlException {
    return localNamespaceContext.size();
  }

  @Override
  public CharSequence getNamespacePrefix(final int i) throws XmlException {
    return localNamespaceContext.getPrefix(i);
  }

  @Override
  public CharSequence getNamespaceUri(final int i) throws XmlException {
    return localNamespaceContext.getNamespaceURI(i);
  }

  @Override
  public NamespaceContext getNamespaceContext() throws XmlException {
    return localNamespaceContext;
  }

  private void extendNamespace() throws XmlException {
    int nsEnd = mDelegate.getNamespaceEnd();
    int nsStart = mDelegate.getNamespaceStart();
    final int nscount = nsEnd - nsStart;
    final String[] prefixes = new String[nscount];
    final String[] namespaces = new String[nscount];
    int j = 0;
    for(int i = nsStart; i<nsEnd; ++i, ++j) {
      prefixes[j] = StringUtil.toString(mDelegate.getNamespacePrefix(i));
      namespaces[j] = StringUtil.toString(mDelegate.getNamespaceUri(i));
    }
    localNamespaceContext = new FragmentNamespaceContext(localNamespaceContext, prefixes, namespaces);
  }


}
