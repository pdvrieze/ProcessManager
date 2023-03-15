@file:OptIn(ExperimentalTypeInference::class)

package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.ActivityBase
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.Namespace
import kotlin.experimental.ExperimentalTypeInference

abstract class CompositeModelBuilderContext<AIC : ActivityInstanceContext> : ModelBuilderContext<AIC>() {
    abstract override val modelBuilder: ActivityBase.CompositeActivityBuilder

    @PublishedApi
    internal val activityBuilder: ActivityBase.CompositeActivityBuilder
        get() = modelBuilder

    inline fun <reified T> input(
        name: String,
        refNode: Identified,
        refName: String? = null,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList(),
    ): InputRef<T> {
        return input(name, refNode, refName, path, content, nsContext, serializer())
    }

    inline fun <reified T> input(
        name: String,
        refNode: ActivityHandle<T>,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList(),
    ): InputRef<T> {
        return input(name, refNode, refNode.propertyName, path, content, nsContext, serializer())
    }

    inline fun <reified T> input(
        name: String,
        refNode: OutputRef<T>,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList(),
    ): InputRef<T> {
        return input(name, refNode.nodeRef, refNode.propertyName, path, content, nsContext, serializer())
    }

    fun <T> input(
        name: String,
        refNode: Identified,
        refName: String? = null,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList(),
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

    infix fun <T> InputRef<T>.named(name: String): DefineHolder<T> {
        val defineType = RunnableActivity.DefineType(name, nodeRef, propertyName, null, serializer)
        return DefineHolder(defineType)
    }

    inline fun <reified T> output(
        name: String,
        refNode: Identified,
        refName: String? = null,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList()
    ): OutputRef<T> {
        return output<T>(name, refNode, refName, path, content, nsContext, serializer())
    }

    inline fun <reified T> output(
        name: String,
        refNode: ActivityHandle<T>,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList()
    ): OutputRef<T> {
        return output(name, refNode, refNode.propertyName, path, content, nsContext, serializer())
    }

    inline fun <reified T> output(
        name: String,
        refNode: OutputRef<T>,
        path: String? = null,
        content: CharArray? = null,
        nsContext: Iterable<Namespace> = emptyList()
    ): OutputRef<T> {
        return output(name, refNode.nodeRef, refNode.propertyName, path, content, nsContext, serializer())
    }

    fun <T> output(
        name: String,
        refNode: Identified,
        refName: String?,
        path: String?,
        content: CharArray?,
        nsContext: Iterable<Namespace>,
        serializer: KSerializer<T>
    ): OutputRef<T> {

        modelBuilder.results.add(XmlResultType(name, "/$name/*"))
        modelBuilder.exports.add(XmlDefineType(name, refNode, refName, path, content, nsContext))
        with (modelBuilder.rootBuilder) {
            modelBuilder.ensureChildId() // Ensure there is an id for the composite model
            modelBuilder.ensureId()// Ensure an id for the activity itself
        }
        return ChildOutputRefImpl(Identifier(requireNotNull(modelBuilder.id)), name, serializer)
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
            action
        )
    }

    private data class InputRefImpl<T>(
        override val nodeRef: Identified?,
        override val propertyName: String,
        override val serializer: DeserializationStrategy<T>
    ) : InputRef<T> {
        constructor(propertyName: String, serializer: DeserializationStrategy<T>) :
            this(null, propertyName, serializer)
    }

    private data class ChildOutputRefImpl<T> internal constructor(
        override val nodeRef: Identified,
        override val propertyName: String,
        override val serializer: KSerializer<T>
    ) : OutputRef<T>

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

class DefineHolder<T> internal constructor(val define: RunnableActivity.DefineType<T>)
