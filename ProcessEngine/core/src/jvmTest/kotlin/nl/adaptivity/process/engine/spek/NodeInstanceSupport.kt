/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.engine.spek

import net.devrieze.util.ComparableHandle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.ProcessTransaction
import nl.adaptivity.process.engine.kfail
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.updateChild
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xmlutil.util.CompactFragment
import org.w3c.dom.Node
import kotlin.reflect.KProperty

class ProcessNodeInstanceDelegate(val instanceSupport: InstanceSupport, val instanceHandle: ComparableHandle<SecureObject<ProcessInstance>>, val nodeId: Identified) {
  operator fun getValue(thisRef: Any?, property: KProperty<*>): ProcessNodeInstance<*> {
    val idString = nodeId.id
    val instance = instanceSupport.transaction.readableEngineData.instance(instanceHandle).withPermission()
    return with(instanceSupport) {
      instance.allChildren().firstOrNull { it.node.id == idString }
      ?: kfail("The process node instance for node id $nodeId could not be found. Instance is: ${instance.toDebugString()}")
    }
  }
}

interface SafeNodeActions {
  val transaction: ProcessTransaction

  fun ProcessNodeInstance<*>.take(): ProcessNodeInstance<*> {
    val ib = transaction.writableEngineData.instance(hProcessInstance).withPermission().builder()
    this.update(ib) { state= NodeInstanceState.Taken }
    return transaction.readableEngineData.nodeInstance(handle()).withPermission()
  }

  fun ProcessNodeInstance<*>.start(): ProcessNodeInstance<*> {
    val instance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    instance.update(transaction.writableEngineData) {
      updateChild(this@start) {
        startTask(transaction.writableEngineData)
      }
    }
    return transaction.readableEngineData.nodeInstance(getHandle()).withPermission()
  }

}

interface ProcessNodeActions: SafeNodeActions {

  fun ProcessNodeInstance<*>.finish(payload: CompactFragment? = null): ProcessNodeInstance<*> {
    val instance = transaction.readableEngineData.instance(hProcessInstance).withPermission()
    instance.update(transaction.writableEngineData) {
      updateChild(this@finish) {
        finishTask(transaction.writableEngineData, payload)
      }
    }
    return transaction.readableEngineData.nodeInstance(handle()).withPermission()
  }

}
