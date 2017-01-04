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
      override var name: String? = null,
      override var handle: Long = -1L,
      override var owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
      roles: Collection<String> = emptyList(),
      override var uuid: UUID? = null,
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()): ProcessCommonBase.Builder<T,M>(nodes, imports, exports), ProcessModel.Builder<T,M> {

    override val roles: MutableSet<String> = roles.toMutableSet()

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


  constructor(builder: Builder<T, M>, pedantic: Boolean): super(builder, pedantic) {
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
  protected constructor(basepm: ProcessModelBase<*, *>, nodeFactory: (ModelCommon<T,M>, ProcessNode<*,*>)->T) : this(
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
              nodeFactory: (ModelCommon<T,M>, ProcessNode<*,*>)->T): super(processNodes, imports, exports, nodeFactory) {
    this._name = name
    this._handle = handle
    this._owner = owner
    this._roles = roles.toMutableArraySet()
    this.uuid = uuid
  }

  abstract override fun builder(): Builder<T,M>

  fun update(body: (Builder<T,M>)->Unit):M {
    return builder().apply(body).build().asM
  }

  override fun withPermission(): M {
    return asM()
  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {

    if (getModelNodes().any { it.id == null }) {
      builder().build().asM!!.serialize(out)
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
  protected open fun removeNode(nodePos: Int): T {
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
    return Collections.unmodifiableCollection(super.getModelNodes())
  }

  private val _processNodes: IdentifyableSet<T> get() = super.getModelNodes() as IdentifyableSet<T>

  @Deprecated("Use the builder to update models")
  protected open fun addNode(node: T): Boolean {
    if (_processNodes.add(node)) {
      return true
    }
    return false
  }

  @Deprecated("Use the builder to update models")
  protected open fun removeNode(node: T): Boolean {
    return _processNodes.remove(node)
  }

  @Deprecated("Use the builder to update models")
  protected open fun setNode(pos: Int, newValue: T): T {
    return _processNodes.set(pos, newValue)
  }

  /**
   * Initiate the notification that a node has changed. Actual implementations can override this.
   * @param node The node that has changed.
   */
  @Deprecated("Use the builder to update models")
  open fun notifyNodeChanged(node: T) {
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
    fun <T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> deserialize(builder: Builder<T, M>, reader: XmlReader): M {
      return Builder.deserialize(builder, reader).build().asM
    }
  }
}

