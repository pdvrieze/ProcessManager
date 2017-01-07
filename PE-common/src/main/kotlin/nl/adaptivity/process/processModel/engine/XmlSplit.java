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
import nl.adaptivity.xml.XmlDeserializer;
import nl.adaptivity.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static nl.adaptivity.xml.XmlUtil.deserializeHelper;


@XmlDeserializer(XmlSplit.Factory.class)
public class XmlSplit extends SplitBase<XmlProcessNode,XmlModelCommon> implements XmlProcessNode {

  public static class Builder extends SplitBase.Builder<XmlProcessNode, XmlModelCommon> implements XmlProcessNode.Builder {

    public Builder(){}

    public Builder(@NotNull final Split<?, ?> node) {
      super(node);
    }

    public Builder(@NotNull final Collection<? extends Identified> predecessors, @NotNull final Collection<? extends Identified> successors, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final int min, final int max) {
      super(id, predecessors, successors, label, defines, results, min, max, x, y);
    }

    @NotNull
    @Override
    public XmlSplit build(@NotNull final XmlModelCommon newOwner) {
      return new XmlSplit(this, newOwner);
    }
  }

  public static class Factory implements XmlDeserializerFactory<XmlSplit> {

    @NotNull
    @Override
    public XmlSplit deserialize(final XmlReader reader) throws XmlException {
      return XmlSplit.deserialize(null, reader);
    }
  }

  public XmlSplit(@NotNull final Split.Builder<?, ?> builder, @NotNull final XmlModelCommon newOwnerModel) {
    super(builder, newOwnerModel);
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

  public static XmlSplit deserialize(XmlProcessModel owner, XmlReader reader) throws XmlException {
    return deserialize(reader).build(owner);
  }

  public static XmlSplit.Builder deserialize(XmlReader reader) throws XmlException {
    return deserializeHelper(new XmlSplit.Builder(), reader);
  }

}
