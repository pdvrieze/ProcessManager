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

package nl.adaptivity.xml;

import nl.adaptivity.io.Writable;
import nl.adaptivity.util.xml.CompactFragment;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;


/**
 * Created by pdvrieze on 27/11/15.
 */
public class WritableCompactFragment implements CompactFragment, Writable {
  private final CompactFragment data;

  public WritableCompactFragment(final Iterable<Namespace> namespaces, final char[] content) {
    data = XmlStreamingKt.CompactFragment(namespaces, content);
  }

  public WritableCompactFragment(@NotNull final String string) {
    data = XmlStreamingKt.CompactFragment(string);
  }

  public WritableCompactFragment(@NotNull final CompactFragment orig) {
    data = XmlStreamingKt.CompactFragment(orig.getNamespaces(), orig.getContentString());
  }

  @Override
  public void writeTo(final Writer destination) throws IOException {
    destination.write(getContent());
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  @NotNull
  @Override
  public SimpleNamespaceContext getNamespaces() {
    return data.getNamespaces();
  }

  @NotNull
  @Override
  public char[] getContent() {
    return data.getContent();
  }

  @NotNull
  @Override
  public String getContentString() {
    return data.getContentString();
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    data.serialize(out);
  }
}
