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

package nl.adaptivity.process.processModel

import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class ConfigurableProcessModel<NodeT : ProcessNode>(
    override val name: String? = null,
    override val owner: Principal,
    override val uuid: UUID
                                                        ) : RootProcessModel<NodeT> {


    class NodeDelegate<T : Identifiable>(override val id: String) : ReadOnlyProperty<ConfigurableProcessModel<*>, T>,
                                                                    Identifiable {
        override fun getValue(thisRef: ConfigurableProcessModel<*>, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            return when {
                thisRef.builder != null -> this
                else                    -> thisRef.model.getNode(Identifier(id))
            } as T // Really nasty hack to allow node references to be used at definition time
        }
    }

    class ChildDelegate<T : Identifiable>(override val id: String) : ReadOnlyProperty<ConfigurableProcessModel<*>, T>,
                                                                     Identifiable {
        override fun getValue(thisRef: ConfigurableProcessModel<*>, property: KProperty<*>): T {
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

    fun childRef(childId: String) = lazy { model.getChildModel(Identifier(childId)) }
    inline val childRef get() = ChildBinder()
    fun nodeRef(nodeId: String) =
        lazy { (model.modelNodes.asSequence() + model.childModels.asSequence().flatMap { it.modelNodes.asSequence() }).firstOrNull { it.id == nodeId } }

    inline val nodeRef get() = NodeBinder()

    override fun builder(): RootProcessModel.Builder {
        return builder
    }

    private var _builder: RootProcessModel.Builder? = null
    protected val builder: RootProcessModel.Builder
        get() {
            return _builder ?: if (_model==null) XmlProcessModel.Builder().apply {
                _builder = this;
                owner = this@ConfigurableProcessModel.owner
                name = this@ConfigurableProcessModel.name
                uuid = this@ConfigurableProcessModel.uuid
            } else throw IllegalStateException("The model has already been built")
        }

    private var _model: RootProcessModel<NodeT>? = null

    protected val model: RootProcessModel<NodeT>
        get() = _model ?: throw IllegalStateException("Model not initialised")

    override val ref get() = model.ref

    override fun getNode(nodeId: Identifiable) = model.getNode(nodeId)

    override val modelNodes: List<NodeT> get() = model.modelNodes

    override val childModels get() = model.childModels

    override fun getChildModel(childId: Identifiable) = model.getChildModel(childId)

    override val roles get() = model.roles

    override val imports get() = model.imports

    override val exports get() = model.exports

    fun <R: RootProcessModel<NodeT>>buildModel(factory: (RootProcessModel.Builder)->R):R {
        return (_model as R?) ?: run {
            factory(builder).also { _model = it }
        }
    }


    operator fun ProcessNode.IBuilder.provideDelegate(
        thisRef: ConfigurableProcessModel<*>,
        property: KProperty<*>
                                                     ): Identifier {
        val modelBuilder = builder
        val nodeBuilder = this
        if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
        with(modelBuilder) {
            if (nodeBuilder is Activity.CompositeActivityBuilder) {
                childModels.add(nodeBuilder.ensureChildId())
            }

            nodes.add(nodeBuilder.ensureId())
        }
        return Identifier(id!!)
    }

    protected inline operator fun Identifier.getValue(
        thisRef: ConfigurableProcessModel<*>,
        property: KProperty<*>
                                                     ): Identifier = this

    protected inline val startNode get():StartNode.Builder = StartNodeBase.Builder()
    protected inline fun startNode(config: StartNode.Builder.() -> Unit): StartNode.Builder =
        StartNodeBase.Builder().apply(
            config
                                    )

    protected inline fun activity(predecessor: Identified): Activity.Builder =
        ActivityBase.Builder(predecessor = predecessor)

    protected inline fun activity(
        predecessor: Identified,
        config: Activity.Builder.() -> Unit
                                 ): Activity.Builder =
        ActivityBase.Builder(
            predecessor = predecessor
                            ).apply(config)

    protected inline fun compositeActivity(predecessor: Identified): Activity.CompositeActivityBuilder =
        ActivityBase.CompositeActivityBuilder(
            builder,
            predecessor = predecessor
                                            )

    protected inline fun compositeActivity(
        predecessor: Identified,
        config: Activity.CompositeActivityBuilder.() -> Unit
                                          ): Activity.CompositeActivityBuilder =
        ActivityBase.CompositeActivityBuilder(
            builder, predecessor = predecessor
                                            ).apply(config)

    protected inline fun split(predecessor: Identified): Split.Builder = SplitBase.Builder(predecessor = predecessor)
    protected inline fun split(
        predecessor: Identified,
        config: Split.Builder.() -> Unit
                              ) : Split.Builder = SplitBase.Builder(
        predecessor = predecessor
                                                                  ).apply(config)

    protected inline fun join(vararg predecessors: Identified): Join.Builder = JoinBase.Builder(
        predecessors = predecessors.toList()
                                                                                               )

    protected inline fun join(predecessors: Collection<Identified>) : Join.Builder = JoinBase.Builder(
        predecessors = predecessors
                                                                                                     )

    protected inline fun join(
        vararg predecessors: Identified,
        config: Join.Builder.() -> Unit
                             ): Join.Builder = JoinBase.Builder(
        predecessors = predecessors.toList()
                                                              ).apply(config)

    protected inline fun join(
        predecessors: Collection<Identified>,
        config: Join.Builder.() -> Unit
                             ) : Join.Builder = JoinBase.Builder(
        predecessors = predecessors
                                                               ).apply(config)

    protected inline fun endNode(predecessor: Identified): EndNode.Builder = EndNodeBase.Builder(predecessor = predecessor)
    protected inline fun endNode(
        predecessor: Identified,
        config: EndNode.Builder.() -> Unit
                                ): EndNode.Builder = EndNodeBase.Builder(
        predecessor = predecessor
                                                                       ).apply(config)


    inline operator fun <T : CompositeActivity> T.provideDelegate(
        thisRef: ConfigurableProcessModel<*>,
        property: KProperty<*>
                                                                 ): T {
        setIdIfEmpty(property.name)
        return this
    }

    inline operator fun <T : CompositeActivity> T.getValue(thisRef: ConfigurableProcessModel<*>, property: KProperty<*>): T =
        this


    protected abstract inner class CompositeActivity(
        predecessor: Identified,
        childId: String? = null,
        id: String? = null
                                                    ) :
        Identified /*: ChildProcessModel.Builder<ExecutableProcessNode, ExecutableModelCommon>*/ {

        private inline fun rootBuilder() = this@ConfigurableProcessModel.builder

        private val builder: Activity.CompositeActivityBuilder = ActivityBase.CompositeActivityBuilder(
            rootBuilder(),
            childId = childId,
            id = id,
            predecessor = predecessor
                                                                                                     )

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

        operator fun ProcessNode.IBuilder.provideDelegate(
            thisRef: CompositeActivity,
            property: KProperty<*>
                                                         ): Identifier {
            val modelBuilder = builder
            val nodeBuilder = this
            if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
            with(modelBuilder) {
                if (nodeBuilder is Activity.CompositeActivityBuilder) {
                    modelBuilder.rootBuilder.childModels.add(nodeBuilder.ensureChildId())
                }

                nodes.add(nodeBuilder.ensureId())
            }
            return Identifier(id!!)
        }

        protected inline operator fun Identifier.getValue(
            thisRef: CompositeActivity,
            property: KProperty<*>
                                                         ): Identifier = this

        protected inline val startNode get(): StartNode.Builder = StartNodeBase.Builder()
        protected inline fun startNode(config: StartNode.Builder.() -> Unit): StartNode.Builder =
            StartNodeBase.Builder().apply(
                config
                                        )

        protected inline fun activity(predecessor: Identified): Activity.Builder =
            ActivityBase.Builder(predecessor = predecessor)
        protected inline fun activity(
            predecessor: Identified,
            config: Activity.Builder.() -> Unit
                                     ) : Activity.Builder = ActivityBase.Builder(
            predecessor = predecessor
                                                                                ).apply(config)

        protected inline fun compositeActivity(predecessor: Identified) : Activity.CompositeActivityBuilder = ActivityBase.CompositeActivityBuilder(
            this@ConfigurableProcessModel.builder, predecessor = predecessor
                                                                                                                                                  )

        protected inline fun compositeActivity(
            predecessor: Identified,
            config: Activity.CompositeActivityBuilder.() -> Unit
                                              ) : Activity.CompositeActivityBuilder = ActivityBase.CompositeActivityBuilder(
            this@ConfigurableProcessModel.builder, predecessor = predecessor
                                                                                                                          ).apply(config)

        protected inline fun split(predecessor: Identified) : Split.Builder = SplitBase.Builder(predecessor = predecessor)
        protected inline fun split(
            predecessor: Identified,
            config: Split.Builder.() -> Unit
                                  ) : Split.Builder = SplitBase.Builder(
            predecessor = predecessor
                                                                      ).apply(config)

        protected inline fun join(vararg predecessors: Identified) : Join.Builder = JoinBase.Builder(
            predecessors = predecessors.toList()
                                                                                                   )

        protected inline fun join(predecessors: Collection<Identified>) : Join.Builder = JoinBase.Builder(
            predecessors = predecessors
                                                                                                        )

        protected inline fun join(
            vararg predecessors: Identified,
            config: Join.Builder.() -> Unit
                                 ) : Join.Builder = JoinBase.Builder(
            predecessors = predecessors.toList()
                                                                   ).apply(config)

        protected inline fun join(
            predecessors: Collection<Identified>,
            config: Join.Builder.() -> Unit
                                 ) : Join.Builder = JoinBase.Builder(
            predecessors = predecessors
                                                                   ).apply(config)

        protected inline fun endNode(predecessor: Identified): EndNode.Builder = EndNodeBase.Builder(predecessor = predecessor)
        protected inline fun endNode(
            predecessor: Identified,
            config: EndNode.Builder.() -> Unit
                                    ) : EndNode.Builder = EndNodeBase.Builder(
            predecessor = predecessor
                                                                            ).apply(config)


    }

}
