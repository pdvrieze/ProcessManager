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

import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.configurableModel.ConfigurableNodeContainer
import nl.adaptivity.process.util.Identified

inline fun <reified I : Any, reified O : Any, C : ActivityInstanceContext> ConfigurableNodeContainer<ExecutableProcessNode>.runnableActivity(
    predecessor: Identified,
    noinline action: RunnableAction2<I, O, C>
): RunnableActivity.Builder<I, O, C> {
    return runnableActivity(
        predecessor,
        serializer<O>(),
        serializer<I>(),
        predecessor.identifier,
        "",
        action
    )
}

inline fun <reified I : Any> RunnableActivity.Builder<I, *, *>.defineInput(refNode: Identified): InputCombiner.InputValue<I> {
    return defineInput(refNode, serializer<I>())
}

