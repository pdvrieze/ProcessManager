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
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.StartNodeBase
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.xml.*
import java.sql.SQLException


@XmlDeserializer(ExecutableStartNode.Factory::class)
class ExecutableStartNode : StartNodeBase<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode {

  class Factory : XmlDeserializerFactory<ExecutableStartNode> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): ExecutableStartNode {
      return ExecutableStartNode.deserialize(null, reader)
    }
  }

  constructor(orig: StartNode<*, *>) : super(orig)

  constructor(ownerModel: ExecutableProcessModel?) : super(ownerModel)

  constructor(ownerModel: ExecutableProcessModel?, imports: List<XmlResultType>) : super(ownerModel) {
    setResults(imports)
  }

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
    fun deserialize(ownerModel: ExecutableProcessModel?, reader: XmlReader): ExecutableStartNode {
      return ExecutableStartNode(ownerModel).deserializeHelper<ExecutableStartNode>(reader)
    }
  }

}
