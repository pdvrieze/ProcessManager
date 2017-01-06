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

package nl.adaptivity.process.processModel.engine

import net.devrieze.util.CollectionUtil
import net.devrieze.util.Handle
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


typealias ExecutableModelCommonAlias = ProcessModel<ExecutableProcessNode, ExecutableModelCommon>

/**
 * A class representing a process model.

 * @author Paul de Vrieze
 */
@XmlDeserializer(ExecutableProcessModel.Factory::class)
class ExecutableProcessModel : RootProcessModelBase<ExecutableProcessNode, ExecutableModelCommon>, ExecutableModelCommon/*, MutableHandleAware<ExecutableProcessModel>*/, SecureObject<ExecutableProcessModel> {

  class Builder : RootProcessModelBase.Builder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableModelCommon.Builder {
    constructor(nodes: Collection<ExecutableProcessNode.Builder> = emptySet(),
                childModels: Collection<ExecutableChildModel.Builder> = emptySet(),
                name: String? = null,
                handle: Long = -1L,
                owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
                roles: Collection<String> = emptyList(),
                uuid: UUID? = null,
                imports: Collection<IXmlResultType> = emptyList(),
                exports: Collection<IXmlDefineType> = emptyList()) : super(nodes, childModels, name, handle, owner, roles, uuid, imports, exports)
    constructor(base: RootProcessModel<*, *>) : super(base)

    override val rootBuilder get() = this

    override fun build(pedantic: Boolean) = ExecutableProcessModel(this, pedantic)

    companion object {
      @JvmStatic
      fun deserialize(reader: XmlReader): Builder {
        return RootProcessModelBase.Builder.deserialize(ExecutableProcessModel.Builder(), reader)
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

  @Volatile var endNodeCount: Int = -1
    get() {
      if (field < 0) {
        field = modelNodes.count { it is ExecutableEndNode }
      }

      return field
    }
    private set

  override val rootModel get() = this

  override fun withPermission() = this

  constructor(basepm: RootProcessModelBase<*, *>) : super(basepm, toExecutableProcessNode)

  @JvmOverloads
  constructor(builder: Builder, pedantic:Boolean = true) : super(builder, EXEC_NODEFACTORY, pedantic)

  override fun builder(): Builder = Builder(this)

  override fun update(body: (RootProcessModelBase.Builder<ExecutableProcessNode, ExecutableModelCommon>) -> Unit): ExecutableProcessModel {
    return Builder(this).apply(body).build()
  }

  @Suppress("UNCHECKED_CAST")
  override fun getHandle() = super.getHandle() as Handle<out ExecutableProcessModel>

  override fun getRef(): IProcessModelRef<ExecutableProcessNode, ExecutableModelCommon, ExecutableProcessModel> {
    return ProcessModelRef(name, this.getHandle(), uuid)
  }

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

    fun from(basepm: RootProcessModelBase<*, *>): ExecutableProcessModel {
      return ExecutableProcessModel(basepm)
    }

    @Throws(XmlException::class)
    @JvmStatic
    fun deserialize(reader: XmlReader): ExecutableProcessModel {
      return Builder.deserialize(reader).build()
    }

    @JvmStatic
    inline fun build(body: Builder.()->Unit)
        = Builder().apply(body).build()

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


val BUILDER_FACTORY = object : ProcessNode.Visitor<ExecutableProcessNode.Builder> {
  override fun visitStartNode(startNode: StartNode<*, *>) = ExecutableStartNode.Builder(startNode)

  override fun visitActivity(activity: Activity<*, *>) = ExecutableActivity.Builder(activity)

  override fun visitSplit(split: Split<*, *>) = ExecutableSplit.Builder(split)

  override fun visitJoin(join: Join<*, *>) = ExecutableJoin.Builder(join)

  override fun visitEndNode(endNode: EndNode<*, *>) = ExecutableEndNode.Builder(endNode)
}

val toExecutableProcessNode = EXEC_NODEFACTORY

object EXEC_NODEFACTORY: ProcessModelBase.NodeFactory<ExecutableProcessNode, ExecutableModelCommon> {

  private fun visitor(newOwner: ExecutableModelCommon, childModel: ExecutableChildModel?=null) = object : ProcessNode.BuilderVisitor<ExecutableProcessNode> {
    override fun visitStartNode(startNode: StartNode.Builder<*, *>) = ExecutableStartNode(startNode, newOwner)

    override fun visitActivity(activity: Activity.Builder<*, *>) = ExecutableActivity(activity, newOwner)

    override fun visitActivity(activity: Activity.ChildModelBuilder<*, *>) = ExecutableActivity(activity, childModel!!)

    override fun visitSplit(split: Split.Builder<*, *>) = ExecutableSplit(split, newOwner)

    override fun visitJoin(join: Join.Builder<*, *>) = ExecutableJoin(join, newOwner)

    override fun visitEndNode(endNode: EndNode.Builder<*, *>) = ExecutableEndNode(endNode, newOwner)
  }


  override operator fun invoke(_newOwner: ProcessModel<ExecutableProcessNode, ExecutableModelCommon>, node: ProcessNode<*, *>): ExecutableProcessNode {
    if (node is ExecutableProcessNode && node.ownerModel===_newOwner) return node
    return node.visit(BUILDER_FACTORY).build(_newOwner.asM)
  }

  override fun invoke(newOwner: ProcessModel<ExecutableProcessNode, ExecutableModelCommon>, baseNodeBuilder: ProcessNode.Builder<*, *>) = baseNodeBuilder.visit(visitor(newOwner.asM))

  override fun invoke(newOwner: ProcessModel<ExecutableProcessNode, ExecutableModelCommon>, baseNodeBuilder: Activity.ChildModelBuilder<*, *>, childModel: ChildProcessModel<ExecutableProcessNode, ExecutableModelCommon>): Activity<ExecutableProcessNode, ExecutableModelCommon> {
    return baseNodeBuilder.visit(visitor(newOwner.asM, childModel as ExecutableChildModel)) as ExecutableActivity
  }

  override fun invoke(ownerModel: RootProcessModel<ExecutableProcessNode, ExecutableModelCommon>, baseChildBuilder: ChildProcessModel.Builder<*, *>, pedantic: Boolean): ChildProcessModel<ExecutableProcessNode, ExecutableModelCommon> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun invoke(ownerModel: RootProcessModel<ExecutableProcessNode, ExecutableModelCommon>, baseModel: ChildProcessModel<*, *>, pedantic: Boolean): ChildProcessModel<ExecutableProcessNode, ExecutableModelCommon> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}