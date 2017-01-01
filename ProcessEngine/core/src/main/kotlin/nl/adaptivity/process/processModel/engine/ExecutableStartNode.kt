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

import net.devrieze.util.Handles
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.StartNodeBase
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper
import java.sql.SQLException


class ExecutableStartNode(builder: StartNode.Builder<*, *>, newOwnerModel: ExecutableProcessModel) : StartNodeBase<ExecutableProcessNode, ExecutableProcessModel>(builder, newOwnerModel), ExecutableProcessNode {

  class Builder : StartNodeBase.Builder<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode.Builder {
    constructor(id: String? = null,
                successor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN) : super(id, successor, label, defines, results, x, y)
    constructor(node: StartNode<*, *>) : super(node)


    override fun build(newOwner: ExecutableProcessModel): ExecutableStartNode {
      return ExecutableStartNode(this, newOwner)
    }
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override fun builder() = Builder(node=this)

  fun createOrReuseInstance(processInstance: ProcessInstance)
      = processInstance.getNodeInstance(this) ?: ProcessNodeInstance(this, Handles.getInvalid(), processInstance)

  override fun condition(engineData: ProcessEngineDataAccess, instance: ProcessNodeInstance) = ConditionResult.TRUE

  @Throws(SQLException::class)
  override fun provideTask(engineData: MutableProcessEngineDataAccess,
                           processInstance: ProcessInstance, instance: ProcessNodeInstance): Boolean {
    return true
  }

  override fun takeTask(instance: ProcessNodeInstance) = true

  override fun startTask(instance: ProcessNodeInstance) = true

  companion object {

    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableStartNode {
      return Builder().deserializeHelper(reader).build(ownerModel)
    }
  }

}
