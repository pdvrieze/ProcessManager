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

import net.devrieze.util.ArraySet
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xml.*
import java.util.*
import javax.xml.XMLConstants


/**
 * A base class for process nodes. Works like [ProcessModelBase]
 * Created by pdvrieze on 23/11/15.
 */
abstract class ProcessNodeBase<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?> @JvmOverloads
    constructor(private var _ownerModel: M,
                predecessors: Collection<Identified> = emptyList(),
                successors: Collection<Identified> = emptyList(),
                id: String?,
                label: String? = null,
                x: Double = java.lang.Double.NaN,
                y: Double = java.lang.Double.NaN,
                defines: Collection<IXmlDefineType> = ArrayList<IXmlDefineType>(),
                results: Collection<IXmlResultType> = ArrayList<IXmlResultType>()) : ProcessNode<T, M> {

  @Deprecated("Don't use this if it can be avoided")
  constructor(ownerModel: M): this (ownerModel, id=null)

  abstract class Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?>(
      override var id: String? = null,
      predecessors: Collection<Identified> = emptyList(),
      successors: Collection<Identified> = emptyList(),
      override var label: String? = null,
      defines: Collection<IXmlDefineType> = emptyList(),
      results: Collection<IXmlResultType> = emptyList(),
      override var x: Double = Double.NaN,
      override var y: Double = Double.NaN) : ProcessNode.Builder<T,M>, XmlDeserializable {

    override var predecessors: MutableSet<Identified> = ArraySet(predecessors)
      set(value) {field.replaceBy(value)}
    override var successors: MutableSet<Identified> = ArraySet(successors)
      set(value) {field.replaceBy(value)}
    override var defines: MutableCollection<IXmlDefineType> = ArrayList(defines)
      set(value) {field.replaceBy(value)}
    override var results: MutableCollection<IXmlResultType> = ArrayList(results)
      set(value) {field.replaceBy(value)}

    constructor(node: ProcessNode<*,*>): this(node.id, node.predecessors, node.successors, node.label, node.defines, node.results, node.getX(), node.getY())

    override abstract fun build(newOwner: M): ProcessNode<T, M>

    override fun onBeforeDeserializeChildren(reader: XmlReader) {
      // By default do nothing
    }

    override fun deserializeAttribute(attributeNamespace: CharSequence,
                                      attributeLocalName: CharSequence,
                                      attributeValue: CharSequence): Boolean {
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
      return "${this.javaClass.name.split('.').last()}(id=$id, label=$label, x=$x, y=$y, predecessors=$predecessors, successors=$successors, defines=$defines, results=$results)"
    }

  }

  @Suppress("LeakingThis")
  private var _predecessors = toIdentifiers(maxPredecessorCount, predecessors)

  override val predecessors: IdentifyableSet<@JvmWildcard Identified>
    get() = _predecessors


  @Suppress("LeakingThis")
  private var _successors = toIdentifiers(maxSuccessorCount, successors)

  override val successors: IdentifyableSet<@JvmWildcard Identified>
    get() = _successors

  private var _x = x
  private var _y = y

  private var _defines: MutableList<XmlDefineType> = toExportableDefines(defines)
  override val defines: List<XmlDefineType> get() = _defines

  private var _results: MutableList<XmlResultType> = toExportableResults(results)
  private var _hashCode = 0

  override val ownerModel: M get() = _ownerModel

  override val idBase: String
    get() = "id"


  override var label: String? = label
    set(value) {
      field = value
      _hashCode = 0
      notifyChange()
    }

  override val maxSuccessorCount: Int get() = Integer.MAX_VALUE

  override val maxPredecessorCount: Int get() = 1

  /**
   * Copy constructor
   * @param orig Original
   */
  constructor(orig: ProcessNode<*, *>, newOwnerModel: M) : this(newOwnerModel,
                                                                 toIdentifiers(orig.maxPredecessorCount,
                                                                               orig.predecessors),
                                                                 toIdentifiers(orig.maxSuccessorCount, orig.successors),
                                                                 orig.id,
                                                                 orig.label,
                                                                 orig.getX(),
                                                                 orig.getY(),
                                                                 orig.defines,
                                                                 orig.results) {
  }

  constructor(builder: ProcessNode.Builder<*,*>, newOwnerModel: M): this(newOwnerModel, builder.predecessors, builder.successors, builder.id, builder.label, builder.x, builder.y
                                                        , builder.defines, builder.results)

  override abstract fun builder(): Builder<T, M>

  fun offset(offsetX: Int, offsetY: Int) {
    x += offsetX
    y += offsetY
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

  @Deprecated("")
  protected fun swapPredecessors(predecessors: Collection<T>) {
    _hashCode = 0
    _predecessors = IdentifyableSet.processNodeSet(maxPredecessorCount, emptyList())

    val tmp = predecessors.asSequence().map { it.identifier }.filterNotNull().toList()
    setPredecessors(tmp)
  }

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

  protected open fun removePredecessor(predecessorId: Identified) {
    _hashCode = 0
    if (_predecessors.remove(predecessorId)) {
      val owner = _ownerModel
      val predecessor: T? = owner?.getNode(predecessorId)
      if (predecessor != null) {
        identifier?.let {
          (predecessor as MutableProcessNode<*, *>).removeSuccessor(it)
        }
      }
    }

    // TODO perhaps make this reciprocal
  }

  protected open fun addSuccessor(nodeId: Identified) {
    _hashCode = 0

    if (nodeId in _successors) return

    if (_successors.size + 1 > maxSuccessorCount) throw IllegalProcessModelException("Can not add more successors")

    _successors.add(nodeId)

    val mutableNode = nodeId as? MutableProcessNode<*,*> ?: _ownerModel?.getNode(nodeId) as? MutableProcessNode<*, *>
    identifier?.let {
      mutableNode?.addPredecessor(it)
    }
  }

  protected open fun removeSuccessor(node: Identified) {
    if (_successors.remove(node)) {
      _hashCode = 0
      val successorNode = node as? MutableProcessNode<*, *> ?: if (_ownerModel == null) null else _ownerModel!!.getNode(
            node) as MutableProcessNode<*, *>
      identifier?.let { successorNode?.removePredecessor(it) }
    }
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#setPredecessors(java.util.Collection)
     */
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

  fun unsetPos() {
    x = java.lang.Double.NaN
    y = java.lang.Double.NaN
  }

  protected open fun setOwnerModel(ownerModel: M) {
    if (_ownerModel !== ownerModel) {
      _hashCode = 0
      val thisT = this.asT()
      if (_ownerModel != null) {
        _ownerModel!!.removeNode(thisT)
      }
      _ownerModel = ownerModel
      ownerModel?.addNode(thisT)
    }
  }

  override fun asT(): T {
    @Suppress("UNCHECKED_CAST")
    return this as T
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

  fun setId(id: String?) {
    mId = id
  }

  protected fun notifyChange() {
    _ownerModel?.notifyNodeChanged(this.asT())
  }

  override fun getX(): Double {
    return _x
  }

  override fun hasPos(): Boolean {
    return !java.lang.Double.isNaN(x) && !java.lang.Double.isNaN(y)
  }

  fun setX(x: Double) {
    _x = x
    _hashCode = 0
    notifyChange()
  }

  override fun getY(): Double {
    return _y
  }

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
    return _results.firstOrNull { it.name == name }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }

    val that = other as ProcessNodeBase<*, *>?

    if (java.lang.Double.compare(that!!._x, _x) != 0) {
      return false
    }
    if (java.lang.Double.compare(that._y, _y) != 0) {
      return false
    }
    if (mId !=that.mId) {
      return false
    }
    if (label != that.label) {
      return false
    }
    if (_defines != that._defines) {
      return false
    }
    if (_results != that._results) {
      return false
    }

    if (_predecessors.mapNotNull { it.id }.sorted() != that._predecessors.mapNotNull { it.id }.sorted()) {
      return false
    }

    if (_successors.mapNotNull { it.id }.sorted() != that._successors.mapNotNull { it.id }.sorted()) {
      return false
    }

    return true
  }

  override fun hashCode(): Int {
    if (_hashCode != 0) {
      return _hashCode
    }
    var result: Int
    var temp: Long
    result = getHashCode(_predecessors)
    result = 31 * result + getHashCode(_successors)
    result = 31 * result + (mId?.hashCode() ?: 0)
    result = 31 * result + (label?.hashCode() ?: 0)
    temp = java.lang.Double.doubleToLongBits(_x)
    result = 31 * result + (temp xor temp.ushr(32)).toInt()
    temp = java.lang.Double.doubleToLongBits(_y)
    result = 31 * result + (temp xor temp.ushr(32)).toInt()
    result = 31 * result + _defines.hashCode()
    result = 31 * result + _results.hashCode()
    _hashCode = result
    return result
  }

  override fun toString(): String {
    var name = javaClass.simpleName
    if (name.endsWith("Impl")) {
      name = name.substring(0, name.length - 4)
    }
    if (name.endsWith("Node")) {
      name = name.substring(0, name.length - 4)
    }
    run {
      for (i in name.length - 1 downTo 0) {
        if (Character.isUpperCase(name[i]) && (i == 0 || !Character.isUpperCase(name[i - 1]))) {
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

      _ownerModel?.getName()?.let { name ->
        if (! name.isEmpty()) append(" owner='$name'")
      }
      append(" )")
    }
  }

  companion object {

    const val ATTR_PREDECESSOR = "predecessor"

    private fun toIdentifiers(maxSize: Int,
                              identifiables: Iterable<Identified>): IdentifyableSet<Identified> {
      return IdentifyableSet.processNodeSet(maxSize, identifiables.map { it as? Identifier ?: Identifier(it.id) })
    }

    fun toExportableDefines(exports: Collection<IXmlDefineType>)
        = exports.asSequence().map { XmlDefineType.get(it) }.toMutableList()

    fun toExportableResults(imports: Collection<IXmlResultType>)
        = imports.asSequence().map { XmlResultType.get(it) }.toMutableList()

    /**
     * Method to only use the specific ids of predecessors / successors for the hash code. Otherwise there may be an infinite loop.
     * @param c The collection of ids
     * *
     * @return The hashcode.
     */
    private fun getHashCode(c: Collection<Identifiable>): Int {
      return c.fold(1) { result, elem -> result * 31 + (elem.id?.hashCode() ?: 1) }
    }
  }

}
