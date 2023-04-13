@file:OptIn(ExperimentalTypeInference::class)

package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.ActivityBase
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xmlutil.Namespace
import kotlin.experimental.ExperimentalTypeInference

abstract class CompositeModelBuilderContext<AIC : ActivityInstanceContext> : ModelBuilderContext<AIC>(), ICompositeModelBuilderContext<AIC> {
    @PublishedApi
    internal val activityBuilder: ActivityBase.CompositeActivityBuilder
        get() = modelBuilder

    override fun <T> input(
        name: String,
        refNode: Identified,
        refName: String?,
        path: String?,
        content: CharArray?,
        nsContext: Iterable<Namespace>,
        deserializer: DeserializationStrategy<T>,
    ): InputRef<T> {
        modelBuilder.defines.add(XmlDefineType(name, refNode, refName, path, content, nsContext))
        modelBuilder.imports.add(XmlResultType(name, "/$name/*"))
        return InputRefImpl(name, deserializer)
    }

    fun <I : Any, O : Any> RunnableActivity.Builder<I, O, AIC>.defineInput(
        reference: InputRef<I>
    ): InputCombiner.InputValue<I> {
        return defineInput(reference.nodeRef, reference.propertyName, reference.serializer)
    }

    fun <T : Any, I : Any, O : Any> RunnableActivity.Builder<I, O, AIC>.defineInput(
        name: String,
        reference: InputRef<T>
    ): InputCombiner.InputValue<T> {
        return defineInput(name, reference.nodeRef, reference.propertyName, reference.serializer)
    }

    fun <I : Any, O : Any> activity(
        predecessor: Identified,
        input: InputRef<I>,
        outputSerializer: SerializationStrategy<O>,
        @BuilderInference
        action: RunnableAction<I, O, AIC>
    ): RunnableActivity.Builder<I, O, AIC> {
        return RunnableActivity.Builder(
            predecessor,
            input.nodeRef,
            input.propertyName,
            input.serializer,
            outputSerializer,
            action = action
        )
    }

}

internal class CompositeModelBuilderContextImpl<AIC: ActivityInstanceContext>(
    predecessor: Identified,
    private val owner: RootModelBuilderContextImpl<AIC>
): CompositeModelBuilderContext<AIC>() {
    override val modelBuilder = ActivityBase.CompositeActivityBuilder(owner.modelBuilder).apply {
        this.predecessor = predecessor
    }

    override fun compositeActivityContext(predecessor: Identified): CompositeModelBuilderContext<AIC> {
        return CompositeModelBuilderContextImpl(predecessor, owner)
    }
}

class DefineHolder<T> constructor(val define: RunnableActivity.DefineType<T>)
