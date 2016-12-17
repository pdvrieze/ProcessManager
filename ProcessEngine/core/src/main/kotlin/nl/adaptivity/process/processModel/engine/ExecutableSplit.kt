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

import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.SplitInstance
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.SplitBase
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper
import java.sql.SQLException


class ExecutableSplit(builder: Split.Builder<*, *>, newOwnerModel: ExecutableProcessModel) : SplitBase<ExecutableProcessNode, ExecutableProcessModel>(builder, newOwnerModel), ExecutableProcessNode {

  class Builder : SplitBase.Builder<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode.Builder {
    constructor(predecessors: Collection<Identified> = emptyList(),
                successors: Collection<Identified> = emptyList(),
                id: String? = null, label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = -1,
                max: Int = -1) : super(predecessors, successors, id, label, x, y, defines, results, min, max)
    constructor(node: Split<*, *>) : super(node)

    override fun build(newOwner: ExecutableProcessModel): ExecutableSplit {
      return ExecutableSplit(this, newOwner)
    }
  }

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override fun builder() = Builder(this)

  override fun createOrReuseInstance(data: ProcessEngineDataAccess, processInstance: ProcessInstance, predecessor: ProcessNodeInstance.HandleT): ProcessNodeInstance {
    return SplitInstance(this, predecessor, processInstance.getHandle(), processInstance.owner)
  }

  override fun condition(engineData: ProcessEngineDataAccess, instance: IExecutableProcessNodeInstance<*>) = true

  @Throws(SQLException::class)
  override fun provideTask(engineData: MutableProcessEngineDataAccess,
                           processInstance: ProcessInstance, instance: ProcessNodeInstance): Boolean {
    return true
  }

  override fun <U : IExecutableProcessNodeInstance<U>> takeTask(
      instance: U): Boolean {
    return true
  }

  override fun <U : IExecutableProcessNodeInstance<U>> startTask(
      instance: U): Boolean {
    return false
  }

  companion object {

    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableSplit {
      return ExecutableSplit.Builder().deserializeHelper(reader).build(ownerModel)
    }
  }

}
