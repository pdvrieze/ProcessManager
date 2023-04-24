package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.repairProcess
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.payload
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.PIHandle
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

class GarageService(
    serviceName: ServiceName<GarageService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger = authService.logger
) : RunnableProcessBackedService<GarageService>(
    serviceName,
    authService,
    adminAuthInfo,
    engineService,
    random,
    logger,
    { repairProcess(adminAuthInfo.principal, serviceInstanceId) },
), RunnableAutomatedService, RunnableUiService, AutoService {

    val garageInfo = GarageInfo(serviceName.serviceName, serviceInstanceId.serviceId)

    val internal: Internal = Internal()
    val xml = XML { this.recommended() }
    val hRepairProcess: Handle<ExecutableProcessModel> get() = processHandles[0]

    private val repairs = mutableMapOf<ClaimId, RepairInfo>()

    /**
     * ContactGarage from Lai's thesis
     */
    fun informGarageOfIncomingCar(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo) {
        validateAuthInfo(authToken, GARAGE.INFORM_INCOMING_CAR)
        val payload = payload("claim", claimId, "accidentInfo", accidentInfo)
        val instanceHandle = startProcess(processHandles[0], payload)
        repairs[claimId]= RepairInfo(instanceHandle, claimId, accidentInfo)
    }

    /** From Lai's thesis. Receive car. from policyHolder */
    fun evReceiveCar(authToken: PmaAuthInfo, carRegistration: CarRegistration, claimId: ClaimId) {
        validateAuthInfo(authToken, GARAGE.SEND_CAR(carRegistration))
    }

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

    fun evNotifyInvoicePaid(authToken: PmaAuthInfo, invoiceId: InvoiceId, amount: Money) {
        validateAuthInfo(authToken, GARAGE.NOTIFY_INVOICE_PAID(invoiceId))

        TODO("not implemented")
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
        val processHandle: PIHandle,
        val claimId: ClaimId,
        val accidentInfo: AccidentInfo,
        var repairState: RepairState = RepairState.WAITING,
        var estimate: Estimate? = null,
        var agreedCosts: AgreedCostInfo? = null,
        val invoiceId: InvoiceId? = null,
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
