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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessTransaction
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.EndNodeBase
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xml.*
import java.sql.SQLException


@XmlDeserializer(ExecutableEndNode.Factory::class)
class ExecutableEndNode : EndNodeBase<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode {

  class Builder : EndNodeBase.Builder<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode.Builder {
    constructor(predecessor: Identifiable?, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>) : super(predecessor, id, label, x, y, defines, results)
    constructor(node: EndNode<*, *>) : super(node)

    override fun build(newOwner: ExecutableProcessModel) = ExecutableEndNode(this, newOwner)
  }

  class Factory : XmlDeserializerFactory<ExecutableEndNode> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): ExecutableEndNode {
      return ExecutableEndNode.deserialize(null, reader)
    }
  }

  constructor(orig: EndNode<*, *>, newOwner: ExecutableProcessModel?) : super(orig, newOwner) {
  }

  constructor(ownerModel: ExecutableProcessModel?, previous: ExecutableProcessNode) : super(ownerModel) {
    predecessor = previous
  }

  constructor(ownerModel: ExecutableProcessModel?) : super(ownerModel)

  constructor(builder: EndNode.Builder<*, *>, newOwnerModel: ExecutableProcessModel) : super(builder, newOwnerModel)

  override fun builder() = Builder(this)

  override fun <T : ProcessTransaction> condition(transaction: T,
                                                  instance: IExecutableProcessNodeInstance<*>): Boolean {
    return true
  }

  @Throws(SQLException::class)
  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> provideTask(transaction: T,
                                                                                              messageService: IMessageService<V, T, in U>,
                                                                                              instance: U): Boolean {
    return true
  }

  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> takeTask(messageService: IMessageService<V, T, in U>,
                                                                                           instance: U): Boolean {
    return true
  }

  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> startTask(messageService: IMessageService<V, T, in U>,
                                                                                            instance: U): Boolean {
    return true
  }

  companion object {

    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel?, reader: XmlReader): ExecutableEndNode {
      return ExecutableEndNode(ownerModel).deserializeHelper(reader)
    }
  }

}
