package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.pma.models.TaskListService
import java.security.Principal

abstract class PMAProcessContextFactory<A : PMAActivityContext<A>>: ProcessContextFactory<A> {
    abstract fun getOrCreateTaskListForUser(principal: Principal): TaskListService
}
