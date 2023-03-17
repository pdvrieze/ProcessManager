package nl.adaptivity.process.engine.pma.test

import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.engine.test.testProcess
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xml.QName
import nl.adaptivity.xmlutil.util.CompactFragment
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class TestPMA : ProcessEngineTestSupport<ActivityInstanceContext>() {


    @Test
    fun testPMAActivity() {
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

        val dest = QName("")
        val modelBuilder = RootProcessModelBase.Builder().apply {
            uuid = UUID.randomUUID()
            owner = testModelOwnerPrincipal
            val startNode = StartNodeBase.Builder().apply { id = "startNode" }
            nodes.add(startNode)

            val activity = MessageActivityBase.Builder().apply {
                predecessor = Identifier("startNode")
                id = "act"
                message = XmlMessage(messageBody = CompactFragment("<dummy/>"))
                accessRestrictions = accessRestriction
            }
            nodes.add(activity)

            val endNode = EndNodeBase.Builder().apply {
                predecessor = Identifier("act")
                id = "end"
            }
            nodes.add(endNode)
        }
        val model = ExecutableProcessModel(modelBuilder)
        val activityUser = SimplePrincipal("activityUser")
        testProcess(model) {
                engine,
                transaction,
                model,
                hinstance ->

            val message = stubMessageService.messages.singleOrNull() ?: fail("Expected a single message, found [${stubMessageService.messages.joinToString()}]")

            engine.takeTask(transaction, message.source, activityUser.getName(), activityUser)
            engine.updateTaskState(transaction, message.source, NodeInstanceState.Started, activityUser)
            engine.finishTask(transaction, message.source, null, activityUser)

            transaction.withInstance(hinstance) { instance ->
                val nodes = instance.childNodes.map { it.withPermission() }
                assertTrue(nodes.all { it.state.isFinal }, "Not all nodes are final: ${nodes.joinToString()}")

                assertEquals(ProcessInstance.State.FINISHED, instance.state)

            }

            transaction.readableEngineData.instance(hinstance).withPermission().let { instance ->
            }
            Unit
        }
    }
}
