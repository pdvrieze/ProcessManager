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

package nl.adaptivity.process.processModel

import net.devrieze.util.*
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xml.*
import nl.adaptivity.xml.XmlStreaming.EventType
import java.security.Principal
import java.util.*
import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 21/11/15.
 */
abstract class ProcessModelBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> : ProcessModel<T, M>, MutableHandleAware<M>, XmlSerializable {

  interface DeserializationFactory<U : ProcessNode<U, M>, M : ProcessModelBase<U, M>> {

    @Throws(XmlException::class)
    fun deserializeEndNode(ownerModel: M, reader: XmlReader): EndNode<out U, M>

    @Throws(XmlException::class)
    fun deserializeActivity(ownerModel: M, reader: XmlReader): Activity<out U, M>

    @Throws(XmlException::class)
    fun deserializeStartNode(ownerModel: M, reader: XmlReader): StartNode<out U, M>

    @Throws(XmlException::class)
    fun deserializeJoin(ownerModel: M, reader: XmlReader): Join<out U, M>

    @Throws(XmlException::class)
    fun deserializeSplit(ownerModel: M, reader: XmlReader): Split<out U, M>
  }

  interface DeserializationFactory2<U : ProcessNode<U, M>, M : ProcessModelBase<U, M>> {

    @Throws(XmlException::class)
    fun deserializeEndNode(reader: XmlReader): EndNode.Builder<U, M>

    @Throws(XmlException::class)
    fun deserializeActivity(reader: XmlReader): Activity.Builder<U, M>

    @Throws(XmlException::class)
    fun deserializeStartNode(reader: XmlReader): StartNode.Builder<U, M>

    @Throws(XmlException::class)
    fun deserializeJoin(reader: XmlReader): Join.Builder<U, M>

    @Throws(XmlException::class)
    fun deserializeSplit(reader: XmlReader): Split.Builder<U, M>
  }

  interface SplitFactory<U : ProcessNode<U, M>, M : ProcessModel<U, M>> {

    /**
     * Create a new join node. This must register the node with the owner, and mark the join as successor to
     * the predecessors. If appropriate, this should also generate an id for the node, and must verify that it
     * is not duplcated in the model.
     * @param ownerModel The owner
     * *
     * @param successors The predecessors
     * *
     * @return The resulting join node.
     */
    fun createSplit(ownerModel: M, successors: Collection<Identifiable>): Split<out U, M>
  }

  interface SplitFactory2<U : ProcessNode<U, M>, M : ProcessModel<U, M>> {

    /**
     * Create a new join node. This must register the node with the owner, and mark the join as successor to
     * the predecessors. If appropriate, this should also generate an id for the node, and must verify that it
     * is not duplcated in the model.
     * @param ownerModel The owner
     * *
     * @param successors The predecessors
     * *
     * @return The resulting join node.
     */
    fun createSplit(successors: Collection<Identifiable>): Split.Builder<U, M>
  }

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>>(
      nodes: Collection<ProcessNode.Builder<T, M>> = emptyList(),
      var name: String? = null,
      var handle: Long = -1L,
      var owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
      roles: Collection<String> = emptyList(),
      var uuid: UUID? = null,
      imports: Collection<IXmlResultType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList()) {

    val nodes: MutableSet<ProcessNode.Builder<T, M>> = nodes.toMutableSet()
    val roles: MutableSet<String> = roles.toMutableSet()
    val imports: MutableList<IXmlResultType> = imports.toMutableList()
    val exports: MutableList<IXmlDefineType> = exports.toMutableList()

    constructor(base:ProcessModelBase<T,M>) :
        this(base.getModelNodes().map { it.builder() }.toMutableSet(),
            base.getName(),
            base.handleValue,
            base.owner,
            base.getRoles().toMutableList(),
            base.uuid,
            base.getImports().toMutableList(),
            base.getExports().toMutableList())



    @Throws(XmlException::class)
    internal fun deserializeChild(factory: DeserializationFactory2<T, M>, reader: XmlReader): Boolean {
      if (ProcessConsts.Engine.NAMESPACE == reader.namespaceUri) {
        val newNode = when (reader.localName.toString()) {
          EndNode.ELEMENTLOCALNAME -> factory.deserializeEndNode(reader)
          Activity.ELEMENTLOCALNAME -> factory.deserializeActivity(reader)
          StartNode.ELEMENTLOCALNAME -> factory.deserializeStartNode(reader)
          Join.ELEMENTLOCALNAME -> factory.deserializeJoin(reader)
          Split.ELEMENTLOCALNAME -> factory.deserializeSplit(reader)
          else -> return false
        }
        nodes.add(newNode)
        return true
      }
      return false
    }

    internal fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
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

    abstract fun build(): ProcessModelBase<T,M>

    override fun toString(): String {
      return "${this.javaClass.name.split('.').last()}(nodes=$nodes, name=$name, handle=$handle, owner=$owner, roles=$roles, uuid=$uuid, imports=$imports, exports=$exports)"
    }

    fun validate() {
      nodes.validate(ValidationSplitFactory())
    }

    companion object {

      @JvmStatic
      fun <B: Builder<T,M>, T : ProcessNode<T, M>, M : ProcessModelBase<T, M>> deserialize(factory: DeserializationFactory2<T, M>, builder: B, reader: XmlReader): B {

        reader.skipPreamble()
        val elementName = ELEMENTNAME
        assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }
        for (i in reader.attributeCount - 1 downTo 0) {
          builder.deserializeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i))
        }

