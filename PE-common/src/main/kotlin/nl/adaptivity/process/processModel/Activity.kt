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

import net.devrieze.util.collection.replaceByNotNull
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.util.Identifiable
import javax.xml.namespace.QName


interface Activity<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode<NodeT, ModelT> {

  interface IBuilder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ProcessNode.IBuilder<T, M> {
    var condition: String?

    var predecessor: Identifiable?
      get() = predecessors.firstOrNull()
      set(value) { predecessors.replaceByNotNull(value?.identifier) }

    var successor: Identifiable?
      get() = successors.firstOrNull()
      set(value) { successors.replaceByNotNull(value?.identifier) }
  }

  interface Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : IBuilder<T, M>, ProcessNode.Builder<T, M> {
    var message: IXmlMessage?
    @Deprecated("Names are not used anymore")
    var name: String?

    override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>) = visitor.visitActivity(this)
  }

  interface ChildModelBuilder<NodeT : ProcessNode<NodeT,ModelT>, ModelT:ProcessModel<NodeT,ModelT>?> : IBuilder<NodeT,ModelT>, ChildProcessModel.Builder<NodeT, ModelT> {
    override fun deserializeAttribute(attributeNamespace: CharSequence, attributeLocalName: CharSequence, attributeValue: CharSequence): Boolean {
      throw UnsupportedOperationException("The SubModelBuilder is a convenience builder that cannot be used in deserialization")
    }

    override val idBase: String get() = "sub"

    override fun buildModel(ownerModel: RootProcessModel<NodeT, ModelT>, pedantic: Boolean): ChildProcessModel<NodeT, ModelT>

    fun buildActivity(childModel: ChildProcessModel<NodeT,ModelT>): Activity<NodeT, ModelT>

    override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>) = visitor.visitActivity(this)
  }

  override fun builder(): Builder<NodeT, ModelT>

  /**
   * The name of this activity. Note that for serialization to XML to work
   * this needs to be unique for the process model at time of serialization, and
   * can not be null or an empty string. While in Java mode other nodes are
   * referred to by reference, not name.
   */
  @Deprecated("Not needed, use id.", ReplaceWith("id"))
  var name: String?

  /**
   * The condition that needs to be true to start this activity. A null value means that the activity can run.
   */
  var condition: String?

  /**
   * Get the list of imports. The imports are provided to the message for use as
   * data parameters. Setting will create a copy of the parameter for safety.
   */
  override val results: List<@JvmWildcard IXmlResultType>

  fun setResults(results: Collection<IXmlResultType>)

  /**
   * Get the list of exports. Exports will allow storing the response of an
   * activity. Setting will create a copy of the parameter for safety.
   */
  override val defines: List<@JvmWildcard IXmlDefineType>

  fun setDefines(defines: Collection<IXmlDefineType>)

  /**
   * The predecessor node for this activity.
   */
  var predecessor: Identifiable?

  /**
   * The message of this activity. This provides all the information to be
   * able to actually invoke the service.
   */
  var message: IXmlMessage?

  val childModel: ChildProcessModel<NodeT, ModelT>?

  companion object {

    /** The name of the XML element.  */
    const val ELEMENTLOCALNAME = "activity"
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
  }

}