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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import net.devrieze.util.TypecheckingCollection
import net.devrieze.util.toComparableHandle
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurableNodeContainer
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.serialization.XML

typealias RunnableAction<I, O> = ActivityInstanceContext.(I) -> O

class RunnableActivity<I : Any, O : Any>(
    builder: Builder<I, O>,
    newOwner: ProcessModel<*>,
    otherNodes: Iterable<ProcessNode.Builder>
) : ActivityBase(builder.checkDefines(), newOwner, otherNodes), ExecutableProcessNode {

    internal val action: RunnableAction<I, O> = builder.action
    internal val inputCombiner: InputCombiner<I> = builder.inputCombiner
    internal val outputSerializer: SerializationStrategy<O>? = builder.outputSerializer
    override val condition: ExecutableCondition? = builder.condition?.toExecutableCondition()

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    @Suppress("UNCHECKED_CAST")
    override val defines: List<DefineType<*>>
        get() = super.defines as List<DefineType<*>>

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override fun builder(): Activity.Builder {
        return Builder(this)
    }

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitGenericActivity(this)
    }

    override fun provideTask(
        engineData: ProcessEngineDataAccess,
        instanceBuilder: ProcessNodeInstance.Builder<*, *>
    ): Boolean {


        return true
    }

    override fun createOrReuseInstance(
        data: MutableProcessEngineDataAccess,
        processInstanceBuilder: ProcessInstance.Builder,
        predecessor: IProcessNodeInstance,
        entryNo: Int,
        allowFinalInstance: Boolean
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
        processInstanceBuilder.getChildNodeInstance(this, entryNo)?.let { return it }
        if (!isMultiInstance && entryNo > 1) {
            processInstanceBuilder.allChildNodeInstances { it.node == this && it.entryNo != entryNo }.forEach {
                processInstanceBuilder.updateChild(it) {
                    invalidateTask(data)
                }
            }
        }
        return RunnableActivityInstance.BaseBuilder(
            this, predecessor.handle.toComparableHandle(),
            processInstanceBuilder,
            processInstanceBuilder.owner, entryNo
        )
    }

    override fun takeTask(instance: ProcessNodeInstance.Builder<*, *>): Boolean = true

    fun getInputData(data: List<ProcessData>): I {
        val mappedData = mutableMapOf<String, Any?>()
        for (define in this.defines) {
            val valueReader = data.singleOrNull() { it.name == define.name }?.contentStream
                ?: throw NoSuchElementException("Could not find single define with name ${define.refName}")
            val value = XML.decodeFromReader(define.deserializer, valueReader)
            mappedData[define.getName()] = value
        }

        return inputCombiner(mappedData)
    }

    class Builder<I : Any, O : Any> : ActivityBase.BaseBuilder {

        var inputCombiner: InputCombiner<I> = InputCombiner()
        val outputSerializer: SerializationStrategy<O>?
        var action: RunnableAction<I, O>

        override val defines: MutableCollection<IXmlDefineType>
            get() = TypecheckingCollection(DefineType::class, super.defines)

        constructor(
            predecessor: Identified,
            refNode: Identified?,
            refName: String,
            inputSerializer: DeserializationStrategy<I>,
            outputSerializer: SerializationStrategy<O>? = null,
            action: RunnableAction<I, O> = { throw UnsupportedOperationException("Action not provided") }
        ) : super() {
            this.predecessor = predecessor
            this.outputSerializer = outputSerializer
            this.action = action
            if (inputSerializer == Unit.serializer()) {
                @Suppress("UNCHECKED_CAST")
                inputCombiner = InputCombiner.UNIT as InputCombiner<I>
            } else {
                defineInput<I>("input", refNode, refName, inputSerializer)
            }

            when (outputSerializer) {
                null,
                Unit.serializer() -> {
                }
                else              -> results.add(XmlResultType("output"))
            }

        }

        constructor(
            predecessor: Identified,
            inputCombiner: InputCombiner<I> = InputCombiner(),
            outputSerializer: SerializationStrategy<O>? = null,
            action: RunnableAction<I, O> = { throw UnsupportedOperationException("Action not provided") }
        ) : super() {
            this.predecessor = predecessor
            results.add(XmlResultType("output"))
            this.outputSerializer = outputSerializer
            this.action = action
        }

        constructor(activity: RunnableActivity<I, O>) : super(activity) {
            this.inputCombiner = activity.inputCombiner
            this.outputSerializer = activity.outputSerializer
            this.action = activity.action
        }

        fun defineInput(
            refNode: Identified?,
            valueName: String,
            deserializer: DeserializationStrategy<I>
        ): InputCombiner.InputValue<I> {
            val defineType = DefineType("input", refNode, valueName, null, deserializer)
            defines.add(defineType)
            return InputValueImpl("input")
        }

        fun <T : Any> defineInput(
            name: String,
            refNode: Identified?,
            valueName: String,
            deserializer: DeserializationStrategy<T>
        ): InputCombiner.InputValue<T> {
            val defineType = DefineType(name, refNode, valueName, null, deserializer)
            defines.add(defineType)
            return InputValueImpl(name)
        }

        fun defineInput(refNode: Identified?, deserializer: DeserializationStrategy<I>): InputCombiner.InputValue<I> {
            val defineType = DefineType("input", refNode, "", null, deserializer)
            defines.add(defineType)
            return InputValueImpl("input")
        }

        fun <T : Any> defineInput(
            name: String,
            refNode: Identified?,
            deserializer: DeserializationStrategy<T>
        ): InputCombiner.InputValue<T> {
            val defineType = DefineType(name, refNode, "", null, deserializer)
            defines.add(defineType)
            return InputValueImpl(name)
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R {
            return visitor.visitGenericActivity(this)
        }


        class InputValueImpl<V>(override val name: String) : InputCombiner.InputValue<V>
    }

    class DefineType<T : Any>(
        private val name: String,
        private val refNode: Identified?,
        private val refName: String,
        private val path: String?,
        val deserializer: DeserializationStrategy<T>,
        pathNSContext: Iterable<Namespace> = emptyList()
    ) : /*XPathHolder(name, path, null, pathNSContext),*/ IXmlDefineType {
        override val content: Nothing? get() = null
        override val originalNSContext: Iterable<Namespace> get() = emptyList()

        override fun getRefNode(): String? = refNode?.id

        override fun setRefNode(value: String?): Nothing = throw UnsupportedOperationException("Immutable type")

        override fun getRefName(): String? = refName

        override fun setRefName(value: String?): Nothing = throw UnsupportedOperationException("Immutable type")

        override fun getName(): String = name

        override fun setName(value: String): Nothing = throw UnsupportedOperationException("Immutable type")

        override fun getPath(): String? = path

        override fun setPath(namespaceContext: Iterable<Namespace>, value: String?): Nothing =
            throw UnsupportedOperationException("Immutable type")

        override fun copy(
            name: String,
            refNode: String?,
            refName: String?,
            path: String?,
            content: CharArray?,
            nsContext: Iterable<Namespace>
        ): DefineType<T> {
            return DefineType(name, Identifier(refNode!!), refName!!, path, deserializer, nsContext)
        }

        fun <U : Any> copy(
            name: String = getName(),
            refNode: Identified? = this.refNode,
            refName: String = getRefName()!!,
            path: String? = getPath(),
            deserializer: DeserializationStrategy<U>,
            nsContext: Iterable<Namespace> = originalNSContext
        ): DefineType<U> {
            return DefineType(name, refNode, refName, path, deserializer, nsContext)
        }

    }
}

