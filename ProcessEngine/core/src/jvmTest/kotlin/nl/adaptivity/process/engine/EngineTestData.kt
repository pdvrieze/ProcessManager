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
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport.Companion.PNI_SET_HANDLE
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport.Companion.cacheInstances
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport.Companion.cacheModels
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport.Companion.cacheNodes
import java.net.URI
import java.util.logging.Logger
import javax.xml.namespace.QName

open class EngineTestData<C : ActivityInstanceContext>(
    val messageService: StubMessageService<C>,
    val engine: ProcessEngine<StubProcessTransaction<C>, C>
) {

    companion object {
        val localEndpoint = EndpointDescriptorImpl(
            QName.valueOf("processEngine"), "processEngine",
            URI.create("http://localhost/")
        )
        val principal = SimplePrincipal("pdvrieze")
        fun defaultEngine() = StubMessageService<ActivityInstanceContext>(localEndpoint)


        private operator fun invoke(messageService: StubMessageService<ActivityInstanceContext>)= EngineTestData(
            messageService,
            object : ProcessTransactionFactory<StubProcessTransaction<ActivityInstanceContext>, ActivityInstanceContext> {
                override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction<ActivityInstanceContext>, ActivityInstanceContext>): StubProcessTransaction<ActivityInstanceContext> {
                    return StubProcessTransaction(engineData)
                }
            },
            cacheModels<Any, ActivityInstanceContext>(MemProcessModelMap(), 3),
            cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance<ActivityInstanceContext>>, StubProcessTransaction<ActivityInstanceContext>>(), 3),
            cacheNodes<Any, ActivityInstanceContext>(
                MemTransactionedHandleMap<SecureObject<ProcessNodeInstance<*, ActivityInstanceContext>>, StubProcessTransaction<ActivityInstanceContext>>(
                    ::PNI_SET_HANDLE
                ), 3
            )
        )


        private operator fun invoke(
            messageService: StubMessageService<ActivityInstanceContext>,
            transactionFactory: ProcessTransactionFactory<StubProcessTransaction<ActivityInstanceContext>, ActivityInstanceContext>,
            processModels: IMutableProcessModelMap<StubProcessTransaction<ActivityInstanceContext>>,
            processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance<ActivityInstanceContext>>, StubProcessTransaction<ActivityInstanceContext>>,
            processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*, ActivityInstanceContext>>, StubProcessTransaction<ActivityInstanceContext>>
        ): EngineTestData<ActivityInstanceContext> {
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
