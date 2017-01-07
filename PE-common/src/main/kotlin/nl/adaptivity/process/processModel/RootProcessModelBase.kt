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
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.process.util.MutableIdentifyableSet
import nl.adaptivity.xml.*
import nl.adaptivity.xml.XmlStreaming.EventType
import java.security.Principal
import java.util.*
import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 21/11/15.
 */
typealias ProcessModelHandle<M> = ComparableHandle<M>
abstract class RootProcessModelBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModelBase<NodeT,ModelT>, RootProcessModel<NodeT, ModelT>, MutableHandleAware<RootProcessModel<out NodeT,out ModelT>>, XmlSerializable {

  @ProcessModelDSL
  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?>(
      nodes: Collection<ProcessNode.Builder<T, M>> = emptyList(),
      childModels: Collection<ChildProcessModel.Builder<T,M>>,
      override var name: String? = null,
      override var handle: Long = -1L,
      override var owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
      roles: Collection<String> = emptyList(),
      override var uuid: UUID? = null,
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()): ProcessModelBase.Builder<T,M>(nodes, imports, exports), RootProcessModel.Builder<T,M> {

    override val roles: MutableSet<String> = roles.toMutableSet()

    override val childModels: MutableList<ChildProcessModel.Builder<T, M>> = childModels.toMutableList()

    constructor(base:RootProcessModel<*,*>) :
        this(emptyList(),
            emptyList(),
            base.name,
            (base as? ReadableHandleAware<*>)?.getHandle()?.handleValue ?: -1L,
            base.owner,
            base.roles.toMutableList(),
            base.uuid,
            base.imports.toMutableList(),
            base.exports.toMutableList()) {

      base.getModelNodes().mapTo(nodes) { it.visit(object : ProcessNode.Visitor<ProcessNode.Builder<T, M>> {
        override fun visitStartNode(startNode: StartNode<*, *>) = startNodeBuilder(startNode)
        override fun visitActivity(activity: Activity<*, *>) = activityBuilder(activity)
        override fun visitSplit(split: Split<*, *>) = splitBuilder(split)
        override fun visitJoin(join: Join<*, *>) = joinBuilder(join)
        override fun visitEndNode(endNode: EndNode<*, *>) = endNodeBuilder(endNode)
      }) }

      // XXX Set child models from the base
    }

    override abstract fun build(pedantic: Boolean): RootProcessModelBase<T, M>

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
          for (pred in node.predecessors) {
            builder.nodes.firstOrNull { it.id == pred.id }?.successors?.add(Identifier(node.id!!))
          }
        }
        return builder
      }

    }


  }

  class ChildModelProvider<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>(private val childModelBuilders: List<ChildProcessModel.Builder<*, *>>, val nodeFactory: NodeFactory<NodeT, ModelT>, private val pedantic: Boolean) {
    private var data: IdentifyableSet<out ChildProcessModel<NodeT, ModelT>>? = null

    operator fun invoke(newOwner:RootProcessModel<NodeT, ModelT>):IdentifyableSet<out ChildProcessModel<NodeT, ModelT>> {
      return data?.let {
        it
      } ?: run {
        IdentifyableSet.processNodeSet(childModelBuilders.asSequence().map { nodeFactory(newOwner, it) })
      }.apply { data=this }
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
  private var _roles: MutableSet<String>? = null
  private var uuid: UUID?

  override val childModels: Collection<ChildProcessModel<NodeT, ModelT>> get() = _childModels
  private val _childModels: IdentifyableSet<out ChildProcessModel<NodeT, ModelT>>


  constructor(builder: Builder<NodeT, ModelT>, nodeFactory: NodeFactory<NodeT, ModelT>, pedantic: Boolean = builder.defaultPedantic): this(builder, ChildModelProvider(builder.childModels, nodeFactory, pedantic), pedantic)

  private constructor(builder: Builder<*, *>, childModelProvider: ChildModelProvider<NodeT, ModelT>, pedantic: Boolean = builder.defaultPedantic): super(builder, childModelProvider, pedantic) {
    this._childModels = childModelProvider(this)
    this._name = builder.name
    this._handle = builder.handle
    this._owner = builder.owner
    this._roles = builder.roles.toMutableArraySet()
    this.uuid = builder.uuid
  }

  /**
   * Copy constructor, but generics mean that the right type of child needs to be provided as parameter
   * @param basepm The base process model
   * *
   * @param modelNodes The "converted" model nodes.
   */
  protected constructor(basepm: RootProcessModelBase<*, *>, nodeFactory: NodeFactory<NodeT, ModelT>) : this(
      basepm.getModelNodes(),
      basepm.getName(),
      basepm.handleValue,
      basepm.owner,
      basepm.getRoles(),
      basepm.getUuid(),
      basepm.getImports(),
      basepm.getExports(),
      basepm._childModels,
      nodeFactory)

  constructor(processNodes: Iterable<ProcessNode<*,*>>,
              name: String? = null,
              handle: Long = -1L,
              owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
              roles: Collection<String> = emptyList(),
              uuid: UUID? = null,
              imports: Collection<IXmlResultType> = emptyList(),
              exports: Collection<IXmlDefineType> = emptyList(),
              childModels: Collection<ChildProcessModel<*, *>> = emptyList(),
              nodeFactory: NodeFactory<NodeT, ModelT>): super(processNodes, imports, exports, nodeFactory) {
    _childModels = IdentifyableSet.processNodeSet(childModels.map { child -> nodeFactory(ownerModel = this, baseModel = child) })
    this._name = name
    this._handle = handle
    this._owner = owner
    this._roles = roles.toMutableArraySet()
    this.uuid = uuid
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
      out.smartStartTag(ELEMENTNAME)
      out.writeAttribute("name", name)
      out.writeAttribute("owner", _owner.name)
      if (_roles != null && _roles!!.size > 0) {
        out.writeAttribute(ATTR_ROLES, StringUtil.join(",", _roles))
      }
      if (uuid != null) {
        out.writeAttribute("uuid", uuid!!.toString())
      }
      out.writeChildren(imports)
      out.writeChildren(exports)
      out.writeChildren(getModelNodes())
      out.endTag(ELEMENTNAME)
    }
  }

  @Deprecated("Use the builder to update models")
  protected open fun removeNode(nodePos: Int): NodeT {
    return _processNodes.removeAt(nodePos)
  }

  fun hasUnpositioned(): Boolean {
    for (node in modelNodes) {
      if (!node.hasPos()) {
        return true
      }
    }
    return false
  }

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
  override fun getModelNodes(): Collection<NodeT> {
    return Collections.unmodifiableCollection(super.getModelNodes())
  }

  private val _processNodes: MutableIdentifyableSet<NodeT> get() = super.getModelNodes() as MutableIdentifyableSet<NodeT>

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
      return Builder.deserialize(builder, reader).build().asM
    }
  }
}

