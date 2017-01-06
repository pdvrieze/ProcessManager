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

import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.engine.ProcessException
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
    val defaultPedantic get() = false
    val rootBuilder: RootProcessModel.Builder<NodeT, ModelT>
    val nodes: MutableSet<ProcessNode.IBuilder<NodeT, ModelT>>
    val imports: MutableList<IXmlResultType>
    val exports: MutableList<IXmlDefineType>

    fun startNodeBuilder(): StartNode.Builder<NodeT,ModelT>
    fun splitBuilder(): Split.Builder<NodeT,ModelT>
    fun joinBuilder(): Join.Builder<NodeT,ModelT>
    fun activityBuilder(): Activity.Builder<NodeT,ModelT>
    fun childModelBuilder(): Activity.ChildModelBuilder<NodeT, ModelT>
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

    fun childModel(body: Activity.ChildModelBuilder<NodeT, ModelT>.()->Unit): Identifiable {
      val builder = childModelBuilder()
      builder.apply(body)
      builder.ensureChildId().ensureId()
      rootBuilder.childModels.add(builder)
      nodes.add(builder)
      return Identifier(builder.id!!)
    }

    fun endNode(body: EndNode.Builder<NodeT,ModelT>.() -> Unit) : Identifiable {
      return nodeHelper(endNodeBuilder(), body)
    }

    fun newId(base:String):String {
      return generateSequence(1, { it+1} ).map { "${base}${it}" }.first { candidateId -> nodes.none { it.id == candidateId } }
    }

    fun <B: ChildProcessModel.Builder<*,*>> B.ensureChildId(): B = apply {
      if (childId==null) { childId = rootBuilder.newChildId(this.childIdBase) }
    }

    fun <B: ProcessNode.IBuilder<*,*>> B.ensureId(): B = apply {
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

    fun validate() {
      val seen = hashSetOf<String>()
      normalize(true)
      val nodeMap = nodes.asSequence().filter { it.id!=null }.associateBy { it.id }

      fun visitSuccessors(node: ProcessNode.IBuilder<NodeT, ModelT>) {
        val id = node.id!!
        if (id in seen) { throw ProcessException("Cycle in process model") }
        seen += id
        node.successors.forEach { visitSuccessors(nodeMap[it.id]!!) }
      }

      // First normalize pedantically

      // Check for cycles and mark each node as seen
      nodes.filter { it.predecessors.isEmpty().apply { if (it !is StartNode.Builder<NodeT, ModelT>) throw nl.adaptivity.process.engine.ProcessException("Non-start node without predecessors found")} }
          .forEach(::visitSuccessors)

      if (seen.size != nodes.size) { // We should have seen all nodes
        val msg = nodes.asSequence().filter { it.id !in seen }.joinToString(prefix = "Disconnected nodes found: ")
        throw ProcessException(msg)
      }

      // This DOES allow for multiple disconnected graphs when multiple start nodes are present.
    }
    fun normalize(pedantic: Boolean) {
      val nodeMap = nodes.asSequence().filter { it.id!=null }.associateBy { it.id }

      // Ensure all nodes are linked up and have ids
      var lastId = 1
      nodes.forEach { nodeBuilder ->
        val curIdentifier = nodeBuilder.id?.let(::Identifier) ?: if(pedantic) {
          throw IllegalArgumentException("Node without id found")
        } else {
          generateSequence(lastId) { lastId+=1; lastId }
              .map { "node$it" }
              .first { it !in nodeMap }
              .apply { nodeBuilder.id = this }
              .let(::Identifier)
        }

        if (pedantic) { // Pedantic will throw exceptions on missing things
          if (nodeBuilder is StartNode.Builder<NodeT, ModelT> && ! nodeBuilder.predecessors.isEmpty()) {
            throw ProcessException("Start nodes have no predecessors")
          }
          if (nodeBuilder is EndNode.Builder<NodeT, ModelT> && ! nodeBuilder.successors.isEmpty()) {
            throw ProcessException("End nodes have no successors")
          }

          nodeBuilder.predecessors.firstOrNull { it.id !in nodeMap }?.let { missingPred ->
            throw ProcessException("The node ${nodeBuilder.id} has a missing predecessor (${missingPred.id})")
          }

          nodeBuilder.successors.firstOrNull { it.id !in nodeMap }?.let { missingSuc ->
            throw ProcessException("The node ${nodeBuilder.id} has a missing successor (${missingSuc.id})")
          }
        } else {
          // Remove "missing" predecessors and successors
          nodeBuilder.predecessors.removeAll { it.id !in nodeMap }
          nodeBuilder.successors.removeAll { it.id !in nodeMap }
        }

        nodeBuilder.predecessors.asSequence()
            .map { nodeMap[it.id]!! }
            .forEach { pred ->
              pred.successors.add(curIdentifier) // If existing, should ignore it
            }

        nodeBuilder.successors.asSequence()
            .map { nodeMap[it.id]!! }
            .forEach { successor ->
              successor.predecessors.add(curIdentifier) // If existing, should ignore it
            }
      }

      nodes.asSequence()
          .filter { it.successors.size > 1 && it !is Split.Builder<NodeT, ModelT> }
          .map { nodeBuilder ->
            splitBuilder().apply {
              successors.addAll(nodeBuilder.successors)

              val curIdentifier = Identifier(nodeBuilder.id!!)

              predecessor = curIdentifier

              val newSplit = this

              val splitId = Identifier(this@Builder.newId(this.idBase))

              nodeBuilder.successors.asSequence()
                  .map { nodeMap[it.id] }
                  .filterNotNull()
                  .forEach {
                    it.predecessors.remove(curIdentifier)
                    it.predecessors.add(splitId)
                  }
              nodeBuilder.successors.replaceBy(splitId)

            }
          }.toList().let { nodes.addAll(it) }
    }

  }

  companion object {
    private fun <B: ProcessNode.Builder<NodeT,ModelT>, NodeT : ProcessNode<NodeT,ModelT>, ModelT: ProcessModel<NodeT,ModelT>?> ProcessModel.Builder<NodeT, ModelT>.nodeHelper(builder:B, body: B.()->Unit): Identifiable {
      return builder.apply(body).ensureId().apply { this@nodeHelper.nodes.add(this) }.let { Identifier(it.id!!) }
    }
  }
}
