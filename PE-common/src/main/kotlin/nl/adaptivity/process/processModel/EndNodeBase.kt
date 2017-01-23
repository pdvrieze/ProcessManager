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
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*
import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 24/11/15.
 */
abstract class EndNodeBase<T : ProcessNode<T, M>, M : ProcessModel<T, M>?>(builder: EndNode.Builder<*, *>,
                                                                           buildHelper: ProcessModel.BuildHelper<T, M>) : ProcessNodeBase<T, M>(builder, buildHelper), EndNode<T, M> {

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ProcessNodeBase.Builder<T, M>, EndNode.Builder<T, M>, SimpleXmlDeserializable {

    override val idBase:String
      get() = "end"

    constructor(): this(id =null)

    constructor(id: String? = null,
                predecessor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                multiInstance: Boolean = false) : super(id, listOfNotNull(predecessor), emptyList(), label, defines, results, x, y, multiInstance)

    constructor(node: EndNode<*, *>) : super(node)

    @Throws(XmlException::class)
    override fun deserializeChild(reader: XmlReader): Boolean {
      if (ProcessConsts.Engine.NAMESPACE == reader.namespaceUri) {
        when (reader.localName.toString()) {
          "export", XmlDefineType.ELEMENTLOCALNAME -> {
            defines.add(XmlDefineType.deserialize(reader))
            return true
          }
        }
      }
      return false
    }

    override fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      if (ProcessNodeBase.ATTR_PREDECESSOR == attributeLocalName) {
        predecessor = Identifier(attributeValue.toString())
        return true
      }
      return super<ProcessNodeBase.Builder>.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
    }

    override fun deserializeChildText(elementText: CharSequence): Boolean {
      return false
    }


    override val elementName: QName
      get() = EndNode.ELEMENTNAME
  }

  override var predecessor: Identified?
    get() = if (predecessors.size==0) null else predecessors.single()
    set(value) {
      setPredecessors(listOfNotNull(value))
    }

  override val maxSuccessorCount: Int get() = 0

  override val successors: IdentifyableSet<Identified>
    get() = IdentifyableSet.empty<Identified>()


  override abstract fun builder(): Builder<T, M>

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    out.smartStartTag(EndNode.ELEMENTNAME)
    serializeAttributes(out)
    serializeChildren(out)
    out.endTag(EndNode.ELEMENTNAME)
  }

  @Throws(XmlException::class)
  override fun serializeAttributes(out: XmlWriter) {
    super.serializeAttributes(out)
    predecessor?.let { out.writeAttribute(ProcessNodeBase.ATTR_PREDECESSOR, it.id) }
  }

  override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
    return visitor.visitEndNode(this)
  }

  // Override to make public.
  override fun setDefines(defines: Collection<IXmlDefineType>) = super.setDefines(defines)
}
