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
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.SimpleXmlDeserializable


/**
 * Base class for activity implementations
 * Created by pdvrieze on 23/11/15.
 */
@Serializable
abstract class ActivityBase : ProcessNodeBase, Activity {

    @SerialName("name")
    internal var _name: String? = null

    @Suppress("OverridingDeprecatedMember")
    override var name: String?
        get() = _name
        set(value) {
            _name = value
        }

    @Required
    @Serializable(with = Identifiable.Companion::class)
    final override val predecessor: Identifiable? = predecessors.singleOrNull()

    @Transient
    final override val successor: Identifiable?
        get() = successors.singleOrNull()

    constructor(builder: Activity.Builder, newOwner: ProcessModel<*>, otherNodes: Iterable<ProcessNode.Builder>) :
        super(builder, newOwner, otherNodes) {
        _name = builder.name
    }


    override abstract fun builder(): Activity.Builder

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(Activity.ELEMENTNAME) {
            serializeAttributes(this)
            serializeChildren(this)
        }
    }

    override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        out.writeAttribute(ATTR_PREDECESSOR, predecessor?.id)
        @Suppress("DEPRECATION")
        out.writeAttribute("name", name)
    }

    override fun serializeChildren(out: XmlWriter) {
        super.serializeChildren(out)
        serializeCondition(out)
    }

    protected abstract fun serializeCondition(out: XmlWriter)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityBase) return false
        if (!super.equals(other)) return false

        if (_name != other._name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (_name?.hashCode() ?: 0)
        return result
    }


    @Serializable
    abstract class BaseBuilder : ProcessNodeBase.Builder, Activity.Builder {

        @Suppress("OverridingDeprecatedMember")
        @XmlDefault("null")
        final override var name: String? = null

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


        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            condition: Condition? = null,
            name: String? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            multiInstance: Boolean = false
                   ) : super(id, label, defines, results, x, y, multiInstance) {
            this.predecessor = predecessor
            this.successor = successor

            this.name = name
            this.condition = condition
        }

        constructor(node: Activity) : super(node) {
            name = node.name
            condition = node.condition
            predecessor = node.predecessor
            successor = node.successor
        }

        override fun toString(): String {
            @Suppress("DEPRECATION")
            return "${super.toString().dropLast(1)}, name=$name, condition=$condition)"
        }


    }

    @Serializable
    open class DeserializationBuilder : BaseBuilder, MessageActivity.Builder, CompositeActivity.ReferenceBuilder,
                                        SimpleXmlDeserializable {
        override var childId: String? = null

        @Serializable(with = IXmlMessage.Companion::class)
        override var message: IXmlMessage? = null

        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            message: XmlMessage? = null,
            childId: String? = null,
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
            condition,
            name,
            x,
            y,
            multiInstance
                            ) {
            this.message = message
            this.childId = childId
        }


        constructor(node: Activity) : super(node) {
            childId = (node as? CompositeActivity)?.childModel?.id
            message = (node as? MessageActivity)?.message
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R = when (childId) {
            null -> visitor.visitActivity(this as MessageActivity.Builder)
            else -> visitor.visitActivity(this as CompositeActivity.ReferenceBuilder)
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
                ATTR_PREDECESSOR                   -> predecessor = Identifier(attributeValue)
                "name"                             -> name = attributeValue
                CompositeActivityBase.ATTR_CHILDID -> throw IllegalProcessModelException("child ID in message activity")
                else                               -> return super<BaseBuilder>.deserializeAttribute(
                    attributeNamespace, attributeLocalName, attributeValue
                                                                                                    )
            }
            return true
        }

        override fun deserializeChildText(elementText: CharSequence): Boolean {
            return false
        }

    }

    @Serializable
    open class ReferenceActivityBuilder : BaseBuilder, CompositeActivity.ReferenceBuilder {
        final override var childId: String?

        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            childId: String? = null,
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
            condition,
            name,
            x,
            y,
            multiInstance
                            ) {
            this.childId = childId
        }

        constructor(node: CompositeActivity) : super(node) {
            childId =
                node.childModel?.id ?: throw IllegalProcessModelException("Missing child id in composite activity")
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R {
            return visitor.visitActivity(this)
        }
    }

    @Serializable
    open class CompositeActivityBuilder : ChildProcessModelBase.ModelBuilder,
                                          CompositeActivity.ModelBuilder {
        override var name: String? = null
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

        override fun <T : ProcessNode> build(
            buildHelper: ProcessModel.BuildHelper<T, *, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
                                            ): T =
            buildHelper.node(this, otherNodes)

        fun buildActivity(
            buildHelper: ProcessModel.BuildHelper<*, *, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
                         ): XmlActivity {
            return XmlActivity(this, buildHelper, otherNodes)
        }

        @Serializer(forClass = CompositeActivityBuilder::class)
        companion object : ChildProcessModelBase.ModelBuilder.BaseSerializer<CompositeActivityBuilder>() {
            override val descriptor: SerialDescriptor =
                SerialClassDescImpl(
                    DeserializationBuilder.serializer().descriptor,
                    ChildProcessModelBase.ModelBuilder::class.name
                                   ).apply {
                    addField(CompositeActivityBuilder::childId)
                }

            override fun builder(): CompositeActivityBuilder {
                return CompositeActivityBuilder()
            }
        }
    }

}
