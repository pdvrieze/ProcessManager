package io.github.pdvrieze.pma.agfil.contexts

import io.github.pdvrieze.pma.agfil.data.CallerInfo
import io.github.pdvrieze.pma.agfil.services.*
import io.github.pdvrieze.process.processModel.dynamicProcessModel.SimpleRolePrincipal
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaContextFactory
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
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

class AgfilContextFactory(private val logger: Logger, private val random: Random = Random, processEngineProvider: (AgfilContextFactory) -> ProcessEngine<StubProcessTransaction>) :
    AbstractDynamicPmaContextFactory<AgfilActivityContext>() {

    private val processEngine: ProcessEngine<StubProcessTransaction>

    private val processContexts = mutableMapOf<PIHandle, AgfilProcessContext>()
    private val browsers = mutableMapOf<String, Browser>()
    private val customerInfo: MutableMap<String, CallerInfo> = mutableMapOf()

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

    override val adminAuthServiceClient: DefaultAuthServiceClient
    val engineService: EngineService
    val taskListService: DynamicTaskList
    val agfilService: AgfilService
    val garageServices: Array<GarageService>
    val europAssistService: EuropAssistService
    val leeCsService: LeeCsService

    init {
        val adminAuth = PmaIdSecretAuthInfo(SimplePrincipal("<PMA-Admin>"))
        authService = AuthService(ServiceNames.authService, adminAuth, logger, nodes, random)
        adminAuthServiceClient = DefaultAuthServiceClient(adminAuth, authService)
        val engine = processEngineProvider(this)
        processEngine = engine
        engineService = EngineService(ServiceNames.engineService, authService, adminAuth).apply { initEngine(engine) }

        // TODO have separate task lists for each organization.
        taskListService = DynamicTaskList(
            ServiceNames.taskListService.serviceName,
            authService,
            engineService,
            authService.registerClient(adminAuth, ServiceName<DynamicTaskList>("TaskList(GLOBAL)"), Random.nextString()),
            ""
        )
        authService.registerGlobalPermission(adminAuth, engineService.authServiceClient.principal, taskListService, CommonPMAPermissions.POST_TASK)

        agfilService = AgfilService(ServiceNames.agfilService, authService, adminAuth, engineService, serviceResolver, random, logger)

        garageServices = ServiceNames.garageServices.arrayMap { GarageService(it, authService, adminAuth, engineService, serviceResolver, random) }
        europAssistService = EuropAssistService(ServiceNames.europAssistService, authService, adminAuth, engineService, serviceResolver, random, logger)
        leeCsService = LeeCsService(ServiceNames.leeCsService, authService, adminAuth, engineService, serviceResolver, random, logger)
    }


    private val _services: MutableList<Service> = mutableListOf(
        authService,
        engineService,
        taskListService,
        europAssistService,
        agfilService,
        leeCsService,
    ).apply { addAll(garageServices) }

    override val services: List<Service>
        get() = _services

    override fun resolveService(targetService: InvokableMethod): ResolvedInvokableMethod? {
        return null // TODO support proper messaging
    }

    override fun getOrCreateTaskListForUser(principal: Principal): TaskList<*> {
        adminAuthServiceClient.registerGlobalPermission(principal, taskListService, CommonPMAPermissions.ACCEPT_TASK)
        return taskListService
    }

    override fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskList<*>> {
        return listOf(taskListService)
    }

    fun resolveBrowser(principal: PrincipalCompat): Browser {
        return browsers.getOrPut(principal.name) { Browser(authService, adminAuthServiceClient.registerClient(principal, Random.nextString())) }
    }

    fun callerInfo(customer: PrincipalCompat): CallerInfo {
        val randomPhoneNumber = "0${(1..9).random(random)}${(1..8).joinToString("") { (0..9).random(random).toString()}}"
        // TODO this is a raw hack that hardcodes the policyHolder
        val policyHolderService = serviceResolver.resolveService(ServiceName<PolicyHolderService>("policyHolder")).serviceInstanceId
        return customerInfo.getOrPut(customer.name) { CallerInfo(customer.name, randomPhoneNumber, policyHolderService)}
    }

    fun createPolicyHolder(name: String) : PolicyHolderService {
        val policyHolder = PolicyHolderService(
            serviceName = ServiceName(name),
            authService = authService,
            adminAuthInfo = adminAuthServiceClient.originatingClientAuth,
            engineService = engineService,
            serviceResolver = serviceResolver,
            random = random,
            logger = logger
        )
        _services.add(policyHolder)
        return policyHolder
    }
}

