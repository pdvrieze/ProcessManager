@file:OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)

package io.github.pdvrieze.process.processModel.dynamicProcessModel

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity.OnActivityProvided
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.util.Identified
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

abstract class ModelBuilderContext<AIC : ActivityInstanceContext> : IModelBuilderContext<AIC> {

    inline fun <I: Any, reified O: Any> activity(
        predecessor: Identified,
        input: InputRef<I>,
        accessRestrictions: RunnableAccessRestriction? = null,
        onActivityProvided: OnActivityProvided<I, O, AIC> = OnActivityProvided.DEFAULT,
        @BuilderInference
        noinline action: RunnableAction<I, O, AIC>
    ) : RunnableActivity.Builder<I, O, AIC> {
        return RunnableActivity.Builder(
            predecessor,
            input.nodeRef,
            input.propertyName,
            input.serializer,
            serializer(),
            action
        ).apply {
            this.accessRestrictions = accessRestrictions
            this.onActivityProvided = onActivityProvided
        }
    }

    inline fun <I : Any, reified O : Any> activity(
        predecessor: Identified,
        input: DefineInputCombiner<I>,
        accessRestrictions: RunnableAccessRestriction? = null,
        onActivityProvided: OnActivityProvided<I, O, AIC> = OnActivityProvided.DEFAULT,
        @BuilderInference
        noinline action: RunnableAction<I, O, AIC>
    ): RunnableActivity.Builder<I, O, AIC> {
        return configuredActivityBuilder<I, O>(predecessor, input, serializer()).apply {
            this.action = action
            this.accessRestrictions = accessRestrictions
            this.onActivityProvided = onActivityProvided
        }
    }

    inline fun <I : Any, reified O : Any> activity(
        predecessor: NodeHandle<I>,
        accessRestrictions: RunnableAccessRestriction? = null,
        onActivityProvided: OnActivityProvided<I, O, AIC> = OnActivityProvided.DEFAULT,
        @BuilderInference
        noinline action: RunnableAction<I, O, AIC>
    ): RunnableActivity.Builder<I, O, AIC> {
        return RunnableActivity.Builder(
            predecessor,
            predecessor.identifier,
            "",
            predecessor.serializer,
            serializer(),
            action
        ).apply {
            this.accessRestrictions = accessRestrictions
            this.onActivityProvided = onActivityProvided
        }
    }

    fun <I : Any, O : Any> activity(
        predecessor: ActivityHandle<I>,
        outputSerializer: SerializationStrategy<O>,
        accessRestrictions: RunnableAccessRestriction? = null,
        onActivityProvided: OnActivityProvided<I, O, AIC> = OnActivityProvided.DEFAULT,
        @BuilderInference
        action: RunnableAction<I, O, AIC>
    ): RunnableActivity.Builder<I, O, AIC> {
        return RunnableActivity.Builder(
            predecessor,
            predecessor,
            predecessor.propertyName,
            predecessor.serializer,
            outputSerializer,
            action
        ).apply {
            this.onActivityProvided = onActivityProvided
            this.accessRestrictions = accessRestrictions
        }
    }

    fun <I : Any, O : Any> activity(
        predecessor: Identified,
        inputSerializer: DeserializationStrategy<I>,
        outputSerializer: SerializationStrategy<O>,
        inputRefNode: Identified? = null,
        accessRestrictions: RunnableAccessRestriction? = null,
        onActivityProvided: OnActivityProvided<I, O, AIC> = OnActivityProvided.DEFAULT,
        @BuilderInference
        action: RunnableAction<I, O, AIC>
    ): RunnableActivity.Builder<I, O, AIC> {
        return RunnableActivity.Builder(
            predecessor,
            inputRefNode,
            "",
            inputSerializer,
            outputSerializer,
            action
        ).apply {
            this.onActivityProvided = onActivityProvided
            this.accessRestrictions = accessRestrictions
        }
    }

    fun <I : Any, O : Any> activity(
        predecessor: Identified,
        inputSerializer: DeserializationStrategy<I>,
        outputSerializer: SerializationStrategy<O>,
        inputRefName: String = "",
        accessRestrictions: RunnableAccessRestriction? = null,
        onActivityProvided: OnActivityProvided<I, O, AIC> = OnActivityProvided.DEFAULT,
        @BuilderInference
        action: RunnableAction<I, O, AIC>
    ): RunnableActivity.Builder<I, O, AIC> {
        return RunnableActivity.Builder(
            predecessor,
            null,
            inputRefName,
            inputSerializer,
            outputSerializer,
            action
        ).apply {
            this.onActivityProvided = onActivityProvided
            this.accessRestrictions = accessRestrictions
        }
    }

    fun <I : Any, O : Any> activity(
        predecessor: Identified,
        inputSerializer: DeserializationStrategy<I>,
        outputSerializer: SerializationStrategy<O>,
        inputRefNode: Identified?,
        inputRefName: String,
        accessRestrictions: RunnableAccessRestriction? = null,
        onActivityProvided: OnActivityProvided<I, O, AIC> = OnActivityProvided.DEFAULT,
        @BuilderInference
        action: RunnableAction<I, O, AIC>
    ): RunnableActivity.Builder<I, O, AIC> {
        return RunnableActivity.Builder(
            predecessor,
            inputRefNode,
            inputRefName,
            inputSerializer,
            outputSerializer,
            action
        ).apply {
            this.onActivityProvided = onActivityProvided
            this.accessRestrictions = accessRestrictions
        }
    }

    @PublishedApi
    internal abstract fun compositeActivityContext(predecessor: Identified) : CompositeModelBuilderContext<AIC>

    @OptIn(ExperimentalContracts::class)
    inline fun <I : Any, reified O : Any> configuredActivity(
        predecessor: Identified,
        @BuilderInference
        noinline config: @ConfigurationDsl RunnableActivity.Builder<I, O, AIC>.() -> Unit
    ): RunnableActivity.Builder<I, O, AIC> {
        contract {
            callsInPlace(config, InvocationKind.EXACTLY_ONCE)
        }
        return configuredActivity(predecessor = predecessor, outputSerializer = serializer(), config = config)
    }

    inline fun <I : Any, reified O : Any> configuredActivity(
        predecessor: Identified,
        input: DefineInputCombiner<I>,
        @BuilderInference
        noinline config: @ConfigurationDsl RunnableActivity.Builder<I, O, AIC>.() -> Unit
    ): RunnableActivity.Builder<I, O, AIC> {
        contract {
            callsInPlace(config, InvocationKind.EXACTLY_ONCE)
        }

        return configuredActivityBuilder<I, O>(predecessor, input, serializer()).apply(config)
    }

    @PublishedApi
    internal fun <I : Any, O : Any> configuredActivityBuilder(
        predecessor: Identified,
        input: DefineInputCombiner<I>,
        outputSerializer: SerializationStrategy<O>): RunnableActivity.Builder<I, O, AIC> {

        return RunnableActivity.Builder<I, O, AIC>(
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
        config: @ConfigurationDsl RunnableActivity.Builder<I, O, AIC>.() -> Unit
    ): RunnableActivity.Builder<I, O, AIC> {
        contract {
            callsInPlace(config, InvocationKind.EXACTLY_ONCE)
        }
        return RunnableActivity.Builder<I, O, AIC>(predecessor, outputSerializer = outputSerializer).apply(config)
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


@OptIn(ExperimentalContracts::class)
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

