package nl.adaptivity.process.engine.test.loanOrigination

import io.github.pdvrieze.process.processModel.dynamicProcessModel.SimpleRolePrincipal
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.GeneralClientService
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.util.logging.Logger
import kotlin.random.Random

abstract class AbstractLoanContextFactory<AIC: ActivityInstanceContext>(val log: Logger, val random: Random) :
    ProcessContextFactory<AIC> {

    protected val nodes = mutableMapOf<PNIHandle, String>()

    val authService: AuthService = AuthService(log, nodes, random)
    val authServiceClient: AuthServiceClient
        get() = AuthServiceClientImpl(authService)

    val engineService : EngineService = EngineService(authService)

    val customerFile = CustomerInformationFile(authService)
    val outputManagementSystem = OutputManagementSystem(authService)
    val accountManagementSystem = AccountManagementSystem(authService)
    val creditBureau = CreditBureau(authService)
    val creditApplication = CreditApplication(authService, customerFile)
    val pricingEngine = PricingEngine(authService)
    val generalClientService = GeneralClientService(authService)
    val signingService = SigningService(authService)


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

    val clerk1: Browser = Browser(authService, principals.clerk1)
    val postProcClerk: Browser = Browser(authService, principals.clerk2)
    val customer: Browser = Browser(authService, principals.customer)


    override fun getPrincipal(userName: String): PrincipalCompat {
        return principals.withName(userName) ?: SimplePrincipal(userName)
    }
}
