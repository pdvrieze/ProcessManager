/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3c.dom.Node
import java.net.URI
import java.security.Principal
import java.util.*
import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 30/12/16.
 */
class TestControlPatterns: Spek({
  val localEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"), "processEngine", URI.create("http://localhost/"))
  val stubMessageService = StubMessageService(localEndpoint)
  val stubTransactionFactory = object : ProcessTransactionFactory<StubProcessTransaction> {
    override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
      return StubProcessTransaction(engineData)
    }
  }
  val principal = SimplePrincipal("pdvrieze")

  beforeEachTest {
    stubMessageService.clear()
  }

  val processEngine = ProcessEngine.newTestInstance(
      stubMessageService,
      stubTransactionFactory,
      TestProcessEngine.cacheModels<Any>(MemProcessModelMap(), 3),
      TestProcessEngine.cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>(), 1),
      TestProcessEngine.cacheNodes<Any>(MemTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction>(TestProcessEngine.PNI_SET_HANDLE), 2), true)

  describe("Basic control-flow patterns") {

    describe("WCP1: A sequential process") {
      testWCP1(processEngine, principal)
    }

    describe("WCP2: Parallel split") {
      testWCP2(processEngine, principal)
    }

    describe("WCP3: Synchronization / And join") {
      testWCP3(processEngine, principal)
    }

    describe("WCP4: XOR split") {
      testWCP4(processEngine, principal)
    }

    describe("WCP5: simple-merge") {
      testWCP5(processEngine, principal)
    }
  }

  describe("Advanced branching and synchronization patterns") {
    describe("WCP6: multi-choice / or-split") {
      given("ac1.condition=true, ac2.condition=false") {
        testWCP6(processEngine, principal, true, false)
      }
      given("ac1.condition=false, ac2.condition=true") {
        testWCP6(processEngine, principal, false, true)
      }
      given("ac1.condition=true, ac2.condition=true") {
        testWCP6(processEngine, principal, true, true)
      }

    }

    describe("WCP7: structured synchronized merge") {
      given("ac1.condition=true, ac2.condition=false") {
        testWCP7(processEngine, principal, true, false)
      }
      given("ac1.condition=false, ac2.condition=true") {
        testWCP7(processEngine, principal, false, true)
      }
      given("ac1.condition=true, ac2.condition=true") {
        testWCP7(processEngine, principal, true, true)
      }

    }

    xdescribe("WCP8: Multi-merge", "Multiple instantiations of a single node are not yet supported") {
      testWCP8(processEngine, principal)
    }

    describe("WCP9: Structured Discriminator") {
      testWCP9(processEngine, principal)
    }
  }

  describe("Structural patterns") {
    xdescribe("WCP10: arbitrary cycles", "Multiple instantiations of a single node are not yet supported") {

    }

    describe("WCP11: Implicit termination") {
      testWCP11(processEngine, principal)
    }
  }

})

private fun Dsl.testWCP1(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start = startNode { id = "start" }
    val ac1 = activity { id = "ac1"; predecessor = start }
    val ac2 = activity { id = "ac2"; predecessor = ac1 }
    endNode { id = "end"; predecessor = ac2 }
  }

  it("should have 4 children") {
    assertEquals(4, model.modelNodes.size)
  }

  testTraces(processEngine, model, principal, valid = listOf(trace("start", "ac1", "ac2", "end")), invalid = listOf(trace("ac1", "ac2", "end"), trace("start", "ac2", "ac1", "end")))
}

private fun Dsl.testWCP2(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start = startNode { id = "start" }
    val split = split { id = "split"; predecessor = start; min = 2; max = 2 }
    val ac1 = activity { id = "ac1"; predecessor = split }
    val ac2 = activity { id = "ac2"; predecessor = split }
    val end1 = endNode { id = "end1"; predecessor = ac1 }
    val end2 = endNode { id = "end2"; predecessor = ac2 }
  }
  testTraces(processEngine, model, principal,
      valid = listOf(
          trace("start", "ac1", "end1", "ac2", "split", "end2"),
          trace("start", "ac1", "end1", "ac2", "end2", "split"),
          trace("start", "ac2", "end2", "ac1", "split", "end1"),
          trace("start", "ac2", "end2", "ac1", "end1", "split")),
      invalid = listOf("ac1", "ac2", "end1", "end2", "split").map { trace(it) } +
          listOf("split", "end1", "end2").map { trace("start", it) } +
          listOf("split", "end2").map { trace("start", "ac1", it) } +
          listOf("split", "end1").map { trace("start", "ac2", it) } +
          listOf("split", "end2").map { trace("start", "ac1", "end1", it) } +
          listOf("split", "end1").map { trace("start", "ac2", "end2", it) })
}

