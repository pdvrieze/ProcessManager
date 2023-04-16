package io.github.pdvrieze.pma.agfil.contexts

import io.github.pdvrieze.pma.agfil.services.*
import io.github.pdvrieze.process.processModel.dynamicProcessModel.SimpleRolePrincipal
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaContextFactory
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.services.DynamicTaskList
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.ResolvedInvokableMethod
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.util.kotlin.arrayMap
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal
import java.util.logging.Logger
import kotlin.random.Random

class AgfilContextFactory(private val logger: Logger, private val random: Random = Random) :
    AbstractDynamicPmaContextFactory<AgfilActivityContext>() {

    private val processContexts = mutableMapOf<PIHandle, AgfilProcessContext>()
    private val browsers = mutableMapOf<String, Browser>()

    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ): AgfilActivityContext {
        val hProcessInstance = processNodeInstance.hProcessInstance

        val processContext = processContexts.getOrPut(hProcessInstance) {
            AgfilProcessContext(engineDataAccess, this, hProcessInstance, random)
        }
        return AgfilActivityContextImpl(processContext, processNodeInstance, random)
    }

    override fun getPrincipal(userName: String): PrincipalCompat {
        val domain = userName.substringAfter('@', "")
        return SimpleRolePrincipal(userName) // TODO actually have a more extensive user database for individual partners
    }

    private val nodes = mutableMapOf<PNIHandle, String>()

    val authService: AuthService

    override val engineServiceAuthServiceClient: DefaultAuthServiceClient

    init {
        val adminAuth = PmaIdSecretAuthInfo(SimplePrincipal("<PMA-Admin>"))
        authService = AuthService(ServiceNames.authService, adminAuth, logger, nodes, random)
        engineServiceAuthServiceClient = DefaultAuthServiceClient(adminAuth, authService)
    }


    val engineService: EngineService = EngineService(ServiceNames.engineService.serviceName, authService)

    // TODO have separate task lists for each organization.
    val taskListService: DynamicTaskList = DynamicTaskList(
        ServiceNames.taskListService.serviceName,
        authService,
        engineService,
        engineServiceAuthServiceClient.registerClient(ServiceName<DynamicTaskList>("TaskList(GLOBAL)"), Random.nextString()),
        ""
    )

    val agfilService: AgfilService = AgfilService(ServiceNames.agfilService, authService)

    val garageServices = ServiceNames.garageServices.arrayMap { GarageService(it, authService, TODO("pass engine for garage"), random) }

    val europAssistService = EuropAssistService(ServiceNames.europAssistService, authService, random, agfilService, serviceResolver)

    val leeCsService = LeeCsService(ServiceNames.leeCsService, authService, serviceResolver)

    override val services: List<Service> = listOf(
        authService,
        engineService,
        taskListService,
        europAssistService,
        agfilService,
        leeCsService,
    ) + garageServices.toList()

    override fun resolveService(targetService: InvokableMethod): ResolvedInvokableMethod? {
        return null // TODO support proper messaging
    }

    override fun getOrCreateTaskListForUser(principal: Principal): TaskList<*> {
        return taskListService
    }

    override fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskList<*>> {
        return listOf(taskListService)
    }

    fun resolveBrowser(principal: PrincipalCompat): Browser {
        return browsers.getOrPut(principal.name) { Browser(authService, engineServiceAuthServiceClient.registerClient(principal, Random.nextString())) }
    }
}

