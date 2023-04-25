package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.AgreedCosts
import io.github.pdvrieze.pma.agfil.data.Claim
import io.github.pdvrieze.pma.agfil.data.DamageAssessment
import io.github.pdvrieze.pma.agfil.parties.assessorProcess
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.PmaServiceResolver
import java.util.logging.Logger
import kotlin.random.Random

class AssessorService(
    serviceName: ServiceName<AssessorService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: PmaServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<AssessorService>(
    serviceName = serviceName,
    authService = authService,
    adminAuthInfo = adminAuthInfo,
    processEngineService = engineService,
    random = random,
    logger = logger,
    { assessorProcess(authServiceClient.principal, serviceInstanceId) }
), RunnableAutomatedService, AutoService {

    val internal: Internal = Internal()

    /** From Lai's thesis */
    fun assignAssessor(): Unit = TODO()

    inner class Internal {

        fun assessDamage(authToken: PmaAuthToken, claim: Claim): DamageAssessment {
            validateAuthInfo(authToken, ASSESSOR.ASSESS_DAMAGE(claim.id))
            withGarage(authToken, claim.assignedGarageInfo, GARAGE.REVIEW_CAR(claim.accidentInfo.carRegistration)) {

                TODO("not implemented")
            }
        }

        fun negotiateRepairCosts(authToken: PmaAuthToken, claim: Claim, assessment: DamageAssessment): AgreedCosts {
            validateAuthInfo(authToken, ASSESSOR.NEGOTIATE_REPAIR_COSTS(claim.id))
            withGarage(authToken, claim.assignedGarageInfo, GARAGE.NEGOTIATE_REPAIR_COSTS(claim.id)) {

                TODO("not implemented")
            }
        }

    }
}
