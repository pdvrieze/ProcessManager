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

import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.JoinBase;
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;


public class XmlJoin extends JoinBase<XmlProcessNode,XmlModelCommon> implements XmlProcessNode {

  public static class Builder extends JoinBase.Builder<XmlProcessNode, XmlModelCommon> implements XmlProcessNode.Builder {

    public Builder() {}

    public Builder(@NotNull final Join<?, ?> node) {
      super(node);
    }

    public Builder(@NotNull final Collection<? extends Identified> predecessors, @NotNull final Identified successor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final int min, final int max) {
      super(id, predecessors, successor, label, defines, results, x, y, min, max);
    }

    @NotNull
    @Override
    public XmlJoin build(@NotNull final BuildHelper<XmlProcessNode, XmlModelCommon> buildHelper) {
      return new XmlJoin(this, buildHelper);
    }
  }

  @NotNull
  public static XmlJoin deserialize(@NotNull final XmlReader in,
                                    final BuildHelper<XmlProcessNode, XmlModelCommon> buildHelper) throws
          XmlException {
    return deserialize(in).build(buildHelper);
  }

  @NotNull
  public static XmlJoin.Builder deserialize(@NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new XmlJoin.Builder(), in);
  }

  public XmlJoin(final XmlProcessModel ownerModel) {
    super(ownerModel);
  }

  public XmlJoin(@NotNull final Join.Builder<?, ?> builder, @NotNull final BuildHelper<XmlProcessNode, XmlModelCommon> buildHelper) {
    super(builder, buildHelper);
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
