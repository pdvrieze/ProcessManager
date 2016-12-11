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
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.schema.annotations.XmlName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


/**
 * Class representing an activity in a process engine. Activities are expected
 * to invoke one (and only one) web service. Some services are special in that
 * they either invoke another process (and the process engine can treat this
 * specially in later versions), or set interaction with the user. Services can
 * use the ActivityResponse soap header to indicate support for processes and
 * what the actual state of the task after return should be (instead of
 *
 * @author Paul de Vrieze
 */
@XmlDeserializer(XmlActivity.Factory.class)
public class XmlActivity extends ActivityBase<XmlProcessNode, XmlProcessModel> implements XmlProcessNode {

  public static class Builder extends ActivityBase.Builder<XmlProcessNode, XmlProcessModel> implements XmlProcessNode.Builder {

    public Builder() { }

    public Builder(@Nullable final Identified predecessor, @Nullable final Identified successor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, @Nullable final XmlMessage message, @Nullable final String condition, @Nullable final String name) {
      super(predecessor, successor, id, label, x, y, defines, results, message, condition, name);
    }

    public Builder(@NotNull final Activity<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public XmlActivity build(@NotNull final XmlProcessModel newOwner) {
      return new XmlActivity(this, newOwner);
    }
  }

  public static class Factory implements XmlDeserializerFactory<XmlActivity> {

    @NotNull
    @Override
    public XmlActivity deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlActivity.deserialize(null, reader);
    }
  }

  @Nullable private XmlCondition mCondition;

  /**
   * Create a new Activity. Note that activities can only have a a single
   * predecessor.
   *
   * @param predecessor The process node that starts immediately precedes this
   *          activity.
   */
  public XmlActivity(final XmlProcessModel ownerModel, final XmlProcessNode predecessor) {
    super(ownerModel);
    setPredecessor(predecessor);
  }

  /**
   * Create an activity without predecessor. This constructor is needed for JAXB
   * to work.
   */
  public XmlActivity(final XmlProcessModel ownerModel) {super(ownerModel);}

  @Deprecated
  public XmlActivity(final Activity<?,?> orig) { this(orig, null); }

  public XmlActivity(final Activity<?, ?> orig, XmlProcessModel newOwner) {
    super(orig, newOwner);
  }

  public XmlActivity(@NotNull final Activity.Builder<?, ?> builder, @NotNull final XmlProcessModel newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @Override
  protected void serializeCondition(final XmlWriter out) throws XmlException {
    XmlWriterUtil.writeChild(out, mCondition);
  }

  @Override
  protected void deserializeCondition(final XmlReader in) throws XmlException {
    mCondition = XmlCondition.deserialize(in);
  }

  /* (non-Javadoc)
         * @see nl.adaptivity.process.processModel.IActivity#getCondition()
         */
  @Nullable
  @Override
  @XmlName(Condition.ELEMENTLOCALNAME)
  public String getCondition() {
    return mCondition == null ? null : mCondition.toString();
  }

  /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.IActivity#setCondition(java.lang.String)
       */
  @Override
  public void setCondition(@Nullable final String condition) {
    mCondition = condition == null ? null : new XmlCondition(condition);
    notifyChange();
  }

  @NotNull
  public static XmlActivity deserialize(final XmlProcessModel ownerModel, @NotNull final XmlReader reader) throws
          XmlException {
    return XmlUtil.<XmlActivity>deserializeHelper(new XmlActivity(ownerModel), reader);
  }

  @NotNull
  public static XmlActivity.Builder deserialize(@NotNull final XmlReader reader) throws
          XmlException {
    return XmlUtil.<XmlActivity.Builder>deserializeHelper(new Builder(), reader);
  }

  @Override
  public void setOwnerModel(@NotNull final XmlProcessModel ownerModel) {
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