private fun <R : RunnableActivity.Builder<*, *>> R.checkDefines(): R = apply {
    val illegalDefine = defines.firstOrNull { it !is RunnableActivity.DefineType<*> }
    if (illegalDefine != null) {
        throw IllegalArgumentException("Invalid define $illegalDefine in runnable activity")
    }
}

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

fun <I : Any, O : Any> ConfigurableNodeContainer<ExecutableProcessNode>.runnableActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>,
    inputSerializer: DeserializationStrategy<I>,
    inputRefNode: Identified?,
    inputRefName: String = "",
    action: RunnableAction<I, O>
): RunnableActivity.Builder<I, O> =
    RunnableActivity.Builder(predecessor, inputRefNode, inputRefName, inputSerializer, outputSerializer, action)

fun <I : Any, O : Any> ConfigurableNodeContainer<ExecutableProcessNode>.configureRunnableActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>,
    inputSerializer: DeserializationStrategy<I>,
    inputRefNode: Identified,
    inputRefName: String = "",
    config: @ConfigurationDsl RunnableActivity.Builder<I, O>.() -> Unit
): RunnableActivity.Builder<I, O> =
    RunnableActivity.Builder(predecessor, inputRefNode, inputRefName, inputSerializer, outputSerializer).apply(config)

fun <I : Any, O : Any> ConfigurableNodeContainer<ExecutableProcessNode>.configureRunnableActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>?,
    config: @ConfigurationDsl RunnableActivity.Builder<I, O>.() -> Unit
): RunnableActivity.Builder<I, O> =
    RunnableActivity.Builder<I, O>(predecessor, outputSerializer = outputSerializer).apply(config)
