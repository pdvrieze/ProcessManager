package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.europAssistProcess
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.pma.agfil.services.ServiceNames.agfilService
import io.github.pdvrieze.process.processModel.dynamicProcessModel.SimpleRolePrincipal
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.payload
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.PmaServiceResolver
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.util.logging.Logger
import kotlin.random.Random

class EuropAssistService(
    serviceName: ServiceName<EuropAssistService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: PmaServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<EuropAssistService>(
    serviceName = serviceName,
    authService = authService,
    adminAuthInfo = adminAuthInfo,
    processEngineService = engineService,
    random = random,
    logger = logger,
    europAssistProcess
), RunnableAutomatedService, RunnableUiService, AutoService {
    private val instanceHandles = mutableMapOf<ClaimId, PIHandle>()

    private var nextClaimId: Long = 0L

    val internal: Internal = Internal()
    private val callHandlers: List<PrincipalCompat>
    init {
        callHandlers = (1..5).map { i ->
            SimpleRolePrincipal("EA Call handler $i", "ea:callhandler") // registration etc. happens in the context factory upon browser init.
        }
    }

    /** From Lai's thesis */
    fun phoneClaim(authToken: PmaAuthInfo, carRegistration: CarRegistration, claimInfo: String, callerInfo: CallerInfo): ClaimId {
        validateAuthInfo(authToken, CommonPMAPermissions.IDENTIFY)
        val claimId = ClaimId(nextClaimId++)
        val claimData = payload(
            "carRegistration", carRegistration,
            "claimInfo", claimInfo,
            "callerInfo", callerInfo,
            "claimId", claimId,
        )
        val processHandle = startProcess(processHandles[0], claimData)
        return claimId.also {// TODO the handles can be independent
            instanceHandles[it] = processHandle
        }
    }

    /** From Lai's thesis */
    fun receiveInfo(authToken: PmaAuthInfo, info: String): ClaimId {
        TODO()
    }

    fun randomCallHandler(): PrincipalCompat {
        return callHandlers.random(random)
    }

    inner class Internal {

        fun pickGarage(authToken: PmaAuthToken, accidentInfo: AccidentInfo): GarageInfo {
            validateAuthInfo(authToken, EUROP_ASSIST.PICK_GARAGE)
            val garageServices = withService(serviceName = agfilService, authToken, LIST_GARAGES) {
                service.getContractedGarages(serviceAccessToken)
            }

            return garageServices.random(random)
        }

        fun assignGarage(
            authToken: PmaAuthToken,
            garage: GarageInfo,
            claimId: ClaimId,
            accidentInfo: AccidentInfo
        ): GarageInfo {
            validateAuthInfo(authToken, EUROP_ASSIST.INTERNAL.ASSIGN_GARAGE(claimId))
            require(claimId in instanceHandles)
            val customerServiceId = withService(agfilService, authToken, AGFIL.GET_CUSTOMER_INFO(accidentInfo.customerId)) {
                service.getCustomerInfo(serviceAccessToken, accidentInfo.customerId).service
            }
            // TODO actually not valid as no token exchange happens
            serviceResolver.resolveService(customerServiceId).evAssignGarage(authToken, claimId, garage)
/*
            withService(customerServiceId, authToken, CommonPMAPermissions.IDENTIFY) {
                service.evAssignGarage(serviceAccessToken, claimId, garage)
            }
*/

            withService(agfilService, authToken, CLAIM.RECORD_ASSIGNED_GARAGE(claimId)) {
                service.recordAssignedGarage(serviceAccessToken, claimId, garage)
            }

            return garage // Smarter way to do something here
        }

        fun processHandleFor(claimId: ClaimId): PIHandle {
            return instanceHandles.getOrElse(claimId) { Handle.invalid() }
        }

    }
}
