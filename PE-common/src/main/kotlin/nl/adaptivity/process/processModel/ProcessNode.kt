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

import nl.adaptivity.diagram.Positioned
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xml.XmlDeserializable
import nl.adaptivity.xml.XmlSerializable


/**
 * Created by pdvrieze on 27/11/16.
 */
interface ProcessNode<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : Positioned, Identifiable, XmlSerializable {

  @ProcessModelDSL
  interface Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : XmlDeserializable {
    var predecessors: MutableSet<Identified>
    var successors: MutableSet<Identified>
    var id: String?
    var label: String?
    var x: Double
    var y: Double
    var defines: MutableCollection<IXmlDefineType>
    var results: MutableCollection<IXmlResultType>
    val idBase: String

    fun predecessors(vararg values:Identifiable) {
      values.forEach {
        predecessors.add(it.identifier ?: throw NullPointerException("Missing identifier for predecessor ${it}"))
      }
    }

    fun build(newOwner: M): ProcessNode<T, M>

    fun result(builder: XmlResultType.Builder.() -> Unit) {
      results.add(XmlResultType.Builder().apply(builder).build())
    }
  }

  interface Visitor<R> {
    fun visitStartNode(startNode: StartNode<*, *>): R
    fun visitActivity(activity: Activity<*, *>): R
    fun visitSplit(split: Split<*, *>): R
    fun visitJoin(join: Join<*, *>): R
    fun visitEndNode(endNode: EndNode<*, *>): R
  }

  fun builder(): Builder<T,M>

  fun asT(): T

  fun isPredecessorOf(node: ProcessNode<*, *>): Boolean

  fun <R> visit(visitor: Visitor<R>): R

  fun getResult(name: String): XmlResultType?

  fun getDefine(name: String): XmlDefineType?

  val ownerModel: M

  val predecessors: IdentifyableSet<out @JvmWildcard Identified>

  val successors: IdentifyableSet<out @JvmWildcard Identified>

  val maxSuccessorCount: Int

  val maxPredecessorCount: Int

  val label: String?

  val results: List<@JvmWildcard IXmlResultType>

  val defines: List<@JvmWildcard IXmlDefineType>

  val idBase: String
}
