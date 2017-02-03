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
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import java.security.Principal
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Class representing a node instance that wraps a composite activity.
 */
class CompositeInstance : ProcessNodeInstance<CompositeInstance> {

  interface Builder: ProcessNodeInstance.Builder<ExecutableActivity, CompositeInstance> {
    var hChildInstance: ComparableHandle<SecureObject<ProcessInstance>>

    override fun doProvideTask(engineData: MutableProcessEngineDataAccess):Boolean {
      val shouldProgress = node.provideTask(engineData, this)

      val childHandle=engineData.instances.put(ProcessInstance(engineData, node.childModel!!, handle) {})
      hChildInstance = childHandle

      store(engineData)
      engineData.commit()
      return shouldProgress
    }


    override fun doStartTask(engineData: MutableProcessEngineDataAccess):Boolean {
      val shouldProgress = tryTask { node.startTask(this) }

      tryTask {
        engineData.instance(hChildInstance)
          .withPermission()
          .start(engineData, build().getPayload(engineData))
      }

      return shouldProgress
    }

    override fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node?) {
      val childInstance = engineData.instance(hChildInstance).withPermission()
      if (childInstance.state!=ProcessInstance.State.FINISHED) {
        throw ProcessException("A Composite task cannot be finished until its child process is. The child state is: ${childInstance.state}")
      }
      return super.doFinishTask(engineData, childInstance.getOutputPayload())
    }

    override fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean {
      return true
    }
  }

  class BaseBuilder(node: ExecutableActivity,
                    predecessor: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>?,
                    processInstanceBuilder: ProcessInstance.Builder,
                    childInstance: ComparableHandle<SecureObject<ProcessInstance>>,
                    owner: Principal,
                    entryNo: Int,
                    handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
                    state: NodeInstanceState = NodeInstanceState.Pending) : ProcessNodeInstance.BaseBuilder<ExecutableActivity, CompositeInstance>(
    node, listOfNotNull(predecessor), processInstanceBuilder, owner,
    entryNo, handle, state), Builder {

    override var hChildInstance: ComparableHandle<SecureObject<ProcessInstance>> = childInstance

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

  @Deprecated("Use builder")
  fun updateComposite(writableEngineData: MutableProcessEngineDataAccess,
                      body: ExtBuilder.() -> Unit): ProcessInstance.PNIPair<CompositeInstance> {
    return super.update(writableEngineData, { (this as ExtBuilder).body() })
  }

  @Deprecated("Use builder")
  override fun startTask(engineData: MutableProcessEngineDataAccess,
                         processInstance: ProcessInstance): ProcessInstance.PNIPair<CompositeInstance> {
    return processInstance.updateWithNode(engineData) {
      builder(this).apply { startTask(engineData) }
    }
  }

  @Deprecated("Use builder")
  override fun finishTask(engineData: MutableProcessEngineDataAccess,
                          processInstance: ProcessInstance,
                          resultPayload: Node?): ProcessInstance.PNIPair<CompositeInstance> {
    @Suppress("DEPRECATION")
    return updateComposite(engineData) {
      finishTask(engineData, resultPayload)
    }
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