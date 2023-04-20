package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.europAssistProcess
import io.github.pdvrieze.pma.agfil.services.ServiceNames.agfilService
import io.github.pdvrieze.process.processModel.dynamicProcessModel.SimpleRolePrincipal
import net.devrieze.util.Handle
import net.devrieze.util.security.RolePrincipal
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.SecureProcessInstance
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.smartStartTag
import java.util.logging.Logger
import javax.xml.namespace.QName
import kotlin.random.Random

class EuropAssistService(
    serviceName: ServiceName<EuropAssistService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: ServiceResolver,
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

    val internal: Internal = Internal()
    private val callHandlers: List<PrincipalCompat>
    init {
        callHandlers = (1..5).map { i ->
            SimpleRolePrincipal("EA Call handler $i", "ea:callhandler") // registration etc. happens in the context factory upon browser init.
        }


    }

    /** From Lai's thesis */
    fun phoneClaim(authToken: PmaAuthInfo, carRegistration: CarRegistration, claimInfo: String, callerInfo: CallerInfo): ClaimId {
        val claimData = CompactFragment { out ->
            out.smartStartTag(QName("carRegistration")) { XML.encodeToWriter(out, carRegistration) }
            out.smartStartTag(QName("claimInfo")) { XML.encodeToWriter(out, claimInfo) }
            out.smartStartTag(QName("callerInfo")) { XML.encodeToWriter(out, callerInfo) }
        }
        val processHandle = startProcess(processHandles[0], claimData)
        return ClaimId(processHandle.handleValue).also {// TODO the handles can be independent
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
            validateAuthInfo(authToken, AgfilPermissions.PICK_GARAGE)
            val garageServices = withService(serviceName = agfilService, authToken, AgfilPermissions.LIST_GARAGES) {
                service.getContractedGarages(serviceAccessToken)
            }

            return garageServices.random(random)
        }

        fun informGarage(
            authToken: PmaAuthToken,
            garage: GarageInfo,
            claimId: ClaimId,
            accidentInfo: AccidentInfo
        ): GarageInfo {
            withService(
                garage.service,
                authToken,
                AgfilPermissions.INFORM_GARAGE
            ) {
                service.informGarageOfIncomingCar(serviceAccessToken, claimId, accidentInfo)
            }

            withService(agfilService, authToken, CommonPMAPermissions.IDENTIFY) {
                service.recordAssignedGarage(serviceAccessToken, claimId, garage)
            }
/*

            val agfilService = serviceResolver.resolveService(agfilService)

            val agfilToken = authServiceClient.exchangeDelegateToken(
                authToken,
                agfilService.serviceInstanceId,
                CommonPMAPermissions.IDENTIFY
            )
            agfilService.recordAssignedGarage(agfilToken, claimId, garage)
*/

            return garage // Smarter way to do something here
        }

        fun processHandleFor(claimId: ClaimId): PIHandle {
            return instanceHandles.getOrElse(claimId) { Handle.invalid() }
        }

    }
}
