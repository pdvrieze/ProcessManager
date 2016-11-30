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

import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.ProcessNodeBase.Builder
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*

import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 26/11/15.
 */
abstract class StartNodeBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : ProcessNodeBase<T, M>, StartNode<T, M>, SimpleXmlDeserializable {

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : ProcessNodeBase.Builder<T, M>(), StartNode.Builder<T, M> {

    abstract override fun build(newOwner: M): StartNodeBase<T, M>

  }

  constructor(_ownerModel: M?=null,
              successor: Identifiable?=null,
              id: String?,
              label: String?=null,
              x: Double = Double.NaN,
              y: Double = Double.NaN,
              defines: Collection<IXmlDefineType> = emptyList(),
              results: Collection<IXmlResultType> = emptyList())
      : super(_ownerModel,
              emptyList(),
              listOfNotNull(successor),
              id, label, x, y, defines, results)
  
  @Deprecated("Use the full constructor")
  constructor(ownerModel: M?) : super(ownerModel) { }

  @JvmOverloads constructor(orig: StartNode<*, *>, newOwnerModel: M?) : super(orig, newOwnerModel)

  constructor(builder: StartNode.Builder<*, *>, newOwnerModel: M) : super(builder, newOwnerModel)

  @Throws(XmlException::class)
  override fun deserializeChild(reader: XmlReader): Boolean {
    if (ProcessConsts.Engine.NAMESPACE == reader.namespaceUri) {
      when (reader.localName.toString()) {
        "import" -> {
          (results as MutableList).add(XmlResultType.deserialize(reader))
          return true
        }
      }
    }
    return false
  }

  override fun deserializeChildText(elementText: CharSequence): Boolean {
    return false
  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    out.smartStartTag(StartNode.ELEMENTNAME) {
      serializeAttributes(this)
      serializeChildren(this)
    }
  }

  override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
    return visitor.visitStartNode(this)
  }

  override val elementName: QName
    get() = StartNode.ELEMENTNAME

  override val maxPredecessorCount: Int get() = 0
}
