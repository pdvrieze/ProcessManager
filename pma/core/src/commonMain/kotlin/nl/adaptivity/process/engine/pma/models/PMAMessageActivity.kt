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

import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.multiplatform.PrincipalCompat


/**
 * Activity version that is used for process execution.
 */
class PMAMessageActivity<C: PMAActivityContext<C>>(
    builder: Builder<C>,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : ExecutableMessageActivity(builder, newOwner, otherNodes) {
    init {
        checkPredSuccCounts()
    }

    private var _condition: ExecutableCondition? = builder.condition?.toExecutableCondition()

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override val predecessor: Identifiable get() = predecessors.single()

    override val successor: Identifiable get() = successors.single()

    override val accessRestrictions: AccessRestriction? = builder.accessRestrictions

    val authorizationTemplates: List<AuthScopeTemplate<C>> = builder.authorizationTemplates.toList()

    override var condition: Condition?
        get() = _condition
        private set(value) {
            _condition = value?.toExecutableCondition()
        }

    override fun builder(): PMAMessageActivity.Builder<C> {
        return Builder(this)
    }

    /**
     * Determine whether the process can start.
     */
    override fun <C : ActivityInstanceContext> evalCondition(
        nodeInstanceSource: IProcessInstance<C>,
        predecessor: IProcessNodeInstance<C>,
        nodeInstance: IProcessNodeInstance<C>
    ): ConditionResult {
        return _condition.evalNodeStartCondition(nodeInstanceSource, predecessor, nodeInstance)
    }

    override fun isOtherwiseCondition(predecessor: ExecutableProcessNode): Boolean {
        return _condition?.isOtherwise == true
    }

    override fun <C: ActivityInstanceContext> canProvideTaskAutoProgress(
        engineData: ProcessEngineDataAccess<C>,
        instanceBuilder: ProcessNodeInstance.Builder<*, *, C>
    ) = false

    /**
     * Take the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically started.
     *
     * @return `false`
     */
    override fun <C: ActivityInstanceContext> canTakeTaskAutoProgress(
        activityContext: C,
        instance: ProcessNodeInstance.Builder<*, *, C>,
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
    override fun <C: ActivityInstanceContext> canStartTaskAutoProgress(instance: ProcessNodeInstance.Builder<*, *, C>): Boolean = false

    open class Builder<C: PMAActivityContext<C>>: MessageActivityBase.Builder, ExecutableProcessNode.Builder {
        constructor() : super() {
            authorizationTemplates = emptyList()
        }

        constructor(
            id: String?,
            predecessor: Identifiable?,
            successor: Identifiable?,
            label: String?,
            defines: Collection<IXmlDefineType>?,
            results: Collection<IXmlResultType>?,
            message: XmlMessage?,
            condition: Condition?,
            name: String?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean,
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

        var authorizationTemplates: List<AuthScopeTemplate<C>>

        override fun build(
            buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): PMAMessageActivity<C> {
            return PMAMessageActivity(this, buildHelper.newOwner, otherNodes)
        }
    }

}
