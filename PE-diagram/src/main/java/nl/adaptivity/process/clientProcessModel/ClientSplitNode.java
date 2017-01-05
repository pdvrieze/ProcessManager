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

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


/**
 * A split node for clients. Note that ClientSplitNodes don't have a compat mode. They have multiple succesors
 * and compatibility concerns their absense.
 *
 * @param <NodeT> The type of ProcessNode used.
 */
public class ClientSplitNode<NodeT extends ClientProcessNode<NodeT, ModelT>, ModelT extends ClientProcessModel<NodeT, ModelT>> extends SplitBase<NodeT, ModelT> implements ClientJoinSplit<NodeT, ModelT> {

  public static class Builder<NodeT extends ClientProcessNode<NodeT, ModelT>, ModelT extends ClientProcessModel<NodeT, ModelT>> extends SplitBase.Builder<NodeT, ModelT> implements ClientJoinSplit.Builder<NodeT, ModelT> {

    public Builder() { }

    public Builder(@NotNull final Collection<? extends Identified> predecessors, @NotNull final Collection<? extends Identified> successors, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final int min, final int max) {
      super(id, predecessors, successors, label, defines, results, min, max, x, y);
    }

    public Builder(@NotNull final Split<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public ClientSplitNode<NodeT, ModelT> build(@NotNull final ModelT newOwner) {
      return new ClientSplitNode<NodeT, ModelT>(this, newOwner);
    }

    @Override
    public boolean isCompat() {
      return false;
    }

    @Override
    public void setCompat(final boolean compat) {
      if (compat) throw new IllegalArgumentException("Split nodes cannot be compatible with their own absense");
    }
  }

  public ClientSplitNode(@NotNull final Split.Builder<?, ?> builder, @Nullable final ModelT newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder<NodeT, ModelT> builder() {
    return new Builder<>(this);
  }

  @Override
  public void setId(@Nullable final String id) {
    super.setId(id);
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean isCompat() {
    return false;
  }

  @Override
  public void setOwnerModel(@NotNull final ModelT newOwnerModel) {
    super.setOwnerModel(newOwnerModel);
  }

  @Override
  public void setPredecessors(@NotNull final Collection<? extends Identifiable> predecessors) {
    super.setPredecessors(predecessors);
  }

  @Override
  public void removePredecessor(@NotNull final Identified node) {
    super.removePredecessor(node);
  }

  @Override
  public void addPredecessor(final Identified nodeId) {
    super.addPredecessor(nodeId);
  }

  @Override
  public void addSuccessor(final Identified node) {
    super.addSuccessor(node);
  }

  @Override
  public void removeSuccessor(@NotNull final Identified node) {
    super.removeSuccessor(node);
  }

  @Override
  public void setSuccessors(@NotNull final Collection<? extends Identified> successors) {
    super.setSuccessors(successors);
  }

}
