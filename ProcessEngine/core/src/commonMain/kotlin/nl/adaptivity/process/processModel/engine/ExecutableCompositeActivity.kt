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

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.multiplatform.PrincipalCompat


/**
 * Activity version that is used for process execution.
 */
class ExecutableCompositeActivity : CompositeActivityBase, ExecutableActivity {

    constructor(
        builder: CompositeActivity.ModelBuilder,
        buildHelper: ProcessModel.BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder, buildHelper, otherNodes) {
        this._condition = builder.condition?.toExecutableCondition()
    }

    constructor(
        builder: CompositeActivity.ReferenceBuilder,
        buildHelper: ProcessModel.BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder, buildHelper, otherNodes) {
        this._condition = builder.condition?.toExecutableCondition()
    }

    init {
        checkPredSuccCounts()
    }

    override val childModel: ExecutableChildModel get() = super.childModel as ExecutableChildModel

    private var _condition: ExecutableCondition?

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override var condition: Condition?
        get() = _condition
        set(value) {
            _condition = value?.toExecutableCondition()
        }

    override val predecessor: Identifiable get() = predecessors.single()

    override val successor: Identifiable get() = successors.single()

    override fun isOtherwiseCondition(predecessor: ExecutableProcessNode): Boolean {
        return _condition?.isOtherwise == true
    }

    /**
     * Determine whether the process can start.
     */
    override fun evalCondition(
        nodeInstanceSource: IProcessInstance,
        predecessor: IProcessNodeInstance,
        nodeInstance: IProcessNodeInstance
    ): ConditionResult {
        return _condition.evalNodeStartCondition(nodeInstanceSource, predecessor, nodeInstance)
    }

    override fun createOrReuseInstance(
        data: MutableProcessEngineDataAccess,
        processInstanceBuilder: ProcessInstance.Builder,
        predecessor: IProcessNodeInstance,
        entryNo: Int,
        allowFinalInstance: Boolean
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, ProcessNodeInstance<*>> {
        return processInstanceBuilder.getChildNodeInstance(this, entryNo) ?: CompositeInstance.BaseBuilder(
            this, predecessor.handle,
            processInstanceBuilder,
            Handle.invalid(),
            processInstanceBuilder.owner,
            entryNo
        )
    }

    override fun canProvideTaskAutoProgress(
        engineData: ProcessEngineDataAccess,
        instanceBuilder: ProcessNodeInstance.Builder<*, *>
    ): Boolean = true

    /**
     * Take the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically taken.
     *
     * @return `false`
     */
    override fun <C : ActivityInstanceContext> canTakeTaskAutoProgress(
        activityContext: C,
        instance: ProcessNodeInstance.Builder<*, *>,
        assignedUser: PrincipalCompat?
    ): Boolean = true

    /**
     * Start the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically started.
     *
     * @return `false`
     */
    override fun canStartTaskAutoProgress(instance: ProcessNodeInstance.Builder<*, *>): Boolean = false

}
