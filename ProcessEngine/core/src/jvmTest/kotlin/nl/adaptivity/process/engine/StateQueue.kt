/*
 * Copyright (c) 2021.
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

package nl.adaptivity.process.engine

/** A Queue of operations to perform */
class StateQueue {
    /** The specific operationsin the queue. */
    private val operations = mutableListOf<() -> Unit>()

    /** The state of the operation execution. A `true` value means it has been executed, `false` not.*/
    private val operationState = mutableListOf<Boolean>()

    /** Add an operation to the queue */
    fun add(operation: () -> Unit) {
        operations.add(operation)
        operationState.add(false)
    }

    /**
     * Create a [SolidQueue] that can be executed to the current state. It remembers the current list so if the
     * queue grows in the future the subqueue is still valid.
     */
    fun solidify() = SolidQueue(operations.size - 1)


    inner class SolidQueue(val position: Int) {
        operator fun invoke() = (0 until position).map { idx ->
            if (!operationState[idx]) {
                operations[idx]()
                operationState[idx] = true
            }
        }
    }
}
