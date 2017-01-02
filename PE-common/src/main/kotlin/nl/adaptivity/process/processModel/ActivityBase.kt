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

import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*
import javax.xml.namespace.QName


/**
 * Base class for activity implementations
 * Created by pdvrieze on 23/11/15.
 */
abstract class ActivityBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?> : ProcessNodeBase<T, M>, Activity<T, M> {

  abstract class Builder<T : ProcessNode<T, M>, M: ProcessModelBase<T, M>?> : ProcessNodeBase.Builder<T,M>, Activity.Builder<T,M>, SimpleXmlDeserializable {

    override var message: IXmlMessage?
    override var name: String?
    override var condition: String?
    override val idBase:String
        get() = "ac"

    constructor(): this(id = null)

    constructor(id: String? = null,
                predecessor: Identified? = null,
                successor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                message: XmlMessage? = null,
                condition: String? = null,
                name: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN) : super(id, listOfNotNull(predecessor), listOfNotNull(successor), label, defines, results, x, y) {
      this.message = message
      this.name = name
      this.condition = condition
    }

    constructor(node: Activity<*, *>) : super(node) {
      this.message = XmlMessage.get(node.message)
      this.name = node.name
      this.condition = node.condition
    }

    override abstract fun build(newOwner: M): ProcessNode<T, M>

    override val elementName: QName get() = Activity.ELEMENTNAME

    @Throws(XmlException::class)
    override fun deserializeChild(reader: XmlReader): Boolean {
      if (Engine.NAMESPACE == reader.namespaceUri) {
        when (reader.localName.toString()) {
          XmlDefineType.ELEMENTLOCALNAME -> (defines as MutableList).add(XmlDefineType.deserialize(reader))

          XmlResultType.ELEMENTLOCALNAME -> (results as MutableList).add(XmlResultType.deserialize(reader))

          Condition.ELEMENTLOCALNAME -> condition = XmlCondition.deserialize(reader).condition

          XmlMessage.ELEMENTLOCALNAME -> message=XmlMessage.deserialize(reader)

          else -> return false
        }
        return true
      }
      return false
    }

    override fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      when (attributeLocalName.toString()) {
        ProcessNodeBase.ATTR_PREDECESSOR -> predecessors.replaceBy(Identifier(attributeValue.toString()))
        "name" -> name = attributeValue.toString()
        else -> return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
      }
      return true
    }

    override fun deserializeChildText(elementText: CharSequence): Boolean {
      return false
    }

    override fun toString(): String {
      return "${super.toString().dropLast(1)}, message=$message, name=$name, condition=$condition)"
    }


  }

  private var _message: XmlMessage? = null

  private var _name: String? = null

  override var name:String?
    get() = _name
    set(value) { _name = value}

  override var predecessor: Identifiable?
    get() = if (predecessors.isEmpty()) null else predecessors.single()
    set(value) {
      setPredecessors(listOfNotNull(value?.identifier))
    }

  override var message: IXmlMessage?
    get() = _message
    set(value) {
      _message = XmlMessage.get(value)
    }

  fun setMessage(message: XmlMessage?) {
    _message = message
  }

  // Object Initialization
  @Deprecated("Don't use")
  constructor(ownerModel: M) : super(ownerModel) { }

  constructor(builder: Activity.Builder<*, *>, newOwnerModel: M) : super(builder, newOwnerModel) {
    this._message = XmlMessage.get(builder.message)
    this._name = builder.name
  }
  // Object Initialization end

  override abstract fun builder(): Builder<T, M>

  override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
    return visitor.visitActivity(this)
  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    out.smartStartTag(Activity.ELEMENTNAME)
    serializeAttributes(out)
    serializeChildren(out)
    out.endTag(Activity.ELEMENTNAME)
  }

  @Throws(XmlException::class)
  override fun serializeAttributes(out: XmlWriter) {
    super.serializeAttributes(out)
    out.writeAttribute(ProcessNodeBase.ATTR_PREDECESSOR, predecessor?.id)
    out.writeAttribute("name", name)
  }

  @Throws(XmlException::class)
  override fun serializeChildren(out: XmlWriter) {
    super.serializeChildren(out)
    serializeCondition(out)

    _message?.serialize(out)
  }

  @Throws(XmlException::class)
  protected abstract fun serializeCondition(out: XmlWriter)

  /* Override to make public */
  override fun setDefines(defines: Collection<IXmlDefineType>) = super.setDefines(defines)

  /* Override to make public */
  override fun setResults(results: Collection<IXmlResultType>) = super.setResults(results)

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    if (!super.equals(other)) {
      return false
    }

    val that = other as ActivityBase<*, *>?

    if (_message != that!!._message) {
      return false
    }
    val condition = condition
    if (condition != that.condition) {
      return false
    }
    return _name == that._name

  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (_message?.hashCode() ?: 0)
    result = 31 * result + (condition?.hashCode() ?: 0)
    result = 31 * result + (_name?.hashCode() ?: 0)
    return result
  }

  // Property acccessors end
}
