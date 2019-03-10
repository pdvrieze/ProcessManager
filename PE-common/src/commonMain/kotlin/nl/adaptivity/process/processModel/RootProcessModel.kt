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

package nl.adaptivity.process.processModel

import kotlinx.serialization.SerialName
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.multiplatform.JvmName
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal


//@XmlDeserializer(XmlProcessModel.Factory::class)
interface RootProcessModel<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel<NodeT, ModelT> {

    override val rootModel: RootProcessModel<NodeT, ModelT> get() = this

    @SerialName("childModel")
    val childModels: Collection<ChildProcessModel<NodeT, ModelT>>

    val owner: Principal

    val ref: IProcessModelRef<NodeT, ModelT, RootProcessModel<NodeT, ModelT>>?

    @SerialName("nodes")
    override val modelNodes: List<NodeT>
    val uuid: UUID?
    val name: String?
    val roles: Set<String>

    fun copy(imports: Collection<IXmlResultType> = this.imports,
             exports: Collection<IXmlDefineType> = this.exports,
             nodes: Collection<NodeT> = modelNodes,
             name: kotlin.String? = this.name,
             uuid: UUID? = this.uuid,
             roles: Set<String> = this.roles,
             owner: Principal = this.owner,
             childModels: Collection<ChildProcessModel<NodeT, ModelT>> = this.childModels): RootProcessModel<NodeT, ModelT>

    fun builder(): RootProcessModel.Builder<NodeT, ModelT>

    /**
     * Get the process node with the given id.
     * @param nodeId The node id to look up.
     *
     * @return The process node with the id.
     */
    override fun getNode(nodeId: Identifiable): NodeT?

    fun getChildModel(childId: Identifiable): ChildProcessModel<NodeT, ModelT>?

    companion object {
        const val ATTR_ROLES = "roles"
        const val ATTR_NAME = "name"
    }

    interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel.Builder<NodeT, ModelT> {
        override val rootBuilder: Builder<NodeT, ModelT> get() = this

        val childModels: MutableList<ChildProcessModel.Builder<NodeT, ModelT>>

        var name: String?
        var handle: Long
        var owner: Principal
        var uuid: UUID?
        val roles: MutableSet<String>

        fun newChildId(base: String): String {
            return generateSequence(1, { it + 1 }).map { "$base$it" }.first { candidateId ->
                (childModels.asSequence()).none { it.childId == candidateId }
            }
        }

        fun build(pedantic: Boolean = defaultPedantic): RootProcessModel<NodeT, ModelT>

        override fun normalize(pedantic: Boolean) {
            super.normalize(pedantic)
            childModels.filter { it.childId == null }.forEach {
                if (pedantic) {
                    throw IllegalProcessModelException("No child id for child model defined")
                } else {
                    it.ensureChildId()
                }
            }
        }
    }

}

/*
val RootProcessModel<*, *>.uuid get() = getUuid()
val <T : ProcessNode<T, M>, M : ProcessModel<T, M>> RootProcessModel<T, M>.ref get() = getRef()
val <T : ProcessNode<T, *>> ProcessModel<T, *>.modelNodes get() = getModelNodes()
val RootProcessModel<*, *>.name get() = getName()
val RootProcessModel<*, *>.roles get() = getRoles()
val ProcessModel<*, *>.imports get() = getImports()
val ProcessModel<*, *>.exports get() = getExports()
*/
