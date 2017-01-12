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
import net.devrieze.util.StringCache
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessNode.Visitor
import nl.adaptivity.process.util.Identifiable
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
@XmlDeserializer(XmlProcessModel.Factory::class)
class XmlProcessModel : RootProcessModelBase<XmlProcessNode, XmlModelCommon>, XmlModelCommon {

  class Builder : RootProcessModelBase.Builder<XmlProcessNode, XmlModelCommon>, XmlModelCommon.Builder {
    override val rootBuilder: Builder get() = this

    constructor(
      nodes: Collection<ProcessNode.IBuilder<XmlProcessNode, XmlModelCommon>> = emptySet(),
      childModels : Collection<XmlChildModel.Builder> = emptySet(),
      name: String? = null,
      handle: Long = -1L,
      owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
      roles: List<String> = emptyList(),
      uuid: UUID? = null,
      imports: List<IXmlResultType> = emptyList(),
      exports: List<IXmlDefineType> = emptyList()) : super(nodes, childModels, name, handle, owner, roles, uuid, imports, exports) {}

    constructor(base: XmlProcessModel) : super(base) {}

    override fun build(pedantic: Boolean): XmlProcessModel {
      return XmlProcessModel(this)
    }

    override fun childModelBuilder(): XmlChildModel.Builder {
      return XmlChildModel.Builder(rootBuilder)
    }

    override fun childModelBuilder(base: ChildProcessModel<*, *>): XmlChildModel.Builder {
      return XmlChildModel.Builder(rootBuilder, base)
    }

    companion object {

      @Throws(XmlException::class)
      fun deserialize(reader: XmlReader):XmlProcessModel.Builder {
        return RootProcessModelBase.Builder.deserialize(XmlProcessModel.Builder(), reader)
      }
    }
  }

  class Factory : XmlDeserializerFactory<XmlProcessModel> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlProcessModel {
      return XmlProcessModel.deserialize(reader)
    }
  }

  @Volatile private var mEndNodeCount = -1

  override val rootModel: XmlProcessModel get() = this

  /**
   * Get the startnodes for this model.

   * @return The start nodes.
   */
  val startNodes: Collection<XmlStartNode>
    get() = Collections.unmodifiableCollection(CollectionUtil.addInstancesOf(ArrayList<XmlStartNode>(), getModelNodes(), XmlStartNode::class.java))

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getEndNodeCount()
     */
  val endNodeCount: Int
    get() {
      if (mEndNodeCount < 0) {
        var endNodeCount = 0
        for (node in getModelNodes()) {
          if (node is XmlEndNode) {
            ++endNodeCount
          }
        }
        mEndNodeCount = endNodeCount
      }

      return mEndNodeCount
    }

  @JvmOverloads constructor(builder: Builder, pedantic: Boolean = false) : super(builder, XML_NODE_FACTORY, pedantic) {}

  override fun builder(): Builder {
    return Builder(this)
  }

  /**
   * Normalize the process model. By default this may do nothing.
   * @return The model (this).
   */
  fun normalized(pedantic: Boolean): XmlProcessModel {
    val builder = builder()
    builder.normalize(pedantic)
    return builder.build()
  }

  override fun getChildModel(childId: Identifiable): XmlChildModel? {
    return super.getChildModel(childId)?.let {it as XmlChildModel}
  }

  /**
   * Ensure that the given node is owned by this model.
   * @param processNode
   */
  public override fun addNode(processNode: XmlProcessNode): Boolean {
    throw UnsupportedOperationException("Xml Process models are immutable")
  }

  public override fun removeNode(processNode: XmlProcessNode): Boolean {
    throw UnsupportedOperationException("This will break in all kinds of ways")
  }

  public override fun setModelNodes(processNodes: Collection<XmlProcessNode>) {
    super.setModelNodes(processNodes)
    var endNodeCount = 0
    for (n in processNodes) {
      if (n is XmlEndNode) {
        ++endNodeCount
      }
    }
    mEndNodeCount = endNodeCount
  }

  fun cacheStrings(stringCache: StringCache) {
    if (owner is SimplePrincipal) {
      owner = SimplePrincipal(stringCache.lookup(owner.name))
    } else if (_cls_darwin_principal != null) {
      if (_cls_darwin_principal!!.isInstance(owner)) {
        try {
          val cacheStrings = _cls_darwin_principal!!.getMethod("cacheStrings", StringCache::class.java)
          if (cacheStrings != null) {
            owner = cacheStrings.invoke(owner, stringCache) as Principal
          }
        } catch (e: Exception) {
          // Ignore
        }

      }
    }
    setName(stringCache.lookup(getName()!!))
    val oldRoles = getRoles()
    if (oldRoles.size > 0) {
      val newRoles = HashSet<String>(oldRoles.size + (oldRoles.size shr 1))
      for (role in oldRoles) {
        newRoles.add(stringCache.lookup(role))
      }
      setRoles(newRoles)
    }
  }

  fun toInputs(payload: Node): List<ProcessData> {
    // TODO make this work properly
    val imports = getImports()
    val result = ArrayList<ProcessData>(imports.size)
    for (import_ in imports) {
      result.add(XmlResultType[import_].apply(payload))
    }
    return result
  }

  fun toOutputs(payload: Node): List<ProcessData> {
    // TODO make this work properly
    val exports = getExports()
    val result = ArrayList<ProcessData>(exports.size)
    for (export in exports) {
      //      result.add(XmlDefineType.get(export).apply(pPayload));
    }
    return result
  }

  companion object {

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): XmlProcessModel {
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
    private fun extractElements(to: MutableCollection<in XmlProcessNode>, seen: HashSet<String>, node: XmlProcessNode) {
      if (seen.contains(node.id)) {
        return
      }
      to.add(node)
      node.id?.let {seen.add(it)}
      for (successor in node.successors) {
        extractElements(to, seen, successor as XmlProcessNode)
      }
    }
  }
}

