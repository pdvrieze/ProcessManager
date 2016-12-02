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
import nl.adaptivity.xml.XmlDeserializer
import nl.adaptivity.xml.XmlDeserializerFactory
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
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
    constructor(nodes: MutableSet<ProcessNode.Builder<ExecutableProcessNode, ExecutableProcessModel>>, name: String?, handle: Long, owner: Principal, roles: MutableList<String>, uuid: UUID?, imports: MutableList<IXmlResultType>, exports: MutableList<IXmlDefineType>) : super(nodes, name, handle, owner, roles, uuid, imports, exports)
    constructor(base: ProcessModelBase<ExecutableProcessNode, ExecutableProcessModel>) : super(base)

    override fun build(): ExecutableProcessModel = ExecutableProcessModel(this)
  }

  enum class Permissions : SecurityProvider.Permission {
    INSTANTIATE
  }

  class Factory : XmlDeserializerFactory<ExecutableProcessModel>, ProcessModelBase.DeserializationFactory<ExecutableProcessNode, ExecutableProcessModel> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): ExecutableProcessModel {
      return ExecutableProcessModel.deserialize(reader)
    }

    @Throws(XmlException::class)
    override fun deserializeEndNode(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableEndNode {
      return ExecutableEndNode.deserialize(ownerModel, reader)
    }

    @Throws(XmlException::class)
    override fun deserializeActivity(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableActivity {
      return ExecutableActivity.deserialize(ownerModel, reader)
    }

    @Throws(XmlException::class)
    override fun deserializeStartNode(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableStartNode {
      return ExecutableStartNode.deserialize(ownerModel, reader)
    }

    @Throws(XmlException::class)
    override fun deserializeJoin(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableJoin {
      return ExecutableJoin.deserialize(ownerModel, reader)
    }

    @Throws(XmlException::class)
    override fun deserializeSplit(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableSplit {
      return ExecutableSplit.deserialize(ownerModel, reader)
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

  constructor(builder: Builder) : super(builder, { newOwner, nodeBuilder -> nodeBuilder.build(newOwner).asT() } )

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
      val processModelImpl = ProcessModelImpl.deserialize(reader)
      return ExecutableProcessModel(processModelImpl)
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
