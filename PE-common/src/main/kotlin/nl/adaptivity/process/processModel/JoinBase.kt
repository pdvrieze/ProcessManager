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

import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.*
import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 26/11/15.
 */
abstract class JoinBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : JoinSplitBase<NodeT, ModelT>, Join<NodeT, ModelT> {

  abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : JoinSplitBase.Builder<NodeT,ModelT>, Join.Builder<NodeT,ModelT> {
    override val idBase:String
      get() = "join"

    override var isMultiMerge: Boolean

    constructor():this(predecessors= emptyList(), isMultiMerge = false, isMultiInstance = false)

    constructor(id: String? = null,
                predecessors: Collection<Identified> = emptyList(),
                successor: Identified? = null, label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                min: Int = -1,
                max: Int = -1,
                isMultiMerge: Boolean = false,
                isMultiInstance: Boolean = false) : super(id, predecessors, listOfNotNull(successor), label, defines, results, x,
                                                          y, min, max, isMultiInstance) {
      this.isMultiMerge = isMultiMerge
    }

    constructor(node: Join<*, *>) : super(node) {
      this.isMultiMerge = node.isMultiMerge
    }

    @Throws(XmlException::class)
    override fun deserializeChild(reader: XmlReader): Boolean {
      if (reader.isElement(Join.PREDELEMNAME)) {
        val id = reader.readSimpleElement().toString()
        predecessors.add(Identifier(id))
        return true
      }
      return super.deserializeChild(reader)
    }

    override val elementName: QName
      get() = Join.ELEMENTNAME

  }

  @Deprecated("")
  constructor(ownerModel: ModelT) : super(ownerModel) {
    isMultiMerge = false
  }

  constructor(builder: Join.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) : super(builder, buildHelper) {
    isMultiMerge = builder.isMultiMerge
  }

  override abstract fun builder(): Builder<NodeT, ModelT>

  override val idBase: String
    get() = IDBASE

  override val isMultiMerge: Boolean

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    out.smartStartTag(Join.ELEMENTNAME)
    serializeAttributes(out)
    serializeChildren(out)
    out.endTag(Join.ELEMENTNAME)
  }

  @Throws(XmlException::class)
  override fun serializeChildren(out: XmlWriter) {
    super.serializeChildren(out)
    for (pred in predecessors) {
      out.smartStartTag(Join.PREDELEMNAME)
      out.text(pred.id)
      out.endTag(Join.PREDELEMNAME)
    }
  }

  override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
    return visitor.visitJoin(this)
  }

  override val maxPredecessorCount: Int
    get() = Integer.MAX_VALUE

  companion object {

    val IDBASE = "join"
  }
}
