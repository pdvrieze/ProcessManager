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
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.StubTransaction
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport.Companion.PNI_SET_HANDLE
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport.Companion.cacheInstances
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport.Companion.cacheModels
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport.Companion.cacheNodes
import java.net.URI
import java.util.logging.Logger
import javax.xml.namespace.QName
import kotlin.coroutines.startCoroutine

open class EngineTestData(
    val messageService: StubMessageService,
    val engine: ProcessEngine<StubProcessTransaction>
) {

    companion object {
        val localEndpoint = EndpointDescriptorImpl(
            QName.valueOf("processEngine"), "processEngine",
            URI.create("http://localhost/")
        )
        val principal = SimplePrincipal("pdvrieze")
        fun defaultEngine(): EngineTestData = EngineTestData(StubMessageService(localEndpoint))


        private operator fun invoke(messageService: StubMessageService) = EngineTestData(
            messageService,
            object : ProcessTransactionFactory<StubProcessTransaction> {
                override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
                    return StubProcessTransaction(engineData)
                }

                override fun <R> inTransaction(
                    engineData: IProcessEngineData<StubProcessTransaction>,
                    action: suspend StubProcessTransaction.() -> R
                ): R {
                    return StubTransaction.inTransaction({ StubProcessTransaction(engineData) }, action)
                }
            },
            cacheModels(MemProcessModelMap(), 3),
            cacheInstances<SecureProcessInstance>(MemTransactionedHandleMap(), 3),
            cacheNodes(
                MemTransactionedHandleMap(::PNI_SET_HANDLE), 3
            )
        )


        private operator fun invoke(
            messageService: StubMessageService,
            transactionFactory: ProcessTransactionFactory<StubProcessTransaction>,
            processModels: IMutableProcessModelMap<StubProcessTransaction>,
            processInstances: MutableTransactionedHandleMap<SecureProcessInstance, StubProcessTransaction>,
            processNodeInstances: MutableTransactionedHandleMap<SecureProcessNodeInstance, StubProcessTransaction>
        ): EngineTestData {
            return EngineTestData(
                messageService,
                ProcessEngine.newTestInstance(
                    messageService,
                    transactionFactory,
                    processModels,
                    processInstances,
                    processNodeInstances,
                    true,
                    Logger.getAnonymousLogger()
                )
            )
        }

    }

}
