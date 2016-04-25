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

import net.devrieze.util.StringUtil;
import net.devrieze.util.Transaction;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance;
import nl.adaptivity.process.processModel.Condition;
import nl.adaptivity.xml.*;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
public class ConditionImpl implements XmlSerializable, Condition {

  private final String mCondition;

  public ConditionImpl(final String condition) {
    mCondition = condition;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeSimpleElement(out, new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX), getCondition());
  }

  @NotNull
  public static ConditionImpl deserialize(@NotNull final XmlReader in) throws XmlException {
    final CharSequence condition = XmlReaderUtil.readSimpleElement(in);
    return new ConditionImpl(StringUtil.toString(condition));
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.engine.Condition#getCondition()
   */
  @Override
  public String getCondition() {
    return mCondition;
  }

  /**
   * Evaluate the condition.
   *
   * @param transaction The transaction to use for reading state
   * @param instance The instance to use to evaluate against.
   * @return <code>true</code> if the condition holds, <code>false</code> if not
   */
  public boolean eval(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    // TODO process the condition as xpath, expose the node's defines as variables
    return true;
  }

}
