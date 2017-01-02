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

package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import nl.adaptivity.xml.XmlWriterUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public class ClientActivityNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ActivityBase<T, M> implements ClientProcessNode<T, M> {

  public static class Builder<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ActivityBase.Builder<T,M> implements ClientProcessNode.Builder<T,M> {

    public Builder() { }

    public Builder(final boolean compat) {
      this.compat = compat;
    }

    public Builder(@Nullable final Identified predecessor, @Nullable final Identified successor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, @Nullable final XmlMessage message, @Nullable final String condition, @Nullable final String name, final boolean compat) {
      super(id, predecessor, successor, label, defines, results, message, condition, name, x, y);
      this.compat = compat;
    }

    public Builder(@NotNull final Activity<?, ?> node) {
      super(node);
      if (node instanceof ClientProcessNode) {
        compat = ((ClientProcessNode) node).isCompat();
      } else {
        compat = false;
      }
    }

    @NotNull
    @Override
    public ClientActivityNode<T, M> build(final M newOwner) {
      return new ClientActivityNode<T, M>(this, newOwner);
    }

    @Override
    public boolean isCompat() {
      return compat;
    }

    @Override
    public void setCompat(final boolean compat) {
      this.compat = compat;
    }

    public boolean compat = false;
  }

  private final boolean mCompat;
  private String mCondition;

  public ClientActivityNode(final M owner, final boolean compat) {
    super(owner);
    mCompat = compat;
  }


  public ClientActivityNode(final M owner, String id, final boolean compat) {
    super(owner);
    setId(id);
    mCompat = compat;
  }

  protected ClientActivityNode(Activity<?, ?> orig, final boolean compat) {
    super(orig.builder(), null);
    mCompat = compat;
  }

  public ClientActivityNode(@NotNull final Activity.Builder<?, ?> builder, @NotNull final M newOwnerModel) {
    super(builder, newOwnerModel);
    if (builder instanceof Builder) {
      mCompat = ((Builder) builder).compat;
    } else {
      mCompat = false;
    }
  }

  @NotNull
  @Override
  public Builder<T, M> builder() {
    return new Builder<>(this);
  }

  @Override
  public String getCondition() {
    return mCondition;
  }

  @Override
  public void setCondition(String condition) {
    mCondition = condition;
  }

  @Override
  protected void serializeCondition(final XmlWriter out) throws XmlException {
    if (mCondition!=null && mCondition.length()>0) {
      XmlWriterUtil.writeSimpleElement(out, Condition.ELEMENTNAME, mCondition);
    }
  }

  @Override
  public int getMaxSuccessorCount() {
    return isCompat() ? Integer.MAX_VALUE : 1;
  }

  @Override
  public boolean isCompat() {
    return mCompat;
  }

  @Override
  public void setOwnerModel(@NotNull final M ownerModel) {
    super.setOwnerModel(ownerModel);
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
