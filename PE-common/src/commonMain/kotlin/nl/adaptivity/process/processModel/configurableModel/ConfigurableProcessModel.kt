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

package nl.adaptivity.process.processModel.configurableModel

import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class ConfigurationDsl

@ConfigurationDsl
abstract class ConfigurableProcessModel<NodeT : ProcessNode>(
    override val name: String? = null,
    override val owner: Principal,
    override val uuid: UUID
                                                            ) : RootProcessModel<NodeT>, ConfigurableNodeContainer {


    class NodeDelegate<T : Identifiable>(override val id: String) : ReadOnlyProperty<ConfigurableProcessModel<*>, T>,
                                                                    Identifiable {
        override fun getValue(thisRef: ConfigurableProcessModel<*>, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            return when {
                thisRef.configurationBuilder != null -> this
                else                                 -> thisRef.model.getNode(Identifier(id))
            } as T // Really nasty hack to allow node references to be used at definition time
        }
    }

    class ChildDelegate<T : Identifiable>(override val id: String) : ReadOnlyProperty<ConfigurableProcessModel<*>, T>,
                                                                     Identifiable {
        override fun getValue(thisRef: ConfigurableProcessModel<*>, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            return when {
                thisRef.configurationBuilder != null -> this
                else                                 -> thisRef.model.getChildModel(this)
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
        return RootProcessModelBase.Builder(this)
    }

    private var _configurationBuilder: RootProcessModel.Builder? = null
    override val configurationBuilder: RootProcessModel.Builder
        get() {
            return _configurationBuilder ?: if (_model==null) RootProcessModelBase.Builder().apply {
                _configurationBuilder = this;
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
            factory(configurationBuilder).also { _model = it }
        }
    }


    operator fun ProcessNode.Builder.provideDelegate(
        thisRef: ConfigurableProcessModel<*>,
        property: KProperty<*>
                                                    ): Identifier {
        val modelBuilder = configurationBuilder
        val nodeBuilder = this
        if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
        with(modelBuilder) {
            if (nodeBuilder is CompositeActivity.Builder) {
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


    inline operator fun <T : ConfigurableCompositeActivity> T.provideDelegate(
        thisRef: ConfigurableProcessModel<*>,
        property: KProperty<*>
                                                                             ): T {
        setIdIfEmpty(property.name)
        return this
    }

    inline operator fun <T : ConfigurableCompositeActivity> T.getValue(thisRef: ConfigurableProcessModel<*>, property: KProperty<*>): T =
        this

    @ConfigurationDsl
    protected abstract inner class ConfigurableCompositeActivity(
        predecessor: Identified,
        childId: String? = null,
        id: String? = null
                                                                ) :
        Identified,
        ConfigurableNodeContainer /*: ChildProcessModel.Builder<ExecutableProcessNode, ExecutableModelCommon>*/ {

        private inline fun rootBuilder() = this@ConfigurableProcessModel.configurationBuilder

        override val configurationBuilder: CompositeActivity.Builder = ActivityBase.CompositeActivityBuilder(
            rootBuilder(),
            childId = childId,
            id = id,
            predecessor = predecessor
                                                                                                            )

        init {
            rootBuilder().childModels.add(configurationBuilder)
            rootBuilder().nodes.add(configurationBuilder)
        }

        var childId: Identifier
            get() = Identifier(with(rootBuilder()) { configurationBuilder.ensureChildId() }.childId!!)
            set(value) {
                configurationBuilder.childId = value.id
            }

        override var id: String
            get() = with(rootBuilder()) { configurationBuilder.ensureId().id!! }
            set(value) {
                configurationBuilder.id = id
            }

        fun setIdIfEmpty(value: String) {
            if (configurationBuilder.id == null) {
                configurationBuilder.id = value
            }
        }

        operator fun ProcessNode.Builder.provideDelegate(
            thisRef: ConfigurableCompositeActivity,
            property: KProperty<*>
                                                        ): Identifier {
            val modelBuilder = configurationBuilder
            val nodeBuilder = this
            if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
            with(modelBuilder) {
                if (nodeBuilder is CompositeActivity.Builder) {
                    modelBuilder.rootBuilder.childModels.add(nodeBuilder.ensureChildId())
                }

                nodes.add(nodeBuilder.ensureId())
            }
            return Identifier(id!!)
        }

        protected inline operator fun Identifier.getValue(
            thisRef: ConfigurableCompositeActivity,
            property: KProperty<*>
                                                         ): Identifier = this

    }

}

@ConfigurationDsl
interface ConfigurableNodeContainer {
    /**
     * Property to access the builder that allows for configuration. This is only valid for as long as
     * the model has not been initialised. After initialisation accessing this property should throw
     * an exception.
     */
    val configurationBuilder: ProcessModel.Builder

}

