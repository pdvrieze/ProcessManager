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

@DslMarker
annotation class ProcessModelDSL

/**
 * Created by pdvrieze on 02/01/17.
 */
interface ProcessModel<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> {
  /**
   * Get the process node with the given id.
   * @param nodeId The node id to look up.
   * *
   * @return The process node with the id.
   */
  fun getNode(nodeId: Identifiable): NodeT?

  fun getModelNodes(): Collection<NodeT>
  fun getImports(): Collection<IXmlResultType>
  fun getExports(): Collection<IXmlDefineType>

  val rootModel: RootProcessModel<NodeT, ModelT>?

  val asM:ModelT get() {
    @Suppress("UNCHECKED_CAST")
    return this as ModelT
  }

  @ProcessModelDSL
  interface Builder<NodeT : ProcessNode<NodeT,ModelT>, ModelT: ProcessModel<NodeT,ModelT>?> {
    val nodes: MutableSet<ProcessNode.Builder<NodeT, ModelT>>
    val imports: MutableList<IXmlResultType>
    val exports: MutableList<IXmlDefineType>

    fun startNodeBuilder(): StartNode.Builder<NodeT,ModelT>
    fun splitBuilder(): Split.Builder<NodeT,ModelT>
    fun joinBuilder(): Join.Builder<NodeT,ModelT>
    fun activityBuilder(): Activity.Builder<NodeT,ModelT>
    fun endNodeBuilder(): EndNode.Builder<NodeT,ModelT>

    fun startNodeBuilder(startNode: StartNode<*,*>): StartNode.Builder<NodeT,ModelT>
    fun splitBuilder(split: Split<*,*>): Split.Builder<NodeT,ModelT>
    fun joinBuilder(join: Join<*,*>): Join.Builder<NodeT,ModelT>
    fun activityBuilder(activity: Activity<*,*>): Activity.Builder<NodeT,ModelT>
    fun endNodeBuilder(endNode: EndNode<*,*>): EndNode.Builder<NodeT,ModelT>

    fun startNode(body: StartNode.Builder<NodeT,ModelT>.() -> Unit) : Identifiable {
      return nodeHelper(startNodeBuilder(), body)
    }

    fun split(body: Split.Builder<NodeT,ModelT>.() -> Unit) : Identifiable {
      return nodeHelper(splitBuilder(), body)
    }

    fun join(body: Join.Builder<NodeT,ModelT>.() -> Unit) : Identifiable {
      return nodeHelper(joinBuilder(), body)
    }

    fun activity(body: Activity.Builder<NodeT,ModelT>.() -> Unit) : Identifiable {
      return nodeHelper(activityBuilder(), body)
    }

    fun endNode(body: EndNode.Builder<NodeT,ModelT>.() -> Unit) : Identifiable {
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

  companion object {
    private fun <B: ProcessNode.Builder<NodeT,ModelT>, NodeT : ProcessNode<NodeT,ModelT>, ModelT: ProcessModel<NodeT,ModelT>?> ProcessModel.Builder<NodeT, ModelT>.nodeHelper(builder:B, body: B.()->Unit): Identifiable {
      return builder.apply(body).ensureId().apply { this@nodeHelper.nodes.add(this) }.let { Identifier(it.id!!) }
    }
  }
}
