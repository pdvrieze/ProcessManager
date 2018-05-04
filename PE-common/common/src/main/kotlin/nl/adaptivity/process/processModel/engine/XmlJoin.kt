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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.JoinBase
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper


class XmlJoin : JoinBase<XmlProcessNode, XmlModelCommon>, XmlProcessNode {

  class Builder : JoinBase.Builder<XmlProcessNode, XmlModelCommon>, XmlProcessNode.Builder {

    constructor() : this(id=null)

    constructor(node: Join<*, *>) : super(node)


    constructor(predecessors: Collection<Identified> = emptyList(),
                successor: Identified? = null,
                id: String? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1,
                isMultiMerge: Boolean = false,
                multiInstance: Boolean = false) : super(id, predecessors, successor, label, defines, results, x, y, min, max, isMultiMerge = isMultiMerge, isMultiInstance = multiInstance) {
    }


    override fun build(buildHelper: BuildHelper<XmlProcessNode, XmlModelCommon>): XmlJoin {
      return XmlJoin(this, buildHelper)
    }
  }

  constructor(ownerModel: XmlProcessModel) : super(ownerModel) {}

  constructor(builder: Join.Builder<*, *>, buildHelper: BuildHelper<XmlProcessNode, XmlModelCommon>) : super(builder,
                                                                                                             buildHelper) {
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  internal val xmlPrececessors: Set<Identifiable>?
    @Deprecated("")
    get() = predecessors

  @Deprecated("")
  internal fun setXmlPrececessors(pred: List<XmlProcessNode>) {
    swapPredecessors(pred)
  }

  public override fun setOwnerModel(newOwnerModel: XmlModelCommon) {
    super.setOwnerModel(newOwnerModel)
  }

  public override fun setPredecessors(predecessors: Collection<Identifiable>) {
    super.setPredecessors(predecessors)
  }

  public override fun removePredecessor(predecessorId: Identified) {
    super.removePredecessor(predecessorId)
  }

  public override fun addPredecessor(predecessorId: Identified) {
    super.addPredecessor(predecessorId)
  }

  public override fun addSuccessor(successorId: Identified) {
    super.addSuccessor(successorId)
  }

  public override fun removeSuccessor(successorId: Identified) {
    super.removeSuccessor(successorId)
  }

  public override fun setSuccessors(successors: Collection<Identified>) {
    super.setSuccessors(successors)
  }

  companion object {

    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader,
                    buildHelper: BuildHelper<XmlProcessNode, XmlModelCommon>): XmlJoin {
      return deserialize(reader).build(buildHelper)
    }

    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): XmlJoin.Builder {
      return XmlJoin.Builder().deserializeHelper(reader)
    }
  }

}
