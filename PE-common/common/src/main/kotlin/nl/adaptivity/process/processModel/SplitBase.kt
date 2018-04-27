/*
 * Copyright (c) 2018.
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

import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.*


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
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                min: Int = -1,
                max: Int = -1,
                multiInstance: Boolean = false) : super(id, predecessors, successors, label, defines, results, x, y, min, max, multiInstance)

    constructor(id: String? = null,
                predecessor: Identified? = null,
                successors: Collection<Identified> = emptyList(),
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                min: Int = -1,
                max: Int = -1,
                multiInstance: Boolean = false) : super(id, listOfNotNull(predecessor), successors, label, defines, results, x,
                                              y, min, max, multiInstance)

    constructor(node: Split<*, *>) : super(node)

    override val elementName: QName
      get() = Split.ELEMENTNAME

    override fun deserializeAttribute(attributeNamespace: CharSequence,
                                      attributeLocalName: CharSequence,
                                      attributeValue: CharSequence): Boolean {
      if (attributeNamespace.isEmpty() && attributeLocalName.toString() == "predecessor") {
        predecessor = Identifier(attributeValue)
        return true
      } else
        return super<JoinSplitBase.Builder>.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
    }
  }

  constructor(ownerModel: ModelT,
              predecessor: Identified? = null,
              successors: Collection<Identified> = emptyList(),
              id: String?,
              label: String? = null,
              x: Double = Double.NaN,
              y: Double = Double.NaN,
              defines: Collection<IXmlDefineType> = ArrayList<IXmlDefineType>(),
              results: Collection<IXmlResultType> = ArrayList<IXmlResultType>(),
              min: Int = -1,
              max: Int = -1) : super(ownerModel, predecessor?.let { listOf(it) } ?: emptyList(), successors, id, label, x, y, defines, results, min, max)

  constructor(builder: Split.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) : super(builder, buildHelper)

  override abstract fun builder(): Builder<NodeT, ModelT>

  override fun serialize(out: XmlWriter) {
    out.smartStartTag(Split.ELEMENTNAME) {
      serializeAttributes(out)
      serializeChildren(out)
    }
  }

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
    get() = Int.MAX_VALUE

}
