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
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*

import javax.xml.namespace.QName
import java.util.Collections


/**
 * Base class for activity implementations
 * Created by pdvrieze on 23/11/15.
 */
abstract class ActivityBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : ProcessNodeBase<T, M>, Activity<T, M>, SimpleXmlDeserializable {

  abstract class Builder<T : ProcessNode<T, M>, M: ProcessModelBase<T, M>> : ProcessNodeBase.Builder<T,M>, Activity.Builder<T,M> {

    override var message: IXmlMessage?
    override var name: String?
    override var condition: String?

    constructor(predecessor: Identifiable? = null,
                successor: Identifiable? = null,
                id: String? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                message: XmlMessage? = null,
                condition: String? = null,
                name: String? = null) : super(listOfNotNull(predecessor), listOfNotNull(successor), id, label, x, y, defines, results) {
      this.message = message
      this.name = name
      this.condition = condition
    }

    constructor(node: Activity<*, *>) : super(node) {
      this.message = XmlMessage.get(node.message)
      this.name = node.name
      this.condition = node.condition
    }

    override abstract fun build(newOwner: M): ActivityBase<T, M>
  }

  private var _message: XmlMessage? = null

  private var _name: String? = null

  override var name:String?
    get() = _name
    set(value) { _name = value}

  override var predecessor: Identifiable?
    get() = if (predecessors.isEmpty()) null else predecessors.single()
    set(value) {
      setPredecessors(listOfNotNull(value))
    }

  override var message: IXmlMessage?
    get() = _message
    set(value) {
      _message = XmlMessage.get(value)
    }

  fun setMessage(message: XmlMessage?) {
    _message = message
  }

  override val elementName: QName get() = Activity.ELEMENTNAME

  constructor(orig: Activity<*, *>, newOwner: M? = null) : super(orig, newOwner) {
    _message = XmlMessage.get(orig.message)
    _name = orig.name
  }

  // Object Initialization
  @Deprecated("Don't use")
  constructor(ownerModel: M?) : super(ownerModel) { }

  constructor(_ownerModel: M?, predecessors: Collection<Identifiable>, successors: Collection<Identifiable>, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>, _message: XmlMessage?, _name: String?) : super(_ownerModel, predecessors, successors, id, label, x, y, defines, results) {
    this._message = _message
    this._name = _name
  }

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
  override fun deserializeChild(reader: XmlReader): Boolean {
    if (Engine.NAMESPACE == reader.namespaceUri) {
      when (reader.localName.toString()) {
        XmlDefineType.ELEMENTLOCALNAME -> (defines as MutableList).add(XmlDefineType.deserialize(reader))

        XmlResultType.ELEMENTLOCALNAME -> (results as MutableList).add(XmlResultType.deserialize(reader))

        Condition.ELEMENTLOCALNAME -> deserializeCondition(reader)

        XmlMessage.ELEMENTLOCALNAME -> setMessage(XmlMessage.deserialize(reader))

        else -> return false
      }
      return true
    }
    return false
  }

  override fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
    when (attributeLocalName.toString()) {
      ProcessNodeBase.ATTR_PREDECESSOR -> predecessor = Identifier(attributeValue.toString())
      "name" -> name = attributeValue.toString()
      else -> return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
    }
    return true
  }

  @Throws(XmlException::class)
  protected abstract fun deserializeCondition(reader: XmlReader)

  override fun deserializeChildText(elementText: CharSequence): Boolean {
    return false
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
  public override fun setDefines(exports: Collection<IXmlDefineType>) = super.setDefines(exports)

  /* Override to make public */
  override fun setResults(imports: Collection<IXmlResultType>) = super.setResults(imports)

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    if (o == null || javaClass != o.javaClass) {
      return false
    }
    if (!super.equals(o)) {
      return false
    }

    val that = o as ActivityBase<*, *>?

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
