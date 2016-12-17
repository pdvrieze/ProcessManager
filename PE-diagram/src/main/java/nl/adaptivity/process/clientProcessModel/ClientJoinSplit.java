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


import nl.adaptivity.process.processModel.JoinSplit;
import nl.adaptivity.process.processModel.JoinSplit.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface ClientJoinSplit<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends JoinSplit<T, M>, ClientProcessNode<T, M> {

  interface Builder<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends JoinSplit.Builder<T,M>, ClientProcessNode.Builder<T,M> {

    @NotNull
    @Override
    ClientJoinSplit<T, M> build(M newOwner);
  }

  @NotNull
  @Override
  Builder<T, M> builder();
}