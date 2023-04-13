package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.KSerializer
import nl.adaptivity.process.processModel.CompositeActivity
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.name
import nl.adaptivity.process.util.Identified
import kotlin.reflect.KProperty

interface IModelBuilderContextDelegates {
    val modelBuilder: ProcessModel.Builder

    operator fun <I> EventNodeHolder<I>.provideDelegate(thisRef: Nothing?, property: KProperty<*>): DataNodeHandle<I> {
        addNodeToModel(builder, property)
        val outputName = builder.results.singleOrNull()?.name ?: ""
        return DataNodeHandleImpl(builder.id!!, outputName, serializer)
    }

    operator fun <I: Any, O: Any> RunnableActivity.Builder<I, O, *>.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): DataNodeHandle<O> {
        addNodeToModel(this, property)
        val outputName = results.singleOrNull()?.name ?: ""
        return DataNodeHandleImpl(id!!, outputName, this.outputSerializer as KSerializer<O>)
    }

    fun addNodeToModel(
        nodeBuilder: ProcessNode.Builder,
        property: KProperty<*>
    ) {
        if (nodeBuilder.id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) nodeBuilder.id = property.name
        with(modelBuilder) {
            if (nodeBuilder is CompositeActivity.ModelBuilder) {
                modelBuilder.rootBuilder.childModels.add(nodeBuilder.ensureChildId())
            }

            nodes.add(nodeBuilder.ensureId())
        }
    }

    operator fun ProcessNode.Builder.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): NodeHandle<Unit> {
        addNodeToModel(this, property)

        return NonActivityNodeHandleImpl(id!!)
    }


    operator fun <T: Identified> T.getValue(
        thisRef: Nothing?,
        property: KProperty<*>
    ): T = this

}
