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
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.processModel.engine.XmlEndNode
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xml.*
import nl.adaptivity.xml.XmlStreaming.EventType
import java.security.Principal
import java.util.*
import javax.xml.namespace.QName

@DslMarker
annotation class ProcessModelDSL

/**
 * Created by pdvrieze on 21/11/15.
 */
typealias ProcessModelHandle<M> = ComparableHandle<M>
abstract class ProcessModelBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?> : ProcessCommonBase<T,M>, ProcessModel<T, M>, MutableHandleAware<M>, XmlSerializable {

  @ProcessModelDSL
  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?>(
      nodes: Collection<ProcessNode.Builder<T, M>> = emptyList(),
      var name: String? = null,
      var handle: Long = -1L,
      var owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
      roles: Collection<String> = emptyList(),
      var uuid: UUID? = null,
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()): ProcessCommonBase.Builder<T,M>(nodes, imports, exports) {

    val roles: MutableSet<String> = roles.toMutableSet()

    constructor(base:ProcessModel<*,*>) :
        this(emptyList(),
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

    }


    internal override fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      val value = attributeValue.toString()
      when (StringUtil.toString(attributeLocalName)) {
        "name" -> name=value
        "owner" -> owner = SimplePrincipal(value)
        ATTR_ROLES -> roles.replaceBy(value.split(" *, *".toRegex()).filter { it.isEmpty() })
        "uuid" -> uuid = UUID.fromString(value)
        else -> return false
      }
      return true
    }

    override fun build(): ProcessModelBase<T,M> = build(false)

    abstract override fun build(pedantic: Boolean): ProcessModelBase<T, M>

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

  private var _processNodes: IdentifyableSet<T>
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
  private var _imports: List<XmlResultType> = ArrayList()
  private var _exports: List<XmlDefineType> = ArrayList()


  constructor(builder: Builder<T, M>, pedantic: Boolean) {
    val newOwner = this.asM()
    val newNodes = builder.apply { normalize(pedantic) }.nodes.map { it.build(newOwner).asT() }
    this._processNodes = IdentifyableSet.processNodeSet(Int.MAX_VALUE, newNodes)
    this._name = builder.name
    this._handle = builder.handle
    this._owner = builder.owner
    this._roles = builder.roles.toMutableArraySet()
    this.uuid = builder.uuid
    this._imports = builder.imports.map { XmlResultType.get(it) }
    this._exports = builder.exports.map { XmlDefineType.get(it) }
  }

  /**
   * Copy constructor, but generics mean that the right type of child needs to be provided as parameter
   * @param basepm The base process model
   * *
   * @param modelNodes The "converted" model nodes.
   */
  protected constructor(basepm: ProcessModelBase<*, *>, nodeFactory: (M, ProcessNode<*,*>)->T) : this(
      basepm.getModelNodes(),
      basepm.getName(),
      basepm.handleValue,
      basepm.owner,
      basepm.getRoles(),
      basepm.getUuid(),
      basepm.getImports(),
      basepm.getExports(),
      nodeFactory)

  constructor(processNodes: Iterable<ProcessNode<*,*>>,
              name: String? = null,
              handle: Long = -1L,
              owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
              roles: Collection<String> = emptyList(),
              uuid: UUID? = null,
              imports: Collection<IXmlResultType> = emptyList(),
              exports: Collection<IXmlDefineType> = emptyList(),
              nodeFactory: (M, ProcessNode<*,*>)->T) {
    val newOwner = this.asM()
    this._processNodes = IdentifyableSet.processNodeSet(Int.MAX_VALUE, processNodes.asSequence().map { nodeFactory(newOwner, it) })
    this._name = name
    this._handle = handle
    this._owner = owner
    this._roles = roles.toMutableArraySet()
    this.uuid = uuid
    this._imports = imports.map { XmlResultType.get(it) }
    this._exports = exports.map { XmlDefineType.get(it) }
  }

  abstract fun builder(): Builder<T,M>

  fun update(body: (Builder<T,M>)->Unit):M {
    return builder().apply(body).build().asM()
  }

  override fun withPermission(): M {
    return asM()
  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {

    if (_processNodes.any { it.id == null }) {
      builder().build().serialize(out)
    }
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
    out.writeChildren(_processNodes)
    out.endTag(ELEMENTNAME)
  }

  open fun removeNode(nodePos: Int): T {
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

  override fun getUuid(): UUID? {
    return uuid
  }

  override fun setUuid(uuid: UUID) {
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
  override fun getHandle(): Handle<M> {
    return Handles.handle<M>(_handle)
  }

  val handleValue: Long get() = _handle

  /**
   * Set the handle for this model.
   */
  override fun setHandleValue(handleValue: Long) {
    _handle = handleValue
  }

  override fun getImports(): Collection<IXmlResultType> {
    return _imports
  }

  override fun getExports(): Collection<IXmlDefineType> {
    return _exports
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getRef()
     */
  override fun getRef(): IProcessModelRef<T, M> {
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
  override fun getModelNodes(): Collection<T> {
    return Collections.unmodifiableCollection(_processNodes)
  }

  /**
   * Set the process nodes for the model. This will actually just retrieve the
   * [XmlEndNode]s and sets the model accordingly. This does mean that only
   * passing [XmlEndNode]s will have the same result, and the other nodes
   * will be pulled in.

   * @param processNodes The process nodes to base the model on.
   */
  open fun setModelNodes(processNodes: Collection<T>) {
    _processNodes = IdentifyableSet.processNodeSet(processNodes)
  }

  open fun addNode(node: T): Boolean {
    if (_processNodes.add(node)) {
      return true
    }
    return false
  }

  open fun removeNode(node: T): Boolean {
    return _processNodes.remove(node)
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getNode(java.lang.String)
     */
  override fun getNode(nodeId: Identifiable): T? {
    if (nodeId is ProcessNode<*, *>) {
      @Suppress("UNCHECKED_CAST")
      return nodeId as T
    }
    return _processNodes.get(nodeId)
  }

  fun getNode(pos: Int): T {
    return _processNodes[pos]
  }

  open fun setNode(pos: Int, newValue: T): T {
    return _processNodes.set(pos, newValue)
  }

  /**
   * Initiate the notification that a node has changed. Actual implementations can override this.
   * @param node The node that has changed.
   */
  open fun notifyNodeChanged(node: T) {
    // no implementation here
  }

  fun asM(): M {
    @Suppress("UNCHECKED_CAST")
    return this as M
  }

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
    fun <T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> deserialize(builder: Builder<T, M>, reader: XmlReader): M {
      return Builder.deserialize(builder, reader).build().asM()
    }
  }
}

