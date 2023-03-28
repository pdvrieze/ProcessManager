package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xmlutil.Namespace

interface IModelBuilderContext<AIC: ActivityInstanceContext> : IModelBuilderContextDelegates<AIC> {
    val startNode: StartNode.Builder
        get() = StartNodeBase.Builder()

    @ConfigurationDsl
    fun startNode(config: @ConfigurationDsl() (StartNode.Builder.() -> Unit)): StartNode.Builder =
        StartNodeBase.Builder().apply(config)

    @ConfigurationDsl
    fun split(predecessor: Identified): Split.Builder =
        SplitBase.Builder().apply { this.predecessor = predecessor }

    @ConfigurationDsl
    fun split(
        predecessor: Identified,
        config: @ConfigurationDsl() (Split.Builder.() -> Unit)
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
        config: @ConfigurationDsl() (Join.Builder.() -> Unit)
    ): Join.Builder = join(*predecessors).apply(config)

    @ConfigurationDsl
    fun join(
        predecessors: Collection<Identified>,
        config: @ConfigurationDsl() (Join.Builder.() -> Unit)
    ): Join.Builder =join(predecessors).apply(config)

    fun endNode(predecessor: Identified): EndNode.Builder =
        EndNodeBase.Builder().apply {
            this.predecessor = predecessor
        }

    fun endNode(
        predecessor: Identified,
        config: @ConfigurationDsl() (EndNode.Builder.() -> Unit)
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
        config: @ConfigurationDsl() (EndNode.Builder.() -> Unit)
    ) {
        EndNodeBase.Builder().apply {
            this.id = name
            this.predecessor = predecessor
            config()
        }
    }

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

}

inline fun <AIC : ActivityInstanceContext, reified T> IModelBuilderContext<AIC>.processResult(
    name: String,
    refNode: OutputRef<T>,
    path: String? = null,
    content: CharArray? = null,
    nsContext: Iterable<Namespace> = emptyList()
): ProcessResultRef<T> {
    return processResult(name, refNode.nodeRef, refNode.propertyName, path, content, nsContext, serializer())
}

inline fun <AIC : ActivityInstanceContext, I> IModelBuilderContext<AIC>.processResult(
    name: String,
    refNode: ActivityHandle<I>,
    path: String? = null,
    content: CharArray? = null,
    nsContext: Iterable<Namespace> = emptyList()
): ProcessResultRef<I> {
    return processResult(name, refNode, refNode.propertyName, path, content, nsContext, refNode.serializer)
}

private data class ProcessResultRefImpl<T>(
    override val propertyName: String,
    override val serializer: KSerializer<T>
) : ProcessResultRef<T> {
    override val nodeRef: Nothing? get() = null
}
