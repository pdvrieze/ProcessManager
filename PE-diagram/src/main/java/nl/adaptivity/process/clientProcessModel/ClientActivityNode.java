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

package nl.adaptivity.process.clientProcessModel;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.ActivityBase;
import nl.adaptivity.process.processModel.Condition;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


public class ClientActivityNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ActivityBase<T, M> implements ClientProcessNode<T, M> {

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
    super(orig, null);
    mCompat = compat;
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
  protected void deserializeCondition(final XmlReader in) throws XmlException {
    mCondition = StringUtil.toString(XmlReaderUtil.readSimpleElement(in));
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
