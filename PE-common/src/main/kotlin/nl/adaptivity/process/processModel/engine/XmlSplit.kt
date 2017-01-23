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

import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.SplitBase
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified


class XmlSplit(builder: Split.Builder<*, *>,
               buildHelper: BuildHelper<XmlProcessNode, XmlModelCommon>) : SplitBase<XmlProcessNode, XmlModelCommon>(
    builder, buildHelper), XmlProcessNode {

  class Builder : SplitBase.Builder<XmlProcessNode, XmlModelCommon>, XmlProcessNode.Builder {

    constructor() {}

    constructor(node: Split<*, *>) : super(node) {}

    constructor(predecessor: Identified? = null,
                successors: Collection<Identified> = emptyList(),
                id: String? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1,
                multiInstance: Boolean = false) : super(id, predecessor, successors, label, defines, results, x, y, min, max, multiInstance) {
    }

    override fun build(buildHelper: BuildHelper<XmlProcessNode, XmlModelCommon>): XmlSplit {
      return XmlSplit(this, buildHelper)
    }
  }

  override fun builder(): Builder {
    return Builder(this)
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
}
