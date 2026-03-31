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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.updateChild
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.configurableModel.ConfigurableNodeContainer
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.IterableNamespaceContext
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.util.CompactFragment

typealias RunnableAction<I, O, C> = C.(I) -> O
typealias NoInputRunnableAction<O, C> = C.() -> O

open class RunnableActivity<I : Any, O : Any, C : ActivityInstanceContext>(
    builder: Builder<I, O, C>,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : AbstractRunnableActivity<I, O, C>(builder, newOwner, otherNodes) {

    internal val action: RunnableAction<I, O, C> = builder.action

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
        return RunnableActivityInstance.BaseBuilder<I, O, ActivityInstanceContext>(
            this as RunnableActivity<I, O, *>, predecessor.handle,
            processInstanceBuilder,
            processInstanceBuilder.owner, entryNo
        )
    }

    fun interface OnActivityProvided<out I: Any, in O: Any, in C: ActivityInstanceContext>:
            (MutableProcessEngineDataAccess, AbstractRunnableActivityInstance.Builder<@UnsafeVariance I, @UnsafeVariance O, *, *, *>) -> Boolean {
        companion object {

            val DEFAULT = OnActivityProvided<Nothing, Any, ActivityInstanceContext> { engineData, instance ->
                true
            }
        }
    }

    open class Builder<I : Any, O : Any, C: ActivityInstanceContext> : AbstractRunnableActivity.Builder<I, O, C> {

        var action: RunnableAction<I, O, C>

        constructor(
            predecessor: Identified,
            refNode: Identified?,
            refName: String,
            inputSerializer: DeserializationStrategy<I>,
            outputSerializer: SerializationStrategy<O>? = null,
            accessRestrictions: RunnableAccessRestriction? = null,
            message: IXmlMessage? = null,
            onActivityProvided: OnActivityProvided<I, O, C> = OnActivityProvided.DEFAULT,
            action: RunnableAction<I, O, C> = { throw UnsupportedOperationException("Action not provided") }
        ) : super(predecessor, refNode, refName, inputSerializer, outputSerializer, accessRestrictions, message, onActivityProvided) {
            this.action = action
        }

        constructor(
            predecessor: Identified,
            inputCombiner: InputCombiner<I> = InputCombiner(),
            outputSerializer: SerializationStrategy<O>? = null,
            accessRestrictions: RunnableAccessRestriction? = null,
            message: IXmlMessage? = null,
            onActivityProvided: OnActivityProvided<I, O, C> = OnActivityProvided.DEFAULT,
            action: RunnableAction<I, O, C> = { throw UnsupportedOperationException("Action not provided") }
        ) : super(predecessor, inputCombiner, outputSerializer, accessRestrictions, message, onActivityProvided) {
            this.action = action
        }

        constructor(activity: RunnableActivity<I, O, C>) : super(activity) {
            this.inputCombiner = activity.inputCombiner
            this.outputSerializer = activity.outputSerializer
            this.action = activity.action
            this.accessRestrictions = activity.accessRestrictions
            this.message = activity.message
            this.onActivityProvided = activity.onActivityProvided
        }

        override fun build(
            buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): RunnableActivity<I, O, C> {
            return RunnableActivity(this, buildHelper.newOwner, otherNodes)
        }
    }

    class DefineType<T> private constructor(
        override val name: String,
        override val refNode: String?,
        override val refName: String,
        override val path: String?,
        val deserializer: DeserializationStrategy<T>,
        override val content: CompactFragment = CompactFragment(""),
    ) : IXmlDefineType {
        constructor(
            name: String,
            refNode: Identified?,
            refName: String,
            path: String?,
            deserializer: DeserializationStrategy<T>,
            pathNSContext: Iterable<Namespace> = emptyList(),
        ): this(name, refNode?.id, refName, path, deserializer, CompactFragment(pathNSContext,""))

        @OptIn(XmlUtilInternal::class)
        override val originalNSContext: IterableNamespaceContext get() = SimpleNamespaceContext()

        override fun copy(
            name: String,
            refNode: String?,
            refName: String?,
            path: String?,
            content: CharArray?,
            nsContext: IterableNamespaceContext
        ): DefineType<T> {
            return DefineType(name, Identifier(refNode!!), refName!!, path, deserializer, nsContext)
        }

        override fun copy(
            name: String,
            refNode: String?,
            refName: String?,
            path: String?,
            content: CompactFragment
        ): IXmlDefineType {
            return DefineType(name, refNode, refName!!, path, deserializer, content)
        }

        fun <U : Any> copy(
            name: String = this.name,
            refNode: Identified?,
            refName: String = this.refName,
            path: String? = this.path,
            deserializer: DeserializationStrategy<U>,
            nsContext: Iterable<Namespace> = originalNSContext
        ): DefineType<U> {
            return DefineType(name, refNode?.id, refName, path, deserializer, CompactFragment(nsContext,""))
        }

        fun <U : Any> copy(
            name: String = this.name,
            refName: String = this.refName,
            path: String? = this.path,
            deserializer: DeserializationStrategy<U>,
            nsContext: Iterable<Namespace> = originalNSContext
        ): DefineType<U> {
            return DefineType(name, refNode, refName, path, deserializer, CompactFragment(nsContext, ""))
        }

    }
}

