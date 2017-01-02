/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.processModel

import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.util.Identifiable

/**
 * Created by pdvrieze on 02/01/17.
 */
interface ModelCommon<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : SecureObject<M> {
  /**
   * Get the process node with the given id.
   * @param nodeId The node id to look up.
   * *
   * @return The process node with the id.
   */
  fun getNode(nodeId: Identifiable): T?

  fun getModelNodes(): Collection<T>
  fun getImports(): Collection<IXmlResultType>
  fun getExports(): Collection<IXmlDefineType>
}