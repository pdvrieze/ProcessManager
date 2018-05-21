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

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.collection.replaceByNotNull
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*


/**
 * Base class for activity implementations
 * Created by pdvrieze on 23/11/15.
 */
@Serializable
abstract class ActivityBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNodeBase<NodeT, ModelT>, Activity<NodeT, ModelT> {

    @Transient
    private var _message: XmlMessage? = null

    @SerialName("name")
    @Optional
    private var _name: String? = null

    private val childId: String? get()= childModel?.id

    @Transient
    override val childModel: ChildProcessModel<NodeT, ModelT>?

    @Transient
    @Suppress("OverridingDeprecatedMember")
    override var name:String?
        get() = _name
        set(value) { _name = value}

    final override var predecessor: Identifiable? = null

    @Transient
    final override val successor: Identifiable? get() = successors.singleOrNull()

    override var message: IXmlMessage?
        get() = _message
        set(value) {
            _message = XmlMessage.get(value)
        }

    fun setMessage(message: XmlMessage?) {
        _message = message
    }

    constructor(builder: Activity.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) : super(builder, buildHelper) {
        if(builder.message!=null && builder.childId!=null) throw IllegalProcessModelException("Activities can not have child models as well as messages")
        this._message = XmlMessage.get(builder.message)
        @Suppress("DEPRECATION")
        _name = builder.name
        @Suppress("LeakingThis")
        childModel = builder.childId?.let{ buildHelper.childModel(it) }
    }

    constructor(builder: Activity.ChildModelBuilder<*, *>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>) : super(builder, buildHelper) {
        this._message = null
        this._name = null
        @Suppress("LeakingThis")
        this.childModel = buildHelper.childModel(builder.childId!!)
    }


    override abstract fun builder(): Builder<NodeT, ModelT>

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitActivity(this)
    }

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(Activity.ELEMENTNAME) {
            serializeAttributes(this)
            serializeChildren(this)
        }
    }

    override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        out.writeAttribute(ProcessNodeBase.ATTR_PREDECESSOR, predecessor?.id)
        out.writeAttribute("childId", childModel?.id)
        out.writeAttribute("name", name)
    }

    override fun serializeChildren(out: XmlWriter) {
        super.serializeChildren(out)
        serializeCondition(out)

        _message?.serialize(out)
    }

    protected abstract fun serializeCondition(out: XmlWriter)

    /* Override to make public */
    override fun setDefines(defines: Collection<IXmlDefineType>) = super.setDefines(defines)

    /* Override to make public */
    override fun setResults(results: Collection<IXmlResultType>) = super.setResults(results)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityBase<*, *>) return false
        if (!super.equals(other)) return false

        if (_message != other._message) return false
        if (_name != other._name) return false
        if (childModel != other.childModel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (_message?.hashCode() ?: 0)
        result = 31 * result + (_name?.hashCode() ?: 0)
        result = 31 * result + (childModel?.hashCode() ?: 0)
        return result
    }


    @Serializable
    abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT: ProcessModel<NodeT, ModelT>?> :
        ProcessNodeBase.Builder<NodeT,ModelT>,
        Activity.Builder<NodeT,ModelT>,
        SimpleXmlDeserializable {

        final override var message: IXmlMessage?
        final override var name: String?
        final override var condition: String?
        @Transient
        override val idBase:String
            get() = "ac"

        final override var childId: String? = null

        @Transient
        override val elementName: QName get() = Activity.ELEMENTNAME

        final override var predecessor: Identifiable? = null

        @Transient
        final override var successor: Identifiable? = null


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
                    y: Double = Double.NaN,
                    multiInstance: Boolean = false) : super(id, label, defines, results, x, y, multiInstance) {
            this.predecessor = predecessor
            this.successor = successor
            this.message = message
            this.name = name
            this.condition = condition
        }

        constructor(node: Activity<*, *>) : super(node) {
            message = XmlMessage.get(node.message)
            name = node.name
            condition = node.condition
            childId = node.childModel?.id
            predecessor = node.predecessor
            successor = node.successor
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            if (Engine.NAMESPACE == reader.namespaceURI) {
                when (reader.localName) {
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

        override fun deserializeAttribute(attributeNamespace: String?, attributeLocalName: String, attributeValue: String): Boolean {
            @Suppress("DEPRECATION")
            when (attributeLocalName) {
                ProcessNodeBase.ATTR_PREDECESSOR -> predecessor=Identifier(attributeValue)
                "name" -> name = attributeValue
                ATTR_CHILDID -> childId = attributeValue
                else -> return super<ProcessNodeBase.Builder>.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
            }
            return true
        }

        override fun deserializeChildText(elementText: CharSequence): Boolean {
            return false
        }

        override fun toString(): String {
            @Suppress("DEPRECATION")
            return "${super.toString().dropLast(1)}, message=$message, name=$name, condition=$condition)"
        }


    }

    abstract class ChildModelBuilder<NodeT : ProcessNode<NodeT, ModelT>, ModelT: ProcessModel<NodeT, ModelT>?>(
        id: String? = null,
        override var childId: String? = null,
        nodes: Collection<ProcessNode.IBuilder<NodeT, ModelT>> = emptyList(),
        override var predecessor: Identifiable? = null,
        @Transient override var successor: Identifiable? = null,
        label: String? = null,
        imports: Collection<IXmlResultType> = emptyList(),
        defines: Collection<IXmlDefineType> = emptyList(),
        exports: Collection<IXmlDefineType> = emptyList(),
        results: Collection<IXmlResultType> = emptyList(),
        x: Double = Double.NaN,
        y: Double = Double.NaN,
        multiInstance: Boolean) : ProcessNodeBase.Builder<NodeT, ModelT>(id, label, defines, results, x, y, multiInstance), Activity.ChildModelBuilder<NodeT,ModelT> {

        override val nodes: MutableList<ProcessNode.IBuilder<NodeT, ModelT>> = nodes.toMutableList()
        override val imports: MutableList<IXmlResultType> = imports.toMutableList()
        override val exports: MutableList<IXmlDefineType> = exports.toMutableList()
        override var condition: String? = null

        override val idBase:String get() = "child"

        override val elementName: QName get() = ChildProcessModel.ELEMENTNAME

        override fun deserializeAttribute(attributeNamespace: String?, attributeLocalName: String, attributeValue: String)=false

    }


    companion object {
        const val ATTR_CHILDID = "childId"
    }

}
