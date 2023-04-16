package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.repairProcess
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.xmlutil.serialization.XML
import java.util.*
import java.util.logging.Logger
import kotlin.random.Random

class GarageService private constructor(
    serviceAuth: PmaIdSecretAuthInfo,
    serviceName: ServiceName<GarageService>,
    authService: AuthService,
    processEngine: ProcessEngine<StubProcessTransaction>,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<GarageService>(
    serviceAuth,
    serviceName,
    authService,
    processEngine,
    random,
    logger,
    repairProcess(serviceAuth.principal, serviceName),
), RunnableAutomatedService, RunnableUiService, AutoService {

    constructor(
        serviceName: ServiceName<GarageService>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        processEngine: ProcessEngine<StubProcessTransaction>,
        serviceResolver: ServiceResolver,
        random: Random,
        logger: Logger = authService.logger
    ) : this(
        serviceAuth = authService.registerClient(adminAuthInfo, serviceName, random.nextString()),
        serviceName = serviceName,
        authService = authService,
        processEngine = processEngine,
        serviceResolver = serviceResolver,
        random = random,
        logger = logger
    )

    constructor(
        serviceName: ServiceName<GarageService>,
        authService: AuthService,
        adminAuth: PmaAuthInfo,
        processEngine: ProcessEngine<StubProcessTransaction>,
        serviceResolver: ServiceResolver,
        random: Random,
    ) : this(
        serviceAuth = authService.registerClient(adminAuth, serviceName, random.nextString()),
        serviceName = serviceName,
        authService = authService,
        processEngine = processEngine,
        serviceResolver = serviceResolver,
        random = random,
        logger = authService.logger
    )

    val internal: Internal = Internal()
    val xml = XML { this.recommended() }
    val hRepairProcess: Handle<ExecutableProcessModel> get() = processHandles[0]

    private val repairs = mutableMapOf<ClaimId, RepairInfo>()

    /**
     * ContactGarage from Lai's thesis
     */
    fun informGarageOfIncomingCar(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo) {
        val payload = CompactFragment { xml.encodeToWriter(it, AccidentInfo.serializer(), accidentInfo) }
        processEngine.inTransaction { tr ->
            startProcess(tr, authServiceClient.principal, hRepairProcess, "estimate repair", UUID.randomUUID(), payload)
            repairs[claimId]= RepairInfo(claimId, accidentInfo)
        }
    }

    /** From Lai's thesis. Receive car. */
    fun sendCar(authToken: PmaAuthInfo, carRegistration: CarRegistration, claimId: ClaimId): Unit = TODO()

    /** From Lai's thesis */
    fun agreeRepair(authToken: PmaAuthInfo, id: ClaimId, carRegistration: CarRegistration): Unit = TODO()

    /** From Lai's thesis */
    fun payRepairCost(authToken: PmaAuthInfo): Unit = TODO()

    inner class Internal {
        fun registerCarReceipt(authToken: PmaAuthInfo, claimId: ClaimId, carRegistration: CarRegistration) {
            val repairInfo = requireNotNull(repairs[claimId]) { "missing repair" }
            with(repairInfo) {
                require(repairState == RepairState.WAITING) {}
                repairState = RepairState.RECEIVED_CAR
            }
        }

        fun repairCar(authToken: PmaAuthInfo, repairAgreement: RepairAgreement) {
            val repairInfo = requireNotNull(repairs[repairAgreement.claimId])
            repairInfo.repairState = RepairState.CAR_REPAIRED
        }

        fun recordEstimatedRepairCost(authToken: PmaAuthInfo, claimId: ClaimId, costs: Money) {
            val repairInfo = requireNotNull(repairs[claimId])
            with(repairInfo) {
                estimate = Estimate(claimId, accidentInfo.carRegistration, costs)
                repairState = RepairState.CAR_ASSESSED
            }

        }

        fun handlePayment(authToken: PmaAuthInfo, payment: Payment) {
            val repairInfo = repairs.values.first { it.invoiceId == payment.invoiceId }
            require(repairInfo.repairState == RepairState.INVOICED)
            val agreedCosts = requireNotNull(repairInfo.agreedCosts?.costs)
            require(agreedCosts < payment.money)
            repairInfo.repairState = RepairState.PAID
        }

        fun closeRecord(authToken: PmaAuthInfo, claimId: ClaimId) {
            val repairInfo = requireNotNull(repairs[claimId])
            require(repairInfo.repairState == RepairState.PAID)
        }
    }

    private data class RepairInfo(
        val claimId: ClaimId,
        val accidentInfo: AccidentInfo,
        var repairState: RepairState = RepairState.WAITING,
        var estimate: Estimate? = null,
        var agreedCosts: AgreedCostInfo? = null,
        val invoiceId: InvoiceId? = null
    )

    private data class AgreedCostInfo(val assessor: Int, val costs: Money)

    private enum class RepairState {
        WAITING,
        RECEIVED_CAR,
        CAR_ASSESSED,
        CAR_REPAIRING,
        CAR_REPAIRED,
        INVOICED,
        PAID
    }
}
