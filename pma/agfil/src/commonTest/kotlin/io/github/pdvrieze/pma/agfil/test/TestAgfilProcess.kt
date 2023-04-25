package io.github.pdvrieze.pma.agfil.test

import RunnablePmaActivity
import io.github.pdvrieze.pma.agfil.contexts.AgfilContextFactory
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import io.github.pdvrieze.pma.agfil.parties.policyHolderProcess
import io.github.pdvrieze.pma.agfil.util.get
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.RunningMessageService
import net.devrieze.util.security.PermissiveProvider
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.runtime.PmaSecurityProvider
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.EventType
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.test.*

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
        lateinit var messageService: RunningMessageService
        val contextFactory = AgfilContextFactory(logger, random) { acf ->
            messageService = RunningMessageService(acf.serviceResolver as RunningMessageService.ServiceResolver)
            defaultEngineFactory(messageService, transactionFactory, acf).also {
                it.setSecurityProvider(PermissiveProvider()) // temporarily during setup
                engine = it
            }
        }
        engine.setSecurityProvider(PmaSecurityProvider(contextFactory.engineService.serviceInstanceId, contextFactory.adminAuthServiceClient))
        val engineService = contextFactory.engineService

        val policyHolder = contextFactory.createPolicyHolder("policyHolder")

        val claimProcessHandle = policyHolder.initiateClaimProcess()
        messageService.processMessages()

        engine.inTransaction { tr ->
            val d = tr.readableEngineData
            val piPolicyHolder = d.instance(claimProcessHandle).withPermission()
            val pniReportClaim = assertNotNull(piPolicyHolder.getNodeInstance(Identifier("reportClaim"), 1))
            assertEquals(NodeInstanceState.Complete, pniReportClaim.state)
            val claimId = pniReportClaim.results.single().get<ClaimId>()
            assertNotNull(claimId, "Claim should not be null")

            val pendingInstances = engine.getVisibleProcessInstances(tr, SYSTEMPRINCIPAL)
            val pendingNodes = pendingInstances.flatMap {pi ->
                pi.activeNodes.map { "${pi.processModel.rootModel.name}($it)" }
            }
            for(pendingInstance in pendingInstances) {
                if(! pendingInstance.state.isFinal)  {
                    System.err.println("Process ${pendingInstance} not finished")
                } else {
                    println("Finished Process -- $pendingInstance")
                }
            }
            if (pendingNodes.isNotEmpty()) {
                System.err.println("Not all node instances were complete: ${pendingNodes.joinToString()}")
            }

            val hpiEuropAssist = contextFactory.europAssistService.internal.processHandleFor(claimId)
            val piEuropAssist = d.instance(hpiEuropAssist).withPermission()

            val pniNotifyAgfil = piEuropAssist.getNodeInstance(Identifier("notifyAgfilClaimAssigned"), 1)
            assertEquals(NodeInstanceState.Complete, pniNotifyAgfil?.state)


            val hpiAgfil = contextFactory.agfilService.internal.processHandleFor(claimId)
            assertTrue(hpiAgfil.isValid)
            val piAgfil = d.instance(hpiAgfil).withPermission()

            val hpiLeeCs = contextFactory.leeCsService.internal.processHandleFor(claimId)!!
            val piLeeCs = d.instance(hpiLeeCs).withPermission()

            val maybeAssignAssessor = piLeeCs.getChild("assignAssessor", 1)?.withPermission()

            val pickGarageInstance = piEuropAssist.getNodeInstance(Identifier("pickGarage"), 1)
            assertNotNull(pickGarageInstance)

            val garageServiceId = pickGarageInstance.results.single().get<GarageInfo>().serviceId
            val garageService = contextFactory.serviceResolver.resolveService(garageServiceId)
            val hpiGarage = garageService.internal.processHandleFor(claimId)
            val piGarage = d.instance(hpiGarage).withPermission()
        }

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