private fun Dsl.testWCP3(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start = startNode { id = "start" }
    val split = split { id = "split"; predecessor = start; min = 2; max = 2 }
    val ac1 = activity { id = "ac1"; predecessor = split }
    val ac2 = activity { id = "ac2"; predecessor = split }
    val join = join { id = "join"; predecessors(ac1, ac2); min = 2; max = 2 }
    val end = endNode { id = "end"; predecessor = join }
  }
  testTraces(processEngine, model, principal,
      valid = listOf(
          trace("start", "ac1", "ac2", "split", "join", "end"),
          trace("start", "ac2", "ac1", "split", "join", "end")),
      invalid = listOf("ac1", "ac2", "join", "end", "split").map { trace(it) } +
          listOf("split", "end", "join").map { trace("start", it) } +
          listOf("split", "join", "end").map { trace("start", "ac1", it) } +
          listOf("split", "join", "end").map { trace("start", "ac2", it) })
}

private fun Dsl.testWCP4(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start = startNode { id = "start" }
    val split = split { id = "split"; predecessor = start; min = 1; max = 1 }
    val ac1 = activity { id = "ac1"; predecessor = split }
    val ac2 = activity { id = "ac2"; predecessor = split }
    val end1 = endNode { id = "end1"; predecessor = ac1 }
    val end2 = endNode { id = "end2"; predecessor = ac2 }
  }
  testTraces(processEngine, model, principal,
      valid = listOf(
          trace("start", "ac1", "end1", "split"),
          trace("start", "ac1", "split", "end1"),
          trace("start", "ac2", "end2", "split"),
          trace("start", "ac2", "split", "end2")),
      invalid = listOf("ac1", "ac2", "end1", "end2", "split").map { trace(it) } +
          listOf("split", "end1", "end2").map { trace("start", it) } +
          listOf("end2", "ac2").map { trace("start", "ac1", it) } +
          listOf("end1", "ac1").map { trace("start", "ac2", it) })
}

private fun Dsl.testWCP5(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start = startNode { id = "start" }
    val split = split { id = "split"; predecessor = start; min = 1; max = 1 }
    val ac1 = activity { id = "ac1"; predecessor = split }
    val ac2 = activity { id = "ac2"; predecessor = split }
    val join = join { id = "join"; predecessors(ac1, ac2); min = 1; max = 1 }
    val ac3 = activity { id = "ac3"; predecessor = join }
    val end = endNode { id = "end"; predecessor = ac3 }
  }
  testTraces(processEngine, model, principal,
      valid = listOf(
          trace("start", "ac1", "split", "join", "ac3", "end"),
          trace("start", "ac2", "split", "join", "ac3", "end")),
      invalid = listOf("ac1", "ac2", "ac3", "end", "join").map { trace(it) } +
          listOf("join", "ac3", "end").map { trace("start", it) } +
          listOf(trace("start", "ac1", "ac2"),
              trace("start", "ac2", "ac1"))

  )
}

