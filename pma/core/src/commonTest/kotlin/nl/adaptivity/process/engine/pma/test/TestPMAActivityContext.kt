package nl.adaptivity.process.engine.pma.test

import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.pma.runtime.PmaActivityContext
import nl.adaptivity.process.engine.pma.runtime.PmaProcessInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat

class TestPMAProcessInstanceContext(
    override val contextFactory: TestPMAContextFactory,
    override val processInstanceHandle: PIHandle,
): PmaProcessInstanceContext<TestPMAActivityContext> {
    override fun instancesForName(name: Identified): List<IProcessNodeInstance> {
        TODO("not implemented")
    }
}

class TestPMAActivityContext(
    override val processContext: TestPMAProcessInstanceContext,
    override val activityInstance: IProcessNodeInstance,
) : PmaActivityContext<TestPMAActivityContext> {

    val taskListService: TaskListService
        get() = processContext.contextFactory.getOrCreateTaskListForUser(activityInstance.assignedUser ?: SYSTEMPRINCIPAL)

    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean = true
}
