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

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.EndNodeBase;


public class ClientEndNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends EndNodeBase<T, M> implements EndNode<T, M>, ClientProcessNode<T, M> {

  public ClientEndNode(final M ownerModel) {
    super(ownerModel);
  }

  public ClientEndNode(final M ownerModel, String id) {
    super(ownerModel);
    setId(id);
  }

  protected ClientEndNode(EndNode<?, ?> orig) {
    super(orig);
  }

  @Override
  public boolean isCompat() {
    return false;
  }
}
