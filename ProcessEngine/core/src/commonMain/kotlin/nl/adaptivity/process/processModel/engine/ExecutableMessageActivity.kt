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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.XML


/**
 * Activity version that is used for process execution.
 */
class ExecutableMessageActivity(
    builder: MessageActivity.Builder,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : MessageActivityBase(builder, newOwner, otherNodes), ExecutableActivity {
    init {
        checkPredSuccCounts()
    }

    private val _condition: ExecutableCondition? = builder.condition?.toExecutableCondition()

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override val predecessor: Identifiable get() = predecessors.single()

    override val successor: Identifiable get() = successors.single()

    override val condition: Condition?
        get() = _condition
/*
        private set(value) {
            _condition = value?.toExecutableCondition()
        }
*/

    override val accessRestrictions: AccessRestriction? = builder.accessRestrictions

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

    override fun <C : ActivityInstanceContext> createOrReuseInstance(
        data: MutableProcessEngineDataAccess<C>,
        processInstanceBuilder: ProcessInstance.Builder<C>,
        predecessor: IProcessNodeInstance<C>,
        entryNo: Int,
        allowFinalInstance: Boolean
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, ProcessNodeInstance<*, C>, C> {
        return super.createOrReuseInstance(data, processInstanceBuilder, predecessor, entryNo, allowFinalInstance)
    }

    override fun <C : ActivityInstanceContext> canProvideTaskAutoProgress(
        engineData: ProcessEngineDataAccess<C>,
        instanceBuilder: ProcessNodeInstance.Builder<*, *, C>
    ): Boolean = false

    /**
     * Take the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically taken.
     *
     * @return `false`
     */
    override fun <C : ActivityInstanceContext> canTakeTaskAutoProgress(
        activityContext: C,
        instance: ProcessNodeInstance.Builder<*, *, C>,
        assignedUser: PrincipalCompat?
    ): Boolean {
        if (assignedUser==null) throw ProcessException("Message activities must have a user assigned for 'taking' them")
        if (instance.assignedUser!=null) throw ProcessException("Users should not have been assigned before being taken")
        instance.assignedUser = assignedUser
        if (! activityContext.canBeAssignedTo(assignedUser))
            throw ProcessException("User $assignedUser has no access to activity: $instance")

        return false
    }

    /**
     * Start the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically started.
     *
     * @return `false`
     */
    override fun <C : ActivityInstanceContext> canStartTaskAutoProgress(instance: ProcessNodeInstance.Builder<*, *, C>): Boolean = false

    @Throws(XmlException::class)
    fun serializeCondition(out: XmlWriter) {
        condition?.let {
            XML.Companion.encodeToWriter(out, XmlCondition(it.condition))
        }
    }

}