private fun Dsl.testWCP6(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal, ac1Condition: Boolean, ac2Condition: Boolean) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start = startNode { id = "start" }
    val split = split { id = "split"; predecessor = start; min = 1; max = 2 }
    val ac1 = activity { id = "ac1"; predecessor = split; condition = ac1Condition.toXPath() }
    val ac2 = activity { id = "ac2"; predecessor = split; condition = ac2Condition.toXPath() }
    val end1 = endNode { id = "end1"; predecessor = ac1 }
    val end2 = endNode { id = "end2"; predecessor = ac2 }
  }
  val invalidTraces = mutableListOf<Trace>()
  val validTraces = when {
    ac1Condition && ac2Condition -> {
      invalidTraces.add(trace("start", "ac1", "end2"))
      invalidTraces.add(trace("start", "ac2", "end1"))
      invalidTraces.add(trace("start", "ac1", "end1", "end2"))
      invalidTraces.add(trace("start", "ac2", "end2", "end1"))

      listOf(
          trace("start", "ac1", "end1", "ac2", "split", "end2"),
          trace("start", "ac1", "end1", "ac2", "end2", "split"),
          trace("start", "ac2", "end2", "ac1", "split", "end1"),
          trace("start", "ac2", "end2", "ac1", "end1", "split"))
    }
    ac1Condition && !ac2Condition -> {
      listOf("ac2", "end2").forEach { invalidTraces.add(trace("start", "ac1", it)) }
      invalidTraces.add(trace("start", "ac2"))
      listOf("ac2", "end2").forEach { invalidTraces.add(trace("start", "ac1", "end1", it)) }

      listOf(
          trace("start", "ac1", "end1", "split"),
          trace("start", "ac1", "split", "end1"))

    }
    !ac1Condition && ac2Condition -> {
      listOf("ac1", "end1").forEach { invalidTraces.add(trace("start", "ac2", it)) }
      invalidTraces.add(trace("start", "ac1"))
      listOf("ac1", "end1").forEach { invalidTraces.add(trace("start", "ac2", "end2", it)) }

      listOf(
          trace("start", "ac2", "end2", "split"),
          trace("start", "ac2", "split", "end2"))

    }
    else -> kfail("All cases need valid traces")
  }

  testTraces(processEngine, model, principal,
      valid = validTraces,
      invalid = listOf("ac1", "ac2", "end1", "end2", "split").map { trace(it) } +
          listOf("split", "end1", "end2").map { trace("start", it) } + invalidTraces)
}

private fun Dsl.testWCP7(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal, ac1Condition: Boolean, ac2Condition: Boolean) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start = startNode { id = "start" }
    val split = split { id = "split"; predecessor = start; min = 1; max = 2 }
    val ac1 = activity { id = "ac1"; predecessor = split; condition = ac1Condition.toXPath() }
    val ac2 = activity { id = "ac2"; predecessor = split; condition = ac2Condition.toXPath() }
    val join = join { id="join"; predecessors(ac1, ac2); min = 1; max=2 }
    val end = endNode { id = "end"; predecessor = join }
  }
  val invalidTraces = mutableListOf<Trace>()
  val validTraces = when {
    ac1Condition && ac2Condition -> {
      listOf("ac1", "ac2").forEach { ac ->
        listOf("end", "join", "split").forEach { invalidTraces.add(trace("start", ac, it)) }
      }

      listOf(
          trace("start", "ac1", "ac2", "split", "join", "end"),
          trace("start", "ac1", "ac2", "join", "split", "end"),
          trace("start", "ac2", "ac1", "split", "join", "end"),
          trace("start", "ac2", "ac1", "join", "split", "end"))
    }
    ac1Condition && !ac2Condition -> {
      invalidTraces.add(trace("start", "ac2"))
      invalidTraces.add(trace("start", "ac1", "ac2"))

      listOf(
          trace("start", "ac1", "join", "split", "end"),
          trace("start", "ac1", "split", "join", "end"))

    }
    !ac1Condition && ac2Condition -> {
      invalidTraces.add(trace("start", "ac1"))
      invalidTraces.add(trace("start", "ac2", "ac1"))

      listOf(
          trace("start", "ac2", "join", "split", "end"),
          trace("start", "ac2", "split", "join", "end"))

    }
    else -> kfail("All cases need valid traces")
  }

  testTraces(processEngine, model, principal,
      valid = validTraces,
      invalid = listOf("ac1", "ac2", "end", "join", "split").map { trace(it) } +
          listOf("split", "end", "join").map { trace("start", it) } + invalidTraces)
}

