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
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


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
