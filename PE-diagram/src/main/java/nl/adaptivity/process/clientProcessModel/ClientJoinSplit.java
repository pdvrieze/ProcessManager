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

package nl.adaptivity.process.clientProcessModel;


import nl.adaptivity.process.processModel.JoinSplit;
import org.jetbrains.annotations.NotNull;


public interface ClientJoinSplit<NodeT extends ClientProcessNode<NodeT, ModelT>, ModelT extends ClientProcessModel<NodeT, ModelT>> extends JoinSplit<NodeT, ModelT>, ClientProcessNode<NodeT, ModelT> {

  interface Builder<NodeT extends ClientProcessNode<NodeT, ModelT>, ModelT extends ClientProcessModel<NodeT, ModelT>> extends JoinSplit.Builder<NodeT, ModelT>, ClientProcessNode.Builder<NodeT, ModelT> {

    @NotNull
    @Override
    ClientJoinSplit<NodeT, ModelT> build(@NotNull ModelT newOwner);
  }

  @NotNull
  @Override
  Builder<NodeT, ModelT> builder();
}