package nl.adaptivity.process.engine.pma.dynamic.runtime

import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.PmaServiceResolver
import nl.adaptivity.process.engine.pma.runtime.PMAProcessContextFactory
import nl.adaptivity.process.processModel.AccessRestriction
import java.security.Principal

interface DynamicPmaProcessContextFactory<out A : DynamicPmaActivityContext<A, *>> : PMAProcessContextFactory<A> {
    val serviceResolver: PmaServiceResolver

    override fun getOrCreateTaskListForUser(principal: Principal): TaskList<*>
    override fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskList<*>>

}
