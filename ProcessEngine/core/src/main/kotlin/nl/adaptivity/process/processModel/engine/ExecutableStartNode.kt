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

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handles
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.ProcessTransaction
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper
import java.sql.SQLException


class ExecutableStartNode : StartNodeBase<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode {

  class Builder : StartNodeBase.Builder<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode.Builder {
    constructor() : this(successor=null)
    constructor(successor: Identified? = null,
                id: String? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList()) : super(successor, id, label, x, y, defines, results)
    constructor(node: StartNode<*, *>) : super(node)


    override fun build(newOwner: ExecutableProcessModel?): ExecutableStartNode {
      return ExecutableStartNode(this, newOwner)
    }
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override val ownerModel: ExecutableProcessModel
    get() = super.ownerModel!!

  constructor(orig: StartNode<*, *>, newOwner: ExecutableProcessModel) : super(orig, newOwner)

  constructor(ownerModel: ExecutableProcessModel) : super(ownerModel)

  constructor(ownerModel: ExecutableProcessModel, imports: List<XmlResultType>) : super(ownerModel) {
    setResults(imports)
  }

  constructor(builder: StartNode.Builder<*, *>, newOwnerModel: ExecutableProcessModel?) : super(builder, newOwnerModel)

  override fun builder() = Builder(node=this)

  fun <T : ProcessTransaction> createOrReuseInstance(transaction: T, processInstance: ProcessInstance): ProcessNodeInstance {
    return ProcessNodeInstance(this, Handles.getInvalid(), processInstance)
  }

  override fun createOrReuseInstance(data: ProcessEngineDataAccess, processInstance: ProcessInstance, predecessor: ProcessNodeInstance.HandleT): ProcessNodeInstance {
    return ProcessNodeInstance(this, predecessor, processInstance)
  }

  override fun condition(engineData: ProcessEngineDataAccess, instance: IExecutableProcessNodeInstance<*>) = true

  @Throws(SQLException::class)
  override fun <V, U : IExecutableProcessNodeInstance<U>> provideTask(engineData: MutableProcessEngineDataAccess,
                                                                                              messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                              processInstance: ProcessInstance, instance: U): Boolean {
    return true
  }

  override fun <V, U : IExecutableProcessNodeInstance<U>> takeTask(messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                           instance: U): Boolean {
    return true
  }

  override fun <V, U : IExecutableProcessNodeInstance<U>> startTask(messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                            instance: U): Boolean {
    return true
  }

  companion object {

    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableStartNode {
      return Builder().deserializeHelper(reader).build(ownerModel)
    }
  }

}
