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
import java.util.List;


@XmlDeserializer(XmlStartNode.Factory.class)
public class XmlStartNode extends StartNodeBase<XmlProcessNode,ProcessModelImpl> implements XmlProcessNode {

  public static class Builder extends StartNodeBase.Builder<XmlProcessNode, ProcessModelImpl> implements XmlProcessNode.Builder {

    @NotNull
    @Override
    public XmlStartNode build(@NotNull final ProcessModelImpl newOwner) {
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

  public XmlStartNode(@NotNull final ProcessNode<?, ?> orig, @Nullable final ProcessModelImpl newOwnerModel) {
    super(orig, newOwnerModel);
  }

  public XmlStartNode(final StartNode<?, ?> orig) {
    this(orig, null);
  }

  public XmlStartNode(@NotNull final Builder builder, @NotNull final ProcessModelImpl newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  public static XmlStartNode deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.deserializeHelper(new XmlStartNode(ownerModel), in);
  }

  public XmlStartNode(final @Nullable ProcessModelImpl ownerModel) {
    super(ownerModel);
  }

  public XmlStartNode(final @Nullable ProcessModelImpl ownerModel, final List<XmlResultType> imports) {
    super(ownerModel);
    setResults(imports);
  }

  @Override
  public void setOwnerModel(@NotNull final ProcessModelImpl ownerModel) {
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
