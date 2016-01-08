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
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;


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
    super(orig);
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
    mCondition = StringUtil.toString(XmlUtil.readSimpleElement(in));
  }

  @Override
  protected void serializeCondition(final XmlWriter out) throws XmlException {
    if (mCondition!=null && mCondition.length()>0) {
      XmlUtil.writeSimpleElement(out, Condition.ELEMENTNAME, mCondition);
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
}
