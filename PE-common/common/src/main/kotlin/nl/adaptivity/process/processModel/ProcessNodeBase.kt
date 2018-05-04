/*
 * Copyright (c) 2018.
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

import net.devrieze.util.ArraySet
import nl.adaptivity.process.util.*
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xml.*


/**
 * A base class for process nodes. Works like [RootProcessModelBase]
 * Created by pdvrieze on 23/11/15.
 */
abstract class ProcessNodeBase<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>
    constructor(private var _ownerModel: ModelT,
                predecessors: Collection<Identified> = emptyList(),
                successors: Collection<Identified> = emptyList(),
                id: String?,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = ArrayList<IXmlDefineType>(),
                results: Collection<IXmlResultType> = ArrayList<IXmlResultType>(),
                override val isMultiInstance: Boolean = false) : ProcessNode<NodeT, ModelT> {

  @Deprecated("Don't use this if it can be avoided")
  constructor(ownerModel: ModelT): this (ownerModel, id=null)

  abstract class Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?>(
      override var id: String? = null,
      predecessors: Collection<Identified> = emptyList(),
      successors: Collection<Identified> = emptyList(),
      override var label: String? = null,
      defines: Collection<IXmlDefineType> = emptyList(),
      results: Collection<IXmlResultType> = emptyList(),
      override var x: Double = Double.NaN,
      override var y: Double = Double.NaN,
      override var isMultiInstance: Boolean = false) : ProcessNode.IBuilder<NodeT,ModelT>, XmlDeserializable {

    override val predecessors: MutableSet<Identified> = ArraySet(predecessors)
    override val successors: MutableSet<Identified> = ArraySet(successors)
    override val defines: MutableCollection<IXmlDefineType> = ArrayList(defines)
    override val results: MutableCollection<IXmlResultType> = ArrayList(results)

    constructor(node: ProcessNode<*,*>): this(node.id, node.predecessors, node.successors, node.label, node.defines, node.results, node.x, node.y, node.isMultiInstance)

    override abstract fun build(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ProcessNode<NodeT, ModelT>

    override fun onBeforeDeserializeChildren(reader: XmlReader) {
      // By default do nothing
    }

    override fun deserializeAttribute(attributeNamespace: String?,
                                      attributeLocalName: String,
                                      attributeValue: String): Boolean {
      if (XMLConstants.NULL_NS_URI == attributeNamespace) {
        val value = attributeValue.toString()
        when (attributeLocalName.toString()) {
          "id"    -> id = value
          "label" -> label=value
          "x"     -> x = value.toDouble()
          "y"     -> y = value.toDouble()
          else -> return false
        }
        return true
      }
      return false
    }

    override fun toString(): String {
      return "${this::class.simpleName}(id=$id, label=$label, x=$x, y=$y, predecessors=$predecessors, successors=$successors, defines=$defines, results=$results)"
    }

  }

  private var _predecessors = toIdentifiers(Int.MAX_VALUE, predecessors)

  override val predecessors: IdentifyableSet<Identified>
    get() = _predecessors


  private var _successors = toIdentifiers(Int.MAX_VALUE, successors)

  override val successors: IdentifyableSet<Identified>
    get() = _successors

  private var _x = x
  override val x: Double get() = _x

  private var _y = y

  override val y: Double get() = _y

  private var _defines: MutableList<XmlDefineType> = toExportableDefines(defines)
  override val defines: List<XmlDefineType> get() = _defines

  private var _results: MutableList<XmlResultType> = toExportableResults(results)
  private var _hashCode = 0

  override val ownerModel: ModelT get() = _ownerModel

  override val idBase: String
    get() = "id"


  private var _label:String? = label

  override val label: String? get() = _label

  @Deprecated("Use builders instead of mutable process models")
  protected open fun setLabel(label: String?) {
      _label = label
      _hashCode = 0
      notifyChange()
    }


  override val maxSuccessorCount: Int get() = Int.MAX_VALUE

  override val maxPredecessorCount: Int get() = 1

  /**
   * Copy constructor
   * @param orig Original
   */
  constructor(orig: ProcessNode<*, *>, newOwnerModel: ModelT) : this(newOwnerModel,
                                                                     toIdentifiers(orig.maxPredecessorCount,
                                                                               orig.predecessors),
                                                                     toIdentifiers(orig.maxSuccessorCount, orig.successors),
                                                                     orig.id,
                                                                     orig.label,
                                                                     orig.x,
                                                                     orig.y,
                                                                     orig.defines,
                                                                     orig.results) {
  }

  constructor(builder: ProcessNode.IBuilder<*,*>, buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>):
    this(buildHelper.newOwner, builder.predecessors, builder.successors, builder.id, builder.label, builder.x, builder.y, builder.defines, builder.results, builder.isMultiInstance)

  override abstract fun builder(): Builder<NodeT, ModelT>

  fun offset(offsetX: Int, offsetY: Int) {
    _x += offsetX
    _y += offsetY
    notifyChange()
  }

  @Throws(XmlException::class)
  protected open fun serializeAttributes(out: XmlWriter) {
    out.writeAttribute("id", id)
    out.writeAttribute("label", label)
    out.writeAttribute("x", x)
    out.writeAttribute("y", y)
  }

  @Throws(XmlException::class)
  protected open fun serializeChildren(out: XmlWriter) {
    out.writeChildren(results)
    out.writeChildren(defines)
  }

  @Deprecated("Don't use")
  open fun onBeforeDeserializeChildren(reader: XmlReader) {
    // do nothing
  }

  @Deprecated("Use builders instead of mutable process models")
  protected fun swapPredecessors(predecessors: Collection<NodeT>) {
    _hashCode = 0
    _predecessors = IdentifyableSet.processNodeSet(maxPredecessorCount, emptyList())

    val tmp = predecessors.asSequence().map { it.identifier }.filterNotNull().toList()
    setPredecessors(tmp)
  }

  @Deprecated("Use builders instead of mutable process models")
  protected open fun addPredecessor(predecessorId: Identified) {
    _hashCode = 0
    if (predecessorId === this || predecessorId.id == id) {
      throw IllegalArgumentException()
    }
    if (true) {
      if (_predecessors.containsKey(predecessorId.id)) {
        return
      }
      if (_predecessors.size + 1 > maxPredecessorCount) {
        throw IllegalProcessModelException("Can not add more predecessors")
      }
    }

    if (_predecessors.add(predecessorId)) {
      val ownerModel = _ownerModel

      val mutableNode = predecessorId as? MutableProcessNode<*,*> ?: ownerModel?.getNode(predecessorId) as? MutableProcessNode<*, *>
      identifier?.let { mutableNode?.addSuccessor(it) }
    }

  }

  @Deprecated("Use builders instead of mutable process models")
  protected open fun removePredecessor(predecessorId: Identified) {
    _hashCode = 0
    if (_predecessors.remove(predecessorId)) {
      val owner = _ownerModel
      val predecessor: NodeT? = owner?.getNode(predecessorId)
      if (predecessor != null) {
        identifier?.let {
          (predecessor as MutableProcessNode<*, *>).removeSuccessor(it)
        }
      }
    }
  }

  @Deprecated("Use builders instead of mutable process models")
  protected open fun addSuccessor(successorId: Identified) {
    _hashCode = 0

    if (successorId in _successors) return

    if (_successors.size + 1 > maxSuccessorCount) throw IllegalProcessModelException("Can not add more successors")

    _successors.add(successorId)

    val mutableNode = successorId as? MutableProcessNode<*,*> ?: _ownerModel?.getNode(successorId) as? MutableProcessNode<*, *>
    identifier?.let {
      mutableNode?.addPredecessor(it)
    }
  }

  @Deprecated("Use builders instead of mutable process models")
  protected open fun removeSuccessor(successorId: Identified) {
    if (_successors.remove(successorId)) {
      _hashCode = 0
      val successorNode = successorId as? MutableProcessNode<*, *> ?: if (_ownerModel == null) null else _ownerModel!!.getNode(
              successorId) as MutableProcessNode<*, *>
      identifier?.let { successorNode?.removePredecessor(it) }
    }
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#setPredecessors(java.util.Collection)
     */
  @Deprecated("Use builders instead of mutable process models")
  protected open fun setPredecessors(predecessors: Collection<Identifiable>) {
    if (predecessors.size > maxPredecessorCount) {
      throw IllegalArgumentException()
    }
    _hashCode = 0

    if (_predecessors.size > 0) {
      val (toRemove, shared) = _predecessors.partition { it !in predecessors }

      toRemove.forEach { removePredecessor(it) }
      (predecessors.asSequence() - shared.asSequence()).forEach { it.identifier?.let { addPredecessor(it) } }
    } else {

      predecessors.toList().forEach { it.identifier?.let { addPredecessor(it) } }
    }
  }

  @Deprecated("Use builders instead of mutable process models")
  protected open fun setSuccessors(successors: Collection<Identified>) {
    if (successors.size > maxSuccessorCount) {
      throw IllegalArgumentException()
    }
    _hashCode = 0

    if (_successors.size>0) {
      val (toRemove, shared) = _successors.partition { it !in successors }
      toRemove.forEach { removeSuccessor(it) }
      (successors.asSequence() - shared.asSequence()).forEach { addSuccessor(it) }
    } else {
      successors.forEach { addSuccessor(it) }
    }
  }

  @Deprecated("Use builders instead")
  protected open fun setOwnerModel(newOwnerModel: ModelT) {
    if (_ownerModel !== newOwnerModel) {
      _hashCode = 0
      val thisT = this.asT()
      (_ownerModel as? MutableRootProcessModel<NodeT, ModelT>)?.removeNode(thisT)
      _ownerModel = newOwnerModel
      (newOwnerModel as? MutableRootProcessModel<NodeT, ModelT>)?.addNode(thisT)
    }
  }

  override fun asT(): NodeT {
    @Suppress("UNCHECKED_CAST")
    return this as NodeT
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#getId()
     */
  override fun compareTo(other: Identifiable): Int {
    return id?.let { other.id?.let { otherId -> it.compareTo(otherId)} ?: 1} ?: other.id?.let{ 1} ?: 0
  }

  private var mId: String? = id
    set(value) {
      field = value
      _hashCode = 0
      notifyChange()
    }

  override val id: String? get() = mId

  @Deprecated("Use builders instead of mutable process models")
  open protected fun setId(id: String) {
    mId = id
  }

  @Deprecated("Use builders instead of mutable process models")
  protected fun notifyChange() {
    (_ownerModel as? MutableRootProcessModel<NodeT, ModelT>)?.notifyNodeChanged(this.asT())
  }

  override fun hasPos(): Boolean {
    return x.isFinite() && y.isFinite()
  }

  @Deprecated("Use builders instead of mutable process models")
  fun setX(x: Double) {
    _x = x
    _hashCode = 0
    notifyChange()
  }


  @Deprecated("Use builders instead of mutable process models")
  fun setY(y: Double) {
    _y = y
    _hashCode = 0
    notifyChange()
  }

  open fun translate(dX: Double, dY: Double) {
    _x += dX
    _y += dY
    _hashCode = 0
    notifyChange()
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#isPredecessorOf(nl.adaptivity.process.processModel.ProcessNode)
     */
  override fun isPredecessorOf(node: ProcessNode<*, *>): Boolean {
    return node.predecessors.any { pred ->
      this===pred ||
          id == pred.id ||
          (pred is ProcessNode<*,*> && isPredecessorOf(pred)) ||
          _ownerModel?.getNode(pred)?.let { node-> isPredecessorOf(node) }?: false
    }
  }

  @Deprecated("Use builders instead of mutable process models")
  protected open fun setDefines(defines: Collection<IXmlDefineType>) {
    _hashCode = 0
    _defines = toExportableDefines(defines)
  }


  fun setDefine(define: IXmlDefineType): XmlDefineType? {
    val targetName = define.name
    val idx = _defines.indexOfFirst { it.name==targetName }
    if (idx>=0) {
      return _defines.set(idx, XmlDefineType.get(define))
    } else {
      _defines.add(XmlDefineType.get(define))
      return null
    }
  }


  override fun getDefine(name: String)
      = _defines.firstOrNull { it.name == name }

  protected open fun setResults(results: Collection<IXmlResultType>) {
    _hashCode = 0
    _results = results.let { toExportableResults(results) }
  }

  override val results: List<XmlResultType> get() = _results

  override fun getResult(name: String): XmlResultType? {
    return _results.firstOrNull { it.getName() == name }
  }

    override fun toString(): String {
    var name = this::class.simpleName!!
    if (name.endsWith("Impl")) {
      name = name.substring(0, name.length - 4)
    }
    if (name.endsWith("Node")) {
      name = name.substring(0, name.length - 4)
    }
    run {
      for (i in name.length - 1 downTo 0) {
        if (name[i].isUpperCase() && (i == 0 || !name[i - 1].isUpperCase())) {
          name = name.substring(i)
          break
        }
      }
    }
    return buildString {
      append(name).append('(')
      mId?.let { id -> append(" id='$id'") }

      if (_predecessors.size > 0) {
        _predecessors.joinTo(this, ", ", " pred='", "'") { it.id }
      }

      (_ownerModel as? RootProcessModel<*,*>)?.getName()?.let { name ->
        if (! name.isEmpty()) append(" owner='$name'")
      }
      append(" )")
    }
  }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessNodeBase<*, *>) return false

        if (_ownerModel != other._ownerModel) return false
        if (isMultiInstance != other.isMultiInstance) return false
        if (_predecessors != other._predecessors) return false
        if (_successors != other._successors) return false
        if (_x != other._x) return false
        if (_y != other._y) return false
        if (_defines != other._defines) return false
        if (_results != other._results) return false
        if (_hashCode != other._hashCode) return false
        if (_label != other._label) return false
        if (mId != other.mId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _ownerModel?.hashCode() ?: 0
        result = 31 * result + isMultiInstance.hashCode()
        result = 31 * result + _predecessors.hashCode()
        result = 31 * result + _successors.hashCode()
        result = 31 * result + _x.hashCode()
        result = 31 * result + _y.hashCode()
        result = 31 * result + _defines.hashCode()
        result = 31 * result + _results.hashCode()
        result = 31 * result + _hashCode
        result = 31 * result + (_label?.hashCode() ?: 0)
        result = 31 * result + (mId?.hashCode() ?: 0)
        return result
    }

    companion object {

    const val ATTR_PREDECESSOR = "predecessor"

    private fun toIdentifiers(maxSize: Int, identifiables: Iterable<Identified>): MutableIdentifyableSet<Identified> =
            IdentifyableSet.processNodeSet(maxSize, identifiables.map { it as? Identifier ?: Identifier(it.id) })

    fun toExportableDefines(exports: Collection<IXmlDefineType>)
        = exports.asSequence().map { XmlDefineType(it) }.toMutableList()

    fun toExportableResults(imports: Collection<IXmlResultType>)
        = imports.asSequence().map { XmlResultType(it) }.toMutableList()

    /**
     * Method to only use the specific ids of predecessors / successors for the hash code. Otherwise there may be an infinite loop.
     * @param c The collection of ids
     *
     * @return The hashcode.
     */
    private fun getHashCode(c: Collection<Identifiable>): Int {
      return c.fold(1) { result, elem -> result * 31 + (elem.id?.hashCode() ?: 1) }
    }
  }

}

private fun Char.isUpperCase() = toUpperCase()==this