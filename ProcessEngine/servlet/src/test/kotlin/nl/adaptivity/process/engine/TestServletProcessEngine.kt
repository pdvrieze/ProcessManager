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

import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.ProcessEngine.Companion.newTestInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.servlet.ServletProcessEngine
import java.net.URI
import java.util.logging.Logger

/**
 * Created by pdvrieze on 09/12/15.
 */
class TestServletProcessEngine(localURL: EndpointDescriptorImpl?) : ServletProcessEngine<StubProcessTransaction<C>>() {
    private val mProcessModels: MemProcessModelMap
    private val mProcessInstances: MemTransactionedHandleMap<SecureObject<ProcessInstance3>, StubProcessTransaction<C>>
    private val mProcessNodeInstances: MemTransactionedHandleMap<SecureObject<ProcessNodeInstance<*, *>>, StubProcessTransaction<C>>
    val transactionFactory: ProcessTransactionFactory<StubProcessTransaction<C>>

    init {
        transactionFactory = object : ProcessTransactionFactory<StubProcessTransaction<C>> {
            override fun startTransaction(
                engineData: IProcessEngineData<StubProcessTransaction<C>>
            ): StubProcessTransaction<C> {
                return StubProcessTransaction<C>(engineData)
            }
        }
        mProcessModels = MemProcessModelMap()
        mProcessInstances = MemTransactionedHandleMap()
        mProcessNodeInstances = MemTransactionedHandleMap()
        val messageService = MessageService(localURL!!)
        val engine = newTestInstance<StubProcessTransaction<C>>(
            messageService, transactionFactory, mProcessModels, mProcessInstances,
            mProcessNodeInstances, false, Logger.getAnonymousLogger()
        )
        init(engine)
    }

    fun reset() {
        mProcessInstances.reset()
        mProcessModels.reset()
        mProcessNodeInstances.reset()
    }

    public override fun setLocalEndpoint(localURL: URI) {
        super.setLocalEndpoint(localURL)
    }
}
