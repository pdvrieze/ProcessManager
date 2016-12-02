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

package nl.adaptivity.process.processModel.engine;

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlDeserializer;
import nl.adaptivity.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


@XmlDeserializer(XmlEndNode.Factory.class)
public class XmlEndNode extends EndNodeBase<XmlProcessNode,XmlProcessModel> implements XmlProcessNode {

  public static class Builder extends EndNodeBase.Builder<XmlProcessNode, XmlProcessModel> implements XmlProcessNode.Builder {

    public Builder(@Nullable final Identifiable predecessor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results) {
      super(predecessor, id, label, x, y, defines, results);
    }

    public Builder(@NotNull final EndNode<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public XmlEndNode build(@NotNull final XmlProcessModel newOwner) {
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

  @Deprecated
  public XmlEndNode(final EndNode<?, ?> orig) {
    this(orig, null);
  }

  public XmlEndNode(final EndNode<?, ?> orig, XmlProcessModel newOwner) {
    super(orig, newOwner);
  }

  public XmlEndNode(@NotNull final EndNode.Builder<?, ?> builder, @NotNull final XmlProcessModel newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @NotNull
  public static XmlEndNode deserialize(final XmlProcessModel ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.<XmlEndNode>deserializeHelper(new XmlEndNode(ownerModel), in);
  }

  public XmlEndNode(final XmlProcessModel ownerModel, final XmlProcessNode previous) {
    super(ownerModel);
    setPredecessor(previous);
  }

  public XmlEndNode(final XmlProcessModel ownerModel) {
    super(ownerModel);
  }

  @Override
  public void setOwnerModel(@NotNull final XmlProcessModel ownerModel) {
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
