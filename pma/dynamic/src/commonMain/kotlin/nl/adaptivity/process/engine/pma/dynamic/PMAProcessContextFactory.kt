package nl.adaptivity.process.engine.pma

import nl.adaptivity.process.engine.ProcessContextFactory
import java.security.Principal

abstract class PMAProcessContextFactory<A : PMAActivityContext<A>>: ProcessContextFactory<A> {
    abstract fun getOrCreateTaskListForUser(principal: Principal): TaskList
}
