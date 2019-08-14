/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.MutableTransactionedHandleMap
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.BaseProcessEngineTestSupport.Companion.PNI_SET_HANDLE
import nl.adaptivity.process.engine.test.BaseProcessEngineTestSupport.Companion.cacheInstances
import nl.adaptivity.process.engine.test.BaseProcessEngineTestSupport.Companion.cacheModels
import nl.adaptivity.process.engine.test.BaseProcessEngineTestSupport.Companion.cacheNodes
import java.net.URI
import java.util.logging.Logger
import javax.xml.namespace.QName

open class EngineTestData(val messageService: StubMessageService, val engine: ProcessEngine<StubProcessTransaction, *>) {

    private constructor(messageService: StubMessageService)
        : this(
        messageService,
        object : ProcessTransactionFactory<StubProcessTransaction> {
            override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
                return StubProcessTransaction(engineData)
            }
        },
        cacheModels<Any>(MemProcessModelMap(), 3),
        cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>(), 3),
        cacheNodes<Any>(MemTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, StubProcessTransaction>(PNI_SET_HANDLE), 3)
              )

  private constructor(messageService: StubMessageService,
                      transactionFactory: ProcessTransactionFactory<StubProcessTransaction>,
                      processModels: IMutableProcessModelMap<StubProcessTransaction>,
                      processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>,
                      processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, StubProcessTransaction>)
    : this(messageService, ProcessEngine.newTestInstance(messageService, transactionFactory, processModels, processInstances, processNodeInstances, true, Logger.getAnonymousLogger()))

  constructor(): this(
    StubMessageService(localEndpoint))

  companion object {
    val localEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"), "processEngine",
                                                                       URI.create("http://localhost/"))
    val principal = SimplePrincipal("pdvrieze")
    fun defaultEngine() = EngineTestData()
  }

}
