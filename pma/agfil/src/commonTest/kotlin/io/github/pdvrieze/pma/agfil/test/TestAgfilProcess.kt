package io.github.pdvrieze.pma.agfil.test

import io.github.pdvrieze.pma.agfil.contexts.AgfilContextFactory
import io.github.pdvrieze.pma.agfil.services.PolicyHolder
import net.devrieze.util.security.PermissiveProvider
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.runtime.PmaSecurityProvider
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.test.Test

class TestAgfilProcess : ProcessEngineTestSupport() {

    @Test
    fun testAgfilProcess() {
        val logger = Logger.getLogger(TestAgfilProcess::class.java.name)
        val random = Random(874365132L)

        val transactionFactory = stubTransactionFactory
        lateinit var engine: ProcessEngine<StubProcessTransaction>
        val contextFactory = AgfilContextFactory(logger, random) { acf ->
            defaultEngineFactory(messageService, transactionFactory, acf).also {
                it.setSecurityProvider(PermissiveProvider()) // temporarily during setup
                engine = it
            }
        }
        engine.setSecurityProvider(PmaSecurityProvider(contextFactory.engineService.serviceInstanceId, contextFactory.adminAuthServiceClient))
        val engineService = contextFactory.engineService


        val policyHolder = PolicyHolder(
            serviceName = ServiceName("policyHolder"),
            authService = contextFactory.authService,
            adminAuthInfo = contextFactory.adminAuthServiceClient.originatingClientAuth,
            engineService = engineService,
            serviceResolver = contextFactory.serviceResolver,
            random = random,
            logger = logger
        )

        policyHolder.initiateClaimProcess()

/*
        engine.startTransaction().use { transaction ->

            val modelHandle = engine.addProcessModel(transaction, agfilProcess, testModelOwnerPrincipal).handle
            val instanceHandle = engine.startProcess(transaction, testModelOwnerPrincipal, modelHandle, "testInstance",
                UUID.randomUUID(), null
            )

            transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission()
            return
        }
*/
    }
}
