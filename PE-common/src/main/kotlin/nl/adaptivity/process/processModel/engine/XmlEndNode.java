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

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


@XmlDeserializer(XmlEndNode.Factory.class)
public class XmlEndNode extends EndNodeBase<XmlProcessNode,XmlModelCommon> implements XmlProcessNode {

  public static class Builder extends EndNodeBase.Builder<XmlProcessNode, XmlModelCommon> implements XmlProcessNode.Builder {

    public Builder() {
      super();
    }

    public Builder(@Nullable final Identified predecessor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results) {
      super(id, predecessor, label, defines, results, x, y);
    }

    public Builder(@NotNull final EndNode<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public XmlEndNode build(@NotNull final XmlModelCommon newOwner) {
      return new XmlEndNode(this, newOwner);
    }
  }

  public static class Factory implements XmlDeserializerFactory<XmlEndNode> {

    @NotNull
    @Override
    public XmlEndNode deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlEndNode.deserialize(null, reader);
    }
  }

  public XmlEndNode(@NotNull final EndNode.Builder<?, ?> builder, @NotNull final XmlModelCommon newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @NotNull
  public static XmlEndNode deserialize(final XmlModelCommon ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.<XmlEndNode.Builder>deserializeHelper(new XmlEndNode.Builder(), in).build(ownerModel);
  }

  @NotNull
  public static XmlEndNode.Builder deserialize(@NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new Builder(), in);
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
