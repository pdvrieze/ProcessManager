package io.github.pdvrieze.pma.agfil.test

import RunnablePmaActivity
import io.github.pdvrieze.pma.agfil.contexts.AgfilContextFactory
import io.github.pdvrieze.pma.agfil.parties.policyHolderProcess
import io.github.pdvrieze.pma.agfil.services.PolicyHolder
import net.devrieze.util.security.PermissiveProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.runtime.PmaSecurityProvider
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAgfilProcess : ProcessEngineTestSupport() {

    @Test
    fun testProcessTypes() {
        val newOwner = SimplePrincipal("newOwner")
        val oldModel = policyHolderProcess(testModelOwnerPrincipal, ServiceId("test"))
        assertEquals(RunnablePmaActivity::class.java as Class<*>, oldModel.getNode("sendCar")?.javaClass as Class<*>?)

        val newModel = ExecutableProcessModel(oldModel.builder().apply { owner = newOwner })
        val sendCarNode = newModel.getNode("sendCar")
        assertEquals(RunnablePmaActivity::class.java as Class<*>, sendCarNode?.javaClass as Class<*>?)

    }

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

        val claimProcessHandle = policyHolder.initiateClaimProcess()


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
