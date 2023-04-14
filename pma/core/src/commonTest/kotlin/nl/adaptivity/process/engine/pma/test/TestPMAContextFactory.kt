package nl.adaptivity.process.engine.pma.test

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.pma.models.ResolvedInvokableMethod
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.pma.runtime.PMAProcessContextFactory
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal

class TestPMAContextFactory(principals: List<PrincipalCompat>) : PMAProcessContextFactory<TestPMAActivityContext> {

    constructor(vararg principals: PrincipalCompat) : this(principals.toList())

    private val principals = principals.associateByTo(mutableMapOf()) { it.name }

    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ): TestPMAActivityContext {
        val pic = TestPMAProcessInstanceContext(this, processNodeInstance.hProcessInstance)
        return TestPMAActivityContext(pic, processNodeInstance)
    }

    override fun getPrincipal(userName: String): PrincipalCompat {
        return principals.getOrPut(userName) { SimplePrincipal(userName) }
    }

    override val engineServiceAuthServiceClient: TestAuthServiceClient = TestAuthServiceClient()

    override fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskListService> {
        TODO("not implemented")
    }

    override fun resolveService(targetService: InvokableMethod): ResolvedInvokableMethod? {
        return object: ResolvedInvokableMethod {
            override val serviceId: ServiceId<*>
                get() = ServiceId<Service>(targetService.url?:targetService.toString())
            override val method: InvokableMethod
                get() = targetService
        }
    }

    override fun getOrCreateTaskListForUser(principal: Principal): TaskListService {
        TODO("not implemented")
    }
}
