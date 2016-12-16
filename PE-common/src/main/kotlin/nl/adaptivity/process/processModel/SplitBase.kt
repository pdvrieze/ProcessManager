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

package nl.adaptivity.process.processModel

import net.devrieze.util.StringUtil
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.process.processModel.engine.XmlSplit
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.*
import java.util.*

import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 26/11/15.
 */
abstract class SplitBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : JoinSplitBase<T, M>, Split<T, M> {

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : JoinSplitBase.Builder<T,M>, Split.Builder<T,M> {
    override val idBase:String
      get() = "split"

    constructor() : this(predecessors = emptyList())

    constructor(predecessors: Collection<Identified> = emptyList(),
                successors: Collection<Identified> = emptyList(),
                id: String? = null, label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1) : super(predecessors, successors, id, label, x, y, defines, results, min, max)

    constructor(node: Split<*, *>) : super(node)

    override abstract fun build(newOwner: M?): ProcessNode<T, M>

    override val elementName: QName
      get() = Split.ELEMENTNAME

  }

  constructor(ownerModel: M?,
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

  @Deprecated("Use general constructor")
  constructor(ownerModel: M?, predecessors: Collection<Identified>, max: Int, min: Int) : super(ownerModel, predecessors, max, min) { }

  @Deprecated("Use general constructor")
  constructor(ownerModel: M?) : super(ownerModel)

  constructor(orig: Split<*, *>, newOwner: M? = null) : super(orig, newOwner)

  constructor(builder: Split.Builder<*, *>, newOwnerModel: M?) : super(builder, newOwnerModel)

  override abstract fun builder(): Builder<T, M>

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
