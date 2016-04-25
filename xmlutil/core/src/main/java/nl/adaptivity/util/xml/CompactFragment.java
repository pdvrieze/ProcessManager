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

import nl.adaptivity.xml.XmlSerializable;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.SimpleNamespaceContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;


/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.
 */
public class CompactFragment implements XmlSerializable {

  public static class Factory implements XmlDeserializerFactory<CompactFragment> {

    @Override
    public CompactFragment deserialize(final XmlReader in) throws XmlException {
      return CompactFragment.deserialize(in);
    }
  }

  public static final Factory FACTORY = new Factory();

  public static CompactFragment deserialize(final XmlReader in) throws XmlException {
    return AbstractXmlReader.siblingsToFragment(in);
  }

  private final nl.adaptivity.xml.SimpleNamespaceContext namespaces;
  private final char[]                                   content;

  public CompactFragment(final Iterable<nl.adaptivity.xml.Namespace> namespaces, final char[] content) {
    this.namespaces = SimpleNamespaceContext.Companion.from(namespaces);
    this.content = content;
  }

  /** Convenience constructor for content without namespaces. */
  public CompactFragment(@NotNull final String string) {
    this(Collections.<nl.adaptivity.xml.Namespace>emptyList(), string.toCharArray());
  }

  public CompactFragment(final CompactFragment orig) {
    namespaces = SimpleNamespaceContext.Companion.from(orig.namespaces);
    content = orig.content;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlReader in = XMLFragmentStreamReader.from(this);
    XmlUtil.serialize(in, out);
    in.close();
  }

  public SimpleNamespaceContext getNamespaces() {
    return namespaces;
  }

  public char[] getContent() {
    return content;
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final CompactFragment that = (CompactFragment) o;

    if (!namespaces.equals(that.namespaces)) return false;
    return Arrays.equals(content, that.content);

  }

  @Override
  public int hashCode() {
    int result = namespaces.hashCode();
    result = 31 * result + Arrays.hashCode(content);
    return result;
  }

  @NotNull
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CompactFragment{");
    sb.append("namespaces=[");
    {
      final int nsCount = namespaces.size();
      for(int i = 0; i< nsCount; ++i) {
        if (i>0) { sb.append(", "); }
        sb.append(namespaces.getPrefix(i)).append(" -> ").append(namespaces.getNamespaceURI(i));
      }
    }
    sb.append(']');
    if (content!=null) {
      sb.append(", content=").append(new String(content));
    }
    sb.append('}');
    return sb.toString();
  }

  @NotNull
  public String getContentString() {
    return new String(getContent());
  }
}
