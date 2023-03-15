/*
 * Copyright (c) 2018.
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

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.IProcessInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.process.processModel.engine.ConditionResult.*
import nl.adaptivity.util.multiplatform.name


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
abstract class ExecutableCondition : Condition, ((IProcessInstance<*>, IProcessNodeInstance<*>) -> ConditionResult) {
    open val isOtherwise: Boolean get()= false

    /**
     * Evaluate the condition.
     *
     * @param nodeInstanceSource Source for nodes in the same process instance
     * @param nodeInstance The instance to use to evaluate against.
     * @return `true` if the condition holds, `false` if not
     */
    abstract fun eval(nodeInstanceSource: IProcessInstance<*>, nodeInstance: IProcessNodeInstance<*>): ConditionResult

    final override operator fun invoke(nodeInstanceSource: IProcessInstance<*>, instance: IProcessNodeInstance<*>): ConditionResult =
        eval(nodeInstanceSource, instance)

    override val condition: String get() = "class:${this::class.name}"

    object TRUE: ExecutableCondition() {
        override fun eval(nodeInstanceSource: IProcessInstance<*>, nodeInstance: IProcessNodeInstance<*>): ConditionResult = ConditionResult.TRUE

        override val condition: String get() = "true()"
        override val label: String? get() = null
    }

    object FALSE: ExecutableCondition() {
        override fun eval(nodeInstanceSource: IProcessInstance<*>, nodeInstance: IProcessNodeInstance<*>): ConditionResult = NEVER

        override val condition: String get() = "false()"
        override val label: String? get() = null
    }

    object OTHERWISE: ExecutableCondition() {
        override val isOtherwise: Boolean get() = true

        override fun eval(nodeInstanceSource: IProcessInstance<*>, nodeInstance: IProcessNodeInstance<*>): ConditionResult = MAYBE

        override val condition: String get() = "otherwise"
        override val label: String? get() = null
    }
}

@Suppress("UNCHECKED_CAST")
fun Condition.toExecutableCondition(): ExecutableCondition = when (this) {
    is ExecutableCondition -> this
    else                   -> ExecutableXSLTCondition(this)
}

enum class ConditionResult {
    /** The result is true now */
    TRUE,
    /** The result may be true in the future but is not now */
    MAYBE,
    /** The result is not going to be true. No timers. */
    NEVER
}

fun ConditionResult(boolean: Boolean): ConditionResult {
    return if (boolean) TRUE else NEVER
}

/**
 * Determine whether the node start condition is satisfied. This is a bit more complex as it also deals
 * with joins etc.
 *
 * @param nodeInstanceSource The process instance containing the node instance with the condition
 *
 * @param predecessor The predecessor that is evaluating the condition
 *
 * @param nodeInstanceBuilder The node instance against which the condition should be evaluated.
 *
 * @return `true` if the node can be started, `false` if not.
 */
fun <C: ActivityInstanceContext> ExecutableCondition?.evalNodeStartCondition(
    nodeInstanceSource: IProcessInstance<C>,
    predecessor: IProcessNodeInstance<C>,
    nodeInstance: IProcessNodeInstance<C>
): ConditionResult {
    // If the instance is final, the condition maps to the state
    if (nodeInstance.state.isFinal) {
        return if(nodeInstance.state == NodeInstanceState.Complete) TRUE else NEVER
    }
    // A lack of condition is a true result
    if (this==null) return TRUE

    if (isOtherwise) { // An alternate is only true if all others are never/finalised
        val successorCount = predecessor.node.successors.size
        val hPred = predecessor.handle
        var nonTakenSuccessorCount:Int = 0
        for (sibling in nodeInstanceSource.allChildNodeInstances()) {
            if (sibling.handle != nodeInstance.handle && hPred in sibling.predecessors) {
                when (sibling.condition(nodeInstanceSource, predecessor)) {
                    TRUE -> return NEVER
                    MAYBE -> return MAYBE
                    NEVER -> nonTakenSuccessorCount++
                }
            }
        }
        if (nonTakenSuccessorCount+1>=successorCount) return TRUE
        return MAYBE
    }

    return eval(nodeInstanceSource, nodeInstance)
}
