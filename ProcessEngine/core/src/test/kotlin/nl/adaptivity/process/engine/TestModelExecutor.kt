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

import net.devrieze.util.CachingHandleMap
import net.devrieze.util.Handle
import net.devrieze.util.Transaction
import net.devrieze.util.TransactionedHandleMap
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.StubTransaction
import nl.adaptivity.process.StubTransactionFactory
import nl.adaptivity.process.engine.processModel.NodeWrapper
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.ProcessModelImpl
import java.net.URI
import java.util.*
import javax.xml.namespace.QName

private fun <V> cache(base: TransactionedHandleMap<V, Transaction>,
                      count: Int): TransactionedHandleMap<V, Transaction> {
  return CachingHandleMap(base, count)
}

private fun <V> cache(base: IProcessModelMap<Transaction>, count: Int): IProcessModelMap<Transaction> {
  return CachingProcessModelMap(base, count)
}

class TestModelExecutor(val model: ProcessModelImpl, val focus: NodeWrapper, modelNodes: List<NodeWrapper>) {
  private val transaction = StubTransaction()
  private val principal = SimplePrincipal("pdvrieze");

  val localEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"),
                                             "processEngine",
                                             URI.create("http://localhost/"))
  val stubMessageService = StubMessageService(localEndpoint)
  val engine = ProcessEngine.newTestInstance<Transaction>(stubMessageService,
                                                                  StubTransactionFactory(),
                                                                  cache<Any>(MemProcessModelMap(), 1),
                                                                  cache<ProcessInstance<Transaction>>(MemTransactionedHandleMap<ProcessInstance<Transaction>>(), 1),
                                                                  cache<ProcessNodeInstance<Transaction>>( MemTransactionedHandleMap<ProcessNodeInstance<Transaction>>(), 2),
                                                                  false)

  val processInstance:ProcessInstance<Transaction>

  init {
    val modelHandle = engine.addProcessModel(transaction, model, SecurityProvider.SYSTEMPRINCIPAL)

    val instanceHandle: HProcessInstance<Transaction> = engine.startProcess(transaction,
                                                  principal,
                                                  modelHandle,
                                                  "testInstance1",
                                                  UUID.randomUUID(),
                                                  null)
    processInstance = engine.getProcessInstance(transaction, instanceHandle, principal)


  }


  val  focusInstance: ProcessNodeInstance<StubTransaction>? get() = focus.instance



  /**
   * Run the instance until the focus element (don't start it yet)
   */
  fun runBeforeFocus() {

  }

  fun runToFocus() {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}