class DefineInputCombiner<T> internal constructor(
    val defines: List<RunnableActivity.DefineType<*>>,
    val combiner: InputCombiner<T>
)

class InputCombiner<T>(val impl: (InputContext.(Map<String, Any?>) -> T)? = null) {
    @Suppress("UNCHECKED_CAST")
    operator fun invoke(input: Map<String, Any?>): T {
        val implLocal = impl
        return when {
            implLocal != null -> InputContextImpl(input).implLocal(input)
            input.isEmpty()   -> Unit as T
            input.size == 1   -> input.values.single() as T
            else              -> throw UnsupportedOperationException("Cannot combine multiple inputs automatically")
        }

    }

    interface InputValue<V> {
        val name: String
    }

    interface InputContext {
        fun <T> valueOf(name: String): T
        fun <T> valueOf(inputValue: InputValue<T>): T

        operator fun <T> InputValue<T>.invoke() = valueOf(this)

        operator fun <T> DefineHolder<T>.invoke() = valueOf<T>(define.name)


    }

    @Suppress("UNCHECKED_CAST")
    private class InputContextImpl(private val input: Map<String, Any?>) : InputContext {

        override fun <T> valueOf(name: String): T {
            return input.get(name) as T
        }

        override fun <T> valueOf(inputValue: InputValue<T>): T {
            return input.get(inputValue.name) as T
        }
    }

    companion object {
        val UNIT = InputCombiner { Unit }
    }
}

fun <I : Any, O : Any, C : ActivityInstanceContext> ConfigurableNodeContainer.runnableActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>,
    inputSerializer: DeserializationStrategy<I>,
    inputRefNode: Identified?,
    inputRefName: String = "",
    action: RunnableAction<I, O, C>
): RunnableActivity.Builder<I, O, C> =
    RunnableActivity.Builder(predecessor, inputRefNode, inputRefName, inputSerializer, outputSerializer, action = action)

fun <I : Any, O : Any, C: ActivityInstanceContext> ConfigurableNodeContainer.configureRunnableActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>,
    inputSerializer: DeserializationStrategy<I>,
    inputRefNode: Identified,
    inputRefName: String = "",
    config: @ConfigurationDsl RunnableActivity.Builder<I, O, C>.() -> Unit
): RunnableActivity.Builder<I, O, C> =
    RunnableActivity.Builder<I, O, C>(predecessor, inputRefNode, inputRefName, inputSerializer, outputSerializer).apply(config)

fun <I : Any, O : Any, C: ActivityInstanceContext> ConfigurableNodeContainer.configureRunnableActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>?,
    config: @ConfigurationDsl RunnableActivity.Builder<I, O, C>.() -> Unit
): RunnableActivity.Builder<I, O, C> =
    RunnableActivity.Builder<I, O, C>(predecessor, outputSerializer = outputSerializer).apply(config)
