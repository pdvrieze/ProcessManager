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
import nl.adaptivity.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@XmlDeserializer(XmlJoin.Factory.class)
public class XmlJoin extends JoinBase<XmlProcessNode,XmlProcessModel> implements XmlProcessNode {

  public static class Builder extends JoinBase.Builder<XmlProcessNode, XmlProcessModel> implements XmlProcessNode.Builder {

    public Builder() {}

    public Builder(@NotNull final Join<?, ?> node) {
      super(node);
    }

    public Builder(@NotNull final Collection<? extends Identifiable> predecessors, @NotNull final Collection<? extends Identifiable> successors, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final int min, final int max) {
      super(predecessors, successors, id, label, x, y, defines, results, min, max);
    }

    @NotNull
    @Override
    public XmlJoin build(@NotNull final XmlProcessModel newOwner) {
      return new XmlJoin(this, newOwner);
    }
  }

  public static class Factory implements XmlDeserializerFactory<XmlJoin> {

    @NotNull
    @Override
    public XmlJoin deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlJoin.deserialize(null, reader);
    }
  }

  @Deprecated
  public XmlJoin(final Join<?, ?> orig) {
    super(orig);
  }

  @NotNull
  public static XmlJoin deserialize(final XmlProcessModel ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new XmlJoin(ownerModel), in);
  }

  @NotNull
  public static XmlJoin.Builder deserialize(@NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new XmlJoin.Builder(), in);
  }

  public XmlJoin(final XmlProcessModel ownerModel, final Collection<? extends Identifiable> predecessors, final int min, final int max) {
    super(ownerModel, predecessors, max, min);
    if ((getMin() < 1) || (max < min)) {
      throw new IllegalProcessModelException("Join range (" + min + ", " + max + ") must be sane");
    }
  }

  public XmlJoin(final XmlProcessModel ownerModel) {
    super(ownerModel);
  }

  public XmlJoin(@NotNull final Join<?, ?> orig, @Nullable final XmlProcessModel newOwner) {
    super(orig, newOwner);
  }

  public XmlJoin(@NotNull final Join.Builder<?, ?> builder, @NotNull final XmlProcessModel newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  public static XmlJoin andJoin(final XmlProcessModel ownerModel, final XmlProcessNode... predecessors) {
    return new XmlJoin(ownerModel, Arrays.asList(predecessors), Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @Deprecated
  @Nullable
  Set<? extends Identifiable> getXmlPrececessors() {
    return getPredecessors();
  }

  @Deprecated
  void setXmlPrececessors(final List<? extends XmlProcessNode> pred) {
    swapPredecessors(pred);
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
