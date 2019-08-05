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

import kotlinx.serialization.*
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.XmlActivity
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.SerialClassDescImpl
import nl.adaptivity.util.addField
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.xmlutil.util.SimpleXmlDeserializable
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Base class for activity implementations
 * Created by pdvrieze on 23/11/15.
 */
@Serializable
abstract class ActivityBase : ProcessNodeBase, Activity {

    @SerialName("message")
    @Serializable(with = IXmlMessage.Companion::class)
    internal var _message: XmlMessage? = null

    @SerialName("name")
    internal var _name: String? = null

    @Transient
    override val childModel: ChildProcessModel<ProcessNode>?

    val childId: String?

    @Suppress("OverridingDeprecatedMember")
    override var name: String?
        get() = _name
        set(value) {
            _name = value
        }

    @Serializable(with = Identifiable.Companion::class)
    final override val predecessor: Identifiable? = predecessors.singleOrNull()

    @Transient
    final override val successor: Identifiable?
        get() = successors.singleOrNull()

    @Transient
    override var message: IXmlMessage?
        get() = _message
        set(value) {
            _message = XmlMessage.from(value)
        }

    fun setMessage(message: XmlMessage?) {
        _message = message
    }

    constructor(builder: ActivityBase.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) : super(
        builder,
        buildHelper.newOwner
                                                                                                         ) {
        if (builder.message != null && builder.childId != null) throw IllegalProcessModelException(
            "Activities can not have child models as well as messages"
                                                                                                  )
        this._message = XmlMessage.from(builder.message)
        @Suppress("DEPRECATION")
        _name = builder.name
        @Suppress("LeakingThis")
        childModel = builder.childId?.let { buildHelper.childModel(it) }
        childId = childModel?.id
    }

