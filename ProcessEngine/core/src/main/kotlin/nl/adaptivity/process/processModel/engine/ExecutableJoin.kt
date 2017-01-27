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

import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.DefaultProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified


class ExecutableJoin(builder: Join.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>)
  : JoinBase<ExecutableProcessNode, ExecutableModelCommon>(builder, buildHelper), ExecutableProcessNode {

  class Builder : JoinBase.Builder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableProcessNode.Builder {
    constructor(id: String? = null,
                predecessors: Collection<Identified> = emptyList(),
                successor: Identified? = null, label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                isMultiMerge: Boolean = false,
                isMultiInstance: Boolean = false) : super(id, predecessors, successor, label, defines, results, x, y, min, max, isMultiMerge,
                                                          isMultiInstance)
    constructor(node: Join<*, *>) : super(node)

    override fun build(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) = ExecutableJoin(
      this, buildHelper)
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override fun builder() = Builder(this)

  override fun createOrReuseInstance(data: ProcessEngineDataAccess,
                                     processInstance: ProcessInstance,
                                     predecessor: ProcessNodeInstance<*>,
                                     entryNo: Int): JoinInstance {
    if (isMultiInstance) TODO("MultiInstance support is not yet properly implemented")
    if (isMultiMerge) {
      var entryNoUnique = true
      var lastEntryNo = -1
      for (candidate in processInstance.childNodes) {
        (candidate as? JoinInstance)?.let {
          if (it.node == this) {
            if (!it.isFinished && it.entryNo==entryNo) return it
            entryNoUnique = entryNoUnique and (entryNo != it.entryNo)
            if (it.entryNo>lastEntryNo) lastEntryNo = it.entryNo
          }
        }
      }
      val usedEntryNo  = if (! entryNoUnique) { lastEntryNo+1 } else { entryNo }
      return JoinInstance(this, listOf(predecessor.getHandle()), processInstance.getHandle(), processInstance.owner, usedEntryNo)
    } else {
      return processInstance.getNodeInstance(this, entryNo) as JoinInstance?
             ?: JoinInstance(this, listOf(predecessor.getHandle()), processInstance.getHandle(), processInstance.owner, entryNo)
    }
  }

  override fun createOrReuseInstance(data: ProcessEngineDataAccess,
                                     processInstanceBuilder: ProcessInstance.ExtBuilder,
                                     predecessor: ProcessNodeInstance<*>,
                                     entryNo: Int): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
    var candidateNo = entryNo
    for(candidate in processInstanceBuilder.getChildren(this).sortedBy { it.entryNo }) {
      if (! candidate.state.isFinal && (candidate.entryNo == entryNo || candidate.predecessors.any { data.nodeInstance(it).withPermission().entryNo == entryNo })) {
        return candidate
      }
      // TODO Throw exceptions for cases where this is not allowed
      if (candidate.entryNo == candidateNo) { candidateNo++ } // Increase the candidate entry number
    }
    if (!(isMultiInstance || isMultiMerge) && candidateNo!=1) { throw ProcessException("Attempting to start a second instance of a single instantiation join") }
    return JoinInstance.BaseBuilder(this, listOf(predecessor.getHandle()), processInstanceBuilder, processInstanceBuilder.owner, candidateNo)
  }
}
