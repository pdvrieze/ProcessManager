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

package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.StartNodeBase;

public class ClientStartNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends StartNodeBase<T, M> implements ClientProcessNode<T, M> {

  private final boolean mCompat;

  public ClientStartNode(final M ownerModel, final boolean compat) {
    super(ownerModel);
    mCompat = compat;
  }

  public ClientStartNode(final M ownerModel, final String id, final boolean compat) {
    super(ownerModel);
    setId(id);
    mCompat = compat;
  }

  protected ClientStartNode(final ClientStartNode<T, M> orig, final boolean compat) {
    super(orig);
    mCompat = compat;
  }

  @Override
  public int getMaxSuccessorCount() {
    return isCompat() ? Integer.MAX_VALUE : 1;
  }

  @Override
  public boolean isCompat() {
    return mCompat;
  }
}
