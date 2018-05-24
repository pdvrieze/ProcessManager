/*
 * Copyright (c) 2018.
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

import kotlinx.serialization.Transient
import net.devrieze.util.Handle
import net.devrieze.util.StringCache
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.processModel.*
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xml.XmlDeserializer
import nl.adaptivity.xml.XmlDeserializerFactory
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader


typealias ExecutableModelCommonAlias = ProcessModel<ExecutableProcessNode, ExecutableModelCommon>

/**
 * A class representing a process model.

 * @author Paul de Vrieze
 */
@XmlDeserializer(ExecutableProcessModel.Factory::class)
class ExecutableProcessModel @JvmOverloads constructor(builder: RootProcessModel.Builder<*, *>,
                                                       pedantic: Boolean = true) :
    RootProcessModelBase<ExecutableProcessNode, ExecutableModelCommon>(builder, EXEC_NODEFACTORY, pedantic),
    ExecutableModelCommon,
    /*MutableHandleAware<ExecutableProcessModel>,*/
    SecureObject<ExecutableProcessModel> {

    @Transient
    override val endNodeCount by lazy { modelNodes.count { it is ExecutableEndNode } }

    @Transient
    override val rootModel get() = this

    @Transient
    override val ref: IProcessModelRef<ExecutableProcessNode, ExecutableModelCommon, ExecutableProcessModel>
        get() = ProcessModelRef(name, this.getHandle(), uuid)

    override fun copy(imports: Collection<IXmlResultType>,
                      exports: Collection<IXmlDefineType>,
                      nodes: Collection<ExecutableProcessNode>,
                      name: String?,
                      uuid: UUID?,
                      roles: Set<String>,
                      owner: Principal,
                      childModels: Collection<ChildProcessModel<ExecutableProcessNode, ExecutableModelCommon>>): ExecutableProcessModel {
        return Builder(nodes.map { it.builder() }, emptySet(), name, handleValue, owner, roles, uuid).also { builder ->
            builder.childModels.replaceBy(childModels.map { it.builder(builder) })
        }.build(false)
    }

    override fun withPermission() = this

    override fun builder(): Builder = Builder(this)

    override fun update(body: RootProcessModelBase.Builder<ExecutableProcessNode, ExecutableModelCommon>.() -> Unit): ExecutableProcessModel {
        return Builder(this).apply(body).build()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getHandle() = super.getHandle() as Handle<ExecutableProcessModel>

    /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.ProcessModel#getEndNodeCount()
       */

    fun cacheStrings(stringCache: StringCache) {
        if (owner is SimplePrincipal) {
            owner = SimplePrincipal(stringCache.lookup(owner.name))
        } else if (_cls_darwin_principal != null) {
            if (_cls_darwin_principal!!.isInstance(owner)) {
                try {
                    val cacheStrings = _cls_darwin_principal?.getMethod("cacheStrings", StringCache::class.java)
                    if (cacheStrings != null) {
                        owner = cacheStrings.invoke(owner, stringCache) as Principal
                    }
                } catch (e: Exception) {
                    // Ignore
                }

            }
        }
        name?.let { setName(stringCache.lookup(it)) }
        val oldRoles = roles
        if (oldRoles.isNotEmpty()) {
            val newRoles = oldRoles.map { stringCache.lookup(it) }
            setRoles(newRoles)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun toString(): String {
        return "ExecutableProcessModel() ${super.toString()}"
    }


    companion object {

        fun from(basepm: RootProcessModel<*, *>): ExecutableProcessModel {
            return basepm as? ExecutableProcessModel ?: ExecutableProcessModel(Builder(basepm))
        }

        @Throws(XmlException::class)
        @JvmStatic
        fun deserialize(reader: XmlReader): ExecutableProcessModel {
            return Builder.deserialize(reader).build()
        }

        @JvmStatic
        inline fun build(body: Builder.() -> Unit) = Builder().apply(body).also {
            if (it.uuid==null) { it.uuid = java.util.UUID.randomUUID() }
        }.build()

        /**
         * A class handle purely used for caching and special casing the DarwinPrincipal class.
         */
        private var _cls_darwin_principal: Class<*>? = null

        init {
            try {
                _cls_darwin_principal = ClassLoader.getSystemClassLoader().loadClass(
                    "uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal")
            } catch (e: ClassNotFoundException) {
                _cls_darwin_principal = null
            }

        }

        /**
         * Helper method that helps enumerating all elements in the model

         * @param destination The collection that will contain the result.
         *
         * @param seen A set of process names that have already been seen (and should
         * *          not be added again.
         *
         * @param node The node to start extraction from. This will go on to the
         * *          successors.
         */
        private fun extractElementsTo(destination: MutableCollection<in ExecutableProcessNode>,
                                      seen: MutableSet<String>,
                                      node: ExecutableProcessNode) {
            if (node.id in seen) return

            destination.add(node)
            seen.add(node.id)
            for (successor in node.successors) {
                extractElementsTo(destination, seen, successor as ExecutableProcessNode)
            }
        }


    }


    class Builder : RootProcessModelBase.Builder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableModelCommon.Builder {
        constructor(nodes: Collection<ExecutableProcessNode.Builder> = emptySet(),
                    childModels: Collection<ExecutableChildModel.Builder> = emptySet(),
                    name: String? = null,
                    handle: Long = -1L,
                    owner: Principal = SYSTEMPRINCIPAL,
                    roles: Collection<String> = emptyList(),
                    uuid: UUID? = null,
                    imports: Collection<IXmlResultType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList()) : super(nodes, childModels, name, handle, owner,
                                                                               roles, uuid, imports, exports)

        constructor(base: RootProcessModel<*, *>) : super(base)

        override val rootBuilder get() = this

        override fun build(pedantic: Boolean) = ExecutableProcessModel(this, pedantic)

        override fun childModelBuilder() = ExecutableChildModel.Builder(rootBuilder)

        override fun childModelBuilder(base: ChildProcessModel<*, *>) = ExecutableChildModel.Builder(rootBuilder, base)

        companion object {
            @JvmStatic
            fun deserialize(reader: XmlReader): Builder {
                return RootProcessModelBase.Builder.deserialize(ExecutableProcessModel.Builder(), reader)
            }
        }
    }

    enum class Permissions : SecurityProvider.Permission {
        INSTANTIATE
    }

    class Factory : XmlDeserializerFactory<ExecutableProcessModel> {

        @Throws(XmlException::class)
        override fun deserialize(reader: XmlReader): ExecutableProcessModel {
            return ExecutableProcessModel.deserialize(reader)
        }
    }

}


val EXEC_BUILDER_VISITOR = object : ProcessNode.Visitor<ExecutableProcessNode.Builder> {
    override fun visitStartNode(startNode: StartNode<*, *>) = ExecutableStartNode.Builder(startNode)

    override fun visitActivity(activity: Activity<*, *>) = ExecutableActivity.Builder(activity)

    override fun visitSplit(split: Split<*, *>) = ExecutableSplit.Builder(split)

    override fun visitJoin(join: Join<*, *>) = ExecutableJoin.Builder(join)

    override fun visitEndNode(endNode: EndNode<*, *>) = ExecutableEndNode.Builder(endNode)
}

object EXEC_NODEFACTORY : ProcessModelBase.NodeFactory<ExecutableProcessNode, ExecutableModelCommon> {

    private fun visitor(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) = object : ProcessNode.BuilderVisitor<ExecutableProcessNode> {
        override fun visitStartNode(startNode: StartNode.Builder<*, *>) = ExecutableStartNode(startNode, buildHelper)

        override fun visitActivity(activity: Activity.Builder<*, *>) = ExecutableActivity(activity, buildHelper)

        override fun visitActivity(activity: Activity.ChildModelBuilder<*, *>) = ExecutableActivity(activity,
                                                                                                    buildHelper)

        override fun visitSplit(split: Split.Builder<*, *>) = ExecutableSplit(split, buildHelper)

        override fun visitJoin(join: Join.Builder<*, *>) = ExecutableJoin(join, buildHelper)

        override fun visitEndNode(endNode: EndNode.Builder<*, *>) = ExecutableEndNode(endNode, buildHelper)
    }


    override fun invoke(baseNodeBuilder: ProcessNode.IBuilder<*, *>,
                        buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) = baseNodeBuilder.visit(
        visitor(
            buildHelper))

    override fun invoke(baseChildBuilder: ChildProcessModel.Builder<*, *>,
                        buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>): ChildProcessModelBase<ExecutableProcessNode, ExecutableModelCommon> {
        return ExecutableChildModel(baseChildBuilder, buildHelper)
    }

    override fun condition(text: String) = ExecutableCondition(text)
}