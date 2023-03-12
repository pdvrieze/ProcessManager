@file:OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class, ExperimentalContracts::class,
    ExperimentalContracts::class, ExperimentalContracts::class, ExperimentalContracts::class
)

package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.IdentifyableSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KProperty
import nl.adaptivity.xmlutil.Namespace

abstract class ModelBuilderContext<C : ActivityInstanceContext> {
    protected abstract val modelBuilder: ProcessModel.Builder
//    private val modelBuilder: RootProcessModel.Builder = RootProcessModelBase.Builder()

    val startNode: StartNode.Builder
        get() = StartNodeBase.Builder()

    @ConfigurationDsl
    fun startNode(config: @ConfigurationDsl StartNode.Builder.() -> Unit): StartNode.Builder =
        StartNodeBase.Builder().apply(config)


    @ConfigurationDsl
    fun split(predecessor: Identified): Split.Builder =
        SplitBase.Builder().apply { this.predecessor = predecessor }

    @ConfigurationDsl
    fun split(
        predecessor: Identified,
        config: @ConfigurationDsl Split.Builder.() -> Unit
    ): Split.Builder =
        split(predecessor).apply(config)

    @ConfigurationDsl
    fun join(vararg predecessors: Identified): Join.Builder = JoinBase.Builder().apply {
        this.predecessors = IdentifyableSet.processNodeSet(predecessors)
    }

    @ConfigurationDsl
    fun join(predecessors: Collection<Identified>): Join.Builder = JoinBase.Builder().apply {
        this.predecessors = IdentifyableSet.processNodeSet(predecessors)
    }

    @ConfigurationDsl
    fun join(
        vararg predecessors: Identified,
        config: @ConfigurationDsl Join.Builder.() -> Unit
    ): Join.Builder = join(*predecessors).apply(config)

    @ConfigurationDsl
    fun join(
        predecessors: Collection<Identified>,
        config: @ConfigurationDsl Join.Builder.() -> Unit
    ): Join.Builder =join(predecessors).apply(config)

    fun endNode(predecessor: Identified): EndNode.Builder =
        EndNodeBase.Builder().apply {
            this.predecessor = predecessor
        }

    fun endNode(
        predecessor: Identified,
        config: @ConfigurationDsl EndNode.Builder.() -> Unit
    ): EndNode.Builder = endNode(predecessor).apply(config)

    fun endNode(name: String, predecessor: Identified) {
        EndNodeBase.Builder().apply {
            this.id = name
            this.predecessor = predecessor
        }
    }

    fun endNode(
        name: String,
        predecessor: Identified,
        config: @ConfigurationDsl EndNode.Builder.() -> Unit
    ) {
        EndNodeBase.Builder().apply {
            this.id = name
            this.predecessor = predecessor
            config()
        }
    }

    inline fun <I: Any, reified O: Any> activity(
        predecessor: Identified,
        input: InputRef<I>,
        @BuilderInference
        noinline action: RunnableAction<I, O, C>
    ) : RunnableActivity.Builder<I, O, C> {
        return RunnableActivity.Builder(
            predecessor,
            input.nodeRef,
            input.propertyName,
            input.serializer,
            serializer(),
            action
        )
    }

    inline fun <I : Any, reified O : Any> activity(
        predecessor: Identified,
        input: DefineInputCombiner<I>,
        @BuilderInference
        noinline action: RunnableAction<I, O, C>
    ): RunnableActivity.Builder<I, O, C> {
        return configuredActivityBuilder<I, O>(predecessor, input, serializer()).apply {
            this.action = action
        }
    }

    inline fun <I : Any, reified O : Any> activity(
        predecessor: NodeHandle<I>,
        @BuilderInference
        noinline action: RunnableAction<I, O, C>
    ): RunnableActivity.Builder<I, O, C> {
        return RunnableActivity.Builder(
            predecessor,
            predecessor.identifier,
            "",
            predecessor.serializer,
            serializer(),
            action
        )
    }

    fun <I : Any, O : Any> activity(
        predecessor: ActivityHandle<I>,
        outputSerializer: SerializationStrategy<O>,
        @BuilderInference
        action: RunnableAction<I, O, C>
    ): RunnableActivity.Builder<I, O, C> {
        return RunnableActivity.Builder(
            predecessor,
            predecessor,
            predecessor.propertyName,
            predecessor.serializer,
            outputSerializer,
            action
        )
    }

