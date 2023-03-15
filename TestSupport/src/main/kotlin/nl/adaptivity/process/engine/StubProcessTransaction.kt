/*
 * Copyright (c) 2019.
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

import nl.adaptivity.process.StubTransaction
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.xmlutil.util.CompactFragment
import java.security.Principal
import java.util.*
import kotlin.reflect.KProperty

/**
 * Created by pdvrieze on 20/11/16.
 */
class StubProcessTransaction<C: ActivityInstanceContext>(private val engineData: IProcessEngineData<StubProcessTransaction<C>, C>) : StubTransaction(),
                                                                                                   ContextProcessTransaction<C> {
  override val readableEngineData: ProcessEngineDataAccess<C>
    get() = engineData.createReadDelegate(this)
  override val writableEngineData: MutableProcessEngineDataAccess<C>
    get() = engineData.createWriteDelegate(this)

  inner class InstanceWrapper(val instanceHandle: HProcessInstance) {

    operator fun invoke(): ProcessInstance<C> { return readableEngineData.instance(instanceHandle).mustExist(instanceHandle).withPermission() }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) : ProcessInstance<C> {
      return this()
    }

  }

    override fun addRollbackHandler(runnable: Runnable) {
        super<StubTransaction>.addRollbackHandler(runnable)
    }

    fun ProcessEngine<StubProcessTransaction<C>, *>.testProcess(model: ExecutableProcessModel, owner: Principal, payload: CompactFragment? = null): InstanceWrapper {
    val modelHandle = addProcessModel(this@StubProcessTransaction, model, owner).handle
    val instanceHandle = startProcess(this@StubProcessTransaction, owner, modelHandle, "TestInstance", UUID.randomUUID(), payload)
    return InstanceWrapper(instanceHandle)
  }

}
