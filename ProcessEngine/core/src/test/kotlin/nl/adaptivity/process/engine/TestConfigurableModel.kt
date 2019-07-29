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

package nl.adaptivity.process.engine

import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.processModel.*
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
internal abstract class TestConfigurableModel(
    name: String? = null,
    owner: Principal = EngineTestData.principal,
    uuid: UUID = UUID.randomUUID()
                                             ) :
    ConfigurableProcessModel<ExecutableProcessNode>(
        name,
        owner,
        uuid
                                                   ) {

    override val rootModel: ExecutableProcessModel by lazy { buildModel { it.uuid=null;ExecutableProcessModel(it, false)} }

    override fun copy(
        imports: Collection<IXmlResultType>,
        exports: Collection<IXmlDefineType>,
        nodes: Collection<ProcessNode>,
        name: String?,
        uuid: UUID?,
        roles: Set<String>,
        owner: Principal,
        childModels: Collection<ChildProcessModel<ExecutableProcessNode>>
                     ): ExecutableProcessModel {
        return ExecutableProcessModel.Builder(
            nodes.map { it.builder() }, emptySet(), name, -1L, owner, roles,
            uuid
                                             ).also { builder ->
            builder.childModels.replaceBy(childModels.map { it.builder(builder) })
        }.build(false)
    }


    operator fun ExecutableProcessNode.Builder.provideDelegate(
        thisRef: TestConfigurableModel,
        property: KProperty<*>
                                                              ): Identifier {
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

    protected operator inline fun Identifier.getValue(
        thisRef: TestConfigurableModel,
        property: KProperty<*>
                                                     ): Identifier = this

    inline protected val startNode get() = ExecutableStartNode.Builder()
    inline protected fun startNode(config: ExecutableStartNode.Builder.() -> Unit) =
        ExecutableStartNode.Builder().apply(
            config
                                           )

    inline protected fun activity(predecessor: Identified) = ExecutableActivity.Builder(predecessor = predecessor)
    inline protected fun activity(
        predecessor: Identified,
        config: ExecutableActivity.Builder.() -> Unit
                                 ) = ExecutableActivity.Builder(
        predecessor = predecessor
                                                               ).apply(config)

    inline protected fun compositeActivity(predecessor: Identified) = ExecutableActivity.ChildModelBuilder(
        builder!!,
        predecessor = predecessor
                                                                                                          )

    inline protected fun compositeActivity(
        predecessor: Identified,
        config: ExecutableActivity.ChildModelBuilder.() -> Unit
                                          ) = ExecutableActivity.ChildModelBuilder(
        builder, predecessor = predecessor
                                                                                  ).apply(config)

    inline protected fun split(predecessor: Identified) = ExecutableSplit.Builder(predecessor = predecessor)
    inline protected fun split(
        predecessor: Identified,
        config: ExecutableSplit.Builder.() -> Unit
                              ) = ExecutableSplit.Builder(
        predecessor = predecessor
                                                         ).apply(config)

    inline protected fun join(vararg predecessors: Identified) = ExecutableJoin.Builder(
        predecessors = Arrays.asList(*predecessors)
                                                                                       )

    inline protected fun join(predecessors: Collection<Identified>) = ExecutableJoin.Builder(
        predecessors = predecessors
                                                                                            )

    inline protected fun join(
        vararg predecessors: Identified,
        config: ExecutableJoin.Builder.() -> Unit
                             ) = ExecutableJoin.Builder(
        predecessors = Arrays.asList(*predecessors)
                                                       ).apply(config)

    inline protected fun join(
        predecessors: Collection<Identified>,
        config: ExecutableJoin.Builder.() -> Unit
                             ) = ExecutableJoin.Builder(
        predecessors = predecessors
                                                       ).apply(config)

    inline protected fun endNode(predecessor: Identified) = ExecutableEndNode.Builder(predecessor = predecessor)
    inline protected fun endNode(
        predecessor: Identified,
        config: ExecutableEndNode.Builder.() -> Unit
                                ) = ExecutableEndNode.Builder(
        predecessor = predecessor
                                                             ).apply(config)


    inline operator fun <T : CompositeActivity> T.provideDelegate(
        thisRef: TestConfigurableModel,
        property: KProperty<*>
                                                                 ): T {
        setIdIfEmpty(property.name)
        return this
    }

    inline operator fun <T : CompositeActivity> T.getValue(thisRef: TestConfigurableModel, property: KProperty<*>): T =
        this


    protected abstract inner class CompositeActivity(
        predecessor: Identified,
        childId: String? = null,
        id: String? = null
                                                    ) :
        Identified /*: ChildProcessModel.Builder<ExecutableProcessNode, ExecutableModelCommon>*/ {

        private inline fun rootBuilder() = this@TestConfigurableModel.builder

        private val builder: ExecutableActivity.ChildModelBuilder = ExecutableActivity.ChildModelBuilder(
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

        operator fun ExecutableProcessNode.Builder.provideDelegate(
            thisRef: CompositeActivity,
            property: KProperty<*>
                                                                  ): Identifier {
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

        protected operator inline fun Identifier.getValue(
            thisRef: CompositeActivity,
            property: KProperty<*>
                                                         ): Identifier = this

        inline protected val startNode get() = ExecutableStartNode.Builder()
        inline protected fun startNode(config: ExecutableStartNode.Builder.() -> Unit) =
            ExecutableStartNode.Builder().apply(
                config
                                               )

        inline protected fun activity(predecessor: Identified) = ExecutableActivity.Builder(predecessor = predecessor)
        inline protected fun activity(
            predecessor: Identified,
            config: ExecutableActivity.Builder.() -> Unit
                                     ) = ExecutableActivity.Builder(
            predecessor = predecessor
                                                                   ).apply(config)

        inline protected fun compositeActivity(predecessor: Identified) = ExecutableActivity.ChildModelBuilder(
            rootBuilder(), predecessor = predecessor
                                                                                                              )

        inline protected fun compositeActivity(
            predecessor: Identified,
            config: ExecutableActivity.ChildModelBuilder.() -> Unit
                                              ) = ExecutableActivity.ChildModelBuilder(
            rootBuilder(), predecessor = predecessor
                                                                                      ).apply(config)

        inline protected fun split(predecessor: Identified) = ExecutableSplit.Builder(predecessor = predecessor)
        inline protected fun split(
            predecessor: Identified,
            config: ExecutableSplit.Builder.() -> Unit
                                  ) = ExecutableSplit.Builder(
            predecessor = predecessor
                                                             ).apply(config)

        inline protected fun join(vararg predecessors: Identified) = ExecutableJoin.Builder(
            predecessors = Arrays.asList(*predecessors)
                                                                                           )

        inline protected fun join(predecessors: Collection<Identified>) = ExecutableJoin.Builder(
            predecessors = predecessors
                                                                                                )

        inline protected fun join(
            vararg predecessors: Identified,
            config: ExecutableJoin.Builder.() -> Unit
                                 ) = ExecutableJoin.Builder(
            predecessors = Arrays.asList(*predecessors)
                                                           ).apply(config)

        inline protected fun join(
            predecessors: Collection<Identified>,
            config: ExecutableJoin.Builder.() -> Unit
                                 ) = ExecutableJoin.Builder(
            predecessors = predecessors
                                                           ).apply(config)

        inline protected fun endNode(predecessor: Identified) = ExecutableEndNode.Builder(predecessor = predecessor)
        inline protected fun endNode(
            predecessor: Identified,
            config: ExecutableEndNode.Builder.() -> Unit
                                    ) = ExecutableEndNode.Builder(
            predecessor = predecessor
                                                                 ).apply(config)


    }
}
