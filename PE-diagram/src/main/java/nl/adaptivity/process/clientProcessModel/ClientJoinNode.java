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

import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.JoinBase;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public class ClientJoinNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends JoinBase<T, M> implements ClientJoinSplit<T, M> {

  public static class Builder<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends JoinBase.Builder<T,M> implements ClientJoinSplit.Builder<T,M> {

    private boolean compat;

    public Builder() { }

    public Builder(final boolean compat) {
      this.compat = compat;
    }

    public Builder(@NotNull final Collection<? extends Identified> predecessors, @NotNull final Identified successor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final int min, final int max) {
      super(id, predecessors, successor, label, defines, results, min, max, x, y);
      compat = false;
    }

    public Builder(@NotNull final Join<?, ?> node) {
      super(node);
      if (node instanceof ClientProcessNode) { compat = ((ClientProcessNode) node).isCompat(); }
      else compat = false;
    }

    @NotNull
    @Override
    public ClientJoinNode<T, M> build(final M newOwner) {
      return new ClientJoinNode<T, M>(this, newOwner);
    }

    @Override
    public boolean isCompat() {
      return compat;
    }

    @Override
    public void setCompat(final boolean value) {
      compat = value;
    }
  }


    private final boolean mCompat;

  public ClientJoinNode(final M ownerModel, final boolean compat) {
    super(ownerModel);
    mCompat = compat;
  }

  public ClientJoinNode(final M ownerModel, String id, final boolean compat) {
    super(ownerModel);
    setId(id);
    mCompat = compat;
  }

  protected ClientJoinNode(Join<?,?> orig, final boolean compat) {
    this(orig, null, compat);
  }

  protected ClientJoinNode(Join<?,?> orig, M newOwner, final boolean compat) {
    super(orig.builder(), newOwner);
    mCompat = compat;
  }

  public ClientJoinNode(@NotNull final Builder<?, ?> builder, @NotNull final M newOwnerModel) {
    super(builder, newOwnerModel);
    mCompat = builder.isCompat();
  }

  @NotNull
  @Override
  public Builder<T, M> builder() {
    return new Builder<>(this);
  }

  @Override
  public int getMaxSuccessorCount() {
    return isCompat() ? Integer.MAX_VALUE : 1;
  }

  @Override
  public boolean isCompat() {
    return mCompat;
  }

  @Override
  public void setOwnerModel(@NotNull final M ownerModel) {
    super.setOwnerModel(ownerModel);
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
