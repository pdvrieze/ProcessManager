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

import net.devrieze.util.toMutableArraySet
import nl.adaptivity.process.util.IdentifyableSet

/**
 * Base class for submodels
 */
abstract class SubModelBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?> : ProcessCommonBase<T, M>, SubModel<T,M> {

  override val ownerNode: T

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?>(nodes: Collection<ProcessNode.Builder<T, M>> = emptyList(),
                                                                             imports: Collection<IXmlResultType> = emptyList(),
                                                                             exports: Collection<IXmlDefineType> = emptyList()) : ProcessCommonBase.Builder<T,M>(nodes, imports, exports), SubModel.Builder<T,M> {

  }

  constructor(builder: SubModel.Builder<T,M>, ownerNode: T, pedantic: Boolean):super(builder, pedantic) {
    this.ownerNode = ownerNode
  }

  abstract override fun builder(): SubModelBase.Builder<T, M>

  abstract fun update(body: (ProcessCommonBase.Builder<T, M>) -> Unit): SubModelBase<T, M>

}