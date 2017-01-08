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

import java.util.*

@Deprecated("Use builders instead")
interface MutableRootProcessModel<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>: RootProcessModel<NodeT, ModelT> {

  @Deprecated("Use builders instead of mutable process models")
  fun setUuid(uuid: UUID)

  @Deprecated("Use builders instead of mutable process models")
  fun addNode(node: NodeT): Boolean

  @Deprecated("Use builders instead of mutable process models")
  fun removeNode(node: NodeT): Boolean

  /**
   * Initiate the notification that a node has changed. Actual implementations can override this.
   * @param node The node that has changed.
   */
  @Deprecated("Use builders instead of mutable process models")
  fun notifyNodeChanged(node: NodeT)
}