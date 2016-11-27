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

import java.util.Collection;


@XmlDeserializer(XmlEndNode.Factory.class)
public class XmlEndNode extends EndNodeBase<XmlProcessNode,ProcessModelImpl> implements XmlProcessNode {

  public static class Factory implements XmlDeserializerFactory<XmlEndNode> {

    @NotNull
    @Override
    public XmlEndNode deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlEndNode.deserialize(null, reader);
    }
  }

  public XmlEndNode(final EndNode<?, ?> orig) {
    super(orig);
  }

  @NotNull
  public static XmlEndNode deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.<XmlEndNode>deserializeHelper(new XmlEndNode(ownerModel), in);
  }

  public XmlEndNode(final ProcessModelImpl ownerModel, final XmlProcessNode previous) {
    super(ownerModel);
    setPredecessor(previous);
  }

  public XmlEndNode(final ProcessModelImpl ownerModel) {
    super(ownerModel);
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
