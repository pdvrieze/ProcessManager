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

import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.smartStartTag
import javax.xml.namespace.QName


/**
 * Base class for start nodes. It knows about the data
 */
abstract class StartNodeBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNodeBase<NodeT, ModelT>, StartNode<NodeT, ModelT> {

  abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNodeBase.Builder<NodeT, ModelT>, StartNode.Builder<NodeT, ModelT>, SimpleXmlDeserializable {
    override val idBase:String
      get() = "start"

    override val elementName: QName
      get() = StartNode.ELEMENTNAME

    constructor() : this(successor = null)

    constructor(id: String? = null,
                successor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN) : super(id, emptyList(), listOfNotNull(successor), label, defines, results, x, y)

    constructor(node: StartNode<*, *>) : super(node)

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

  }

  constructor(ownerModel: ModelT,
              successor: Identified?=null,
              id: String?=null,
              label: String?=null,
              x: Double = Double.NaN,
              y: Double = Double.NaN,
              defines: Collection<IXmlDefineType> = emptyList(),
              results: Collection<IXmlResultType> = emptyList())
      : super(ownerModel,
              emptyList(),
              listOfNotNull(successor),
              id, label, x, y, defines, results)
  
  @Deprecated("Use the full constructor")
  constructor(ownerModel: ModelT) : super(ownerModel) { }

  constructor(builder: StartNode.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) : super(builder, buildHelper)

  override abstract fun builder(): Builder<NodeT, ModelT>

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

  override val maxPredecessorCount: Int get() = 0
}
