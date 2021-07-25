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

package nl.adaptivity.process.processModel.engine

import kotlinx.serialization.Transient
import kotlinx.serialization.serializer
import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.engine.impl.getClass
import nl.adaptivity.process.processModel.*
import nl.adaptivity.util.multiplatform.randomUUID
import nl.adaptivity.xmlutil.XmlDeserializerFactory
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A class representing a process model.

 * @author Paul de Vrieze
 */
//@XmlDeserializer(ExecutableProcessModel.Factory::class)
class ExecutableProcessModel @JvmOverloads constructor(
    builder: RootProcessModel.Builder,
    pedantic: Boolean = true
                                                      ) :
    RootProcessModelBase<ExecutableProcessNode>(builder, EXEC_NODEFACTORY, pedantic),
    ExecutableModelCommon,
    /*MutableHandleAware<ExecutableProcessModel>,*/
    SecureObject<ExecutableProcessModel> {

    @Transient
    override val endNodeCount by lazy { modelNodes.count { it is ExecutableEndNode } }

    @Transient
    override val rootModel
        get() = this

    @Transient
    override val ref: ExecutableProcessModelRef
        get() = ProcessModelRef(name, handle, uuid)

    override fun withPermission() = this

    override fun builder(): RootProcessModel.Builder = Builder(this)

    override fun update(body: RootProcessModel.Builder.() -> Unit): ExecutableProcessModel {
        return ExecutableProcessModel(Builder(this).apply(body))
    }

    @Suppress("UNCHECKED_CAST")
    override val handle: Handle<ExecutableProcessModel>
        get() = super.handle as Handle<ExecutableProcessModel>

    /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.ProcessModel#getEndNodeCount()
       */

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (getClass() != other?.getClass()) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun toString(): String {
        return "ExecutableProcessModel() ${super.toString()}"
    }


    companion object {

        fun from(basepm: RootProcessModel<*>): ExecutableProcessModel {
            return basepm as? ExecutableProcessModel ?: ExecutableProcessModel(Builder(basepm))
        }

        @JvmStatic
        fun deserialize(reader: XmlReader): ExecutableProcessModel {
            return ExecutableProcessModel(XML.decodeFromReader<Builder>(reader))
        }

        @JvmStatic
        inline fun build(body: Builder.() -> Unit) = Builder().apply(body).also {
            if (it.uuid == null) {
                it.uuid = randomUUID()
            }
        }.let { ExecutableProcessModel(it) }

        /**
         * Helper method that helps enumerating all elements in the model

         * @param destination The collection that will contain the result.
         *
         * @param seen A set of process names that have already been seen (and should
         * *          not be added again.
         *
         * @param node The node to start extraction from. This will go on to the
         * *          successors.
         */
        private fun extractElementsTo(
            destination: MutableCollection<in ExecutableProcessNode>,
            seen: MutableSet<String>,
            node: ExecutableProcessNode
                                     ) {
            if (node.id in seen) return

            destination.add(node)
            seen.add(node.id)
            for (successor in node.successors) {
                extractElementsTo(destination, seen, successor as ExecutableProcessNode)
            }
        }


    }


    enum class Permissions : SecurityProvider.Permission {
        INSTANTIATE
    }

    class Factory : XmlDeserializerFactory<ExecutableProcessModel> {

        override fun deserialize(reader: XmlReader): ExecutableProcessModel {
            return ExecutableProcessModel.deserialize(reader)
        }
    }

}


val EXEC_BUILDER_VISITOR = object : ProcessNode.Visitor<ProcessNode.Builder> {
    override fun visitStartNode(startNode: StartNode) = ExecutableStartNode.Builder(startNode)

    override fun visitActivity(messageActivity: MessageActivity) = MessageActivityBase.Builder(messageActivity)

    override fun visitActivity(compositeActivity: CompositeActivity) =
        CompositeActivityBase.ReferenceBuilder(compositeActivity)

    override fun visitSplit(split: Split) = ExecutableSplit.Builder(split)

    override fun visitJoin(join: Join) = ExecutableJoin.Builder(join)

    override fun visitEndNode(endNode: EndNode) = ExecutableEndNode.Builder(endNode)
}

object EXEC_NODEFACTORY :
    ProcessModelBase.NodeFactory<ExecutableProcessNode, ExecutableProcessNode, ExecutableChildModel> {

    private fun visitor(
        buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
                       ) =
        ExecutableProcessNodeBuilderVisitor(buildHelper, otherNodes)

    private class ExecutableProcessNodeBuilderVisitor(
        private val buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
        val otherNodes: Iterable<ProcessNode.Builder>
                                                     ) :
        ProcessNode.BuilderVisitor<ExecutableProcessNode> {
        override fun visitStartNode(startNode: StartNode.Builder) =
            ExecutableStartNode(startNode, buildHelper)

        override fun visitActivity(activity: MessageActivity.Builder) =
            ExecutableMessageActivity(activity, buildHelper.newOwner, otherNodes)

        override fun visitActivity(activity: CompositeActivity.ModelBuilder) =
            ExecutableCompositeActivity(activity, buildHelper, otherNodes)

        override fun visitActivity(activity: CompositeActivity.ReferenceBuilder) =
            ExecutableCompositeActivity(activity, buildHelper, otherNodes)

        override fun visitGenericActivity(builder: Activity.Builder): ExecutableProcessNode {
            return when (builder) {
                is RunnableActivity.Builder<*, *> -> RunnableActivity(builder, buildHelper.newOwner, otherNodes)
                else                              -> super.visitGenericActivity(builder)
            }
        }

        override fun visitSplit(split: Split.Builder) =
            ExecutableSplit(split, buildHelper.newOwner, otherNodes)

        override fun visitJoin(join: Join.Builder) =
            ExecutableJoin(join, buildHelper, otherNodes)

        override fun visitEndNode(endNode: EndNode.Builder) =
            ExecutableEndNode(endNode, buildHelper, otherNodes)
    }

    override fun invoke(
        baseNodeBuilder: ProcessNode.Builder,
        buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
                       ): ExecutableProcessNode =
        baseNodeBuilder.visit(visitor(buildHelper, otherNodes))


    override fun invoke(
        baseChildBuilder: ChildProcessModel.Builder,
        buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, *, *, *>
                       ): ExecutableChildModel {
        return ExecutableChildModel(baseChildBuilder, buildHelper)
    }

    override fun condition(condition: Condition) = condition.toExecutableCondition()
}

typealias ExecutableProcessModelRef = IProcessModelRef<ExecutableProcessNode, ExecutableProcessModel>
