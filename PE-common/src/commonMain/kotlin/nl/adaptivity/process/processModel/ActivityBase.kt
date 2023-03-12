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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.XmlActivity
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Base class for activity implementations
 * Created by pdvrieze on 23/11/15.
 */
abstract class ActivityBase(
    builder: Activity.Builder,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : ProcessNodeBase(builder, newOwner, otherNodes), Activity {

    @Deprecated("Not needed, use id.", replaceWith = ReplaceWith("id"))
    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override var name: String? = builder.name

    override val predecessor: Identifiable? get() = predecessors.singleOrNull()

    override val successor: Identifiable?
        get() = successors.singleOrNull()


    abstract override fun builder(): Activity.Builder

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityBase) return false
        if (!super.equals(other)) return false

        @Suppress("DEPRECATION")
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        @Suppress("DEPRECATION")
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    @Suppress("RemoveRedundantQualifierName")
    abstract class BaseBuilder : ProcessNodeBase.Builder, Activity.Builder {

        @Deprecated("Not needed, use id.", replaceWith = ReplaceWith("id"))
        @Suppress("OverridingDeprecatedMember")
        final override var name: String? = null

        final override var condition: Condition? = null

        override val idBase: String
            get() = "ac"

        final override var predecessor: Identifiable? = null

        final override var successor: Identifiable? = null

        constructor(): this(null, null, null, null, null, null, null, null, Double.NaN, Double.NaN, false)

        constructor(
            id: String?,
            predecessor: Identifiable?,
            successor: Identifiable?,
            label: String?,
            defines: Collection<IXmlDefineType>?,
            results: Collection<IXmlResultType>?,
            condition: Condition?,
            name: String?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean
       ) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.predecessor = predecessor
            this.successor = successor

            @Suppress("DEPRECATION")
            this.name = name
            this.condition = condition
        }

        constructor(node: Activity) : super(node) {
            @Suppress("DEPRECATION")
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
    class SerialDelegate : ProcessNodeBase.SerialDelegate {
        var childId: Identifier? = null
            private set

        var message: XmlMessage? = null
            private set

        var predecessor: Identifier? = null
            private set

        @SerialName("condition")
        @XmlSerialName(Condition.ELEMENTLOCALNAME, "", "")
        @XmlElement(false)
        var rawCondition: String? = null
            private set

        @SerialName("labeledCondition")
        var elementCondition: XmlCondition? = null
            get() = field ?: rawCondition?.let { XmlCondition(it)}
            private set

        var name: String? = null
            private set

        constructor(
            id: String?,
            label: String?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean,
            predecessor: Identifier?,
            defines: List<XmlDefineType>,
            results: List<XmlResultType>,
            message: XmlMessage?,
            childId: Identifier?,
            condition: XmlCondition?,
            name: String?,
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
            when (condition?.label) {
                null -> rawCondition = condition?.condition
                else -> elementCondition
            }
            this.name = name
        }

        @Suppress("DEPRECATION")
        constructor(node: MessageActivity) : this(
            id = node.id,
            label = node.label,
            x = node.x,
            y = node.y,
            isMultiInstance = node.isMultiInstance,
            predecessor = node.predecessor?.identifier,
            defines = node.defines.map { XmlDefineType(it) },
            results = node.results.map { XmlResultType(it) },
            message = XmlMessage.from(node.message),
            condition = node.condition?.let { XmlCondition(it.condition, it.label) },
            name = node.name,
            childId = null
        )

        @Suppress("DEPRECATION")
        constructor(node: MessageActivity.Builder) : this(
            id = node.id,
            label = node.label,
            x = node.x,
            y = node.y,
            isMultiInstance = node.isMultiInstance,
            predecessor = node.predecessor?.identifier,
            defines = node.defines.map { XmlDefineType(it) },
            results = node.results.map { XmlResultType(it) },
            message = XmlMessage.from(node.message),
            condition = node.condition?.let { XmlCondition(it.condition, it.label) },
            name = node.name,
            childId = null
        )

        @Suppress("DEPRECATION")
        constructor(node: CompositeActivity) : this(
            id = node.id,
            label = node.label,
            x = node.x,
            y = node.y,
            isMultiInstance = node.isMultiInstance,
            predecessor = node.predecessor?.identifier,
            defines = node.defines.map { XmlDefineType(it) },
            results = node.results.map { XmlResultType(it) },
            message = null,
            childId = node.childModel?.identifier,
            condition = node.condition?.let { XmlCondition(it.condition, it.label) },
            name = node.name
        )

        @Suppress("DEPRECATION")
        constructor(node: CompositeActivity.ReferenceBuilder) : this(
            id = node.id,
            label = node.label,
            x = node.x,
            y = node.y,
            isMultiInstance = node.isMultiInstance,
            predecessor = node.predecessor?.identifier,
            defines = node.defines.map { XmlDefineType(it) },
            results = node.results.map { XmlResultType(it) },
            message = null,
            childId = node.childId?.let { Identifier(it) },
            condition = node.condition?.let { XmlCondition(it.condition, it.label) },
            name = node.name
        )

        @Suppress("DEPRECATION")
        constructor(node: CompositeActivity.ModelBuilder) : this(
            id = node.id,
            label = node.label,
            x = node.x,
            y = node.y,
            isMultiInstance = node.isMultiInstance,
            predecessor = node.predecessor?.identifier,
            defines = node.defines.map { XmlDefineType(it) },
            results = node.results.map { XmlResultType(it) },
            message = null,
            childId = node.childId?.let { Identifier(it) },
            condition = node.condition?.let { XmlCondition(it.condition, it.label) },
            name = node.name
        )

    }

    open class ReferenceActivityBuilder : BaseBuilder, CompositeActivity.ReferenceBuilder {
        final override var childId: String?

        @Suppress("unused")
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

    open class CompositeActivityBuilder : ChildProcessModelBase.ModelBuilder,
                                          CompositeActivity.ModelBuilder {
        @Deprecated("Not needed, use id.", replaceWith = ReplaceWith("id"))
        @Suppress("OverridingDeprecatedMember")
        final override var name: String? = null
        final override var id: String?
        final override var condition: Condition?
        final override var label: String?
        final override var x: Double
        final override var y: Double
        final override var isMultiInstance: Boolean
        final override var predecessor: Identifiable? = null
        final override var successor: Identifiable? = null

        final override var defines: MutableCollection<IXmlDefineType> = mutableListOf()
            set(value) {
                field.replaceBy(value)
            }

        final override var results: MutableCollection<IXmlResultType> = mutableListOf()
            set(value) {
                field.replaceBy(value)
            }

        override val idBase: String get() = "child"

        @Suppress("unused")
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

        @Suppress("unused")
        fun buildActivity(
            buildHelper: ProcessModel.BuildHelper<*, *, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): XmlActivity {
            return XmlActivity(this, buildHelper, otherNodes)
        }

    }

}