    fun <I : Any, O : Any> activity(
        predecessor: Identified,
        inputSerializer: DeserializationStrategy<I>,
        outputSerializer: SerializationStrategy<O>,
        inputRefNode: Identified? = null,
        @BuilderInference
        action: RunnableAction<I, O, C>
    ): RunnableActivity.Builder<I, O, C> {
        return RunnableActivity.Builder(
            predecessor,
            inputRefNode,
            "",
            inputSerializer,
            outputSerializer,
            action
        )
    }

    fun <I : Any, O : Any> activity(
        predecessor: Identified,
        inputSerializer: DeserializationStrategy<I>,
        outputSerializer: SerializationStrategy<O>,
        inputRefName: String = "",
        @BuilderInference
        action: RunnableAction<I, O, C>
    ): RunnableActivity.Builder<I, O, C> {
        return RunnableActivity.Builder(
            predecessor,
            null,
            inputRefName,
            inputSerializer,
            outputSerializer,
            action
        )
    }

    fun <I : Any, O : Any> activity(
        predecessor: Identified,
        inputSerializer: DeserializationStrategy<I>,
        outputSerializer: SerializationStrategy<O>,
        inputRefNode: Identified?,
        inputRefName: String,
        @BuilderInference
        action: RunnableAction<I, O, C>
    ): RunnableActivity.Builder<I, O, C> {
        return RunnableActivity.Builder(
            predecessor,
            inputRefNode,
            inputRefName,
            inputSerializer,
            outputSerializer,
            action
        )
    }

    @PublishedApi
    internal abstract fun compositeActivityContext(predecessor: Identified) : CompositeModelBuilderContext<C>

