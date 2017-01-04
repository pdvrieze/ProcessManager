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
import net.devrieze.util.MutableHandleAware
import net.devrieze.util.StringCache
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessNode.Visitor
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xml.XmlDeserializer
import nl.adaptivity.xml.XmlDeserializerFactory
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import org.w3c.dom.Node

import java.lang.reflect.Method
import java.security.Principal
import java.util.*


/**
 * A class representing a process model.

 * @author Paul de Vrieze
 */
@XmlDeserializer(XmlProcessModel.Factory::class)
class XmlProcessModel : ProcessModelBase<XmlProcessNode, XmlProcessModel>, MutableHandleAware<XmlProcessModel>, SecureObject<XmlProcessModel>, XmlModelCommon {

  class Builder : ProcessModelBase.Builder<XmlProcessNode, XmlProcessModel>, XmlModelCommon.Builder {

    constructor(nodes: Set<ProcessNode.Builder<XmlProcessNode, XmlProcessModel>>, name: String?, handle: Long, owner: Principal, roles: List<String>, uuid: UUID?, imports: List<IXmlResultType>, exports: List<IXmlDefineType>) : super(nodes, name, handle, owner, roles, uuid, imports, exports) {}

    constructor() {}

    constructor(base: ProcessModelBase<XmlProcessNode, XmlProcessModel>) : super(base) {}

    override fun build(): XmlProcessModel {
      return build(false)
    }

    override fun build(pedantic: Boolean): XmlProcessModel {
      return XmlProcessModel(this)
    }

    companion object {

      @Throws(XmlException::class)
      fun deserialize(reader: XmlReader): Builder {
        return ProcessModelBase.Builder.deserialize(Builder(), reader)
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

  /**
   * Create a new processModel based on the given nodes. These nodes should be complete

   */
  constructor(processNodes: Collection<XmlProcessNode>) : super(ArrayList(processNodes), null, -1L, SecurityProvider.SYSTEMPRINCIPAL, emptyList<String>(), null, emptyList<IXmlResultType>(), emptyList<IXmlDefineType>(), XML_NODE_FACTORY) {
  }

  constructor(basepm: ProcessModelBase<*, *>) : super(basepm, XML_NODE_FACTORY) {}

  @JvmOverloads constructor(builder: ProcessModelBase.Builder<XmlProcessNode, XmlProcessModel>, pedantic: Boolean = false) : super(builder, pedantic) {}

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

  /**
   * Get the startnodes for this model.

   * @return The start nodes.
   */
  val startNodes: Collection<XmlStartNode>
    get() = Collections.unmodifiableCollection(CollectionUtil.addInstancesOf(ArrayList<XmlStartNode>(), getModelNodes(), XmlStartNode::class.java))

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

  /**
   * Faster method that doesn't require an [intermediate][nl.adaptivity.process.util.Identifier]
   * @param nodeId
   * *
   * @return
   */
  fun getNode(nodeId: String): XmlProcessNode? {
    return getNode(Identifier(nodeId))
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

    private val XML_NODE_FACTORY = fun(newOwner: ModelCommon<XmlProcessNode, XmlProcessModel>, processNode: ProcessNode<*, *>): XmlProcessNode {
      return toXmlNode(newOwner as XmlModelCommon, processNode)
    }

    private fun toXmlNode(newOwner: XmlModelCommon, node: ProcessNode<*, *>): XmlProcessNode {
      return node.visit(object : Visitor<XmlProcessNode.Builder> {
        override fun visitStartNode(startNode: StartNode<*, *>): XmlProcessNode.Builder {
          return XmlStartNode.Builder(startNode)
        }

        override fun visitActivity(activity: Activity<*, *>): XmlProcessNode.Builder {
          return XmlActivity.Builder(activity)
        }

        override fun visitSplit(split: Split<*, *>): XmlProcessNode.Builder {
          return XmlSplit.Builder(split)
        }

        override fun visitJoin(join: Join<*, *>): XmlProcessNode.Builder {
          return XmlJoin.Builder(join)
        }

        override fun visitEndNode(endNode: EndNode<*, *>): XmlProcessNode.Builder {
          return XmlEndNode.Builder(endNode)
        }
      }).build(newOwner)
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): XmlProcessModel {
      return Builder.deserialize(reader).build().asM()
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
