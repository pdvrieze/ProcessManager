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

import nl.adaptivity.process.processModel.configurableModel.ConfigurableNodeContainer
import nl.adaptivity.process.util.Identified
import kotlinx.serialization.serializer

@kotlinx.serialization.ImplicitReflectionSerializer
inline fun <reified I: Any, reified O:Any> ConfigurableNodeContainer<ExecutableProcessNode>.runnableActivity(predecessor: Identified, noinline action: (I) -> O): RunnableActivity.Builder<I,O> {
    return runnableActivity(predecessor, predecessor.identifier, "result", I::class.serializer(), O::class.serializer(), action)
}