val XML_BUILDER_VISITOR: ProcessNode.Visitor<XmlProcessNode.Builder> = object : Visitor<XmlProcessNode.Builder> {
  override fun visitStartNode(startNode: StartNode<*, *>) = XmlStartNode.Builder(startNode)

  override fun visitActivity(activity: Activity<*, *>) = XmlActivity.Builder(activity)

  override fun visitSplit(split: Split<*, *>) = XmlSplit.Builder(split)

  override fun visitJoin(join: Join<*, *>) = XmlJoin.Builder(join)

  override fun visitEndNode(endNode: EndNode<*, *>) = XmlEndNode.Builder(endNode)
}


object XML_NODE_FACTORY:ProcessModelBase.NodeFactory<XmlProcessNode, XmlModelCommon> {

  private fun visitor(buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>) = object : ProcessNode.BuilderVisitor<XmlProcessNode> {
    override fun visitStartNode(startNode: StartNode.Builder<*, *>) = XmlStartNode(startNode, buildHelper)

    override fun visitActivity(activity: Activity.Builder<*, *>) = XmlActivity(activity, buildHelper)

    override fun visitActivity(activity: Activity.ChildModelBuilder<*, *>) = XmlActivity(activity, buildHelper)

    override fun visitSplit(split: Split.Builder<*, *>) = XmlSplit(split, buildHelper)

    override fun visitJoin(join: Join.Builder<*, *>) = XmlJoin(join, buildHelper)

    override fun visitEndNode(endNode: EndNode.Builder<*, *>) = XmlEndNode(endNode, buildHelper)
  }

  override fun invoke(baseNodeBuilder: ProcessNode.IBuilder<*, *>,
                      buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>): XmlProcessNode {
    return baseNodeBuilder.visit(visitor(buildHelper))
  }

  override fun invoke(baseChildBuilder: ChildProcessModel.Builder<*, *>,
                      buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>): ChildProcessModelBase<XmlProcessNode, XmlModelCommon> {
    return XmlChildModel(baseChildBuilder, buildHelper)
  }

}
