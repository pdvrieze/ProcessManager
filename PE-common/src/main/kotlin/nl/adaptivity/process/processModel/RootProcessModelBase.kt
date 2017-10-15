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

import net.devrieze.util.*
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.process.util.MutableIdentifyableSet
import nl.adaptivity.xml.*
import java.security.Principal
import java.util.*
import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 21/11/15.
 */
typealias ProcessModelHandle<M> = ComparableHandle<M>
abstract class RootProcessModelBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModelBase<NodeT,ModelT>, RootProcessModel<NodeT, ModelT>, MutableHandleAware<RootProcessModel<out NodeT,out ModelT>>, XmlSerializable {

  @ProcessModelDSL
  abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>(
      nodes: Collection<ProcessNode.IBuilder<NodeT, ModelT>> = emptyList(),
      childModels: Collection<ChildProcessModel.Builder<NodeT,ModelT>>,
      override var name: String? = null,
      override var handle: Long = -1L,
      override var owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
      roles: Collection<String> = emptyList(),
      override var uuid: UUID? = null,
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()): ProcessModelBase.Builder<NodeT,ModelT>(nodes, imports, exports), RootProcessModel.Builder<NodeT,ModelT> {

    override val roles: MutableSet<String> = roles.toMutableSet()

    override val childModels: MutableList<ChildProcessModel.Builder<NodeT, ModelT>> = childModels.toMutableList()

    constructor(base:RootProcessModel<*,*>) :
        this(nodes = emptyList(),
             childModels = emptyList(),
             name = base.name,
             handle = (base as? ReadableHandleAware<*>)?.getHandle()?.handleValue ?: -1L,
             owner = base.owner,
             roles = base.roles.toMutableList(),
             uuid = base.uuid,
             imports = base.imports.toMutableList(),
             exports = base.exports.toMutableList()) {

      base.childModels.mapTo(childModels) { childProcessModel -> childModelBuilder(childProcessModel) }

      base.getModelNodes().mapTo(nodes) { it.visit(object : ProcessNode.Visitor<ProcessNode.IBuilder<NodeT, ModelT>> {
        override fun visitStartNode(startNode: StartNode<*, *>) = startNodeBuilder(startNode)
        override fun visitActivity(activity: Activity<*, *>) = activityBuilder(activity)
        override fun visitSplit(split: Split<*, *>) = splitBuilder(split)
        override fun visitJoin(join: Join<*, *>) = joinBuilder(join)
        override fun visitEndNode(endNode: EndNode<*, *>) = endNodeBuilder(endNode)
      }) }
    }

    override abstract fun build(pedantic: Boolean): RootProcessModelBase<NodeT, ModelT>

    override val elementName: QName
      get() = ELEMENTNAME

    abstract fun childModelBuilder(): ChildProcessModelBase.Builder<NodeT, ModelT>

    abstract fun childModelBuilder(base:ChildProcessModel<*,*>): ChildProcessModelBase.Builder<NodeT, ModelT>

    override fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      val value = attributeValue.toString()
      when (attributeLocalName.toString()) {
        "name"                      -> name=value
        "owner"                     -> owner = SimplePrincipal(value)
        RootProcessModel.ATTR_ROLES -> roles.replaceBy(value.split(" *, *".toRegex()).filter(String::isEmpty))
        "uuid"                      -> uuid = UUID.fromString(value)
        else                        -> return false
      }
      return true
    }

    override fun deserializeChild(reader: XmlReader): Boolean {
      return when {
        reader.isElement(ProcessConsts.Engine.NAMESPACE,
                         ChildProcessModel.ELEMENTLOCALNAME) -> { childModels.add(childModelBuilder().deserializeHelper(reader)); true }
        else                                                 -> super.deserializeChild(reader)
      }
    }

    override fun toString(): String {
      return "${this.javaClass.name.split('.').last()}(nodes=$nodes, name=$name, handle=$handle, owner=$owner, roles=$roles, uuid=$uuid, imports=$imports, exports=$exports)"
    }

