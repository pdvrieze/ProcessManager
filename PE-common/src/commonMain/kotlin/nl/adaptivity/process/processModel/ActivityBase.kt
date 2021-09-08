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

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.XmlActivity
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.XmlSerialName


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
            defines: Collection<IXmlDefineType>? = emptyList(),
            results: Collection<IXmlResultType>? = emptyList(),
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
    @SerialName(Activity.ELEMENTLOCALNAME)
    @XmlSerialName(Activity.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    open class SerialDelegate : ProcessNodeBase.SerialDelegate {
        var childId: Identifier? = null

        var message: XmlMessage? = null

        var predecessor: Identifier? = null

        var condition: XmlCondition? = null

        var name: String? = null

        constructor(
            id: String? = null,
            label: String? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false,
            predecessor: Identifier? = null,
            defines: List<XmlDefineType> = emptyList(),
            results: List<XmlResultType> = emptyList(),
            message: XmlMessage? = null,
            childId: Identifier? = null,
            condition: XmlCondition? = null,
            name: String? = null,
        ) : super(
            id = id,
            label = label,
            defines = defines,
            results = results,
            x = x,
            y = y,
            isMultiInstance = isMultiInstance
        ) {
            this.predecessor = predecessor
            this.message = message
            this.childId = childId
            this.condition = condition
            this.name = name
        }

        constructor(node: MessageActivity) : this(
            node.id,
            node.label,
            node.x,
            node.y,
            node.isMultiInstance,
            node.predecessor?.identifier,
            node.defines.map { XmlDefineType(it) },
            node.results.map { XmlResultType(it) },
            XmlMessage.from(node.message),
            condition = node.condition?.let { XmlCondition(it.condition) },
            name = node.name
        )

        constructor(node: MessageActivity.Builder) : this(
            node.id,
            node.label,
            node.x,
            node.y,
            node.isMultiInstance,
            node.predecessor?.identifier,
            node.defines.map { XmlDefineType(it) },
            node.results.map { XmlResultType(it) },
            XmlMessage.from(node.message),
            condition = node.condition?.let { XmlCondition(it.condition) },
            name = node.name
        )

        constructor(node: CompositeActivity) : this(
            node.id,
            node.label,
            node.x,
            node.y,
            node.isMultiInstance,
            node.predecessor?.identifier,
            node.defines.map { XmlDefineType(it) },
            node.results.map { XmlResultType(it) },
            childId = node.childModel?.identifier,
            condition = node.condition?.let { XmlCondition(it.condition) },
            name = node.name
        )

        constructor(node: CompositeActivity.ReferenceBuilder) : this(
            node.id,
            node.label,
            node.x,
            node.y,
            node.isMultiInstance,
            node.predecessor?.identifier,
            node.defines.map { XmlDefineType(it) },
            node.results.map { XmlResultType(it) },
            childId = node.childId?.let { Identifier(it) },
            condition = node.condition?.let { XmlCondition(it.condition) },
            name = node.name
        )

        constructor(node: CompositeActivity.ModelBuilder) : this(
            node.id,
            node.label,
            node.x,
            node.y,
            node.isMultiInstance,
            node.predecessor?.identifier,
            node.defines.map { XmlDefineType(it) },
            node.results.map { XmlResultType(it) },
            childId = node.childId?.let { Identifier(it) },
            condition = node.condition?.let { XmlCondition(it.condition) },
            name = node.name
        )

    }

    @Serializable
    @SerialName(StartNode.ELEMENTLOCALNAME)
    @XmlSerialName(StartNode.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    open class DeserializationBuilder : BaseBuilder, MessageActivity.Builder, CompositeActivity.ReferenceBuilder {
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
        final override var name: String? = null
        final override var id: String?
        @Serializable(XmlCondition.Companion::class)
        final override var condition: Condition?
        final override var label: String?
        @XmlDefault("NaN")
        final override var x: Double
        @XmlDefault("NaN")
        final override var y: Double
        final override var isMultiInstance: Boolean
        @Serializable(with = Identifiable.Companion::class)
        final override var predecessor: Identifiable? = null
        @Transient
        final override var successor: Identifiable? = null

        @Serializable(IXmlDefineTypeListSerializer::class)
        @SerialName("define")
        final override var defines: MutableCollection<IXmlDefineType> = mutableListOf()
            set(value) {
                field.replaceBy(value)
            }

        @Serializable(IXmlResultTypeListSerializer::class)
        @SerialName("result")
        final override var results: MutableCollection<IXmlResultType> = mutableListOf()
            set(value) {
                field.replaceBy(value)
            }

        override val idBase: String get() = "child"

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
       ) : super(rootBuilder, childId, nodes, imports, exports) {
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

    }

}
