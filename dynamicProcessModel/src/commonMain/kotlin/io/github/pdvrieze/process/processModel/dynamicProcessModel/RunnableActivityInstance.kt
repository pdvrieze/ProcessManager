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
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.util.multiplatform.PrincipalCompat


class RunnableActivityInstance<I : Any, O : Any, C : ActivityInstanceContext>(builder: Builder<I, O, C>) :
    AbstractRunnableActivityInstance<I, O, C, RunnableActivity<I, O, *>, RunnableActivityInstance<I, O, C>>(builder) {


    override fun builder(processInstanceBuilder: ProcessInstance.Builder<*>): RunnableActivityInstance.ExtBuilder<I, O, C> =
        ExtBuilder(this, processInstanceBuilder)


    interface Builder<I : Any, O : Any, C : ActivityInstanceContext> :
        AbstractRunnableActivityInstance.Builder<I, O, C, RunnableActivity<I, O, *>, RunnableActivityInstance<I, O, C>>

    class BaseBuilder<I : Any, O : Any, C : ActivityInstanceContext>(
        node: RunnableActivity<I, O, *>,
        predecessor: PNIHandle?,
        processInstanceBuilder: ProcessInstance.Builder<*>,
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
        processInstanceBuilder: ProcessInstance.Builder<*>
    ) : AbstractRunnableActivityInstance.ExtBuilder<I, O, C, RunnableActivity<I, O, *>, RunnableActivityInstance<I, O, C>>(
        base,
        processInstanceBuilder
    ), Builder<I, O, C> {

        override fun build(): RunnableActivityInstance<I, O, C> {
            return if (changed) RunnableActivityInstance(this).also { invalidateBuilder(it) } else base
        }
    }

}