        var event: EventType? = null
        while (reader.hasNext() && event !== EventType.END_ELEMENT) {
          event = reader.next()
          if (!(event== EventType.START_ELEMENT && builder.deserializeChild(factory, reader))) {
            reader.unhandledEvent()
          }
        }

        for (node in builder.nodes) {
          for (pred in node.predecessors) {
            builder.nodes.firstOrNull { it.id == pred.id }?.successors?.add(Identifier(node.id))
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


  @Deprecated("We want to use a builder instead")
  private constructor(processNodes: Collection<T>) {
    _processNodes = IdentifyableSet.processNodeSet(processNodes)
    uuid = null
  }

  constructor(builder: Builder<T, M>, splitFactory: SplitFactory2<T, M>, pedantic: Boolean) {
    val newOwner = this.asM()
    val newNodes = builder.nodes.normalize<MutableSet<ProcessNode.Builder<T, M>>, T, M>(splitFactory, pedantic).map { it.build(newOwner).asT() }
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

  @Deprecated("This constructor implies a mutable process model")
  constructor(processNodes: Iterable<ProcessNode<*,*>>, nodeFactory: (M, ProcessNode<*,*>)->T): this(processNodes, null, nodeFactory=nodeFactory)

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

  @Deprecated("Use Builders")
  @Throws(XmlException::class)
  protected fun deserializeChild(factory: DeserializationFactory<T, M>, reader: XmlReader): Boolean {
    if (ProcessConsts.Engine.NAMESPACE == reader.namespaceUri) {
      val newNode = when (reader.localName.toString()) {
        EndNode.ELEMENTLOCALNAME -> factory.deserializeEndNode(asM(), reader)
        Activity.ELEMENTLOCALNAME -> factory.deserializeActivity(asM(), reader)
        StartNode.ELEMENTLOCALNAME -> factory.deserializeStartNode(asM(), reader)
        Join.ELEMENTLOCALNAME -> factory.deserializeJoin(asM(), reader)
        Split.ELEMENTLOCALNAME -> factory.deserializeSplit(asM(), reader)
        else -> return false
      }
      addNode(newNode.asT())
      return true
    }
    return false
  }

  @Deprecated("Use builders")
  protected fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
    val value = attributeValue.toString()
    when (StringUtil.toString(attributeLocalName)) {
      "name" -> setName(value)
      "owner" -> _owner = SimplePrincipal(value)
      ATTR_ROLES -> _roles!!.addAll(Arrays.asList(*value.split(" *, *".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
      "uuid" -> setUuid(UUID.fromString(value))
      else -> return false
    }
    return true
  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    ensureIds()
    out.smartStartTag(ELEMENTNAME)
    out.writeAttribute("name", name)
    val value = if (_owner == null) null else _owner.name
    out.writeAttribute("owner", value)
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

  open fun ensureIds() {
    val ids = HashSet<String>()
    val unnamedNodes = ArrayList<MutableProcessNode<*, *>>()
    for (node in modelNodes) {
      val id = node.id
      // XXX this is rather much of a hack that should happen through updates.
      if (id == null && node is MutableProcessNode<*, *>) {
        unnamedNodes.add(node)
      } else {
        ids.add(id)
      }
    }
    val startCounts = HashMap<String, Int>()
    for (unnamed in unnamedNodes) {
      val idBase = unnamed.idBase
      var startCount = getOrDefault(startCounts, idBase, 1)
      var id = idBase + Integer.toString(startCount)
      while (ids.contains(id)) {
        ++startCount
        id = idBase + Integer.toString(startCount)
      }
      unnamed.setId(idBase + Integer.toString(startCount))
      startCounts.put(idBase, startCount + 1)
    }
  }

  fun setImports(imports: Collection<IXmlResultType>) {
    _imports = ProcessNodeBase.toExportableResults(imports)
  }

  fun setExports(exports: Collection<IXmlDefineType>) {
    _exports = ProcessNodeBase.toExportableDefines(exports)
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
    return ProcessModelRef(name, this.handle, uuid)
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
    if (nodeId is MutableProcessNode<*, *>) {
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

    @Deprecated("Use the version that takes a builder and a DeserializationFactory2")
    @Throws(XmlException::class)
    fun <T : MutableProcessNode<T, M>, M : ProcessModelBase<T, M>> deserialize(factory: DeserializationFactory<T, M>, processModel: M, reader: XmlReader): M {

      reader.skipPreamble()
      val elementName = ELEMENTNAME
      assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }
      for (i in reader.attributeCount - 1 downTo 0) {
        processModel.deserializeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i))
      }

      var event: EventType? = null
      loop@ while (reader.hasNext() && event !== EventType.END_ELEMENT) {
        event = reader.next()
        if (!(event== EventType.START_ELEMENT && processModel.deserializeChild(factory, reader))) {
          reader.unhandledEvent()
        }
      }

      for (node in processModel.modelNodes) {
        for (pred in node.predecessors) {
          processModel.getNode(pred)?.addSuccessor(node)
        }
      }
      return processModel
    }

    @Throws(XmlException::class)
    @JvmStatic
    @Deprecated("Remove convenience building", ReplaceWith("Builder.deserialize(factory, builder, reader).build().asM()"))
    fun <T : MutableProcessNode<T, M>, M : ProcessModelBase<T, M>> deserialize(factory: DeserializationFactory2<T, M>, builder: Builder<T,M>, reader: XmlReader): M {
      return Builder.deserialize(factory, builder, reader).build().asM()
    }
  }
}

fun <C: MutableCollection<ProcessNode.Builder<T,M>>, T : ProcessNode<T, M>, M : ProcessModelBase<T, M>>
    C.normalize(splitFactory: ProcessModelBase.SplitFactory2<T,M>, pedantic: Boolean): C {
  val nodes = this.asSequence().filter { it.id!=null }.associateBy { it.id }

  // Ensure all nodes are linked up and have ids
  var lastId = 1
  this.forEach { nodeBuilder ->
    val curIdentifier = nodeBuilder.id?.let(::Identifier) ?: if(pedantic) {
      throw IllegalArgumentException("Node without id found")
    } else {
      generateSequence(lastId) { lastId+=1; lastId }
          .map { "node$it" }
          .first { it !in nodes }
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

      nodeBuilder.predecessors.firstOrNull { it.id !in nodes }?.let { missingPred ->
        throw ProcessException("The node ${nodeBuilder.id} has a missing predecessor (${missingPred.id})")
      }

      nodeBuilder.successors.firstOrNull { it.id !in nodes }?.let { missingSuc ->
        throw ProcessException("The node ${nodeBuilder.id} has a missing successor (${missingSuc.id})")
      }
    } else {
      // Remove "missing" predecessors and successors
      nodeBuilder.predecessors.removeAll { it.id !in nodes }
      nodeBuilder.successors.removeAll { it.id !in nodes }
    }

    nodeBuilder.predecessors.asSequence()
        .map { nodes[it.id]!! }
        .forEach { pred ->
      pred.successors.add(curIdentifier) // If existing, should ignore it
    }

    nodeBuilder.successors.asSequence()
        .map { nodes[it.id]!! }
        .forEach { successor ->
      successor.predecessors.add(curIdentifier) // If existing, should ignore it
    }
  }

  var lastSplitId = 1

  this.asSequence()
      .filter { it.successors.size > 1 && it !is Split.Builder }
      .map { nodeBuilder ->
        splitFactory.createSplit(nodeBuilder.successors).apply {
          val newSplit = this
          val curIdentifier = Identifier(nodeBuilder.id!!)

          val splitId = (id?.let { if (it.isEmpty() || it in nodes) null else it } ?:
              generateSequence(lastSplitId) { lastSplitId += 1; lastSplitId }
                  .map { "split$it" }.first { it !in nodes }
                  .apply {
                    newSplit.id = this
                    newSplit.predecessor = curIdentifier
                  })
              .let(::Identifier)

          nodeBuilder.successors.asSequence()
              .map { nodes[it.id] }
              .filterNotNull()
              .forEach {
                it.predecessors.remove(curIdentifier)
                it.predecessors.add(splitId)
              }
          nodeBuilder.successors.replaceBy(splitId)

        }
      }.toList().let { this.addAll(it) }

  return this
}

fun <C: MutableCollection<ProcessNode.Builder<T,M>>, T : ProcessNode<T, M>, M : ProcessModelBase<T, M>>
    C.validate(splitFactory: ProcessModelBase.SplitFactory2<T,M>): C {
  val seen = hashSetOf<String>()
  normalize(splitFactory, true)
  val nodes = this.asSequence().filter { it.id!=null }.associateBy { it.id }

  fun visitSuccessors(node: ProcessNode.Builder<T,M>) {
    val id = node.id!!
    if (id in seen) { throw ProcessException("Cycle in process model") }
    seen += id
    node.successors.forEach { visitSuccessors(nodes[it.id]!!) }
  }

  // First normalize pedantically
  return this.apply {

    // Check for cycles and mark each node as seen
    this.filter { it.predecessors.isEmpty().apply { if (it !is StartNode.Builder) throw nl.adaptivity.process.engine.ProcessException("Non-start node without predecessors found")} }
        .forEach(::visitSuccessors)

    if (seen.size != this.size) { // We should have seen all nodes
      val msg = asSequence().filter { it.id !in seen }.joinToString(prefix = "Disconnected nodes found: ")
      throw ProcessException(msg)
    }

    // This DOES allow for multiple disconnected graphs when multiple start nodes are present.
  }
}

@Suppress("UNCHECKED_CAST")
fun <U : ProcessNode<U, M>, M : ProcessModel<U, M>> ValidationSplitFactory(): ProcessModelBase.SplitFactory2<U, M> {
  // This object carefully is independent of the type but must specify one nonetheless
  return ValidationSplitFactoryObj as ProcessModelBase.SplitFactory2<U, M>
}

private object ValidationSplitFactoryObj :ProcessModelBase.SplitFactory2<XmlProcessNode, XmlProcessModel> {
  override fun createSplit(successors: Collection<Identifiable>): Split.Builder<XmlProcessNode, XmlProcessModel> {
    throw ProcessException("Missing split in process model")
  }
}

inline fun <U : ProcessNode<U, M>, M : ProcessModel<U, M>> SplitFactory2(crossinline factory: (Collection<Identifiable>)->Split.Builder<U,M>): ProcessModelBase.SplitFactory2<U,M> {
  return object : ProcessModelBase.SplitFactory2<U,M> {
    override fun createSplit(successors: Collection<Identifiable>): Split.Builder<U, M> {
      return factory(successors)
    }
  }
}