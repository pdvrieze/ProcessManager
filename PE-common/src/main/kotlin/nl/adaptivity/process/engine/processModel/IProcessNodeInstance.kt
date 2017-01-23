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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.ReadableHandleAware
import net.devrieze.util.StringUtil
import net.devrieze.util.security.SecureObject
import java.util.*
import javax.xml.bind.annotation.XmlRootElement


/**
 * Class representing the instantiation of an executable process node.
 *
 * @author Paul de Vrieze
 *
 * @param V The actual type of the implementing class.
 */
interface IProcessNodeInstance<out V : IProcessNodeInstance<V>> : ReadableHandleAware<SecureObject<V>> {

  /**
   * Get the state of the task.

   * @return the state.
   */
  val state: NodeInstanceState
}