    companion object {

        @Throws(XmlException::class)
      @JvmStatic
      fun <B: Builder<*,*>> deserialize(builder: B, reader: XmlReader): B {

        reader.skipPreamble()
        val elementName = ELEMENTNAME
        assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }
        for (i in reader.attributeCount - 1 downTo 0) {
          builder.deserializeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i))
        }

        var event: EventType? = null
        while (reader.hasNext() && event !== EventType.END_ELEMENT) {
          event = reader.next()
          if (!(event== EventType.START_ELEMENT && builder.deserializeChild(reader))) {
            reader.unhandledEvent()
          }
        }

        for (node in builder.nodes) {
          node.id?.let { nodeId ->
            for (pred in node.predecessors) {
              builder.nodes.firstOrNull { it.id == pred.id }?.successors?.add(Identifier(nodeId))
            }
          }
        }
        return builder
      }

    }


  }

  private class ChildModelProvider<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel.BuildHelper<NodeT, ModelT>, Sequence<ChildProcessModelBase<NodeT, ModelT>> {

    private val nodeFactory: NodeFactory<NodeT, ModelT>
    override val pedantic: Boolean
    override val newOwner: ModelT

    private val data: LinkedHashMap<String?, Node>

    constructor(childModelBuilders: List<ChildProcessModel.Builder<*, *>>,
                nodeFactory: NodeFactory<NodeT, ModelT>,
                pedantic: Boolean,
                newOwner: RootProcessModel<NodeT, ModelT>) {
      this.nodeFactory = nodeFactory
      this.pedantic = pedantic
      this.newOwner = newOwner.asM
      this.data = childModelBuilders.associateByTo(LinkedHashMap(childModelBuilders.size), ChildProcessModel.Builder<*, *>::childId, this::Node)
    }

    constructor(childModels: Collection<ChildProcessModelBase<NodeT, ModelT>>,
                nodeFactory: NodeFactory<NodeT, ModelT>,
                newOwner: RootProcessModelBase<NodeT, ModelT>, dummy:Boolean) {
      this.nodeFactory = nodeFactory
      this.pedantic = false
      this.newOwner = newOwner.asM
      val childModels = childModels
      this.data = childModels.associateByTo(LinkedHashMap(childModels.size), ChildProcessModelBase<NodeT, ModelT>::id, this::Node)
    }

    constructor(orig: ChildModelProvider<NodeT, ModelT>, newOwner: ModelT) {
      nodeFactory = orig.nodeFactory
      pedantic = orig.pedantic
      data = orig.data
      this.newOwner = newOwner
    }

    override fun withOwner(newOwner: ModelT): ProcessModel.BuildHelper<NodeT, ModelT> {
      return ChildModelProvider<NodeT, ModelT>(this, newOwner)
    }

    @Deprecated("Use the childModel method", ReplaceWith("childModel(id)"))
    operator fun get(id:String) = childModel(id)

    override fun childModel(childId: String): ChildProcessModel<NodeT, ModelT> {
      return data[childId]?.invoke() ?: throw IllegalProcessModelException("No child model with id ${childId} exists")
    }

    override fun node(builder: ProcessNode.IBuilder<*, *>): NodeT = nodeFactory.invoke(builder, this)

    override fun iterator() = data.values.asSequence().map{it.invoke()}.iterator()

    private inner class Node {
      constructor(builder: ChildProcessModel.Builder<*, *>) {
        this.builder = builder
      }
      constructor(model:ChildProcessModelBase<NodeT, ModelT>) {
        builder = null
        this.model = model
      }

      var builder:ChildProcessModel.Builder<*,*>?
      var model: ChildProcessModelBase<NodeT, ModelT>? = null

      operator fun invoke():ChildProcessModelBase<NodeT, ModelT> {
        model?.let { return it }
        val b = builder ?: throw IllegalProcessModelException("The process model has cyclic/recursive child models. This is not allowed")
        builder = null
        return nodeFactory(baseChildBuilder = b, buildHelper = this@ChildModelProvider).apply { model = this }
      }
    }
  }

    private var _name: String? = null
  private var _handle = -1L

  /**
   * Set the owner of a model
   * @param owner
   */
  private var _owner: Principal = SecurityProvider.SYSTEMPRINCIPAL
  override var owner: Principal
    get() = _owner
    set(value) { _owner = value }
  private var _roles: MutableSet<String> = ArraySet()
  private var uuid: UUID?

  override val childModels: Collection<ChildProcessModelBase<NodeT, ModelT>> get() = _childModels
  private val _childModels: IdentifyableSet<out ChildProcessModelBase<NodeT, ModelT>>

  override final val _processNodes: MutableIdentifyableSet<NodeT>

  constructor(builder: Builder<NodeT, ModelT>, nodeFactory: NodeFactory<NodeT, ModelT>, pedantic: Boolean = builder.defaultPedantic): super( builder, pedantic) {
    @Suppress("LeakingThis")
    val childModelProvider = ChildModelProvider(builder.childModels, nodeFactory, pedantic, this)
    _processNodes = buildNodes(builder, childModelProvider)
    this._childModels = IdentifyableSet.processNodeSet(childModelProvider)

    this._name = builder.name
    this._handle = builder.handle
    this._owner = builder.owner
    this._roles = builder.roles.toMutableArraySet()
    this.uuid = builder.uuid
  }

  abstract fun builder(): Builder<NodeT,ModelT>

  open fun update(body: (Builder<NodeT,ModelT>)->Unit): ModelT {
    return builder().apply(body).build().asM
  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {

    if (getModelNodes().any { it.id == null }) {
      builder().build().serialize(out)
    } else {
      out.smartStartTag(ELEMENTNAME) {
        writeAttribute("name", name)
        writeAttribute("owner", _owner.name)

        if (_roles.isNotEmpty()) {
          writeAttribute(ATTR_ROLES, StringUtil.join(",", _roles))
        }
        if (uuid != null) {
          writeAttribute("uuid", uuid!!.toString())
        }
        out.ignorableWhitespace("\n  ")
        out.writeChildren(imports)
        out.writeChildren(exports)
        out.writeChildren(_childModels)
        out.writeChildren(getModelNodes())

      }
    }
  }

  @Deprecated("Use the builder to update models")
  protected open fun removeNode(nodePos: Int): NodeT {
    return _processNodes.removeAt(nodePos)
  }

  fun hasUnpositioned() = modelNodes.any { !it.hasPos }

  override fun getChildModel(childId: Identifiable) = _childModels[childId]

  override fun getUuid(): UUID? {
    return uuid
  }

  fun setUuid(uuid: UUID) {
    this.uuid = uuid
  }

  /**
   * Get the name of the model.

   * @return
   */
  override fun getName(): String? {
    return _name
  }

  /**
   * Set the name of the model.

   * @param name The name
   */
  fun setName(name: String?) {
    _name = name
  }

  /**
   * Get the handle recorded for this model.
   */
  override fun getHandle(): Handle<out @JvmWildcard RootProcessModelBase<NodeT, ModelT>> {
    return Handles.handle(_handle)
  }

  val handleValue: Long get() = _handle

  /**
   * Set the handle for this model.
   */
  override fun setHandleValue(handleValue: Long) {
    _handle = handleValue
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getRef()
     */
  override fun getRef(): IProcessModelRef<NodeT, ModelT, @JvmWildcard RootProcessModel<NodeT, ModelT>> {
    return ProcessModelRef(name, this.getHandle(), uuid)
  }

  override fun getRoles(): Set<String> {
    return _roles ?: HashSet<String>().apply { _roles = this }
  }

  fun setRoles(roles: Collection<String>) {
    _roles = HashSet(roles)
  }

  /**
   * Get an array of all process nodes in the model. Used by XmlProcessModel

   * @return An array of all nodes.
   */
  override fun getModelNodes(): List<NodeT> {
    return Collections.unmodifiableList(super.getModelNodes())
  }

  @Deprecated("Use the builder to update models")
  protected open fun addNode(node: NodeT): Boolean {
    if (_processNodes.add(node)) {
      return true
    }
    return false
  }

  @Deprecated("Use the builder to update models")
  protected open fun removeNode(node: NodeT): Boolean {
    return _processNodes.remove(node)
  }

  @Deprecated("Use the builder to update models")
  protected open fun setNode(pos: Int, newValue: NodeT): NodeT {
    return _processNodes.set(pos, newValue)
  }

  /**
   * Initiate the notification that a node has changed. Actual implementations can override this.
   * @param node The node that has changed.
   */
  @Deprecated("Use the builder to update models")
  open fun notifyNodeChanged(node: NodeT) {
    // no implementation here
  }

  @Deprecated("Use the value", ReplaceWith("this.asM"))
  fun asM() = this.asM

  companion object {

    val ELEMENTLOCALNAME = "processModel"
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
    val ATTR_ROLES = "roles"
    val ATTR_NAME = "name"

    private fun getOrDefault(map: Map<String, Int>, key: String, defaultValue: Int): Int {
      return map[key] ?: defaultValue
    }

    @Throws(XmlException::class)
    @JvmStatic
    @Deprecated("Remove convenience building", ReplaceWith("Builder.deserialize(builder, reader).build().asM()"))
    fun <T : ProcessNode<T, M>, M : RootProcessModelBase<T, M>> deserialize(builder: Builder<T, M>, reader: XmlReader): M {
      return builder.deserialize(reader).build().asM
    }
  }
}


fun <B: RootProcessModelBase.Builder<*, *>> B.deserialize(reader: XmlReader): B {
  return this.deserializeHelper(reader)
}
