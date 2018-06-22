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

import kotlinx.serialization.Transient
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.diagram.Positioned
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.util.multiplatform.JvmDefault
import nl.adaptivity.xml.XmlDeserializable
import nl.adaptivity.xml.XmlSerializable


/**
 * Created by pdvrieze on 27/11/16.
 */
interface ProcessNode<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : Positioned, Identifiable, XmlSerializable {

    val ownerModel: ModelT

    val predecessors: IdentifyableSet<Identified>

    val successors: IdentifyableSet<Identified>

    val maxSuccessorCount: Int

    val maxPredecessorCount: Int

    val label: String?

    val results: List<IXmlResultType>

    val defines: List<IXmlDefineType>

    val idBase: String

    val isMultiInstance: Boolean

    fun builder(): IBuilder<NodeT, ModelT>

    fun asT(): NodeT

    fun isPredecessorOf(node: ProcessNode<*, *>): Boolean

    fun <R> visit(visitor: Visitor<R>): R

    fun getResult(name: String): XmlResultType?

    fun getDefine(name: String): XmlDefineType?

    @ProcessModelDSL
    interface IBuilder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : XmlDeserializable {
        val predecessors: Set<Identified>
        val successors: Set<Identified>
        var id: String?
        var label: String?
        var x: Double
        var y: Double
        val defines: MutableCollection<IXmlDefineType>
        val results: MutableCollection<IXmlResultType>
        @Transient
        val idBase: String
        var isMultiInstance: Boolean

        fun result(builder: XmlResultType.Builder.() -> Unit) {
            results.add(XmlResultType.Builder().apply(builder).build())
        }

        fun build(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ProcessNode<NodeT, ModelT>

        fun <R> visit(visitor: BuilderVisitor<R>): R

        fun setDefines(value: Iterable<IXmlDefineType>) = defines.replaceBy(value)
        fun setResults(value: Iterable<IXmlResultType>) = results.replaceBy(value)

        /** Add a successor in an appropriate way for the kind. It may fail if not valid */
        fun addSuccessor(identifier: Identifier)

        /** Add a predecessor in an appropriate way for the kind. It may fail if not valid */
        fun addPredecessor(identifier: Identifier)

        /** Remove a successor in an appropriate way for the kind. It may fail if not valid */
        fun removeSuccessor(identifier: Identifiable)

        /** Remove a predecessor in an appropriate way for the kind. It may fail if not valid */
        fun removePredecessor(identifier: Identifiable)

        @JvmDefault
        fun getResult(name: String): IXmlResultType? = results.firstOrNull { it.name == name }

        @JvmDefault
        fun getDefine(name: String): IXmlDefineType? = defines.firstOrNull { it.name == name }

    }

    interface BuilderVisitor<R> {
        fun visitStartNode(startNode: StartNode.Builder<*, *>): R
        fun visitActivity(activity: Activity.Builder<*, *>): R
        fun visitActivity(activity: Activity.ChildModelBuilder<*, *>): R
        fun visitSplit(split: Split.Builder<*, *>): R
        fun visitJoin(join: Join.Builder<*, *>): R
        fun visitEndNode(endNode: EndNode.Builder<*, *>): R
    }

    interface Visitor<R> {
        fun visitStartNode(startNode: StartNode<*, *>): R
        fun visitActivity(activity: Activity<*, *>): R
        fun visitSplit(split: Split<*, *>): R
        fun visitJoin(join: Join<*, *>): R
        fun visitEndNode(endNode: EndNode<*, *>): R
    }
}


inline operator fun <NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    StartNode.Builder<NodeT, ModelT>?.invoke(body: StartNode.Builder<NodeT, ModelT>.() -> Unit) {
    this?.body()
}

inline operator fun <NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    Activity.Builder<NodeT, ModelT>?.invoke(body: Activity.Builder<NodeT, ModelT>.() -> Unit) {
    this?.body()
}

inline operator fun <NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    Split.Builder<NodeT, ModelT>?.invoke(body: Split.Builder<NodeT, ModelT>.() -> Unit) {
    this?.body()
}

inline operator fun <NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    Join.Builder<NodeT, ModelT>?.invoke(body: Join.Builder<NodeT, ModelT>.() -> Unit) {
    this?.body()
}

inline operator fun <NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    EndNode.Builder<NodeT, ModelT>?.invoke(body: EndNode.Builder<NodeT, ModelT>.() -> Unit) {
    this?.body()
}

internal inline fun ProcessNode.IBuilder<*,*>.removeAllPredecessors(predicate: (Identified) -> Boolean) {
    predecessors.filter(predicate).forEach { removePredecessor(it.identifier) }
}

internal inline fun ProcessNode.IBuilder<*,*>.removeAllSuccessors(predicate: (Identified) -> Boolean) {
    successors.filter(predicate).forEach { removeSuccessor(it.identifier) }
}
