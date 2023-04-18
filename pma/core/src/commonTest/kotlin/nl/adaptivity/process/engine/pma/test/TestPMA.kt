package nl.adaptivity.process.engine.pma.test

import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.pma.models.PMAMessageActivity
import nl.adaptivity.process.engine.pma.models.toServiceId
import nl.adaptivity.process.engine.pma.runtime.PmaActivityContext
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.test.ProcessEngineFactory
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.messaging.SOAPMethodDesc
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment
import java.util.*
import kotlin.test.*

class TestPMA : ProcessEngineTestSupport() {

    @Test
    fun testPMAActivity() {
        val activityUser = SimplePrincipal("activityUser")
        val factory: ProcessEngineFactory = { messageService, transactionFactory ->
            defaultEngineFactory(
                messageService,
                transactionFactory,
                TestPMAContextFactory(testModelOwnerPrincipal, activityUser)
            )
        }


        val accessRestriction = object : AccessRestriction {
            override fun hasAccess(
                context: Any?,
                principal: PrincipalCompat,
                permission: SecurityProvider.Permission
            ): Boolean {
                assertEquals("activityUser", principal.name)
//                assertEquals(ProcessEnginePermissions.ASSIGNED_TO_ACTIVITY, permission)
                return true
            }

            override fun serializeToString(): String {
                return "UNSUPPORTED"
            }
        }

        val dest = SOAPMethodDesc(QName("http://example.org/myservice","myService"),"soap", "operation")
        val modelBuilder = RootProcessModelBase.Builder().apply {
            uuid = UUID.randomUUID()
            owner = testModelOwnerPrincipal
            val startNode = StartNodeBase.Builder().apply { id = "startNode" }
            nodes.add(startNode)

            val activity = PMAMessageActivity.Builder<PmaActivityContext<*>>().apply {
                predecessor = Identifier("startNode")
                id = "act"
                message = XmlMessage(dest, messageBody = CompactFragment("<dummy/>"))
                accessRestrictions = accessRestriction
                authorizationTemplates = mutableListOf(EvalMessageScope)
            }
            nodes.add(activity)

            val endNode = EndNodeBase.Builder().apply {
                predecessor = Identifier("act")
                id = "end"
            }
            nodes.add(endNode)
        }
        val model = ExecutableProcessModel(modelBuilder)
        testProcess(factory, model) {
                engine,
                transaction,
                model,
                hinstance ->

            val message = messageService.messages.singleOrNull() ?: fail("Expected a single message, found [${messageService.messages.joinToString()}]")
            val authData = assertIs<DummyTokenServiceAuthData>(message.authData)
            assertEquals(EvalMessageScope, authData.authorizations.single())
            assertEquals(dest.endpoint.toServiceId<Nothing>(), authData.targetService)

            engine.updateTaskState(transaction, message.source, NodeInstanceState.Started, testModelOwnerPrincipal)
            engine.finishTask(transaction, message.source, null, testModelOwnerPrincipal)

            transaction.withInstance(hinstance) { instance ->
                val nodes = instance.childNodes.map { it.withPermission() }
                assertTrue(nodes.all { it.state.isFinal }, "Not all nodes are final: ${nodes.joinToString()}")

                assertEquals(ProcessInstance.State.FINISHED, instance.state)

            }
        }
    }
}
