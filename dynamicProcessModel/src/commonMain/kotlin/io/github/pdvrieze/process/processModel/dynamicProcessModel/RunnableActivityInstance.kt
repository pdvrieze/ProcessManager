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

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.tryCreateTask
import nl.adaptivity.process.engine.processModel.tryRunTask
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment


class RunnableActivityInstance<I : Any, O : Any, C : ActivityInstanceContext>(builder: Builder<I, O, C>) :
    AbstractRunnableActivityInstance<I, O, C, RunnableActivity<I, O, *>, RunnableActivityInstance<I, O, C>>(builder) {


    override fun builder(processInstanceBuilder: ProcessInstance.Builder): RunnableActivityInstance.ExtBuilder<I, O, C> =
        ExtBuilder(this, processInstanceBuilder)


    interface Builder<InputT : Any, OutputT : Any, C : ActivityInstanceContext> :
        AbstractRunnableActivityInstance.Builder<InputT, OutputT, C, RunnableActivity<InputT, OutputT, *>, RunnableActivityInstance<InputT, OutputT, C>> {

        override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
            fun <C: ActivityInstanceContext> doRun(contextFactory: ProcessContextFactory<C>, builtNodeInstance: RunnableActivityInstance<InputT, OutputT, *>) : CompactFragment? {
                val icontext: C = contextFactory.newActivityInstanceContext(engineData, this)

                val input: InputT = with(builtNodeInstance) { icontext.getInputData(processInstanceBuilder) }

                val action: RunnableAction<InputT, OutputT, C> =
                    node.action as RunnableAction<InputT, OutputT, C>

                val result: OutputT = icontext.action(input)

                return node.outputSerializer?.let { os ->
                    CompactFragment { writer ->
                        XML.defaultInstance.encodeToWriter(writer, os, result)
                    }
                }

            }

            val shouldProgress = tryCreateTask { node.canStartTaskAutoProgress(this) }

            if (shouldProgress) {

                val resultFragment = tryRunTask {
                    doRun(engineData.processContextFactory, build())
                }

                finishTask(engineData, resultFragment)
            }
            return false // we call finish ourselves, so don't call it afterwards.
        }

        }

    class BaseBuilder<I : Any, O : Any, C : ActivityInstanceContext>(
        node: RunnableActivity<I, O, *>,
        predecessor: PNIHandle?,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        assignedUser: PrincipalCompat? = null,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : AbstractRunnableActivityInstance.BaseBuilder<I, O, C, RunnableActivity<I, O, *>, RunnableActivityInstance<I, O, C>>(
        node, predecessor, processInstanceBuilder, owner,
        entryNo, assignedUser, handle, state
    ), Builder<I, O, C> {

        override fun build(): RunnableActivityInstance<I, O, C> {
            return RunnableActivityInstance(this)
        }
    }

    class ExtBuilder<I : Any, O : Any, C : ActivityInstanceContext>(
        base: RunnableActivityInstance<I, O, C>,
        processInstanceBuilder: ProcessInstance.Builder
    ) : AbstractRunnableActivityInstance.ExtBuilder<I, O, C, RunnableActivity<I, O, *>, RunnableActivityInstance<I, O, C>>(
        base,
        processInstanceBuilder
    ), Builder<I, O, C> {

        override fun build(): RunnableActivityInstance<I, O, C> {
            return if (changed) RunnableActivityInstance(this).also { invalidateBuilder(it) } else base
        }
    }

}

