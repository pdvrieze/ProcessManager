package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.AgreedCosts
import io.github.pdvrieze.pma.agfil.data.Claim
import io.github.pdvrieze.pma.agfil.data.DamageAssessment
import io.github.pdvrieze.pma.agfil.parties.assessorProcess
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import java.util.logging.Logger
import kotlin.random.Random

class AssessorService(
    serviceAuth: PmaIdSecretAuthInfo,
    serviceName: ServiceName<AssessorService>,
    authService: AuthService,
    engineService: EngineService,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<AssessorService>(
    serviceAuth = serviceAuth,
    serviceName = serviceName,
    authService = authService,
    processEngineService = engineService,
    random = random,
    logger = logger,
    assessorProcess(serviceAuth.principal, ServiceId(serviceAuth.id))
), RunnableAutomatedService, AutoService {

    constructor(
        serviceName: ServiceName<AssessorService>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        engineService: EngineService,
        serviceResolver: ServiceResolver,
        random: Random,
        logger: Logger = authService.logger
    ) : this(
        authService.registerClient(adminAuthInfo, serviceName, random.nextString()),
        serviceName,
        authService,
        engineService,
        serviceResolver,
        random,
        logger
    )

    val internal: Internal = Internal()

    /** From Lai's thesis */
    fun assignAssessor(): Unit = TODO()

    inner class Internal {

        fun assessDamage(authToken: PmaAuthToken, claim: Claim): DamageAssessment {
            withGarage(authToken, claim.assignedGarageInfo) {

                TODO("not implemented")
            }
        }

        fun negotiateRepairCosts(authToken: PmaAuthToken, claim: Claim, assessment: DamageAssessment): AgreedCosts {
            TODO("not implemented")
        }

    }
}
