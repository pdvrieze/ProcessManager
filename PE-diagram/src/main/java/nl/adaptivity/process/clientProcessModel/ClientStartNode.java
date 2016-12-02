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

import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.StartNode.Builder;
import nl.adaptivity.process.processModel.StartNodeBase;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public class ClientStartNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends StartNodeBase<T, M> implements ClientProcessNode<T, M> {

  public static class Builder<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends StartNodeBase.Builder<T,M> implements ClientProcessNode.Builder<T,M> {

    public Builder(@Nullable final Identifiable successor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results) {
      super(successor, id, label, x, y, defines, results);
    }

    public Builder(@NotNull final StartNode<?, ?> node) {
      super(node);
      if (node instanceof ClientStartNode) {
        compat = ((ClientStartNode) node).isCompat();
      } else {
        compat = false;
      }
    }

    @NotNull
    @Override
    public ClientStartNode<T, M> build(@NotNull final M newOwner) {
      return new ClientStartNode<T, M>(this, newOwner);
    }

    @Override
    public boolean isCompat() {
      return compat;
    }

    @Override
    public void setCompat(final boolean compat) {
      this.compat = compat;
    }

    public boolean compat = false;
  }

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
    super(orig, null);
    mCompat = compat;
  }

  public ClientStartNode(@NotNull final StartNode.Builder<?, ?> builder, @NotNull final M newOwnerModel) {
    super(builder, newOwnerModel);
    if (builder instanceof Builder) {
      mCompat = ((Builder) builder).compat;
    } else {
      mCompat = false;
    }
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
  public void resolveRefs() {
    super.resolveRefs();
  }

  @Override
  public void setPredecessors(final Collection<? extends Identifiable> predecessors) {
    super.setPredecessors(predecessors);
  }

  @Override
  public void removePredecessor(final Identifiable node) {
    super.removePredecessor(node);
  }

  @Override
  public void addPredecessor(final Identifiable node) {
    super.addPredecessor(node);
  }

  @Override
  public void addSuccessor(final Identifiable node) {
    super.addSuccessor(node);
  }

  @Override
  public void removeSuccessor(final Identifiable node) {
    super.removeSuccessor(node);
  }

  @Override
  public void setSuccessors(final Collection<? extends Identifiable> successors) {
    super.setSuccessors(successors);
  }

}
