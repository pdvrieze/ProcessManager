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
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurableNodeContainer
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlWriter
import net.devrieze.util.TypecheckingCollection

class RunnableActivity<I, O> : ActivityBase, ExecutableProcessNode {

    private val action: (I) -> O
    private val inputCombiner: InputCombiner<I>
    private val outputSerializer: SerializationStrategy<O>?
    override val condition: ExecutableCondition?

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    override val defines: List<DefineType<*>>
        get() = super.defines as List<DefineType<*>>

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override fun builder(): Activity.Builder {
        return Builder(this)
    }

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitGenericActivity(this)
    }

    constructor(
        builder: Builder<I, O>,
        buildHelper: ProcessModel.BuildHelper<*, *, *, *>
               ) : super(
        builder,
        buildHelper
                        ) {
        this.inputCombiner = builder.inputCombiner
        this.outputSerializer = builder.outputSerializer
        this.action = builder.action
        this.condition = builder.condition?.toExecutableCondition()
        if (builder.defines.any { it !is DefineType<*> }) {
            throw IllegalArgumentException("RunnableActivities don't support alternative defines")
        }
    }

    override fun serializeCondition(out: XmlWriter): Nothing {
        throw UnsupportedOperationException("Runnable Activities cannot be serialized")
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
        entryNo: Int
                                      ): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
        processInstanceBuilder.getChild(this, entryNo)?.let { return it }
        if (!isMultiInstance && entryNo > 1) {
            processInstanceBuilder.allChildren { it.node == this && it.entryNo != entryNo }.forEach {
                processInstanceBuilder.updateChild(it) {
                    invalidateTask(data)
                }
            }
        }
        return RunnableActivityInstance.BaseBuilder(
            this, predecessor.handle(),
            processInstanceBuilder,
            processInstanceBuilder.owner, entryNo
                                                   )
    }

    override fun takeTask(instance: ProcessNodeInstance.Builder<*, *>): Boolean = true

    fun getInputData(defines: List<ProcessData>): I {
        TODO("not implemented")
    }

    class Builder<I, O> : ActivityBase.BaseBuilder {

        var inputCombiner: InputCombiner<I> = InputCombiner()
        val outputSerializer: SerializationStrategy<O>?
        val action: (I) -> O

        constructor(
            refNode: Identified,
            refName: String,
            inputSerializer: DeserializationStrategy<I>,
            outputSerializer: SerializationStrategy<O>? = null,
            action: (I) -> O
                   ) : super() {
            this.outputSerializer = outputSerializer
            this.action = action
            defineInput("input", refNode, refName, inputSerializer)
        }

        constructor(activity: RunnableActivity<I, O>) : super(activity) {
            this.inputCombiner = activity.inputCombiner
            this.outputSerializer = activity.outputSerializer
            this.action = activity.action
        }

        override val defines: MutableCollection<IXmlDefineType>
            get() = TypecheckingCollection(DefineType::class, super.defines)

        fun <T> defineInput(name: String, refNode: Identified, valueName: String, deserializer: DeserializationStrategy<T>) {
            val defineType = DefineType(name, refNode, valueName, null, deserializer)
            defines.add(defineType)
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R {
            return visitor.visitGenericActivity(this)
        }
    }

    class DefineType<T>(
        private val name: String,
        private val refNode: Identified,
        private val valueName: String,
        private val path: String?,
        val deserializer: DeserializationStrategy<T>,
        pathNSContext: Iterable<Namespace> = emptyList()
                    ) : /*XPathHolder(name, path, null, pathNSContext),*/ IXmlDefineType {
        override val content: Nothing? get() = null
        override fun getRefNode(): String = refNode.id

        override fun setRefNode(value: String?): Nothing = throw UnsupportedOperationException("Immutable type")

        override fun getRefName(): String? = refName

        override fun setRefName(value: String?): Nothing = throw UnsupportedOperationException("Immutable type")

        override fun getName(): String = name

        override fun setName(value: String): Nothing = throw UnsupportedOperationException("Immutable type")

        override fun getPath(): String? = path

        override fun setPath(namespaceContext: Iterable<Namespace>, value: String?) : Nothing = throw UnsupportedOperationException("Immutable type")

        override val originalNSContext: Iterable<Namespace> get() = emptyList()

        override fun serialize(out: XmlWriter) : Nothing = throw UnsupportedOperationException("Cannot serialize coded define")
    }
}

class InputCombiner<T>(val impl: ((Map<String, Any?>)->T)? = null) {
    operator fun invoke(input: Map<String, Any?>): T {
        val impl = impl
        return when {
            impl!=null      -> impl(input)
            input.isEmpty() -> Unit as T
            input.size==1   -> input.values.single() as T
            else            -> throw UnsupportedOperationException("Cannot combine multiple inputs automatically")
        }

    }
}

fun <I, O> ConfigurableNodeContainer<ExecutableProcessNode>.runnableActivity(
    predecessor: Identified,
    inputRefNode: Identified,
    inputRefName: String,
    inputSerializer: DeserializationStrategy<I>,
    outputSerializer: SerializationStrategy<O>,
    action: (I) -> O
                                                                            ): RunnableActivity.Builder<I, O> =
    RunnableActivity.Builder(inputRefNode, inputRefName, inputSerializer, outputSerializer, action).also {
        it.predecessor = predecessor
    }
/*

fun ConfigurableNodeContainer<ExecutableProcessNode>.runnableActivity(
    predecessor: Identified,
    config: @ConfigurationDsl MessageActivity.Builder.() -> Unit
                                                                     ): MessageActivity.Builder =
    MessageActivityBase.Builder(predecessor = predecessor).apply(config)
*/

