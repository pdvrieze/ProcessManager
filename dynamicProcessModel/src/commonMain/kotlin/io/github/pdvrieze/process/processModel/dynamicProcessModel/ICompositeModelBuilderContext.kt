package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.ActivityBase
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.IterableNamespaceContext
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlUtilInternal

interface ICompositeModelBuilderContext<AIC: ActivityInstanceContext>: IModelBuilderContext<AIC> {

    override val modelBuilder: ActivityBase.CompositeActivityBuilder

    @Deprecated("Avoid using CharArray due to efficiency issues")
    fun <T> input(
        name: String,
        refNode: Identified,
        refName: String? = null,
        path: String? = null,
        content: CharArray,
        nsContext: Iterable<Namespace>,
        deserializer: DeserializationStrategy<T>,
    ): InputRef<T> {
        @Suppress("DEPRECATION")
        @OptIn(XmlUtilInternal::class)
        return input(name, refNode, refName, path, content, SimpleNamespaceContext(nsContext), deserializer)
    }

    fun <T> input(
        name: String,
        refNode: Identified,
        refName: String? = null,
        path: String? = null,
        content: String? = null,
        nsContext: Iterable<Namespace>,
        deserializer: DeserializationStrategy<T>,
    ): InputRef<T> {
        @OptIn(XmlUtilInternal::class)
        return input(name, refNode, refName, path, content, SimpleNamespaceContext(nsContext), deserializer)
    }

    @Deprecated("Avoid using CharArray due to efficiency issues")
    @OptIn(XmlUtilInternal::class)
    fun <T> input(
        name: String,
        refNode: Identified,
        refName: String? = null,
        path: String? = null,
        content: CharArray,
        nsContext: IterableNamespaceContext = SimpleNamespaceContext(),
        deserializer: DeserializationStrategy<T>,
    ): InputRef<T>

    @OptIn(XmlUtilInternal::class)
    fun <T> input(
        name: String,
        refNode: Identified,
        refName: String? = null,
        path: String? = null,
        content: String? = null,
        nsContext: IterableNamespaceContext = SimpleNamespaceContext(),
        deserializer: DeserializationStrategy<T>,
    ): InputRef<T>

    override infix fun <T> InputRef<T>.named(name: String): DefineHolder<T> {
        val defineType = RunnableActivity.DefineType(name, nodeRef, propertyName, null, serializer)
        return DefineHolder(defineType)
    }

    @Deprecated("Avoid using CharArray due to efficiency issues")
    fun <T> output(
        name: String,
        refNode: Identified,
        refName: String?,
        path: String?,
        content: CharArray,
        nsContext: Iterable<Namespace>,
        serializer: KSerializer<T>
    ): OutputRef<T> {
        @Suppress("DEPRECATION")
        @OptIn(XmlUtilInternal::class)
        return output(name, refNode, refName, path, content, SimpleNamespaceContext(nsContext), serializer)
    }

    fun <T> output(
        name: String,
        refNode: Identified,
        refName: String?,
        path: String?,
        content: String?,
        nsContext: Iterable<Namespace>,
        serializer: KSerializer<T>
    ): OutputRef<T> {
        @OptIn(XmlUtilInternal::class)
        return output(name, refNode, refName, path, content, SimpleNamespaceContext(nsContext), serializer)
    }

    @Deprecated("Avoid using CharArray due to efficiency issues")
    fun <T> output(
        name: String,
        refNode: Identified,
        refName: String?,
        path: String?,
        content: CharArray,
        nsContext: IterableNamespaceContext,
        serializer: KSerializer<T>
    ): OutputRef<T> {

        modelBuilder.results.add(XmlResultType(name, "/$name/node()"))
        @Suppress("DEPRECATION")
        modelBuilder.exports.add(XmlDefineType(name, refNode, refName, path, content, nsContext))
        with (modelBuilder.rootBuilder) {
            modelBuilder.ensureChildId() // Ensure there is an id for the composite model
            modelBuilder.ensureId()// Ensure an id for the activity itself
        }
        return ChildOutputRefImpl(Identifier(requireNotNull(modelBuilder.id)), name, serializer)
    }

    fun <T> output(
        name: String,
        refNode: Identified,
        refName: String?,
        path: String?,
        content: String?,
        nsContext: IterableNamespaceContext,
        serializer: KSerializer<T>
    ): OutputRef<T> {

        modelBuilder.results.add(XmlResultType(name, "/$name/node()"))
        modelBuilder.exports.add(XmlDefineType(name, refNode, refName, path, content, nsContext))
        with (modelBuilder.rootBuilder) {
            modelBuilder.ensureChildId() // Ensure there is an id for the composite model
            modelBuilder.ensureId()// Ensure an id for the activity itself
        }
        return ChildOutputRefImpl(Identifier(requireNotNull(modelBuilder.id)), name, serializer)
    }

}

