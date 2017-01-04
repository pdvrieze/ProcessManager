/*
 * Copyright (c) 2017.
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

import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper

/**
 * Created by pdvrieze on 02/01/17.
 */
interface ModelCommon<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> {
  /**
   * Get the process node with the given id.
   * @param nodeId The node id to look up.
   * *
   * @return The process node with the id.
   */
  fun getNode(nodeId: Identifiable): T?

  fun getModelNodes(): Collection<T>
  fun getImports(): Collection<IXmlResultType>
  fun getExports(): Collection<IXmlDefineType>

  val rootModel: M

  interface Builder<T : ProcessNode<T,M>, M: ProcessModel<T,M>?> {
    val nodes: MutableSet<ProcessNode.Builder<T, M>>
    val imports: MutableList<IXmlResultType>
    val exports: MutableList<IXmlDefineType>

    fun startNodeBuilder(): StartNode.Builder<T,M>
    fun splitBuilder(): Split.Builder<T,M>
    fun joinBuilder(): Join.Builder<T,M>
    fun activityBuilder(): Activity.Builder<T,M>
    fun endNodeBuilder(): EndNode.Builder<T,M>

    fun startNodeBuilder(startNode: StartNode<*,*>): StartNode.Builder<T,M>
    fun splitBuilder(split: Split<*,*>): Split.Builder<T,M>
    fun joinBuilder(join: Join<*,*>): Join.Builder<T,M>
    fun activityBuilder(activity: Activity<*,*>): Activity.Builder<T,M>
    fun endNodeBuilder(endNode: EndNode<*,*>): EndNode.Builder<T,M>

    fun startNode(body: StartNode.Builder<T,M>.() -> Unit) : Identifiable {
      return nodeHelper(startNodeBuilder(), body)
    }

    fun split(body: Split.Builder<T,M>.() -> Unit) : Identifiable {
      return nodeHelper(splitBuilder(), body)
    }

    fun join(body: Join.Builder<T,M>.() -> Unit) : Identifiable {
      return nodeHelper(joinBuilder(), body)
    }

    fun activity(body: Activity.Builder<T,M>.() -> Unit) : Identifiable {
      return nodeHelper(activityBuilder(), body)
    }

    fun endNode(body: EndNode.Builder<T,M>.() -> Unit) : Identifiable {
      return nodeHelper(endNodeBuilder(), body)
    }

    fun newId(base:String):String {
      return generateSequence(1, { it+1} ).map { "${base}${it}" }.first { candidateId -> nodes.none { it.id == candidateId } }
    }

    fun <B: ProcessNode.Builder<*,*>> B.ensureId(): B = apply {
      if (id ==null) { id = this@Builder.newId(this.idBase) }
    }

    @Throws(XmlException::class)
    fun deserializeChild(reader: XmlReader): Boolean {
      if (ProcessConsts.Engine.NAMESPACE == reader.namespaceUri) {
        val newNode = when (reader.localName.toString()) {
          EndNode.ELEMENTLOCALNAME -> endNodeBuilder().deserializeHelper(reader)
          Activity.ELEMENTLOCALNAME -> activityBuilder().deserializeHelper(reader)
          StartNode.ELEMENTLOCALNAME -> startNodeBuilder().deserializeHelper(reader)
          Join.ELEMENTLOCALNAME -> joinBuilder().deserializeHelper(reader)
          Split.ELEMENTLOCALNAME -> splitBuilder().deserializeHelper(reader)
          else -> return false
        }
        nodes.add(newNode)
        return true
      }
      return false
    }

    fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      return false
    }

    fun validate()
    fun normalize(pedantic: Boolean)

  }
}

private fun <B: ProcessNode.Builder<T,M>, T : ProcessNode<T,M>, M: ProcessModel<T,M>?> ModelCommon.Builder<T, M>.nodeHelper(builder:B, body: B.()->Unit): Identifiable {
  return builder.apply(body).ensureId().apply { this@nodeHelper.nodes.add(this) }.let { Identifier(it.id!!) }
}
