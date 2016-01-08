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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SingletonNodeList implements NodeList {

  private final Node mNode;

  public SingletonNodeList(final Node node) {
    mNode = node;
  }

  @Nullable
  @Override
  public Node item(final int index) {
    if (index!=0) { return null; }
    return mNode;
  }

  @Override
  public int getLength() {
    return 1;
  }

  @NotNull
  @Override
  public String toString() {
    return "[" + mNode.toString()+"]";
  }

}