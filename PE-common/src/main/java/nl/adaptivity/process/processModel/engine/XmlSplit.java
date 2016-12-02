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
import java.util.Collections;

import static nl.adaptivity.xml.XmlUtil.deserializeHelper;


@XmlDeserializer(XmlSplit.Factory.class)
public class XmlSplit extends SplitBase<XmlProcessNode,XmlProcessModel> implements XmlProcessNode {

  public static class Builder extends SplitBase.Builder<XmlProcessNode, XmlProcessModel> implements XmlProcessNode.Builder {

    public Builder(@NotNull final Split<?, ?> node) {
      super(node);
    }

    public Builder(@NotNull final Collection<? extends Identifiable> predecessors, @NotNull final Collection<? extends Identifiable> successors, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final int min, final int max) {
      super(predecessors, successors, id, label, x, y, defines, results, min, max);
    }

    @NotNull
    @Override
    public XmlSplit build(@NotNull final XmlProcessModel newOwner) {
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

  @Deprecated
  public XmlSplit(final @Nullable XmlProcessModel ownerModel, final XmlProcessNode predecessor, final int min, final int max) {
    super(ownerModel, Collections.singleton(predecessor), max, min);
    if ((getMin() < 1) || (max < min)) {
      throw new IllegalProcessModelException("Join range (" + min + ", " + max + ") must be sane");
    }
  }

  @Deprecated
  public XmlSplit(final @Nullable XmlProcessModel ownerModel) {
    super(ownerModel);
  }

  public XmlSplit(final Split<?, ?> orig, XmlProcessModel newOwner) {
    super(orig, newOwner);
  }

  @Deprecated
  public XmlSplit(final Split<?, ?> orig) {
    this(orig, null);
  }

  public XmlSplit(@NotNull final Split.Builder<?, ?> builder, @NotNull final XmlProcessModel newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  public static XmlSplit andSplit(final XmlProcessModel ownerModel, final XmlProcessNode predecessor) {
    return new XmlSplit(ownerModel, predecessor, Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
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

  public static XmlSplit deserialize(XmlProcessModel owner, XmlReader reader) throws XmlException {
    return deserializeHelper(new XmlSplit(owner), reader);
  }

}