@Deprecated("Avoid using CharArray due to efficiency issues")
inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.input(
    name: String,
    refNode: OutputRef<T>,
    path: String? = null,
    content: CharArray,
    nsContext: Iterable<Namespace> = emptyList(),
): InputRef<T> {
    @Suppress("DEPRECATION")
    return input(name, refNode.nodeRef, refNode.propertyName, path, content, nsContext, serializer())
}

inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.input(
    name: String,
    refNode: OutputRef<T>,
    path: String? = null,
    content: String? = null,
    nsContext: Iterable<Namespace> = emptyList(),
): InputRef<T> {
    return input(name, refNode.nodeRef, refNode.propertyName, path, content, nsContext, serializer())
}

@Deprecated("Avoid using CharArray due to efficiency issues")
inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.input(
    name: String,
    refNode: DataNodeHandle<T>,
    path: String? = null,
    content: CharArray,
    nsContext: Iterable<Namespace> = emptyList(),
): InputRef<T> {
    @Suppress("DEPRECATION")
    return input(name, refNode, refNode.propertyName, path, content, nsContext, serializer())
}

inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.input(
    name: String,
    refNode: DataNodeHandle<T>,
    path: String? = null,
    content: String? = null,
    nsContext: Iterable<Namespace> = emptyList(),
): InputRef<T> {
    return input(name, refNode, refNode.propertyName, path, content, nsContext, serializer())
}

@Deprecated("Avoid using CharArray due to efficiency issues")
inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.input(
    name: String,
    refNode: Identified,
    refName: String? = null,
    path: String? = null,
    content: CharArray,
    nsContext: Iterable<Namespace> = emptyList(),
): InputRef<T> {
    @Suppress("DEPRECATION")
    return input(name, refNode, refName, path, content, nsContext, serializer())
}

inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.input(
    name: String,
    refNode: Identified,
    refName: String? = null,
    path: String? = null,
    content: String? = null,
    nsContext: Iterable<Namespace> = emptyList(),
): InputRef<T> {
    return input(name, refNode, refName, path, content, nsContext, serializer())
}

@Deprecated("Avoid using CharArray due to efficiency issues")
inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.output(
    name: String,
    refNode: DataNodeHandle<T>,
    path: String? = null,
    content: CharArray,
    nsContext: Iterable<Namespace> = emptyList()
): OutputRef<T> {
    @Suppress("DEPRECATION")
    return output(name, refNode, refNode.propertyName, path, content, nsContext, serializer())
}

inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.output(
    name: String,
    refNode: DataNodeHandle<T>,
    path: String? = null,
    content: String? = null,
    nsContext: Iterable<Namespace> = emptyList()
): OutputRef<T> {
    return output(name, refNode, refNode.propertyName, path, content, nsContext, serializer())
}

inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.output(
    name: String,
    refNode: Identified,
    refName: String? = null,
    path: String? = null,
    content: CharArray,
    nsContext: Iterable<Namespace> = emptyList()
): OutputRef<T> {
    @Suppress("DEPRECATION")
    return output(name, refNode, refName, path, content, nsContext, serializer())
}

inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.output(
    name: String,
    refNode: Identified,
    refName: String? = null,
    path: String? = null,
    content: String? = null,
    nsContext: Iterable<Namespace> = emptyList()
): OutputRef<T> {
    return output(name, refNode, refName, path, content, nsContext, serializer())
}

@Deprecated("Avoid using CharArray due to efficiency issues")
inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.output(
    name: String,
    refNode: OutputRef<T>,
    path: String? = null,
    content: CharArray,
    nsContext: Iterable<Namespace> = emptyList()
): OutputRef<T> {
    @Suppress("DEPRECATION")
    return output(name, refNode.nodeRef, refNode.propertyName, path, content, nsContext, serializer())
}

inline fun <AIC : ActivityInstanceContext, reified T> ICompositeModelBuilderContext<AIC>.output(
    name: String,
    refNode: OutputRef<T>,
    path: String? = null,
    content: String? = null,
    nsContext: Iterable<Namespace> = emptyList()
): OutputRef<T> {
    return output(name, refNode.nodeRef, refNode.propertyName, path, content, nsContext, serializer())
}

data class InputRefImpl<T>(
    override val nodeRef: Identified?,
    override val propertyName: String,
    override val serializer: DeserializationStrategy<T>
) : InputRef<T> {
    constructor(propertyName: String, serializer: DeserializationStrategy<T>) :
        this(null, propertyName, serializer)
}

@ConsistentCopyVisibility
data class ChildOutputRefImpl<T> internal constructor(
    override val nodeRef: Identified,
    override val propertyName: String,
    override val serializer: KSerializer<T>
) : OutputRef<T>
