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

import nl.adaptivity.io.Writable;
import nl.adaptivity.util.xml.CompactFragment;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;


/**
 * Created by pdvrieze on 27/11/15.
 */
public class WritableCompactFragment extends CompactFragment implements Writable {

  public WritableCompactFragment(final Iterable<Namespace> namespaces, final char[] content) {
    super(namespaces, content);
  }

  public WritableCompactFragment(@NotNull final String string) {
    super(string);
  }

  public WritableCompactFragment(@NotNull final CompactFragment orig) {
    super(orig);
  }

  @Override
  public void writeTo(final Writer destination) throws IOException {
    destination.write(getContent());
  }
}
