/*
 * Copyright (c) 2019.
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

package nl.adaptivity.process.engine.pma.models

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.IProcessInstance
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.engine.pma.runtime.PmaActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface IPMAMessageActivity<C: PmaActivityContext<C>>: ExecutableActivity, MessageActivity {
    val authorizationTemplates: List<AuthScopeTemplate<C>>

    override fun builder(): Builder<C>

    interface Builder<C: PmaActivityContext<C>>: ExecutableProcessNode.Builder, MessageActivity.Builder {
        var authorizationTemplates: List<AuthScopeTemplate<C>>
    }
}

/**
 * Activity version that is used for process execution.
 */
open class PMAMessageActivity<C: PmaActivityContext<C>>(
    builder: Builder<C>,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : ExecutableMessageActivity(builder, newOwner, otherNodes), IPMAMessageActivity<C> {
    init {
        checkPredSuccCounts()
    }

    private var _condition: ExecutableCondition? = builder.condition?.toExecutableCondition()

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel

    override val id: String get() = super.id

    override val predecessor: Identifiable get() = predecessors.single()

    override val successor: Identifiable get() = successors.single()

    override val accessRestrictions: AccessRestriction? = builder.accessRestrictions

    override val authorizationTemplates: List<AuthScopeTemplate<C>> = builder.authorizationTemplates.toList()

    override val condition: Condition?
        get() = _condition

    override fun builder(): PMAMessageActivity.Builder<C> {
        return Builder(this)
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

    override fun isOtherwiseCondition(predecessor: ExecutableProcessNode): Boolean {
        return _condition?.isOtherwise == true
    }

    override fun canProvideTaskAutoProgress(
        engineData: ProcessEngineDataAccess,
        instanceBuilder: ProcessNodeInstance.Builder<*, *>
    ) = false

    /**
     * Take the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically started.
     *
     * @return `false`
     */
    override fun <C: ActivityInstanceContext> canTakeTaskAutoProgress(
        activityContext: C,
        instance: ProcessNodeInstance.Builder<*, *>,
        assignedUser: PrincipalCompat?
    ): Boolean {
        if (assignedUser==null) throw ProcessException("Message activities must have a user assigned for 'taking' them")
        if (instance.assignedUser!=null) throw ProcessException("Users should not have been assigned before being taken")
        instance.assignedUser = assignedUser

        return false
    }

    /**
     * Start the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically finished.
     *
     * @return `false`
     */
    override fun canStartTaskAutoProgress(instance: ProcessNodeInstance.Builder<*, *>): Boolean = false

    class Builder<C: PmaActivityContext<C>>: MessageActivityBase.Builder, IPMAMessageActivity.Builder<C> {
        constructor() : super() {
            authorizationTemplates = emptyList()
        }

        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType>? = null,
            results: Collection<IXmlResultType>? = null,
            message: IXmlMessage? = null,
            condition: Condition? = null,
            name: String? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false,
            authorizations: List<AuthScopeTemplate<C>> = emptyList(),
        ) : super(id, predecessor, successor, label, defines, results, message, condition, name, x, y, isMultiInstance) {
            this.authorizationTemplates = authorizations
        }

        constructor(activity: PMAMessageActivity<C>) : super(activity) {
            authorizationTemplates = emptyList()
        }

        constructor(serialDelegate: SerialDelegate) : super(serialDelegate) {
            authorizationTemplates = emptyList()
            // TODO Instantiate templates with some context
        }

        override var authorizationTemplates: List<AuthScopeTemplate<C>>

        override fun build(
            buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): PMAMessageActivity<C> {
            return PMAMessageActivity(this, buildHelper.newOwner, otherNodes)
        }
    }

}
