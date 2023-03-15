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

package io.github.pdvrieze.process.processModel.dynamicProcessModel

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity.OnActivityProvided
import net.devrieze.util.Handle
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML


class RunnableActivityInstance<I : Any, O : Any, C: ActivityInstanceContext>(builder: Builder<I, O, C>) :
    ProcessNodeInstance<RunnableActivityInstance<I, O, C>, C>(builder) {

    interface Builder<I : Any, O : Any, C: ActivityInstanceContext> :
        ProcessNodeInstance.Builder<RunnableActivity<I, O, C>, RunnableActivityInstance<I, O, C>, C> {

        override var assignedUser: PrincipalCompat?

        override fun doProvideTask(engineData: MutableProcessEngineDataAccess<C>): Boolean {
            node.provideTask(engineData, this)
            return node.onActivityProvided(engineData, this)
        }


        override fun doStartTask(engineData: MutableProcessEngineDataAccess<C>): Boolean {
            val shouldProgress = tryCreateTask { node.startTask(this) }

            if (shouldProgress) {
                val n: RunnableActivity<I, O, C> = node

                val resultFragment = tryRunTask {
                    val build = build()
                    val icontext = engineData.processContextFactory.newActivityInstanceContext(engineData, this)
                    val input: I = with(build) { icontext.getInputData(processInstanceBuilder) }
                    val action: RunnableAction<I, O, C> = n.action
                    val context = engineData.processContextFactory.newActivityInstanceContext(engineData, this)
                    val result: O = context.action(input)

//                engineData.instance(hChildInstance)
//                    .withPermission()
//                    .start(engineData, build().getPayload(engineData))
                    n.outputSerializer?.let { os ->
                        CompactFragment { writer ->
                            XML.defaultInstance.encodeToWriter(writer, os, result)
                        }
                    }
                }

                finishTask(engineData, resultFragment)
            }
            return false // we call finish ourselves, so don't call it afterwards.
        }

        override fun canTakeTaskAutomatically(): Boolean = node.onActivityProvided == OnActivityProvided.DEFAULT

        override fun doTakeTask(
            engineData: MutableProcessEngineDataAccess<C>,
            assignedUser: PrincipalCompat?
        ): Boolean {
            return node.takeTask(createActivityContext(engineData), this, assignedUser)
        }
    }

    class BaseBuilder<I : Any, O : Any, C: ActivityInstanceContext>(
        node: RunnableActivity<I, O, C>,
        predecessor: Handle<SecureObject<ProcessNodeInstance<*, C>>>?,
        processInstanceBuilder: ProcessInstance.Builder<C>,
        owner: PrincipalCompat,
        entryNo: Int,
        override var assignedUser: PrincipalCompat? = null,
        handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<RunnableActivity<I, O, C>, RunnableActivityInstance<I, O, C>, C>(
        node, listOfNotNull(predecessor), processInstanceBuilder, owner,
        entryNo, handle, state
    ), Builder<I, O, C> {

        override fun invalidateBuilder(engineData: ProcessEngineDataAccess<C>) {
            val h: Handle<SecureObject<ProcessNodeInstance<*, C>>> = handle
            engineData.nodeInstances[h]?.withPermission()?.let { n ->
                val newBase = n as RunnableActivityInstance<I, O, C>
                node = newBase.node
                predecessors.replaceBy(newBase.predecessors)
                owner = newBase.owner
                state = newBase.state
            }
        }

        override fun build(): RunnableActivityInstance<I, O, C> {
            return RunnableActivityInstance(this)
        }
    }

    class ExtBuilder<I : Any, O : Any, C: ActivityInstanceContext>(
        base: RunnableActivityInstance<I, O, C>,
        processInstanceBuilder: ProcessInstance.Builder<C>
    ) : ProcessNodeInstance.ExtBuilder<RunnableActivity<I, O, C>, RunnableActivityInstance<I, O, C>, C>(
        base,
        processInstanceBuilder
    ), Builder<I, O, C> {

        override var node: RunnableActivity<I, O, C> by overlay { base.node }

        override var assignedUser: PrincipalCompat? by overlay { base.assignedUser }

        override fun build(): RunnableActivityInstance<I, O, C> {
            return if (changed) RunnableActivityInstance(this).also { invalidateBuilder(it) } else base
        }
    }

    @Suppress("UNCHECKED_CAST")
    override val node: RunnableActivity<I, O, C> get() = super.node as RunnableActivity<I, O, C>


    override fun builder(processInstanceBuilder: ProcessInstance.Builder<C>) = ExtBuilder(this, processInstanceBuilder)

    fun <C: ActivityInstanceContext> C.getInputData(nodeInstanceSource: IProcessInstance<C>): I {
        val defines = getDefines(nodeInstanceSource)
        return this@RunnableActivityInstance.node.getInputData(defines)
    }

}
