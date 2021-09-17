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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.MutableIdentifyableSet


class ExecutableEndNode(
    builder: EndNode.Builder,
    buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, *, *, *>,
    otherNodes: Iterable<ProcessNode.Builder>
                       ) :
    EndNodeBase(builder, buildHelper.newOwner, otherNodes), ExecutableProcessNode {

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    class Builder : EndNodeBase.Builder, ExecutableProcessNode.Builder {
    constructor(): this(predecessor=null)
    constructor(id: String? = null,
                predecessor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                multiInstance: Boolean = false) : super(id, predecessor, label, defines, results, x, y, multiInstance)

    constructor(node: EndNode) : super(node)
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

}
