/*
 * Copyright (c) 2016.
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

import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.util.Identifiable
import java.security.Principal
import java.util.*


//@XmlDeserializer(XmlProcessModel.Factory::class)
interface RootProcessModel<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel<NodeT,ModelT> {

  interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel.Builder<NodeT,ModelT> {
    override val rootBuilder: Builder<NodeT, ModelT> get() = this

    val childModels: MutableList<ChildProcessModel.Builder<NodeT, ModelT>>

    var name: String?
    var handle: Long
    var owner: Principal
    var uuid: UUID?
    val roles: MutableSet<String>


    fun newChildId(base:String):String {
      return generateSequence(1, { it+1} ).map { "${base}${it}" }.first { candidateId ->
        (childModels.asSequence()).none { it.childId == candidateId }
      }
    }

    fun build(pedantic: Boolean = defaultPedantic): RootProcessModel<NodeT,ModelT>

    override fun normalize(pedantic: Boolean) {
      super.normalize(pedantic)
      childModels.filter { it.childId==null }.forEach {
        if (pedantic) {
          throw IllegalProcessModelException("No child id for child model defined")
        } else {
          it.ensureChildId()
        }
      }
    }
  }

  /**
   * Get the UUID for this process model.
   * @return The UUID this process model has.
   */
  fun getUuid(): UUID?

  /**
   * Get a reference node for this model.

   * @return A reference node.
   */
  fun getRef(): IProcessModelRef<NodeT, ModelT, @JvmWildcard RootProcessModel<NodeT, ModelT>>?

  /**
   * Get the process node with the given id.
   * @param nodeId The node id to look up.
   * *
   * @return The process node with the id.
   */
  override fun getNode(nodeId: Identifiable): NodeT?

  override fun getModelNodes(): Collection<NodeT>

  fun getName(): String?

  val childModels: Collection<ChildProcessModel<NodeT, ModelT>>

  fun getChildModel(childId: Identifiable): ChildProcessModel<NodeT, ModelT>?

  val owner: Principal

  fun getRoles(): Set<String>

  override fun getImports(): Collection<IXmlResultType>

  override fun getExports(): Collection<IXmlDefineType>

  override val rootModel: RootProcessModel<NodeT, ModelT> get() = this

  companion object {
    const val ATTR_ROLES = "roles"
    const val ATTR_NAME = "name"
  }

}

val RootProcessModel<*,*>.uuid get() = getUuid()
val <T : ProcessNode<T, M>, M : ProcessModel<T, M>> RootProcessModel<T, M>.ref get() = getRef()
val <T : ProcessNode<T, *>> ProcessModel<T, *>.modelNodes get() = getModelNodes()
val RootProcessModel<*,*>.name get() = getName()
val RootProcessModel<*,*>.roles get() = getRoles()
val ProcessModel<*,*>.imports get() = getImports()
val ProcessModel<*,*>.exports get() = getExports()