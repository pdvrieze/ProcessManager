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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel.engine

import net.devrieze.util.CollectionUtil
import net.devrieze.util.MutableHandleAware
import net.devrieze.util.StringCache
import net.devrieze.util.lookup
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import java.security.Principal
import java.util.*


/**
 * A class representing a process model.

 * @author Paul de Vrieze
 */
@XmlDeserializer(ExecutableProcessModel.Factory::class)
class ExecutableProcessModel : ProcessModelBase<ExecutableProcessNode, ExecutableProcessModel>, MutableHandleAware<ExecutableProcessModel>, SecureObject<ExecutableProcessModel> {

  class Builder : ProcessModelBase.Builder<ExecutableProcessNode, ExecutableProcessModel> {
    constructor(nodes: Collection<ExecutableProcessNode.Builder> = emptySet(),
                name: String? = null,
                handle: Long = -1L,
                owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
                roles: Collection<String> = emptyList(),
                uuid: UUID? = null,
                imports: Collection<IXmlResultType> = emptyList(),
                exports: Collection<IXmlDefineType> = emptyList()) : super(nodes, name, handle, owner, roles, uuid, imports, exports)
    constructor(base: ProcessModelBase<ExecutableProcessNode, ExecutableProcessModel>) : super(base)

    override fun build(): ExecutableProcessModel = ExecutableProcessModel(this)

    override fun startNodeBuilder() = ExecutableStartNode.Builder()

    override fun splitBuilder() = ExecutableSplit.Builder()

    override fun joinBuilder() = ExecutableJoin.Builder()

    override fun activityBuilder() = ExecutableActivity.Builder()

    override fun endNodeBuilder() = ExecutableEndNode.Builder()

    companion object {
      @JvmStatic
      fun deserialize(reader: XmlReader): Builder {
        return ProcessModelBase.Builder.deserialize(NodeFactory.INSTANCE, ExecutableProcessModel.Builder(), reader)
      }
    }
  }

  enum class Permissions : SecurityProvider.Permission {
    INSTANTIATE
  }

