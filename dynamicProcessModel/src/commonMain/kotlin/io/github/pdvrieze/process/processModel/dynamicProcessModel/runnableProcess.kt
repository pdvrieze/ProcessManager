@file:OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)

package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.*
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurableProcessModel
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.xmlutil.Namespace
import java.util.Collections.emptyList
import kotlin.contracts.ExperimentalContracts
import kotlin.experimental.ExperimentalTypeInference


fun <C : ActivityInstanceContext> runnableProcess(
    name: String,
    owner: PrincipalCompat = SYSTEMPRINCIPAL,
    uuid: UUID = UUID.randomUUID(),
    @ConfigurationDsl
    configureAction: ModelBuilderContext<C>.() -> Unit
): ExecutableProcessModel {
    val context = RootModelBuilderContextImpl<C>(name, owner, uuid).apply(configureAction)
    return ExecutableProcessModel(context.modelBuilder, true)
}

private class RootModelBuilderContextImpl<C: ActivityInstanceContext>(
    name: String,
    owner: PrincipalCompat,
    uuid: UUID,
) : ModelBuilderContext<C>() {
    public override val modelBuilder: RootProcessModel.Builder = RootProcessModelBase.Builder().apply {
        this.name = name
        this.owner = owner
        this.uuid = uuid
    }

    override fun compositeActivityContext(predecessor: Identified): CompositeModelBuilderContext<C> {
        return CompositeModelBuilderContextImpl(predecessor, this)
    }
}

private class CompositeModelBuilderContextImpl<C: ActivityInstanceContext>(
    val predecessor: Identified,
    private val owner: RootModelBuilderContextImpl<C>
): CompositeModelBuilderContext<C>() {
    override val modelBuilder = ActivityBase.CompositeActivityBuilder(owner.modelBuilder)

    override fun compositeActivityContext(predecessor: Identified): CompositeModelBuilderContext<C> {
        return CompositeModelBuilderContextImpl(predecessor, owner)
    }
}

abstract class CompositeModelBuilderContext<C : ActivityInstanceContext> : ModelBuilderContext<C>() {
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

    fun <I : Any, O : Any> RunnableActivity.Builder<I, O, C>.defineInput(
        reference: InputRef<I>
    ): InputCombiner.InputValue<I> {
        return defineInput(reference.nodeRef, reference.propertyName, reference.serializer)
    }

    fun <T : Any, I : Any, O : Any> RunnableActivity.Builder<I, O, C>.defineInput(
        name: String,
        reference: InputRef<T>
    ): InputCombiner.InputValue<T> {
        return defineInput(name, reference.nodeRef, reference.propertyName, reference.serializer)
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
        action: RunnableAction<I, O, C>
    ): RunnableActivity.Builder<I, O, C> {
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



