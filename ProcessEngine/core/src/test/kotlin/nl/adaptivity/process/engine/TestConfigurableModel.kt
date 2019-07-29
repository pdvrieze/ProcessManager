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
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import java.util.*
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

    override val rootModel: ExecutableProcessModel by lazy {
        buildModel {
            it.uuid = null;ExecutableProcessModel(
            it,
            false
                                                 )
        }
    }

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


    operator fun ProcessNode.IBuilder.provideDelegate(
        thisRef: TestConfigurableModel,
        property: KProperty<*>
                                                     ): Identifier {
        val modelBuilder = builder
        val nodeBuilder = this
        if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
        with(modelBuilder) {
            if (nodeBuilder is Activity.ChildModelBuilder) {
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

    inline protected val startNode get():StartNode.Builder = XmlStartNode.Builder()
    inline protected fun startNode(config: XmlStartNode.Builder.() -> Unit): StartNode.Builder =
        XmlStartNode.Builder().apply(
            config
                                 )

    inline protected fun activity(predecessor: Identified): Activity.Builder =
        XmlActivity.Builder(predecessor = predecessor)

    inline protected fun activity(
        predecessor: Identified,
        config: Activity.Builder.() -> Unit
                                 ): Activity.Builder =
        XmlActivity.Builder(
            predecessor = predecessor
                           ).apply(config)

    inline protected fun compositeActivity(predecessor: Identified): Activity.ChildModelBuilder =
        XmlActivity.ChildModelBuilder(
            builder,
            predecessor = predecessor
                                     )

    inline protected fun compositeActivity(
        predecessor: Identified,
        config: Activity.ChildModelBuilder.() -> Unit
                                          ): Activity.ChildModelBuilder =
        XmlActivity.ChildModelBuilder(
            builder, predecessor = predecessor
                                     ).apply(config)

    inline protected fun split(predecessor: Identified): Split.Builder = XmlSplit.Builder(predecessor = predecessor)
    inline protected fun split(
        predecessor: Identified,
        config: Split.Builder.() -> Unit
                              ) : Split.Builder = XmlSplit.Builder(
    predecessor = predecessor
    ).apply(config)

    inline protected fun join(vararg predecessors: Identified): Join.Builder = XmlJoin.Builder(
        predecessors = Arrays.asList(*predecessors)
                                                                             )

    inline protected fun join(predecessors: Collection<Identified>) : Join.Builder = XmlJoin.Builder(
        predecessors = predecessors
                                                                                  )

    inline protected fun join(
        vararg predecessors: Identified,
        config: Join.Builder.() -> Unit
                             ): Join.Builder = XmlJoin.Builder(
        predecessors = Arrays.asList(*predecessors)
                                             ).apply(config)

    inline protected fun join(
        predecessors: Collection<Identified>,
        config: Join.Builder.() -> Unit
                             ) : Join.Builder = XmlJoin.Builder(
        predecessors = predecessors
                                             ).apply(config)

    inline protected fun endNode(predecessor: Identified): EndNode.Builder = XmlEndNode.Builder(predecessor = predecessor)
    inline protected fun endNode(
        predecessor: Identified,
        config: EndNode.Builder.() -> Unit
                                ): EndNode.Builder = XmlEndNode.Builder(
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

        private val builder: Activity.ChildModelBuilder = XmlActivity.ChildModelBuilder(
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
                if (nodeBuilder is Activity.ChildModelBuilder) {
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

        inline protected val startNode get(): StartNode.Builder = XmlStartNode.Builder()
        inline protected fun startNode(config: StartNode.Builder.() -> Unit): StartNode.Builder =
            XmlStartNode.Builder().apply(
                config
                                     )

        inline protected fun activity(predecessor: Identified): Activity.Builder = XmlActivity.Builder(predecessor = predecessor)
        inline protected fun activity(
            predecessor: Identified,
            config: Activity.Builder.() -> Unit
                                     ) : Activity.Builder = ActivityBase.Builder(
            predecessor = predecessor
                                                         ).apply(config)

        inline protected fun compositeActivity(predecessor: Identified) : Activity.ChildModelBuilder = XmlActivity.ChildModelBuilder(
            rootBuilder(), predecessor = predecessor
                                                                                                    )

        inline protected fun compositeActivity(
            predecessor: Identified,
            config: Activity.ChildModelBuilder.() -> Unit
                                              ) : Activity.ChildModelBuilder = XmlActivity.ChildModelBuilder(
            rootBuilder(), predecessor = predecessor
                                                                            ).apply(config)

        inline protected fun split(predecessor: Identified) : Split.Builder = XmlSplit.Builder(predecessor = predecessor)
        inline protected fun split(
            predecessor: Identified,
            config: Split.Builder.() -> Unit
                                  ) : Split.Builder = XmlSplit.Builder(
            predecessor = predecessor
                                                   ).apply(config)

        inline protected fun join(vararg predecessors: Identified) : Join.Builder = XmlJoin.Builder(
            predecessors = Arrays.asList(*predecessors)
                                                                                 )

        inline protected fun join(predecessors: Collection<Identified>) : Join.Builder = XmlJoin.Builder(
            predecessors = predecessors
                                                                                      )

        inline protected fun join(
            vararg predecessors: Identified,
            config: Join.Builder.() -> Unit
                                 ) : Join.Builder = XmlJoin.Builder(
            predecessors = Arrays.asList(*predecessors)
                                                 ).apply(config)

        inline protected fun join(
            predecessors: Collection<Identified>,
            config: Join.Builder.() -> Unit
                                 ) : Join.Builder = XmlJoin.Builder(
            predecessors = predecessors
                                                 ).apply(config)

        inline protected fun endNode(predecessor: Identified): EndNode.Builder = XmlEndNode.Builder(predecessor = predecessor)
        inline protected fun endNode(
            predecessor: Identified,
            config: EndNode.Builder.() -> Unit
                                    ) : EndNode.Builder = XmlEndNode.Builder(
            predecessor = predecessor
                                                       ).apply(config)


    }
}
