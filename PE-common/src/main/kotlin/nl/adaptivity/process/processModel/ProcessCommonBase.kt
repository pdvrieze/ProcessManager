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
import nl.adaptivity.xml.*

/**
 * Created by pdvrieze on 02/01/17.
 */
abstract class ProcessCommonBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?> : ModelCommon<T, M>, XmlSerializable {

  @ProcessModelDSL
  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?>(
      nodes: Collection<ProcessNode.Builder<T, M>> = emptyList(),
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()) {

    val nodes: MutableSet<ProcessNode.Builder<T, M>> = nodes.toMutableSet()
    val imports: MutableList<IXmlResultType> = imports.toMutableList()
    val exports: MutableList<IXmlDefineType> = exports.toMutableList()

    constructor(base:ProcessModel<*,*>) :
        this(emptyList(),
            base.imports.toMutableList(),
            base.exports.toMutableList()) {

      base.getModelNodes().mapTo(nodes) { it.visit(object : ProcessNode.Visitor<ProcessNode.Builder<T, M>> {
        override fun visitStartNode(startNode: StartNode<*, *>) = startNodeBuilder(startNode)
        override fun visitActivity(activity: Activity<*, *>) = activityBuilder(activity)
        override fun visitSplit(split: Split<*, *>) = splitBuilder(split)
        override fun visitJoin(join: Join<*, *>) = joinBuilder(join)
        override fun visitEndNode(endNode: EndNode<*, *>) = endNodeBuilder(endNode)
      }) }

    }



    @Throws(XmlException::class)
    internal fun deserializeChild(reader: XmlReader): Boolean {
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

    internal open fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      return false
    }

    open fun build(): ProcessCommonBase<T,M> = build(false)

    abstract fun build(pedantic: Boolean): ProcessCommonBase<T, M>

    override fun toString(): String {
      return "${this.javaClass.name.split('.').last()}(nodes=$nodes, imports=$imports, exports=$exports)"
    }

    fun validate() {
      val seen = hashSetOf<String>()
      normalize(true)
      val nodeMap = nodes.asSequence().filter { it.id!=null }.associateBy { it.id }

      fun visitSuccessors(node: ProcessNode.Builder<T,M>) {
        val id = node.id!!
        if (id in seen) { throw ProcessException("Cycle in process model") }
        seen += id
        node.successors.forEach { visitSuccessors(nodeMap[it.id]!!) }
      }

      // First normalize pedantically

      // Check for cycles and mark each node as seen
      nodes.filter { it.predecessors.isEmpty().apply { if (it !is StartNode.Builder) throw nl.adaptivity.process.engine.ProcessException("Non-start node without predecessors found")} }
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
          if (nodeBuilder is StartNode.Builder && ! nodeBuilder.predecessors.isEmpty()) {
            throw ProcessException("Start nodes have no predecessors")
          }
          if (nodeBuilder is EndNode.Builder && ! nodeBuilder.successors.isEmpty()) {
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
          .filter { it.successors.size > 1 && it !is Split.Builder }
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

    abstract protected fun startNodeBuilder(): StartNode.Builder<T,M>
    abstract protected fun splitBuilder(): Split.Builder<T,M>
    abstract protected fun joinBuilder(): Join.Builder<T,M>
    abstract protected fun activityBuilder(): Activity.Builder<T,M>
    abstract protected fun endNodeBuilder(): EndNode.Builder<T,M>

    abstract protected fun startNodeBuilder(startNode: StartNode<*,*>): StartNode.Builder<T,M>
    abstract protected fun splitBuilder(split: Split<*,*>): Split.Builder<T,M>
    abstract protected fun joinBuilder(join: Join<*,*>): Join.Builder<T,M>
    abstract protected fun activityBuilder(activity: Activity<*,*>): Activity.Builder<T,M>
    abstract protected fun endNodeBuilder(endNode: EndNode<*,*>): EndNode.Builder<T,M>

    private fun <B: ProcessNode.Builder<T,M>>nodeHelper(builder:B, body: B.()->Unit): Identifiable {
      return builder.apply(body).ensureId().apply { this@Builder.nodes.add(this) }.let { Identifier(it.id!!) }
    }

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

    companion object {


      @Throws(XmlException::class)
      @JvmStatic
      fun <B: Builder<*,*>> deserialize(builder: B, reader: XmlReader): B {

        reader.skipPreamble()
        val elementName = ProcessModelBase.ELEMENTNAME
        assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }
        for (i in reader.attributeCount - 1 downTo 0) {
          builder.deserializeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i))
        }

        var event: XmlStreaming.EventType? = null
        while (reader.hasNext() && event !== XmlStreaming.EventType.END_ELEMENT) {
          event = reader.next()
          if (!(event== XmlStreaming.EventType.START_ELEMENT && builder.deserializeChild(reader))) {
            reader.unhandledEvent()
          }
        }

        for (node in builder.nodes) {
          for (pred in node.predecessors) {
            builder.nodes.firstOrNull { it.id == pred.id }?.successors?.add(Identifier(node.id!!))
          }
        }
        return builder
      }

    }


  }

}