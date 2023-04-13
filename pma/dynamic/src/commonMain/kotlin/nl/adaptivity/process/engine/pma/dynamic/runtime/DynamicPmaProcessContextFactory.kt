package nl.adaptivity.process.engine.pma.dynamic.runtime

import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.runtime.PMAProcessContextFactory
import nl.adaptivity.process.processModel.AccessRestriction
import java.security.Principal

interface DynamicPmaProcessContextFactory<out A : DynamicPmaActivityContext<A, *>> : PMAProcessContextFactory<A> {
    override fun getOrCreateTaskListForUser(principal: Principal): TaskList
    override fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskList>
    fun <S: Service> resolveService(serviceName: ServiceName<S>): S
    fun <S: Service> resolveService(serviceId: ServiceId<S>): S
}
