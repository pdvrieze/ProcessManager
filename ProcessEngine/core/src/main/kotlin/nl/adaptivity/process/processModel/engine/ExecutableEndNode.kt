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
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper


class ExecutableEndNode(builder: EndNode.Builder<*, *>, newOwnerModel: ExecutableModelCommon) : EndNodeBase<ExecutableProcessNode, ExecutableModelCommon>(builder, newOwnerModel), ExecutableProcessNode {

  class Builder : EndNodeBase.Builder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableProcessNode.Builder {
    constructor(): this(predecessor=null)
    constructor(id: String? = null,
                predecessor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN) : super(id, predecessor, label, defines, results, x, y)

    constructor(node: EndNode<*, *>) : super(node)

    override fun build(newOwner: ExecutableModelCommon) = ExecutableEndNode(this, newOwner)
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override fun builder() = Builder(this)

  companion object {

    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableEndNode {
      return ExecutableEndNode.Builder().deserializeHelper(reader).build(ownerModel)
    }
  }

}
