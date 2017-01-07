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

package nl.adaptivity.process.processModel

import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.process.processModel.engine.XmlSplit
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.*
import java.util.*
import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 26/11/15.
 */
abstract class SplitBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : JoinSplitBase<NodeT, ModelT>, Split<NodeT, ModelT> {

  abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : JoinSplitBase.Builder<NodeT,ModelT>, Split.Builder<NodeT,ModelT> {
    override val idBase:String
      get() = "split"

    constructor() : this(id = null)

    @Deprecated("use the constructor that takes a single predecessor")
    constructor(id: String? = null,
                predecessors: Collection<Identified>,
                successors: Collection<Identified> = emptyList(),
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1,
                x: Double = Double.NaN,
                y: Double = Double.NaN) : super(id, predecessors, successors, label, defines, results, x, y, min, max)

    constructor(id: String? = null,
                predecessor: Identified? = null,
                successors: Collection<Identified> = emptyList(),
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                min: Int = -1,
                max: Int = -1) : super(id, listOfNotNull(predecessor), successors, label, defines, results, x,
                                              y, min, max)

    constructor(node: Split<*, *>) : super(node)

    override val elementName: QName
      get() = Split.ELEMENTNAME

  }

  constructor(ownerModel: ModelT,
              predecessor: Identified? = null,
              successors: Collection<Identified> = emptyList(),
              id: String?,
              label: String? = null,
              x: Double = java.lang.Double.NaN,
              y: Double = java.lang.Double.NaN,
              defines: Collection<IXmlDefineType> = ArrayList<IXmlDefineType>(),
              results: Collection<IXmlResultType> = ArrayList<IXmlResultType>(),
              min: Int = -1,
              max: Int = -1) : super(ownerModel, predecessor?.let { listOf(it) } ?: emptyList(), successors, id, label, x, y, defines, results, min, max)

  constructor(builder: Split.Builder<*, *>, newOwnerModel: ModelT) : super(builder, newOwnerModel)

  override abstract fun builder(): Builder<NodeT, ModelT>

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    out.smartStartTag(Split.ELEMENTNAME) {
      serializeAttributes(out)
      serializeChildren(out)
    }
  }

  @Throws(XmlException::class)
  override fun serializeAttributes(out: XmlWriter) {
    super.serializeAttributes(out)
    if (predecessors.size > 0) {
      out.writeAttribute(ProcessNodeBase.ATTR_PREDECESSOR, predecessors.iterator().next().id)
    }
  }

  override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
    return visitor.visitSplit(this)
  }

  override val maxSuccessorCount: Int
    get() = Integer.MAX_VALUE

  companion object {

    @Deprecated("Use a final class deserializer such as XmlSplit.deserialize")
    @Throws(XmlException::class)
    fun deserialize(ownerModel: XmlProcessModel, reader: XmlReader): XmlSplit {
      return XmlSplit.deserialize(ownerModel, reader)
    }
  }
}
