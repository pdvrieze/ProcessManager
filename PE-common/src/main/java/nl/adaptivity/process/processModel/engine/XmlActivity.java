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
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.schema.annotations.XmlName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


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
public class XmlActivity extends ActivityBase<XmlProcessNode, ProcessModelImpl> implements XmlProcessNode {

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
  public XmlActivity(final ProcessModelImpl  ownerModel, final XmlProcessNode predecessor) {
    super(ownerModel);
    setPredecessor(predecessor);
  }

  /**
   * Create an activity without predecessor. This constructor is needed for JAXB
   * to work.
   */
  public XmlActivity(final ProcessModelImpl ownerModel) {super(ownerModel);}

  public XmlActivity(final Activity<?, ?> orig) {
    super(orig);
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
  public static XmlActivity deserialize(final ProcessModelImpl  ownerModel, @NotNull final XmlReader reader) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.<XmlActivity>deserializeHelper(new XmlActivity(ownerModel), reader);
  }

}
