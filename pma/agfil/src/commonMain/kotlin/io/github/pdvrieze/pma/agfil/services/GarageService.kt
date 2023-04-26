package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.repairProcess
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.payload
import net.devrieze.util.Handle
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.PmaServiceResolver
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML
import java.util.logging.Logger
import kotlin.random.Random

class GarageService(
    serviceName: ServiceName<GarageService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: PmaServiceResolver,
    random: Random,
    logger: Logger = authService.logger,
) : RunnableProcessBackedService<GarageService>(
    serviceName,
    authService,
    adminAuthInfo,
    engineService,
    random,
    logger,
    { repairProcess(adminAuthInfo.principal, serviceInstanceId) },
), RunnableAutomatedService, RunnableUiService, AutoService {

    private val unassignedCars = mutableMapOf<ClaimId, CarRegistration>()
    val garageInfo = GarageInfo(serviceName.serviceName, serviceInstanceId.serviceId)

    val internal: Internal = Internal()
    val xml = XML { this.recommended() }
    val hRepairProcess: Handle<ExecutableProcessModel> get() = processHandles[0]

    private val repairs = mutableMapOf<ClaimId, RepairInfo>()
    private val receptionists: List<PrincipalCompat> = (1..3).map { SimplePrincipal("${serviceName.serviceName} receptionist ${it}") }
    private val mechanics: List<PrincipalCompat> = (1..3).map { SimplePrincipal("${serviceName.serviceName} mechanic $it") }

    /**
     * ContactGarage from Lai's thesis
     */
    fun informGarageOfIncomingCar(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo) {
        validateAuthInfo(authToken, GARAGE.INFORM_INCOMING_CAR)
        val payload = payload("claim", claimId, "accidentInfo", accidentInfo)
        val instanceHandle = startProcess(processHandles[0], payload)
        val repair = RepairInfo(instanceHandle, claimId, accidentInfo)
        repairs[claimId]= repair
        val unassignedCar = unassignedCars[claimId]

        if (unassignedCar!=null) { // XXX there is an order problem that allows cars to arrive before the garage is informed
            require(accidentInfo.carRegistration == unassignedCar)
            processEngineService.deliverEvent(instanceHandle, Identifier("onReceiveCar"), payload(unassignedCar))
        }
    }

    /** From Lai's thesis. Receive car. from policyHolder */
    fun evReceiveCar(authToken: PmaAuthInfo, carRegistration: CarRegistration, claimId: ClaimId) {
        validateAuthInfo(authToken, GARAGE.SEND_CAR(carRegistration))
        val repair = repairs[claimId]
        if (repair == null) {
            unassignedCars[claimId] = carRegistration
        } else {

            require(repair.accidentInfo.carRegistration == carRegistration)
            require(repair.repairState == RepairState.WAITING)
            val instanceHandle = repair.processHandle
            processEngineService.deliverEvent(instanceHandle, Identifier("onReceiveCar"), payload(carRegistration))
            repair.repairState = RepairState.RECEIVED_CAR
        }
    }

    /** From Lai's thesis */
    fun evRepairAgreed(authToken: PmaAuthInfo, id: ClaimId, carRegistration: CarRegistration, agreedCosts: Money) {
        validateAuthInfo(authToken, GARAGE.AGREE_REPAIR(id))
        val processHandle = requireNotNull(repairs[id]).processHandle
        deliverEvent(processHandle, Identifier("onRepairAgreed"), payload(RepairAgreement(id, carRegistration, agreedCosts)))
    }

    private var _nextInvoiceId=1
    fun nextInvoiceId(): InvoiceId = InvoiceId(_nextInvoiceId++)

    /** From Lai's thesis */
    fun payRepairCost(authToken: PmaAuthInfo): Unit = TODO()

    fun evNotifyInvoicePaid(authToken: PmaAuthInfo, invoiceId: InvoiceId, amount: Money) {
        validateAuthInfo(authToken, GARAGE.NOTIFY_INVOICE_PAID(invoiceId))
        val piHandle = repairs.values.first { it.invoice?.invoiceId == invoiceId }.processHandle
        processEngineService.deliverEvent(piHandle, Identifier("onReceivePayment"), payload(Payment(invoiceId, amount)))
    }

    inner class Internal {
        fun registerCarReceipt(authToken: PmaAuthInfo, claimId: ClaimId, carRegistration: CarRegistration) {
            // TODO validate auth
            val repairInfo = requireNotNull(repairs[claimId]) { "missing repair" }
            with(repairInfo) {
                require(repairState == RepairState.WAITING) { "When car is received it should be in waiting state. Was: $repairState" }
                repairState = RepairState.RECEIVED_CAR
            }
        }

        fun repairCar(authToken: PmaAuthInfo, repairAgreement: RepairAgreement) {
            // TODO validate auth
            val repairInfo = requireNotNull(repairs[repairAgreement.claimId])
            repairInfo.repairState = RepairState.CAR_REPAIRED
            repairInfo.agreedCosts = AgreedCostInfo(-1, repairAgreement.agreedCosts)
        }

        fun recordEstimatedRepairCost(authToken: PmaAuthInfo, claimId: ClaimId, costs: Money) {
            val repairInfo = requireNotNull(repairs[claimId])
            with(repairInfo) {
                estimate = Estimate(claimId, accidentInfo.carRegistration, costs)
                repairState = RepairState.CAR_ASSESSED
            }

        }

        fun handlePayment(authToken: PmaAuthInfo, payment: Payment) {
            val repairInfo = repairs.values.first { it.invoice?.invoiceId == payment.invoiceId }
            require(repairInfo.repairState == RepairState.INVOICED) { "Repair state should be invoiced, was: ${repairInfo.repairState}" }
            val agreedCosts = requireNotNull(repairInfo.agreedCosts?.costs)
            require(agreedCosts <= payment.money)
            repairInfo.repairState = RepairState.PAID
        }

        fun closeRecord(authToken: PmaAuthInfo, claimId: ClaimId) {
            val repairInfo = requireNotNull(repairs[claimId])
            require(repairInfo.repairState == RepairState.PAID)
        }

        fun sendInvoice(authToken: PmaAuthToken, agreement: RepairAgreement): InvoiceId {
            val invoice = Invoice(nextInvoiceId(), agreement.claimId, garageInfo, agreement.agreedCosts)
            withService(ServiceNames.leeCsService, authToken, LEECS.SEND_INVOICE(agreement.claimId)) {
                service.evSendInvoice(serviceAccessToken, invoice)
            }
            requireNotNull(repairs[agreement.claimId]).run {
                require(this.repairState == RepairState.CAR_REPAIRED)
                this.repairState = RepairState.INVOICED
                this.invoice = invoice
            }
            return invoice.invoiceId
        }


        fun processHandleFor(claimId: ClaimId): PIHandle {
            return requireNotNull(repairs[claimId]).processHandle
        }

        fun randomGarageReceptionist(): PrincipalCompat {
            return receptionists.random(random)
        }

        fun randomMechanic(): PrincipalCompat {
            return mechanics.random(random)
        }

    }

    private data class RepairInfo(
        val processHandle: PIHandle,
        val claimId: ClaimId,
        val accidentInfo: AccidentInfo,
        var repairState: RepairState = RepairState.WAITING,
        var estimate: Estimate? = null,
        var agreedCosts: AgreedCostInfo? = null,
        var invoice: Invoice? = null,
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