    operator fun <I: Any, O: Any> RunnableActivity.Builder<I, O, C>.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): ActivityHandle<O> {
        val nodeBuilder = this
        if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
        with(modelBuilder) {
            nodes.add(nodeBuilder.ensureId())
        }
        val outputName = results.singleOrNull()?.name ?: ""
        return ActivityHandleImpl(id!!, outputName, this.outputSerializer as KSerializer<O>)
    }

    operator fun ProcessNode.Builder.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): NodeHandle<Unit> {
        val nodeBuilder = this
        if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
        with(modelBuilder) {
            if (nodeBuilder is CompositeActivity.ModelBuilder) {
                modelBuilder.rootBuilder.childModels.add(nodeBuilder.ensureChildId())
            }

            nodes.add(nodeBuilder.ensureId())
        }
        return NonActivityNodeHandleImpl(id!!)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun <T: Identified> T.getValue(
        thisRef: Nothing?,
        property: KProperty<*>
    ): T = this

    inline fun <I : Any, reified O : Any> configuredActivity(
        predecessor: Identified,
        @BuilderInference
        noinline config: @ConfigurationDsl RunnableActivity.Builder<I, O, C>.() -> Unit
    ): RunnableActivity.Builder<I, O, C> {
        contract {
            callsInPlace(config, InvocationKind.EXACTLY_ONCE)
        }
        return configuredActivity(predecessor = predecessor, outputSerializer = serializer(), config = config)
    }

    inline fun <I : Any, reified O : Any> configuredActivity(
        predecessor: Identified,
        input: DefineInputCombiner<I>,
        @BuilderInference
        noinline config: @ConfigurationDsl RunnableActivity.Builder<I, O, C>.() -> Unit
    ): RunnableActivity.Builder<I, O, C> {
        contract {
            callsInPlace(config, InvocationKind.EXACTLY_ONCE)
        }

        return configuredActivityBuilder<I, O>(predecessor, input, serializer()).apply(config)
    }

    @PublishedApi
    internal fun <I : Any, O : Any> configuredActivityBuilder(
        predecessor: Identified,
        input: DefineInputCombiner<I>,
        outputSerializer: SerializationStrategy<O>): RunnableActivity.Builder<I, O, C> {

        return RunnableActivity.Builder<I, O, C>(
            predecessor = predecessor,
            outputSerializer = outputSerializer,
            inputCombiner = input.combiner
        ).apply {
            defines.addAll(input.defines)
        }
    }

    fun <I : Any, O : Any> configuredActivity(
        predecessor: Identified,
        outputSerializer: SerializationStrategy<O>?,
        @BuilderInference
        config: @ConfigurationDsl RunnableActivity.Builder<I, O, C>.() -> Unit
    ): RunnableActivity.Builder<I, O, C> {
        contract {
            callsInPlace(config, InvocationKind.EXACTLY_ONCE)
        }
        return RunnableActivity.Builder<I, O, C>(predecessor, outputSerializer = outputSerializer).apply(config)
    }

/*
    operator fun <T : ConfigurableProcessModel<ExecutableProcessNode>.ConfigurableCompositeActivity> T.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): T {
        setIdIfEmpty(property.name)
        return this
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun <T : ConfigurableProcessModel<ExecutableProcessNode>.ConfigurableCompositeActivity>
        T.getValue(thisRef: Nothing?, property: KProperty<*>): T = this
*/

    fun <T> processResult(
        name: String,
        refNode: NodeHandle<T>,
        refName: String? = null,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList()
    ): ProcessResultRef<T> {
        return processResult(name, refNode, refName, path, content, nsContext, refNode.serializer)
    }

    inline fun <I> processResult(
        name: String,
        refNode: ActivityHandle<I>,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList()
    ): ProcessResultRef<I> {
        return processResult(name, refNode, refNode.propertyName, path, content, nsContext, refNode.serializer)
    }

    inline fun <reified T> processResult(
        name: String,
        refNode: OutputRef<T>,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList()
    ): ProcessResultRef<T> {
        return processResult(name, refNode.nodeRef, refNode.propertyName, path, content, nsContext, serializer())
    }

    fun <T> processResult(
        name: String,
        refNode: Identified,
        refName: String? = null,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList(),
        serializer: KSerializer<T>
    ): ProcessResultRef<T> {
        modelBuilder.exports.add(XmlDefineType(name, refNode, refName, path, content, nsContext))
        return ProcessResultRefImpl(name, serializer)

    }

    fun <I1, I2> combine(
        input1: DefineHolder<I1>,
        input2: DefineHolder<I2>,
    ): DefineInputCombiner<Pair<I1, I2>> {
        return DefineInputCombiner(
            listOf(input1.define, input2.define),
            InputCombiner {
                Pair(input1(), input2())
            }
        )
    }

    fun <T, I1, I2> combine(
        input1: DefineHolder<I1>,
        input2: DefineHolder<I2>,
        combiner: (I1, I2) -> T
    ): DefineInputCombiner<T> {
        return DefineInputCombiner(
            listOf(input1.define, input2.define),
            InputCombiner {
                combiner(input1(), input2())
            }
        )
    }

    fun <T, I1, I2, I3> combine(
        input1: DefineHolder<I1>,
        input2: DefineHolder<I2>,
        input3: DefineHolder<I3>,
        combiner: (I1, I2, I3) -> T
    ): DefineInputCombiner<T> {
        return DefineInputCombiner(
            listOf(input1.define, input2.define, input3.define),
            InputCombiner {
                combiner(input1(), input2(), input3())
            }
        )
    }

    fun <T, I1, I2, I3, I4> combine(
        input1: DefineHolder<I1>,
        input2: DefineHolder<I2>,
        input3: DefineHolder<I3>,
        input4: DefineHolder<I4>,
        combiner: (I1, I2, I3, I4) -> T
    ): DefineInputCombiner<T> {
        return DefineInputCombiner(
            listOf(input1.define, input2.define, input3.define, input4.define),
            InputCombiner {
                combiner(input1(), input2(), input3(), input4())
            }
        )
    }

    fun <T> combine(
        vararg inputs: DefineHolder<*>,
        combiner: InputCombiner<T>
    ): DefineInputCombiner<T> {
        return DefineInputCombiner(inputs.map { it.define }, combiner)
    }

}


inline fun <C : ActivityInstanceContext> ModelBuilderContext<C>.compositeActivity(
    predecessor: Identified,
    @ConfigurationDsl configure: CompositeModelBuilderContext<C>.() -> Unit
): ActivityBase.CompositeActivityBuilder {
    contract {
        callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
    }
    val context = compositeActivityContext(predecessor).apply(configure)
    return context.activityBuilder
}


private data class ProcessResultRefImpl<T>(
    override val propertyName: String,
    override val serializer: KSerializer<T>
) : ProcessResultRef<T> {
    override val nodeRef: Nothing? get() = null
}
