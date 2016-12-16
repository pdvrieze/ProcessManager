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
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.JoinBase
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.*
import java.sql.SQLException
import java.util.*


@XmlDeserializer(ExecutableJoin.Factory::class)
class ExecutableJoin : JoinBase<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode {

  class Builder : JoinBase.Builder<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode.Builder {
    constructor(predecessors: Collection<Identified> = emptyList(),
                successor: Identified? = null,
                id: String? = null, label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1) : super(predecessors, successor, id, label, x, y, defines, results, min, max)
    constructor(node: Join<*, *>) : super(node)

    override fun build(newOwner: ExecutableProcessModel) = ExecutableJoin(this, newOwner)
  }

  class Factory : XmlDeserializerFactory<ExecutableJoin> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): ExecutableJoin {
      return ExecutableJoin.deserialize(null, reader)
    }
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override val ownerModel: ExecutableProcessModel
    get() = super.ownerModel!!

  constructor(orig: Join<*, *>, newOwner: ExecutableProcessModel) : super(orig, newOwner)

  @Deprecated("Use the full constructor")
  constructor(ownerModel: ExecutableProcessModel, predecessors: Collection<Identified>, min: Int, max: Int)
        : super(ownerModel, predecessors, max, min)

  constructor(ownerModel: ExecutableProcessModel?) : super(ownerModel)

  constructor(builder: Join.Builder<*, *>, newOwnerModel: ExecutableProcessModel) : super(builder, newOwnerModel)

  override fun builder() = Builder(this)

  override fun createOrReuseInstance(data: ProcessEngineDataAccess, processInstance: ProcessInstance, predecessor: ComparableHandle<out SecureObject<out ProcessNodeInstance>>): ProcessNodeInstance {
    return processInstance.getJoinInstance(data, this, predecessor)
  }

  @Deprecated("")
  internal fun getXmlPrececessors(): Set<Identifiable>? {
    return predecessors
  }

  @Deprecated("")
  internal fun setXmlPrececessors(pred: List<ExecutableProcessNode>) {
    swapPredecessors(pred)
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
    fun deserialize(ownerModel: ExecutableProcessModel?, reader: XmlReader): ExecutableJoin {
      return ExecutableJoin(ownerModel).deserializeHelper(reader)
    }

    fun andJoin(ownerModel: ExecutableProcessModel, vararg predecessors: ExecutableProcessNode): ExecutableJoin {
      return ExecutableJoin(ownerModel, Arrays.asList(*predecessors), Integer.MAX_VALUE, Integer.MAX_VALUE)
    }
  }

}
