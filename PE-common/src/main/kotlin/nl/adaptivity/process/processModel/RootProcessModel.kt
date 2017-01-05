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

import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.util.Identifiable
import java.security.Principal
import java.util.*


//@XmlDeserializer(XmlProcessModel.Factory::class)
interface RootProcessModel<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel<NodeT,ModelT> {

  interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel.Builder<NodeT,ModelT> {
    var name: String?
    var handle: Long
    var owner: Principal
    var uuid: UUID?
    val roles: MutableSet<String>

    override fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      val value = attributeValue.toString()
      when (attributeLocalName.toString()) {
        "name" -> name=value
        "owner" -> owner = SimplePrincipal(value)
        ATTR_ROLES -> roles.replaceBy(value.split(" *, *".toRegex()).filter { it.isEmpty() })
        "uuid" -> uuid = UUID.fromString(value)
        else -> return false
      }
      return true
    }

    fun build() = build(false)
    fun build(pedantic: Boolean): RootProcessModel<NodeT,ModelT>

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