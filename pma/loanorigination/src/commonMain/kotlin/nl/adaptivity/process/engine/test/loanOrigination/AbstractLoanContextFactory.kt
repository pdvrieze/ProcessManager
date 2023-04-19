package nl.adaptivity.process.engine.test.loanOrigination

import io.github.pdvrieze.process.processModel.dynamicProcessModel.SimpleRolePrincipal
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.util.logging.Logger
import kotlin.random.Random

abstract class AbstractLoanContextFactory<AIC: ActivityInstanceContext>(val log: Logger, val random: Random) :
    ProcessContextFactory<AIC> {

    protected val nodes = mutableMapOf<PNIHandle, String>()

    val adminAuthServiceClient = run {
        val adminAuth = PmaIdSecretAuthInfo(SimplePrincipal("<AuthServiceAdmin>"))
        val authService = AuthService(ServiceNames.authService, adminAuth, log, nodes, random)
        DefaultAuthServiceClient(adminAuth, authService)
    }
    val authService: AuthService get()=  adminAuthServiceClient.authService
    val engineService: EngineService = EngineService(ServiceNames.engineService, authService, adminAuthServiceClient.originatingClientAuth)
    val customerFile = CustomerInformationFile(ServiceNames.customerFile, authService, adminAuthServiceClient.originatingClientAuth)
    val outputManagementSystem = OutputManagementSystem(ServiceNames.outputManagementSystem, authService, adminAuthServiceClient.originatingClientAuth)
    val accountManagementSystem = AccountManagementSystem(ServiceNames.accountManagementSystem, authService, adminAuthServiceClient.originatingClientAuth)
    val creditBureau = CreditBureau(ServiceNames.creditBureau, authService, adminAuthServiceClient.originatingClientAuth)
    val creditApplication = CreditApplication(ServiceNames.creditApplication, authService, adminAuthServiceClient.originatingClientAuth, customerFile)
    val pricingEngine = PricingEngine(ServiceNames.pricingEngine, authService, adminAuthServiceClient.originatingClientAuth)
    val generalClientService =
        GeneralClientService(ServiceNames.generalClientService, authService, adminAuthServiceClient.originatingClientAuth)
    val signingService = SigningService(ServiceNames.signingService, authService, adminAuthServiceClient.originatingClientAuth)


    val customerData = CustomerData(
        "cust123456",
        "taxId234",
        "passport345",
        "John Doe",
        "10 Downing Street"
    )

    object principals: AbstractList<PrincipalCompat>() {
        fun withName(userName: String): PrincipalCompat? {
            return map[userName]
        }

        val clerk1 = SimpleRolePrincipal("preprocessing clerk 1", "clerk", "bankuser")
        val clerk2 = SimpleRolePrincipal("postprocessing clerk 2", "clerk", "bankuser")
        val customer = SimpleRolePrincipal("John Doe", "customer")

        private val all = listOf(clerk1, clerk2, customer)

        private val map = all.associateBy { it.name }

        override val size: Int get() = all.size

        override fun get(index: Int): PrincipalCompat = all[index]
    }

    val clerk1: Browser = Browser(authService, adminAuthServiceClient.registerClient(principals.clerk1, random.nextString()))
    val postProcClerk: Browser = Browser(authService, adminAuthServiceClient.registerClient(principals.clerk2, random.nextString()))
    val customer: Browser = Browser(authService, adminAuthServiceClient.registerClient(principals.customer, random.nextString()))

    init {
        for (browser in listOf(clerk1, postProcClerk, customer)) {
            adminAuthServiceClient.registerGlobalPermission(browser.user, authService, CommonPMAPermissions.VALIDATE_AUTH(authService.serviceInstanceId))
        }

        adminAuthServiceClient.registerGlobalPermission(postProcClerk.user, authService, CommonPMAPermissions.VALIDATE_AUTH(authService.serviceInstanceId))
        adminAuthServiceClient.registerGlobalPermission(customer.user, authService, CommonPMAPermissions.VALIDATE_AUTH(authService.serviceInstanceId))
    }

    override fun getPrincipal(userName: String): PrincipalCompat {
        return principals.withName(userName) ?: SimplePrincipal(userName)
    }
}
