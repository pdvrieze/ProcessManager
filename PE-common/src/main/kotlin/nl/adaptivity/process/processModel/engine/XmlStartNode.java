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

package nl.adaptivity.process.processModel.engine;

import nl.adaptivity.process.processModel.ProcessModel.BuildHelper;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.StartNodeBase;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;


public class XmlStartNode extends StartNodeBase<XmlProcessNode,XmlModelCommon> implements XmlProcessNode {

  public static class Builder extends StartNodeBase.Builder<XmlProcessNode, XmlModelCommon> implements XmlProcessNode.Builder {

    public Builder() { }

    public Builder(StartNode base) {
      super(base);
    }

    @NotNull
    @Override
    public XmlStartNode build(@NotNull final BuildHelper<XmlProcessNode, XmlModelCommon> buildHelper) {
      return new XmlStartNode(this, buildHelper);
    }
  }

  public XmlStartNode(@NotNull final StartNode.Builder<?, ?> builder, final BuildHelper<XmlProcessNode, XmlModelCommon> buildHelper) {
    super(builder, buildHelper);
  }

  public XmlStartNode(final @Nullable XmlProcessModel ownerModel) {
    super(ownerModel);
  }

  public XmlStartNode(final @Nullable XmlProcessModel ownerModel, final List<XmlResultType> imports) {
    super(ownerModel);
    setResults(imports);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @Override
  public void setOwnerModel(@NotNull final XmlModelCommon newOwnerModel) {
    super.setOwnerModel(newOwnerModel);
  }

  @Override
  public void setPredecessors(@NotNull final Collection<? extends Identifiable> predecessors) {
    super.setPredecessors(predecessors);
  }

  @Override
  public void removePredecessor(@NotNull final Identified predecessorId) {
    super.removePredecessor(predecessorId);
  }

  @Override
  public void addPredecessor(final Identified predecessorId) {
    super.addPredecessor(predecessorId);
  }

  @Override
  public void addSuccessor(final Identified successorId) {
    super.addSuccessor(successorId);
  }

  @Override
  public void removeSuccessor(@NotNull final Identified successorId) {
    super.removeSuccessor(successorId);
  }

  @Override
  public void setSuccessors(@NotNull final Collection<? extends Identified> successors) {
    super.setSuccessors(successors);
  }

}
