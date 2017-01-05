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

import nl.adaptivity.process.processModel.ModelCommon;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.StartNodeBase;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;


@XmlDeserializer(XmlStartNode.Factory.class)
public class XmlStartNode extends StartNodeBase<XmlProcessNode,XmlModelCommon> implements XmlProcessNode {

  public static class Builder extends StartNodeBase.Builder<XmlProcessNode, XmlModelCommon> implements XmlProcessNode.Builder {

    public Builder() { }

    public Builder(StartNode base) {
      super(base);
    }

    @NotNull
    @Override
    public XmlStartNode build(@NotNull final XmlModelCommon newOwner) {
      return new XmlStartNode(this, newOwner);
    }
  }

  public static class Factory implements XmlDeserializerFactory<XmlStartNode> {

    @NotNull
    @Override
    public XmlStartNode deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlStartNode.deserialize(null, reader);
    }
  }

  public XmlStartNode(@NotNull final StartNode.Builder<?,?> builder, @NotNull final XmlModelCommon newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  public static XmlStartNode deserialize(final XmlModelCommon ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.<Builder>deserializeHelper(new Builder(), in).build(ownerModel);
  }

  @NotNull
  public static XmlStartNode.Builder deserialize(@NotNull final XmlReader in) throws
          XmlException {

    return XmlUtil.deserializeHelper(new XmlStartNode.Builder(), in);
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
