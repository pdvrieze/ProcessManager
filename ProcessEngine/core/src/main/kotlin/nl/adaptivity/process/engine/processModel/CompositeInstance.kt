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
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import java.security.Principal
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Created by pdvrieze on 09/01/17.
 */
class CompositeInstance : ProcessNodeInstance<CompositeInstance> {

  interface Builder: ProcessNodeInstance.Builder<ExecutableActivity, CompositeInstance> {
    var hChildInstance: ComparableHandle<SecureObject<ProcessInstance>>
  }

  class BaseBuilder : ProcessNodeInstance.BaseBuilder<ExecutableActivity, CompositeInstance>, Builder {
    constructor(node: ExecutableActivity,
                predecessor: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>?,
                processInstanceBuilder: ProcessInstance.Builder,
                childInstance: ComparableHandle<SecureObject<ProcessInstance>>,
                owner: Principal,
                entryNo: Int,
                handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
                state: NodeInstanceState = NodeInstanceState.Pending) : super(node, listOfNotNull(predecessor), processInstanceBuilder, owner,
                                                                                   entryNo, handle, state)

    override var hChildInstance: ComparableHandle<SecureObject<ProcessInstance>> = Handles.getInvalid()

    override fun build(): CompositeInstance {
      return CompositeInstance(this)
    }
  }

  class ExtBuilder(base: CompositeInstance, processInstanceBuilder: ProcessInstance.Builder) : ProcessNodeInstance.ExtBuilder<ExecutableActivity, CompositeInstance>(base, processInstanceBuilder), Builder {

    override var node: ExecutableActivity by overlay { base.node }

    override var hChildInstance: ComparableHandle<SecureObject<ProcessInstance>> by overlay(observer) { base.hChildInstance }

    override fun build(): CompositeInstance {
      return if(changed) CompositeInstance(this) else base
    }
  }

  val hChildInstance: ComparableHandle<SecureObject<ProcessInstance>>

  override val node: ExecutableActivity get() = super.node as ExecutableActivity

  constructor(node: ExecutableActivity,
              predecessor: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
              processInstance: ProcessInstance,
              entryNo: Int,
              childInstance: ComparableHandle<SecureObject<ProcessInstance>> = Handles.getInvalid()) : super(node, listOf(predecessor),
                                                                                      processInstance.getHandle(), processInstance.owner, entryNo) {
    this.hChildInstance = childInstance
  }

  constructor(builder: Builder) : super(builder) {
    hChildInstance = builder.hChildInstance
  }

  override fun builder(processInstanceBuilder: ProcessInstance.Builder) = ExtBuilder(this, processInstanceBuilder)

  fun updateComposite(writableEngineData: MutableProcessEngineDataAccess,
                      instance: ProcessInstance,
                      body: ExtBuilder.() -> Unit): ProcessInstance.PNIPair<CompositeInstance> {
    return super.update(writableEngineData, { (this as ExtBuilder).body() })
  }

  override fun provideTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): ProcessInstance.PNIPair<CompositeInstance> {
    val shouldProgress = tryCreate(engineData, processInstance) {
      node.provideTask(engineData, processInstance, this)
    }
    val pniPair = tryCreate(engineData, processInstance) {
      val childHandle=engineData.instances.put(ProcessInstance(engineData, node.childModel!!, getHandle()) {})
      updateComposite(engineData, processInstance) {
        state = NodeInstanceState.Sent
        hChildInstance = childHandle
      }
    }
    return when {
      shouldProgress -> pniPair.startTask(engineData)
      else           -> pniPair
    }
  }

  override fun startTask(engineData: MutableProcessEngineDataAccess,
                         processInstance: ProcessInstance): ProcessInstance.PNIPair<CompositeInstance> {
    val shouldProgress = tryTask(engineData, processInstance) {
      node.startTask(this)
    }
    val pniPair = tryTask(engineData, processInstance) {
      engineData.instance(hChildInstance)
        .withPermission()
        .start(engineData, getPayload(engineData))
      update(engineData) {
        state = NodeInstanceState.Started
      }
    }
    return when {
      shouldProgress -> pniPair.finishTask(engineData, null)
      else -> pniPair
    }
  }

  override fun finishTask(engineData: MutableProcessEngineDataAccess,
                          processInstance: ProcessInstance,
                          resultPayload: Node?): ProcessInstance.PNIPair<CompositeInstance> {
    val childInstance = engineData.instance(hChildInstance).withPermission()
    if (childInstance.state!=ProcessInstance.State.FINISHED) {
      throw ProcessException("A Composite task cannot be finished until its child process is. The child state is: ${childInstance.state}")
    }
    return super.finishTask(engineData, processInstance, childInstance.getOutputPayload())
  }

  fun getPayload(engineData: ProcessEngineDataAccess):DocumentFragment? {
    val defines = getDefines(engineData)
    if (defines.isEmpty()) return null

    val doc = DocumentBuilderFactory
      .newInstance()
      .apply { isNamespaceAware=true }
      .newDocumentBuilder()
      .newDocument()

    val frag = doc.createDocumentFragment()

    for (data in defines) {
      val owner = doc.createElement(data.name)
      owner.appendChild(doc.adoptNode(data.contentFragment))
    }

    return frag
  }
}