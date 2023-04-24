package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.agfilProcessContext
import io.github.pdvrieze.pma.agfil.parties.europAssistProcess
import io.github.pdvrieze.pma.agfil.parties.policyHolderProcess
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.engine.SecureProcessInstance
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import nl.adaptivity.process.util.Identifier
import java.util.logging.Logger
import kotlin.random.Random

class PolicyHolderService(
    serviceName: ServiceName<PolicyHolderService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<PolicyHolderService>(
    serviceName = serviceName,
    authService = authService,
    adminAuthInfo = adminAuthInfo,
    processEngineService = engineService,
    random = random,
    logger = logger,
    { policyHolderProcess(authServiceClient.principal, serviceInstanceId) }
), AutoService, AutomatedService {



    val internal: Internal = Internal()
    private val claimProcesses = mutableMapOf<ClaimId, PIHandle>()


    /** From Lai's thesis */
    fun evAssignGarage(authToken: PmaAuthInfo, claimId: ClaimId, garage: GarageInfo) {
        // TODO validate auth
        val piHandle = requireNotNull(claimProcesses[claimId])
        deliverEvent(piHandle, Identifier("onGarageAssigned"), garage)
    }

    /** From Lai's thesis */
    fun estimateRepairCost(): Unit = TODO()

    /** From Lai's thesis. Matches sendClaimForm */
    fun evReceiveClaimForm(authToken: PmaAuthInfo, form: IncompleteClaimForm) {
        validateAuthInfo(authToken, POLICYHOLDER.SEND_CLAIM_FORM(form.claimId))
        val piHandle = claimProcesses[form.claimId] ?: throw ProcessException("No process found for claim: ${form.claimId}")
        deliverEvent(piHandle, Identifier("receiveClaimForm"), form)
    }

    /** From Lai's thesis */
    fun repairCar(): Unit = TODO()

    fun initiateClaimProcess(): PIHandle {
        // try to add something that will record the claim id.
        return startProcess(processHandles[0])
    }

    inner class Internal {

        fun pickupCar(authToken: PmaAuthInfo, claimId: ClaimId) {
//            TODO("not implemented")
        }

        fun sendCar(authToken: PmaAuthToken, carRegistration: CarRegistration, claimId: ClaimId, garage: GarageInfo) {
            validateAuthInfo(authToken, POLICYHOLDER.INTERNAL.SEND_CAR(carRegistration))
            withGarage(authToken, garage) {
                service.evReceiveCar(authToken, carRegistration, claimId)
            }
        }

        fun reportClaim(authToken: PmaAuthToken, piHandle: PIHandle, callerInfo: CallerInfo, carRegistration: CarRegistration): ClaimId {
            return withService(ServiceNames.europAssistService, authToken, CommonPMAPermissions.IDENTIFY) {
                service.phoneClaim(authToken, carRegistration, "Random Accident info", callerInfo).also {
                    claimProcesses[it] = piHandle
                }
            }
        }

    }
}