    constructor(builder: MessageActivity.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        super(builder, buildHelper.newOwner) {
        this._message = XmlMessage.from(builder.message)
        @Suppress("DEPRECATION")
        _name = builder.name

        childModel = null
        childId = null
    }

    constructor(builder: CompositeActivity.ReferenceBuilder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        super(builder, buildHelper.newOwner) {
        this._message = null
        @Suppress("DEPRECATION")
        _name = null

        val childModel = buildHelper.childModel(builder.childId!!)
        @Suppress("LeakingThis")
        this.childModel = childModel
        childId = childModel.id
    }

    constructor(builder: CompositeActivity.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        super(builder, buildHelper.newOwner) {

        this._message = null
        this._name = null
        val childModel = buildHelper.childModel(builder.childId!!)
        @Suppress("LeakingThis")
        this.childModel = childModel
        childId = childModel.id
    }


    override fun builder(): Activity.Builder = Builder(this)

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
        @Suppress("DEPRECATION")
        out.writeAttribute("name", name)
    }

    override fun serializeChildren(out: XmlWriter) {
        super.serializeChildren(out)
        serializeCondition(out)

        _message?.serialize(out)
    }

    protected abstract fun serializeCondition(out: XmlWriter)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityBase) return false
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
    abstract class BaseBuilder : ProcessNodeBase.Builder, Activity.Builder, SimpleXmlDeserializable {

        @Serializable(with = IXmlMessage.Companion::class)
        final var message: IXmlMessage?

        @Suppress("OverridingDeprecatedMember")
        final var name: String?

        @Serializable(XmlCondition.Companion::class)
        final override var condition: Condition? = null

        @Transient
        override val idBase: String
            get() = "ac"

        @Transient
        override val elementName: QName
            get() = Activity.ELEMENTNAME

        @SerialName("predecessor")
        @XmlSerialName("predecessor", "", "")
        @Serializable(with = Identifiable.Companion::class)
        final override var predecessor: Identifiable? = null

        @Transient
        final override var successor: Identifiable? = null


        constructor() : this(id = null)

        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            message: XmlMessage? = null,
            condition: Condition? = null,
            name: String? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            multiInstance: Boolean = false
                   ) : super(id, label, defines, results, x, y, multiInstance) {
            this.predecessor = predecessor
            this.successor = successor
            this.message = message

            @Suppress("DEPRECATION")
            this.name = name
            this.condition = condition
        }

        constructor(node: Activity) : super(node) {
            message = XmlMessage.from(node.message)

            @Suppress("DEPRECATION")
            name = node.name
            condition = node.condition
            predecessor = node.predecessor
            successor = node.successor
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            if (Engine.NAMESPACE == reader.namespaceURI) {
                when (reader.localName) {
                    XmlDefineType.ELEMENTLOCALNAME -> (defines as MutableList).add(XmlDefineType.deserialize(reader))

                    XmlResultType.ELEMENTLOCALNAME -> (results as MutableList).add(XmlResultType.deserialize(reader))

                    Condition.ELEMENTLOCALNAME     -> condition = XmlCondition.deserialize(reader)

                    XmlMessage.ELEMENTLOCALNAME    -> message = XmlMessage.deserialize(reader)

                    else                           -> return false
                }
                return true
            }
            return false
        }

        override fun deserializeAttribute(
            attributeNamespace: String?,
            attributeLocalName: String,
            attributeValue: String
                                         ): Boolean {
            @Suppress("DEPRECATION")
            when (attributeLocalName) {
                ProcessNodeBase.ATTR_PREDECESSOR -> predecessor = Identifier(attributeValue)
                "name" -> name = attributeValue
                CompositeActivityBase.ATTR_CHILDID -> throw IllegalProcessModelException("child ID in message activity")
                else -> return super<ProcessNodeBase.Builder>.deserializeAttribute(
                    attributeNamespace, attributeLocalName, attributeValue
                                                                                  )
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

    @Serializable
    open class Builder : BaseBuilder, MessageActivity.Builder, CompositeActivity.ReferenceBuilder,
                         SimpleXmlDeserializable {
        final override var childId: String? = null

        constructor() : super()
        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            message: XmlMessage? = null,
            condition: Condition? = null,
            name: String? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            multiInstance: Boolean = false
                   ) : super(
            id,
            predecessor,
            successor,
            label,
            defines,
            results,
            message,
            condition,
            name,
            x,
            y,
            multiInstance
                            )

        constructor(node: Activity) : super(node) {
            childId = node.childModel?.id
        }


        constructor(node: CompositeActivity) : super(node) {
            childId = node.childModel.id ?: throw IllegalProcessModelException("Missing child id in composite activity")
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R = when (childId) {
            null -> visitor.visitActivity(this as MessageActivity.Builder)
            else -> visitor.visitActivity(this as CompositeActivity.ReferenceBuilder)
        }
    }

    @Serializable
    open class ReferenceActivityBuilder : BaseBuilder, CompositeActivity.ReferenceBuilder {
        constructor() : super()
        constructor(
            id: String?,
            predecessor: Identifiable?,
            successor: Identifiable?,
            label: String?,
            defines: Collection<IXmlDefineType>,
            results: Collection<IXmlResultType>,
            message: XmlMessage?,
            condition: Condition?,
            name: String?,
            x: Double,
            y: Double,
            multiInstance: Boolean
                   ) : super(
            id,
            predecessor,
            successor,
            label,
            defines,
            results,
            message,
            condition,
            name,
            x,
            y,
            multiInstance
                            )

        final override var childId: String? = null

        constructor(node: CompositeActivity) : super(node) {
            childId = node.childModel.id ?: throw IllegalProcessModelException("Missing child id in composite activity")
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R {
            return visitor.visitActivity(this)
        }
    }

    @Serializable
    open class CompositeActivityBuilder : ChildProcessModelBase.Builder,
                                          CompositeActivity.Builder {

        override var id: String?
        @Serializable(XmlCondition.Companion::class)
        override var condition: Condition?
        override var label: String?
        @XmlDefault("NaN")
        override var x: Double
        @XmlDefault("NaN")
        override var y: Double
        override var isMultiInstance: Boolean
        @Serializable(with = Identifiable.Companion::class)
        override var predecessor: Identifiable? = null
        @Transient
        override var successor: Identifiable? = null

        @Serializable(IXmlDefineTypeListSerializer::class)
        @SerialName("define")
        override var defines: MutableCollection<IXmlDefineType> = mutableListOf()
            set(value) {
                field.replaceBy(value)
            }

        @Serializable(IXmlResultTypeListSerializer::class)
        @SerialName("result")
        override var results: MutableCollection<IXmlResultType> = mutableListOf()
            set(value) {
                field.replaceBy(value)
            }

        override val idBase: String get() = "child"

        override val elementName: QName get() = ChildProcessModel.ELEMENTNAME

        private constructor() : super() {
            id = null
            condition = null
            label = null
            x = Double.NaN
            y = Double.NaN
            isMultiInstance = false

            defines = mutableListOf()
            results = mutableListOf()
        }

        constructor(
            rootBuilder: RootProcessModel.Builder,
            id: String? = null,
            childId: String? = null,
            nodes: Collection<ProcessNode.Builder> = emptyList(),
            predecessor: Identifiable? = null,
            condition: Condition? = null,
            successor: Identifiable? = null,
            label: String? = null,
            imports: Collection<IXmlResultType> = emptyList(),
            defines: Collection<IXmlDefineType> = emptyList(),
            exports: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false
                   ) : super(
            rootBuilder, childId, nodes, imports,
            exports
                            ) {
            this.id = id
            this.condition = condition
            this.label = label
            this.x = x
            this.y = y
            this.isMultiInstance = isMultiInstance
            this.predecessor = predecessor
            this.successor = successor
            this.defines = defines.toMutableList()
            this.results = results.toMutableList()
        }

        override fun <T : ProcessNode> build(buildHelper: ProcessModel.BuildHelper<T, *, *, *>): T =
            buildHelper.node(this)

        fun buildActivity(buildHelper: ProcessModel.BuildHelper<*, *, *, *>): XmlActivity {
            return XmlActivity(this, buildHelper)
        }

        @Serializer(forClass = CompositeActivityBuilder::class)
        companion object : ChildProcessModelBase.Builder.BaseSerializer<CompositeActivityBuilder>() {
            override val descriptor: SerialDescriptor =
                SerialClassDescImpl(Builder.serializer().descriptor, ChildProcessModelBase.Builder::class.name).apply {
                    addField(CompositeActivityBuilder::childId)
                }

            override fun builder(): CompositeActivityBuilder {
                return CompositeActivityBuilder()
            }
        }
    }

}
