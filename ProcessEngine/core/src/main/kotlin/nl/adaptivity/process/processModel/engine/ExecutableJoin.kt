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

import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified


class ExecutableJoin(builder: Join.Builder, buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, *, *, *>)
  : JoinBase<ExecutableProcessNode, ExecutableModelCommon>(builder, buildHelper), ExecutableProcessNode {

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    class Builder : JoinBase.Builder,
                    ExecutableProcessNode.Builder {

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
    constructor(node: Join) : super(node)

  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  fun getExistingInstance(data: ProcessEngineDataAccess, processInstanceBuilder: ProcessInstance.Builder, predecessor: IProcessNodeInstance, entryNo:Int): Pair<JoinInstance.Builder?, Int> {
    var candidateNo = entryNo
    for(candidate in processInstanceBuilder.getChildren(this).sortedBy { it.entryNo }) {
      if (! candidate.state.isFinal && (candidate.entryNo == entryNo || candidate.predecessors.any { data.nodeInstance(it).withPermission().entryNo == entryNo })) {
        return (candidate as JoinInstance.Builder) to candidateNo
      }
      // TODO Throw exceptions for cases where this is not allowed
      if (candidate.entryNo == candidateNo) { candidateNo++ } // Increase the candidate entry number
    }
    return null to candidateNo
  }

    override fun condition(engineData: ProcessEngineDataAccess,
                           predecessor: IProcessNodeInstance,
                           instance: IProcessNodeInstance): ConditionResult {
        val condition = conditions[predecessor.node.identifier] as ExecutableCondition?
        return condition?.run { eval(engineData, instance) } ?: ConditionResult.TRUE
    }

    override fun createOrReuseInstance(data: MutableProcessEngineDataAccess,
                                       processInstanceBuilder: ProcessInstance.Builder,
                                       predecessor: IProcessNodeInstance,
                                       entryNo: Int): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
    val (existingInstance, candidateNo) = getExistingInstance(data, processInstanceBuilder, predecessor, entryNo)
    existingInstance?.let { return it }

    if (!(isMultiInstance || isMultiMerge) && candidateNo!=1) { throw ProcessException("Attempting to start a second instance of a single instantiation join") }
    return JoinInstance.BaseBuilder(this, listOf(predecessor.handle()), processInstanceBuilder, processInstanceBuilder.owner, candidateNo)
  }
}
