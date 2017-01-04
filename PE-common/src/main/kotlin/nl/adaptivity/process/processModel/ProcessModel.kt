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
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xml.XmlDeserializer

import java.security.Principal
import java.util.UUID


//@XmlDeserializer(XmlProcessModel.Factory::class)
interface ProcessModel<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ModelCommon<T,M>, SecureObject<M> {

  interface Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ModelCommon.Builder<T,M> {
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
    fun build(pedantic: Boolean): ProcessModel<T,M>

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
  fun getRef(): IProcessModelRef<T, M>?

  /**
   * Get the process node with the given id.
   * @param nodeId The node id to look up.
   * *
   * @return The process node with the id.
   */
  override fun getNode(nodeId: Identifiable): T?

  override fun getModelNodes(): Collection<T>

  fun getName(): String?

  override val owner: Principal

  fun getRoles(): Set<String>

  override fun getImports(): Collection<IXmlResultType>

  override fun getExports(): Collection<IXmlDefineType>

  val asM:M get() {
    @Suppress("UNCHECKED_CAST")
    return this as M
  }

  override val rootModel: M get() = asM

  companion object {
    const val ATTR_ROLES = "roles"
    const val ATTR_NAME = "name"
  }

}

@Deprecated("Use builders instead")
interface MutableProcessModel<T : ProcessNode<T, M>, M : ProcessModel<T, M>?>: ProcessModel<T,M> {

  fun setUuid(uUID: UUID)

  fun addNode(node: T): Boolean
  fun removeNode(node: T): Boolean

  /**
   * Initiate the notification that a node has changed. Actual implementations can override this.
   * @param node The node that has changed.
   */
  fun notifyNodeChanged(node: T)
}

val ProcessModel<*,*>.uuid get() = getUuid()
val <T : ProcessNode<T, M>, M : ProcessModel<T, M>> ProcessModel<T, M>.ref get() = getRef()
val <T : ProcessNode<T, *>> ModelCommon<T, *>.modelNodes get() = getModelNodes()
val ProcessModel<*,*>.name get() = getName()
val ProcessModel<*,*>.roles get() = getRoles()
val ModelCommon<*,*>.imports get() = getImports()
val ModelCommon<*,*>.exports get() = getExports()