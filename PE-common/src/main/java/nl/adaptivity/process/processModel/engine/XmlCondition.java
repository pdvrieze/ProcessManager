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
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.processModel.Condition;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
public class XmlCondition implements XmlSerializable, Condition {

  private final String mCondition;

  public XmlCondition(final String condition) {
    mCondition = condition;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlWriterUtil.writeSimpleElement(out, new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX), getCondition());
  }

  @NotNull
  public static XmlCondition deserialize(@NotNull final XmlReader reader) throws XmlException {
    final CharSequence condition = XmlReaderUtil.readSimpleElement(reader);
    return new XmlCondition(StringUtil.toString(condition));
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.engine.Condition#getCondition()
   */
  @Override
  public String getCondition() {
    return mCondition;
  }

}
