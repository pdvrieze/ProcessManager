/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.engine

import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.processModel.ChildProcessModel
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("NOTHING_TO_INLINE")
internal abstract class ConfigurableModel(
    override val name: String? = null,
    override val owner: Principal = EngineTestData.principal,
    override val uuid: UUID = UUID.randomUUID())
    :
    RootProcessModel {

    class NodeDelegate<T : Identifiable>(override val id: String) : ReadOnlyProperty<ConfigurableModel, T>, Identifiable {
        override fun getValue(thisRef: ConfigurableModel, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            return when {
                thisRef.builder != null -> this
                else                    -> thisRef.model.getNode(id)
            } as T // Really nasty hack to allow node references to be used at definition time
        }
    }

    class ChildDelegate<T : Identifiable>(override val id: String) : ReadOnlyProperty<ConfigurableModel, T>, Identifiable {
        override fun getValue(thisRef: ConfigurableModel, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            return when {
                thisRef.builder != null -> this
                else                    -> thisRef.model.getChildModel(this)
            } as T // Really nasty hack to allow node references to be used at definition time
        }
    }

    inner class NodeBinder {
        inline operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = nodeRef(property.name)
    }

    inner class ChildBinder {
        inline operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = childRef(property.name)
    }

    override fun builder(): RootProcessModel.Builder {
        return builder
    }

    private var _builder: ExecutableProcessModel.Builder? = null
    private val builder: ExecutableProcessModel.Builder
        get() {
            return _builder ?: if (!_model.isInitialized()) ExecutableProcessModel.Builder().apply {
                _builder = this;
                owner = this@ConfigurableModel.owner
                name = this@ConfigurableModel.name
            } else throw IllegalStateException("The model has already been built")
        }

    private val _model: Lazy<ExecutableProcessModel> = lazy {
        builder.also {
            if (it.uuid==null) { it.uuid == UUID.randomUUID() }
        }.build(false).apply { _builder = null }
    }

    private val model: ExecutableProcessModel get() = _model.value

    override val rootModel get() = model

    override fun copy(imports: Collection<IXmlResultType>,
                      exports: Collection<IXmlDefineType>,
                      nodes: Collection<ExecutableProcessNode>,
                      name: String?,
                      uuid: UUID?,
                      roles: Set<String>,
                      owner: nl.adaptivity.util.security.Principal,
                      childModels: Collection<ChildProcessModel>): ExecutableProcessModel {
        return ExecutableProcessModel.Builder(nodes.map { it.builder() }, emptySet(), name, -1L, owner, roles,
                                              uuid).also { builder ->
            builder.childModels.replaceBy(childModels.map { it.builder(builder) })
        }.build(false)
    }


    operator fun ExecutableProcessNode.Builder.provideDelegate(thisRef: ConfigurableModel,
                                                               property: KProperty<*>): Identifier {
        val modelBuilder = builder
        val nodeBuilder = this
        if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
        with(modelBuilder) {
            if (nodeBuilder is ExecutableActivity.ChildModelBuilder) {
                childModels.add(nodeBuilder.ensureChildId())
            }

            nodes.add(nodeBuilder.ensureId())
        }
        return Identifier(id!!)
    }

    protected operator inline fun Identifier.getValue(thisRef: ConfigurableModel,
                                                      property: KProperty<*>): Identifier = this

    inline fun childRef(childId: String) = lazy { model.getChildModel(Identifier(childId)) }
    inline val childRef get() = ChildBinder()
    inline fun nodeRef(nodeId: String) = lazy { (model.modelNodes.asSequence() + model.childModels.asSequence().flatMap { it.modelNodes.asSequence() }).firstOrNull { it.id == nodeId } }
    inline val nodeRef get() = NodeBinder()
    inline protected val startNode get() = ExecutableStartNode.Builder()
    inline protected fun startNode(config: ExecutableStartNode.Builder.() -> Unit) = ExecutableStartNode.Builder().apply(
        config)

    inline protected fun activity(predecessor: Identified) = ExecutableActivity.Builder(predecessor = predecessor)
    inline protected fun activity(predecessor: Identified,
                                  config: ExecutableActivity.Builder.() -> Unit) = ExecutableActivity.Builder(
        predecessor = predecessor).apply(config)

    inline protected fun compositeActivity(predecessor: Identified) = ExecutableActivity.ChildModelBuilder(builder!!,
                                                                                                           predecessor = predecessor)

    inline protected fun compositeActivity(predecessor: Identified,
                                           config: ExecutableActivity.ChildModelBuilder.() -> Unit) = ExecutableActivity.ChildModelBuilder(
        builder, predecessor = predecessor).apply(config)

    inline protected fun split(predecessor: Identified) = ExecutableSplit.Builder(predecessor = predecessor)
    inline protected fun split(predecessor: Identified,
                               config: ExecutableSplit.Builder.() -> Unit) = ExecutableSplit.Builder(
        predecessor = predecessor).apply(config)

    inline protected fun join(vararg predecessors: Identified) = ExecutableJoin.Builder(
        predecessors = Arrays.asList(*predecessors))

    inline protected fun join(predecessors: Collection<Identified>) = ExecutableJoin.Builder(
        predecessors = predecessors)

    inline protected fun join(vararg predecessors: Identified,
                              config: ExecutableJoin.Builder.() -> Unit) = ExecutableJoin.Builder(
        predecessors = Arrays.asList(*predecessors)).apply(config)

    inline protected fun join(predecessors: Collection<Identified>,
                              config: ExecutableJoin.Builder.() -> Unit) = ExecutableJoin.Builder(
        predecessors = predecessors).apply(config)

    inline protected fun endNode(predecessor: Identified) = ExecutableEndNode.Builder(predecessor = predecessor)
    inline protected fun endNode(predecessor: Identified,
                                 config: ExecutableEndNode.Builder.() -> Unit) = ExecutableEndNode.Builder(
        predecessor = predecessor).apply(config)

    override val ref get() = model.ref

    override fun getNode(nodeId: Identifiable) = model.getNode(nodeId)

    override val modelNodes: List<ExecutableProcessNode> get() = model.modelNodes

    override val childModels get() = model.childModels

    override fun getChildModel(childId: Identifiable) = model.getChildModel(childId)

    override val roles get() = model.roles

    override val imports get() = model.imports

    override val exports get() = model.exports


    inline operator fun <T : CompositeActivity> T.provideDelegate(thisRef: ConfigurableModel,
                                                                  property: KProperty<*>): T {
        setIdIfEmpty(property.name)
        return this
    }

    inline operator fun <T : CompositeActivity> T.getValue(thisRef: ConfigurableModel, property: KProperty<*>): T = this


    protected abstract inner class CompositeActivity(predecessor: Identified,
                                                     childId: String? = null,
                                                     id: String? = null) : Identified /*: ChildProcessModel.Builder<ExecutableProcessNode, ExecutableModelCommon>*/ {

        private inline fun rootBuilder() = this@ConfigurableModel.builder

        private val builder: ExecutableActivity.ChildModelBuilder = ExecutableActivity.ChildModelBuilder(rootBuilder(),
                                                                                                         childId = childId,
                                                                                                         id = id,
                                                                                                         predecessor = predecessor)

        init {
            rootBuilder().childModels.add(builder)
            rootBuilder().nodes.add(builder)
        }

        var childId: Identifier
            get() = Identifier(with(rootBuilder()) { builder.ensureChildId() }.childId!!)
            set(value) {
                builder.childId = value.id
            }

        override var id: String
            get() = with(rootBuilder()) { builder.ensureId().id!! }
            set(value) {
                builder.id = id
            }

        fun setIdIfEmpty(value: String) {
            if (builder.id == null) {
                builder.id = value
            }
        }

        operator fun ExecutableProcessNode.Builder.provideDelegate(thisRef: CompositeActivity,
                                                                   property: KProperty<*>): Identifier {
            val modelBuilder = builder
            val nodeBuilder = this
            if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
            with(modelBuilder) {
                if (nodeBuilder is ExecutableActivity.ChildModelBuilder) {
                    modelBuilder.rootBuilder.childModels.add(nodeBuilder.ensureChildId())
                }

                nodes.add(nodeBuilder.ensureId())
            }
            return Identifier(id!!)
        }

        protected operator inline fun Identifier.getValue(thisRef: CompositeActivity,
                                                          property: KProperty<*>): Identifier = this

        inline protected val startNode get() = ExecutableStartNode.Builder()
        inline protected fun startNode(config: ExecutableStartNode.Builder.() -> Unit) = ExecutableStartNode.Builder().apply(
            config)

        inline protected fun activity(predecessor: Identified) = ExecutableActivity.Builder(predecessor = predecessor)
        inline protected fun activity(predecessor: Identified,
                                      config: ExecutableActivity.Builder.() -> Unit) = ExecutableActivity.Builder(
            predecessor = predecessor).apply(config)

        inline protected fun compositeActivity(predecessor: Identified) = ExecutableActivity.ChildModelBuilder(
            rootBuilder(), predecessor = predecessor)

        inline protected fun compositeActivity(predecessor: Identified,
                                               config: ExecutableActivity.ChildModelBuilder.() -> Unit) = ExecutableActivity.ChildModelBuilder(
            rootBuilder(), predecessor = predecessor).apply(config)

        inline protected fun split(predecessor: Identified) = ExecutableSplit.Builder(predecessor = predecessor)
        inline protected fun split(predecessor: Identified,
                                   config: ExecutableSplit.Builder.() -> Unit) = ExecutableSplit.Builder(
            predecessor = predecessor).apply(config)

        inline protected fun join(vararg predecessors: Identified) = ExecutableJoin.Builder(
            predecessors = Arrays.asList(*predecessors))

        inline protected fun join(predecessors: Collection<Identified>) = ExecutableJoin.Builder(
            predecessors = predecessors)

        inline protected fun join(vararg predecessors: Identified,
                                  config: ExecutableJoin.Builder.() -> Unit) = ExecutableJoin.Builder(
            predecessors = Arrays.asList(*predecessors)).apply(config)

        inline protected fun join(predecessors: Collection<Identified>,
                                  config: ExecutableJoin.Builder.() -> Unit) = ExecutableJoin.Builder(
            predecessors = predecessors).apply(config)

        inline protected fun endNode(predecessor: Identified) = ExecutableEndNode.Builder(predecessor = predecessor)
        inline protected fun endNode(predecessor: Identified,
                                     config: ExecutableEndNode.Builder.() -> Unit) = ExecutableEndNode.Builder(
            predecessor = predecessor).apply(config)


    }
}
