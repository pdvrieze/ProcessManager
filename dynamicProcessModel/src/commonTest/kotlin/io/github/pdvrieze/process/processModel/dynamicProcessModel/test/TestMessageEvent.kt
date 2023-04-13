package io.github.pdvrieze.process.processModel.dynamicProcessModel.test

import io.github.pdvrieze.process.processModel.dynamicProcessModel.runnableProcess
import kotlinx.serialization.Serializable
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.engine.test.testProcess
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestMessageEvent  : ProcessEngineTestSupport() {

    @Test
    fun testEventModel() {
        testProcess(testModel) { engine, transaction, model, piHandle ->
            run {
                val pinst = transaction.readableEngineData.instance(piHandle).withPermission()
                val startInst = pinst.child(transaction, "start")
                assertEquals(NodeInstanceState.Complete, startInst.state)

                val ac1Inst = pinst.child(transaction, "ac1")
                assertEquals(NodeInstanceState.Complete, ac1Inst.state)

                val evInst = pinst.child(transaction, "ev")
                assertEquals(NodeInstanceState.Sent, evInst.state)

                assertNull(pinst.getChild( "ac2", evInst.entryNo))
                val data = CompactFragment { writer -> XML.encodeToWriter(writer, Data("foobar")) }
                engine.finishTask(transaction, evInst.handle, data, testModelOwnerPrincipal)
            }

            run {
                val pinst = transaction.readableEngineData.instance(piHandle).withPermission()
                val evInst = pinst.child(transaction, "ev")
                assertEquals(NodeInstanceState.Complete, evInst.state)

                val ac2Inst = pinst.child(transaction, "ac2")
                assertEquals(NodeInstanceState.Complete, ac2Inst.state)

                assertEquals(ProcessInstance.State.FINISHED, pinst.state)
                val result = pinst.outputs.single().content.contentString
                assertEquals("<Data>foobarfoobar</Data>", result)
            }

        }
    }


    val testModel = runnableProcess<ActivityInstanceContext>("test") {
        val start by startNode
        val ac1 by activity(start) {

        }
        val ev by eventNode(ac1, Data.serializer())
        val ac2 by activity(ev) {
            Data(it.content+it.content)
        }
        val end by endNode(ac2)

        processResult("result", ac2)
    }


    @Serializable
    class Data(@XmlValue(true) val content: String)
}
