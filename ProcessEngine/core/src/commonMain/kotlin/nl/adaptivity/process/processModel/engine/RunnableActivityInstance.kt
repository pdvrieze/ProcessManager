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

package nl.adaptivity.process.processModel.engine

import net.devrieze.util.ComparableHandle
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.getInvalidHandle
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.dom.DocumentFragment
import nl.adaptivity.process.engine.impl.dom.Node
import nl.adaptivity.process.engine.impl.dom.isNamespaceAware
import nl.adaptivity.process.engine.impl.dom.newDocumentBuilderFactory
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.util.security.Principal

class RunnableActivityInstance<I,O>(builder: Builder<I,O>):
    ProcessNodeInstance<RunnableActivityInstance<I, O>>(builder) {


    interface Builder<I,O>: ProcessNodeInstance.Builder<RunnableActivity<I,O>, RunnableActivityInstance<I,O>> {
        override fun doProvideTask(engineData: MutableProcessEngineDataAccess):Boolean {
            return node.provideTask(engineData, this)
/*
            TODO("IMplement")
            val shouldProgress = node.provideTask(engineData, this)

//            val childHandle=engineData.instances.put(ProcessInstance(engineData, node.childModel, handle) {})

            store(engineData)
            engineData.commit()
            return shouldProgress
*/
        }


        override fun doStartTask(engineData: MutableProcessEngineDataAccess):Boolean {
            val shouldProgress = tryTask { node.startTask(this) }



            tryTask {
                val input: I = build().getInputData(engineData)


//                engineData.instance(hChildInstance)
//                    .withPermission()
//                    .start(engineData, build().getPayload(engineData))
            }

            return shouldProgress
        }

        override fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node?) {
            TODO()
/*
            val childInstance = engineData.instance(hChildInstance).withPermission()
            if (childInstance.state!= ProcessInstance.State.FINISHED) {
                throw ProcessException("A Composite task cannot be finished until its child process is. The child state is: ${childInstance.state}")
            }
            return super.doFinishTask(engineData, childInstance.getOutputPayload())
*/
        }

        override fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean {
            return true
        }
    }

    class BaseBuilder<I,O>(node: RunnableActivity<I,O>,
                      predecessor: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>?,
                      processInstanceBuilder: ProcessInstance.Builder,
                      owner: Principal,
                      entryNo: Int,
                      handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = getInvalidHandle(),
                      state: NodeInstanceState = NodeInstanceState.Pending) : ProcessNodeInstance.BaseBuilder<RunnableActivity<I,O>, RunnableActivityInstance<I,O>>(
        node, listOfNotNull(predecessor), processInstanceBuilder, owner,
        entryNo, handle, state), Builder<I,O> {

        override fun invalidateBuilder(engineData: ProcessEngineDataAccess) {
            engineData.nodeInstances[handle]?.withPermission()?.let { n ->
                val newBase = n as RunnableActivityInstance<I,O>
                node = newBase.node
                predecessors.replaceBy(newBase.predecessors)
                owner = newBase.owner
                state = newBase.state
            }
        }

        override fun build(): RunnableActivityInstance<I,O> {
            return RunnableActivityInstance(this)
        }
    }

    class ExtBuilder<I,O>(base: RunnableActivityInstance<I,O>, processInstanceBuilder: ProcessInstance.Builder) : ProcessNodeInstance.ExtBuilder<RunnableActivity<I,O>, RunnableActivityInstance<I,O>>(base, processInstanceBuilder), Builder<I,O> {

        override var node: RunnableActivity<I,O> by overlay { base.node }

        override fun build(): RunnableActivityInstance<I,O> {
            return if(changed) RunnableActivityInstance(this) else base
        }
    }

    @Suppress("UNCHECKED_CAST")
    override val node: RunnableActivity<I,O> get() = super.node as RunnableActivity<I,O>

    override fun builder(processInstanceBuilder: ProcessInstance.Builder) = ExtBuilder(this, processInstanceBuilder)

    fun getInputData(engineData: ProcessEngineDataAccess): I {
        val defines = getDefines(engineData)

        return node.getInputData(defines)
    }


}