private fun Dsl.testWCP8(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start1 = startNode { id = "start1" }
    val start2 = startNode { id = "start2" }
    val ac1 = activity { id = "ac1"; predecessor = start1 }
    val ac2 = activity { id = "ac2"; predecessor = start2 }
    val join = join { id = "join"; predecessors(ac1, ac2); min = 1; max = 1 }
    val ac3 = activity { id = "ac3"; predecessor = join }
    val end = endNode { id = "end"; predecessor = ac3 }
  }
  testTraces(processEngine, model, principal,
      valid = listOf(
          trace("start1", "start2", "ac1", "join:1", "ac3:1", "end:1", "ac2", "join:2", "ac3:2", "end:2"),
          trace("start1", "start2", "ac1", "join:1", "ac2", "join:2", "ac3:1", "end:1", "ac3:2", "end:2"),
          trace("start1", "start2", "ac1", "join:1", "ac2", "join:2", "ac3:2", "end:2", "ac3:1", "end:1"),
          trace("start1", "start2", "ac2", "join:1", "ac3:1", "end:1", "ac1", "join:1", "ac3:2", "end:2"),
          trace("start1", "start2", "ac2", "join:1", "ac1", "join:1", "ac3:1", "end:1", "ac3:2", "end:2"),
          trace("start1", "start2", "ac2", "join:1", "ac1", "join:1", "ac3:2", "end:2", "ac3:1", "end:1")),
      invalid = listOf("ac1", "ac2", "ac3", "end", "join").map { trace(it) } +
          listOf("join", "ac3", "end").map { trace("start1", "start2", it) })
}

private fun Dsl.testWCP9(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start1 = startNode { id = "start1" }
    val start2 = startNode { id = "start2" }
    val ac1 = activity { id = "ac1"; predecessor = start1 }
    val ac2 = activity { id = "ac2"; predecessor = start2 }
    val join = join { id = "join"; predecessors(ac1, ac2); min = 1; max = 1 }
    val ac3 = activity { id = "ac3"; predecessor = join }
    val end = endNode { id = "end"; predecessor = ac3 }
  }
  testTraces(processEngine, model, principal,
      valid = listOf(
          trace("start1", "start2", "ac1", "join", "ac3", "end"),
          trace("start1", "start2", "ac2", "join", "ac3", "end")),
      invalid = listOf("ac1", "ac2", "ac3", "end", "join").map { trace(it) } +
          listOf("join", "ac3", "end").map { trace("start1", "start2", it) } +
          listOf(trace("start1", "start2", "ac1", "ac2"),
                 trace("start1", "start2", "ac2", "ac1"))
  )
}

private fun Dsl.testWCP11(processEngine: ProcessEngine<StubProcessTransaction>, principal: SimplePrincipal) {
  val model = ExecutableProcessModel.build {
    owner = principal
    val start1 = startNode { id = "start1" }
    val start2 = startNode { id = "start2" }
    val ac1 = activity { id = "ac1"; predecessor = start1 }
    val ac2 = activity { id = "ac2"; predecessor = start2 }
    val end1 = endNode { id = "end1"; predecessor = ac1 }
    val end2 = endNode { id = "end2"; predecessor = ac2 }
  }
  testTraces(processEngine, model, principal,
      valid = listOf(
          trace("start1", "start2", "ac1", "end1", "ac2", "end2"),
          trace("start1", "start2", "ac2", "end2", "ac1", "end1")),
      invalid = listOf("ac1", "ac2", "end1", "end2").map { trace(it) } +
          listOf("end1", "end2").map { trace("start1", "start2", it) } +
          listOf(trace("start1", "start2", "ac1", "end2"),
              trace("start1", "start2", "ac1", "end1", "end2"),
              trace("start1", "start2", "ac2", "end1"),
              trace("start1", "start2", "ac2", "end2", "end1")))
}

private fun Boolean.toXPath() = if (this) "true()" else "false()"

private inline fun <R> ProcessEngine<StubProcessTransaction>.testProcess(model: ExecutableProcessModel, owner: Principal, payload: Node? = null, body: (ProcessTransaction, ExecutableProcessModel, HProcessInstance) -> R):R {
  startTransaction().use { transaction ->

    val modelHandle = addProcessModel(transaction, model, owner)
    val instanceHandle = startProcess(transaction, owner, modelHandle, "testInstance", UUID.randomUUID(), payload)

    return body(transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
  }
}

