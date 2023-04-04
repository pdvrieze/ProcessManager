/*
 * Copyright (c) 2023.
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

import io.github.pdvrieze.process.processModel.dynamicProcessModel.AbstractRunnableActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputCombiner
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableAccessRestriction
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity.OnActivityProvided
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.pma.dynamic.model.PmaAction
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityInstance
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate
import nl.adaptivity.process.engine.pma.models.IPMAMessageActivity
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.updateChild
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identified

class RunnablePmaActivity<I : Any, O : Any, C : AbstractDynamicPmaActivityContext<C, *>>(
    builder: Builder<I, O, C>,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : AbstractRunnableActivity<I, O, C>(builder, newOwner, otherNodes), IPMAMessageActivity<C> {

    internal val action: PmaAction<I, O, C> = builder.action
    override val authorizationTemplates: List<AuthScopeTemplate<C>> = builder.authorizationTemplates.toList()

    override fun builder(): Builder<I, O, C> {
        return Builder(this)
    }

    override fun createOrReuseInstance(
        data: MutableProcessEngineDataAccess,
        processInstanceBuilder: ProcessInstance.Builder,
        predecessor: IProcessNodeInstance,
        entryNo: Int,
        allowFinalInstance: Boolean
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, ProcessNodeInstance<*>> {
        processInstanceBuilder.getChildNodeInstance(this, entryNo)?.let { return it }
        if (!isMultiInstance && entryNo > 1) {
            processInstanceBuilder.allChildNodeInstances { it.node == this && it.entryNo != entryNo }.forEach {
                processInstanceBuilder.updateChild(it) {
                    invalidateTask(data)
                }
            }
        }

        return DynamicPmaActivityInstance.BaseBuilder<I, O, C>(
            this, predecessor.handle,
            processInstanceBuilder,
            processInstanceBuilder.owner,
            entryNo
        )
    }

    class Builder<I : Any, O : Any, C : AbstractDynamicPmaActivityContext<C, *>> :
        AbstractRunnableActivity.Builder<I, O, C>, IPMAMessageActivity.Builder<C> {

        var action: PmaAction<I, O, C>
        override var authorizationTemplates: List<AuthScopeTemplate<C>>

        constructor(activity: RunnablePmaActivity<I, O, C>) : super(activity) {
            action = activity.action
            authorizationTemplates = activity.authorizationTemplates
        }

        constructor(
            predecessor: Identified,
            refNode: Identified?,
            refName: String,
            inputSerializer: DeserializationStrategy<I>,
            outputSerializer: SerializationStrategy<O>? = null,
            accessRestrictions: RunnableAccessRestriction? = null,
            message: IXmlMessage? = null,
            onActivityProvided: OnActivityProvided<I, O, C> = OnActivityProvided.DEFAULT,
            authorizationTemplates: List<AuthScopeTemplate<C>> = emptyList(),
            action: PmaAction<I, O, C>
        ) : super(
            predecessor = predecessor,
            refNode = refNode,
            refName = refName,
            inputSerializer = inputSerializer,
            outputSerializer = outputSerializer,
            accessRestrictions = accessRestrictions,
            message = message,
            onActivityProvided = onActivityProvided,
        ) {
            this.action = action
            this.authorizationTemplates = authorizationTemplates
        }

        constructor(
            predecessor: Identified,
            inputCombiner: InputCombiner<I>,
            outputSerializer: SerializationStrategy<O>? = null,
            accessRestrictions: RunnableAccessRestriction? = null,
            message: IXmlMessage? = null,
            onActivityProvided: OnActivityProvided<I, O, C> = OnActivityProvided.DEFAULT,
            authorizationTemplates: List<AuthScopeTemplate<C>> = emptyList(),
            action: PmaAction<I, O, C>
        ) : super(
            predecessor = predecessor,
            inputCombiner = inputCombiner,
            outputSerializer = outputSerializer,
            accessRestrictions = accessRestrictions,
            message = message,
            onActivityProvided = onActivityProvided,
        ) {
            this.action = action
            this.authorizationTemplates = authorizationTemplates
        }

        override fun build(
            buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): RunnablePmaActivity<I, O, C> {
            return RunnablePmaActivity(this, buildHelper.newOwner, otherNodes)
        }
    }

}

