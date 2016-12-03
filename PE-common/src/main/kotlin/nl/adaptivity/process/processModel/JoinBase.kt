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

import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.*
import java.util.*

import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 26/11/15.
 */
abstract class JoinBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : JoinSplitBase<T, M>, Join<T, M> {

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : JoinSplitBase.Builder<T,M>, Join.Builder<T,M> {

    constructor():this(predecessors= emptyList())

    constructor(predecessors: Collection<Identifiable> = emptyList(),
                successors: Collection<Identifiable> = emptyList(),
                id: String? = null, label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1) : super(predecessors, successors, id, label, x, y, defines, results, min, max)

    constructor(node: Join<*, *>) : super(node)

    override abstract fun build(newOwner: M): JoinBase<T, M>

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

  constructor(ownerModel: M?,
              predecessors: Collection<Identifiable> = emptyList(),
              successor: Identifiable? = null,
              id: String?,
              label: String? = null,
              x: Double = java.lang.Double.NaN,
              y: Double = java.lang.Double.NaN,
              defines: Collection<IXmlDefineType> = ArrayList<IXmlDefineType>(),
              results: Collection<IXmlResultType> = ArrayList<IXmlResultType>(),
              min: Int = -1,
              max: Int = -1) : super(ownerModel, predecessors, successor?.let { listOf(it) } ?: emptyList(), id, label, x, y, defines, results, min, max)

  @Deprecated("Use the normal constructor")
  constructor(ownerModel: M?, predecessors: Collection<Identifiable>, max: Int, min: Int) : this(ownerModel, predecessors, id=null, max=max, min=min)

  @Deprecated("")
  constructor(ownerModel: M?) : super(ownerModel)

  @Deprecated("")
  constructor(orig: Join<*, *>) : this(orig, null) {
  }

  constructor(orig: Join<*, *>, newOwner: M?) : super(orig, newOwner)

  constructor(builder: Join.Builder<*, *>, newOwnerModel: M) : super(builder, newOwnerModel)

  override abstract fun builder(): Builder<T, M>

  override val idBase: String
    get() = IDBASE

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

  @Throws(XmlException::class)
  override fun deserializeChild(reader: XmlReader): Boolean {
    if (reader.isElement(Join.PREDELEMNAME)) {
      val id = reader.readSimpleElement().toString()
      addPredecessor(Identifier(id))
      return true
    }
    return super.deserializeChild(reader)
  }

  override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
    return visitor.visitJoin(this)
  }

  override val elementName: QName
    get() = Join.ELEMENTNAME

  override val maxPredecessorCount: Int
    get() = Integer.MAX_VALUE

  companion object {

    val IDBASE = "join"
  }
}
