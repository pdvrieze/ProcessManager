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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handles
import net.devrieze.util.MutableHandleMap
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import java.security.Principal

/**
 * Created by pdvrieze on 09/01/17.
 */
class CompositeInstance : ProcessNodeInstance {

  class BaseBuilder : ProcessNodeInstance.BaseBuilder<ExecutableActivity> {
    constructor(node: ExecutableActivity,
                predecessors: Iterable<ComparableHandle<SecureObject<ProcessNodeInstance>>>,
                hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>,
                childInstance: ComparableHandle<SecureObject<ProcessInstance>>,
                owner: Principal,
                handle: ComparableHandle<SecureObject<ProcessNodeInstance>>,
                state: IProcessNodeInstance.NodeInstanceState) : super(node, predecessors, hProcessInstance, owner,
                                                                                                                 handle, state)
  }

  class ExtBuilder(base: CompositeInstance) : ExtBuilderBase<ExecutableProcessNode>(base) {

    override var node: ExecutableProcessNode by overlay { base.node }

    var childInstance: ComparableHandle<SecureObject<ProcessInstance>> = base.childInstance

    override fun build(): CompositeInstance {
      return CompositeInstance(this)
    }
  }

  val childInstance: ComparableHandle<SecureObject<ProcessInstance>>

  override val node: ExecutableActivity get() = super.node as ExecutableActivity

  constructor(node: ExecutableActivity,
              predecessor: ComparableHandle<SecureObject<ProcessNodeInstance>>,
              processInstance: ProcessInstance,
              childInstance: ComparableHandle<SecureObject<ProcessInstance>> = Handles.getInvalid()) : super(node, predecessor,
                                                                                      processInstance) {
    this.childInstance = childInstance
  }

  constructor(builder: ExtBuilder) : super(builder) {
    childInstance = builder.childInstance
  }

  override fun builder() = ExtBuilder(this)

  fun updateComposite(writableEngineData: MutableProcessEngineDataAccess,
                      instance: ProcessInstance,
                      body: ExtBuilder.() -> Unit): ProcessInstance.PNIPair<ProcessNodeInstance> {
    return super.update(writableEngineData, instance, { (this as ExtBuilder).body() })
  }

  fun withChildInstance(engineData: MutableProcessEngineDataAccess, childHandle: ComparableHandle<SecureObject<ProcessInstance>>): CompositeInstance {
    // In this case we know that the child handle is not actually stored in the node instance as the reference is the other way around
    // As such this method hacks to not update the process instance
    return builder().apply { this.childInstance = childHandle }.build().apply { (engineData.nodeInstances as MutableHandleMap)[getHandle()]=this }
  }


}