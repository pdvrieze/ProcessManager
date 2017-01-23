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
import nl.adaptivity.diagram.Positioned
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xml.XmlDeserializable
import nl.adaptivity.xml.XmlSerializable


/**
 * Created by pdvrieze on 27/11/16.
 */
interface ProcessNode<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : Positioned, Identifiable, XmlSerializable {

  @ProcessModelDSL
  interface IBuilder<NodeT: ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>: XmlDeserializable {
    val predecessors: MutableSet<Identified>
    val successors: MutableSet<Identified>
    var id: String?
    var label: String?
    var x: Double
    var y: Double
    val defines: MutableCollection<IXmlDefineType>
    val results: MutableCollection<IXmlResultType>
    val idBase: String
    var isMultiInstance: Boolean

    fun predecessors(vararg values:Identifiable) {
      values.forEach {
        predecessors.add(it.identifier ?: throw NullPointerException("Missing identifier for predecessor ${it}"))
      }
    }

    fun result(builder: XmlResultType.Builder.() -> Unit) {
      results.add(XmlResultType.Builder().apply(builder).build())
    }

    fun build(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ProcessNode<NodeT, ModelT>

    fun <R> visit(visitor: BuilderVisitor<R>):R

    fun setPredecessors(value: Iterable<Identified>) = predecessors.replaceBy(value)
    fun setSuccessors(value: Iterable<Identified>) = successors.replaceBy(value)
    fun setDefines(value: Iterable<IXmlDefineType>) = defines.replaceBy(value)
    fun setResults(value: Iterable<IXmlResultType>) = results.replaceBy(value)
  }

  @ProcessModelDSL
  interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : IBuilder<NodeT, ModelT> {


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

  fun builder(): IBuilder<NodeT,ModelT>

  fun asT(): NodeT

  fun isPredecessorOf(node: ProcessNode<*, *>): Boolean

  fun <R> visit(visitor: Visitor<R>): R

  fun getResult(name: String): XmlResultType?

  fun getDefine(name: String): XmlDefineType?

  val ownerModel: ModelT

  val predecessors: IdentifyableSet<out @JvmWildcard Identified>

  val successors: IdentifyableSet<out @JvmWildcard Identified>

  val maxSuccessorCount: Int

  val maxPredecessorCount: Int

  val label: String?

  val results: List<@JvmWildcard IXmlResultType>

  val defines: List<@JvmWildcard IXmlDefineType>

  val idBase: String

  val isMultiInstance: Boolean
}


inline operator fun <NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
  ProcessNode.IBuilder<NodeT, ModelT>?.invoke(body: ProcessNode.IBuilder<NodeT, ModelT>.() -> Unit) {
  this?.body()
}
