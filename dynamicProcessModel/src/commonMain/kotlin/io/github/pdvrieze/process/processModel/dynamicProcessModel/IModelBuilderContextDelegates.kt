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

    operator fun <I: Any, O: Any> RunnableActivity.Builder<I, O, *>.provideDelegate(
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


    operator fun <T: Identified> T.getValue(
        thisRef: Nothing?,
        property: KProperty<*>
    ): T = this

}
