package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.xmlutil.serialization.XML
import java.util.logging.Logger
import kotlin.random.Random

class GarageService : RunnableProcessBackedService<GarageService>/*(
    serviceAuth,
    serviceName,
    authService,
    engineService,
    random,
    logger,
    repairProcess(serviceAuth.principal, serviceName),
)*/, RunnableAutomatedService, RunnableUiService, AutoService {

    override val serviceResolver: ServiceResolver

    constructor(
        serviceName: ServiceName<GarageService>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        engineService: EngineService,
        serviceResolver: ServiceResolver,
        random: Random,
        logger: Logger = authService.logger
    ) : super(
        serviceName = serviceName,
        authService = authService,
        processEngineService = engineService,
        adminAuthInfo = adminAuthInfo,
        random = random,
        logger = logger
    ) {
        this.serviceResolver = serviceResolver
    }

    val garageInfo = GarageInfo(serviceName.serviceName, serviceInstanceId.serviceId)

    val internal: Internal = Internal()
    val xml = XML { this.recommended() }
    val hRepairProcess: Handle<ExecutableProcessModel> get() = processHandles[0]

    private val repairs = mutableMapOf<ClaimId, RepairInfo>()

    /**
     * ContactGarage from Lai's thesis
     */
    fun informGarageOfIncomingCar(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo) {
        val payload = CompactFragment { xml.encodeToWriter(it, AccidentInfo.serializer(), accidentInfo) }
/*
        processEngineService.startProcess(hRepairProcess, "estimate repair", payload)
        processEngine.inTransaction { tr ->
            startProcess(tr, authServiceClient.principal, hRepairProcess, "estimate repair", UUID.randomUUID(), payload)
        }
*/
        repairs[claimId]= RepairInfo(claimId, accidentInfo)
    }

    /** From Lai's thesis. Receive car. */
    fun sendCar(authToken: PmaAuthInfo, carRegistration: CarRegistration, claimId: ClaimId): Unit = TODO()

    /** From Lai's thesis */
    fun agreeRepair(authToken: PmaAuthInfo, id: ClaimId, carRegistration: CarRegistration): Unit = TODO()

    private var _nextInvoiceId=1
    fun nextInvoiceId(): InvoiceId = InvoiceId(_nextInvoiceId++)

    /** From Lai's thesis */
    fun payRepairCost(authToken: PmaAuthInfo): Unit = TODO()
    fun sendInvoice(agreement: RepairAgreement): InvoiceId {
        val invoice = Invoice(nextInvoiceId(), agreement.claimId, garageInfo, agreement.agreedCosts)
        serviceResolver.resolveService(ServiceNames.leeCsService).sendInvoice(invoice)
        return invoice.invoiceId
    }

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