  class Factory : XmlDeserializerFactory<ExecutableProcessModel> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): ExecutableProcessModel {
      return ExecutableProcessModel.deserialize(reader)
    }
  }

  enum class NodeFactory : ProcessModelBase.DeserializationFactory2<ExecutableProcessNode, ExecutableProcessModel> {
    INSTANCE;

    @Throws(XmlException::class)
    override fun deserializeEndNode(reader: XmlReader): ExecutableEndNode.Builder {
      return ExecutableEndNode.Builder().deserializeHelper(reader);
    }

    @Throws(XmlException::class)
    override fun deserializeActivity(reader: XmlReader): ExecutableActivity.Builder {
      return ExecutableActivity.Builder().deserializeHelper(reader)
    }

    @Throws(XmlException::class)
    override fun deserializeStartNode(reader: XmlReader): ExecutableStartNode.Builder {
      return ExecutableStartNode.Builder().deserializeHelper(reader)
    }

    @Throws(XmlException::class)
    override fun deserializeJoin(reader: XmlReader): ExecutableJoin.Builder {
      return ExecutableJoin.Builder().deserializeHelper(reader)
    }

    @Throws(XmlException::class)
    override fun deserializeSplit(reader: XmlReader): ExecutableSplit.Builder {
      return ExecutableSplit.Builder().deserializeHelper(reader)
    }
  }

  @Volatile var endNodeCount: Int = -1
    get() {
      if (field < 0) {
        field = modelNodes.count { it is ExecutableEndNode }
      }

      return field
    }
    private set

  constructor(basepm: ProcessModelBase<*, *>) : super(basepm, ::toExecutableProcessNode)

  /**
   * Create a new processModel based on the given nodes. These nodes should be complete

   */
  constructor(processNodes: Collection<ExecutableProcessNode>) : super(processNodes, nodeFactory = ::toExecutableProcessNode)

  @JvmOverloads
  constructor(builder: Builder, pedantic:Boolean = true) : super(builder, SplitFactory2({ successors -> ExecutableSplit.Builder(successors = successors)}), pedantic)

  override fun builder(): Builder = Builder(this)

  /**
   * Ensure that the given node is owned by this model.
   * @param processNode
   */
  override fun addNode(processNode: ExecutableProcessNode): Boolean {
    throw UnsupportedOperationException("Editing not supported")
/*
    if (super.addNode(processNode)) {
      processNode.setOwnerModel(this)
      return true
    }
    return false
*/
    // XXX Remove
  }

  override fun removeNode(processNode: ExecutableProcessNode): Boolean {
    throw UnsupportedOperationException("This will break in all kinds of ways")
  }

  /**
   * Get the startnodes for this model.

   * @return The start nodes.
   */
  val startNodes: Collection<ExecutableStartNode>
    get() = Collections.unmodifiableCollection(CollectionUtil.addInstancesOf(ArrayList<ExecutableStartNode>(),
                                                                             modelNodes,
                                                                             ExecutableStartNode::class.java))

  override fun setModelNodes(processNodes: Collection<ExecutableProcessNode>) {
    super.setModelNodes(processNodes)
    endNodeCount = processNodes.count { it is ExecutableEndNode }
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getEndNodeCount()
     */

  fun cacheStrings(stringCache: StringCache) {
    if (owner is SimplePrincipal) {
      owner = SimplePrincipal(stringCache.lookup(owner.getName()))
    } else if (_cls_darwin_principal != null) {
      if (_cls_darwin_principal!!.isInstance(owner)) {
        try {
          val cacheStrings = _cls_darwin_principal?.getMethod("cacheStrings", StringCache::class.java)
          if (cacheStrings != null) {
            owner = cacheStrings.invoke(owner, stringCache) as Principal
          }
        } catch (e: Exception) {
          // Ignore
        }

      }
    }
    setName(stringCache.lookup(name))
    val oldRoles = roles
    if (oldRoles.isNotEmpty()) {
      val newRoles = HashSet<String>(oldRoles.size + (oldRoles.size shr 1))
      for (role in oldRoles) {
        newRoles.add(stringCache.lookup(role))
      }
      setRoles(newRoles)
    }
  }

  /**
   * Faster method that doesn't require an [intermediate][Identifier]
   * @param nodeId
   * *
   * @return
   */
  fun getNode(nodeId: String): ExecutableProcessNode? {
    return getNode(Identifier(nodeId))
  }

  fun toInputs(payload: Node?): List<ProcessData> {
    // TODO make this work properly
    val imports = imports
    val result = ArrayList<ProcessData>(imports.size)
    for (import_ in imports) {
      result.add(XmlResultType.get(import_).apply(payload))
    }
    return result
  }

  fun toOutputs(payload: Node?): List<ProcessData> {
    // TODO make this work properly
    val exports = exports
    val result = ArrayList<ProcessData>(exports.size)
    for (export in exports) {
      //      result.add(XmlDefineType.get(export).apply(pPayload));
    }
    return result
  }

  companion object {

    fun from(basepm: ProcessModelBase<*, *>): ExecutableProcessModel {
      return ExecutableProcessModel(basepm)
    }

    @Throws(XmlException::class)
    @JvmStatic
    fun deserialize(reader: XmlReader): ExecutableProcessModel {
      return Builder.deserialize(reader).build()
    }

    /**
     * A class handle purely used for caching and special casing the DarwinPrincipal class.
     */
    private var _cls_darwin_principal: Class<*>? = null

    init {
      try {
        _cls_darwin_principal = ClassLoader.getSystemClassLoader().loadClass("uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal")
      } catch (e: ClassNotFoundException) {
        _cls_darwin_principal = null
      }

    }

    /**
     * Helper method that helps enumerating all elements in the model

     * @param to The collection that will contain the result.
     * *
     * @param seen A set of process names that have already been seen (and should
     * *          not be added again.
     * *
     * @param node The node to start extraction from. This will go on to the
     * *          successors.
     */
    private fun extractElements(to: MutableCollection<in ExecutableProcessNode>,
                                seen: HashSet<String>,
                                node: ExecutableProcessNode) {
      if (seen.contains(node.id)) {
        return
      }
      to.add(node)
      seen.add(node.id)
      for (successor in node.successors) {
        extractElements(to, seen, successor as ExecutableProcessNode)
      }
    }
  }
}

fun toExecutableProcessNode(newOwner: ExecutableProcessModel, node: ProcessNode<*, *>): ExecutableProcessNode {
  return node.visit(object : ProcessNode.Visitor<ExecutableProcessNode> {
    override fun visitStartNode(startNode: StartNode<*, *>) = ExecutableStartNode(startNode, newOwner)

    override fun visitActivity(activity: Activity<*, *>) = ExecutableActivity(activity, newOwner)

    override fun visitSplit(split: Split<*, *>) = ExecutableSplit(split, newOwner)

    override fun visitJoin(join: Join<*, *>) = ExecutableJoin(join, newOwner)

    override fun visitEndNode(endNode: EndNode<*, *>) = ExecutableEndNode(endNode, newOwner)
  })
}
