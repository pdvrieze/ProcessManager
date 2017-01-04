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


public class ClientEndNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends EndNodeBase<T, M> implements EndNode<T, M>, ClientProcessNode<T, M> {

  public static class Builder<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends EndNodeBase.Builder<T,M> implements ClientProcessNode.Builder<T,M> {

    public Builder() {}

    public Builder(final String id) {
      setId(id);
    }

    public Builder(@Nullable final String id, @Nullable final Identified predecessor, @Nullable final String label, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final double x, final double y) {
      super(id, predecessor, label, defines, results, x, y);
    }

    public Builder(@NotNull final EndNode<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public ClientEndNode<T, M> build(@NotNull final ModelCommon<T, M> newOwner) {
      return new ClientEndNode<T, M>(this, newOwner);
    }

    @Override
    public boolean isCompat() {
      return false;
    }

    @Override
    public void setCompat(final boolean compat) {
      if (compat) throw new IllegalArgumentException("Compatibility not supported on end nodes.");
    }

  }

  public ClientEndNode(final ModelCommon<T,M> ownerModel) {
    super(new Builder(), ownerModel);
  }

  public ClientEndNode(final ModelCommon<T,M> ownerModel, String id) {
    super(new Builder<>(id), ownerModel);
    setId(id);
  }

  protected ClientEndNode(EndNode<?, ?> orig) {
    super(orig.builder(), null);
  }

  public ClientEndNode(@NotNull final EndNode.Builder<?, ?> builder, @NotNull final ModelCommon<T,M> newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder<T, M> builder() {
    return new Builder<>(this);
  }


  @Override
  public boolean isCompat() {
    return false;
  }


  @Override
  public void setOwnerModel(@NotNull final ModelCommon<T, M> newOwnerModel) {
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
