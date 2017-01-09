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

package nl.adaptivity.process.processModel.engine

import net.devrieze.util.ComparableHandle
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SecureObject
import net.devrieze.util.toMutableArraySet
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.*
import java.sql.SQLException


/**
 * Activity version that is used for process execution.
 */
class ExecutableActivity : ActivityBase<ExecutableProcessNode, ExecutableModelCommon>, ExecutableProcessNode {

  constructor(builder: Activity.Builder<*, *>, newOwnerModel: ExecutableModelCommon) : super(builder, newOwnerModel) {
    this._condition = builder.condition?.let(::ExecutableCondition)
  }

  constructor(builder: Activity.ChildModelBuilder<*, *>, childModel: ExecutableChildModel) : super(builder, childModel) {
    this._condition = builder.condition?.let(::ExecutableCondition)
  }

  class Builder : ActivityBase.Builder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableProcessNode.Builder {

    constructor(id: String? = null,
                predecessor: Identified? = null,
                successor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                message: XmlMessage? = null,
                condition: String? = null,
                name: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN) : super(id, predecessor, successor, label, defines, results, message, condition, name, x, y)

    constructor(node: Activity<*, *>) : super(node)

    override fun build(newOwner: ExecutableModelCommon) = ExecutableActivity(this, newOwner)
  }


  class ChildModelBuilder(
      override val rootBuilder: ExecutableProcessModel.Builder,
      override var id: String? = null,
      childId: String? = null,
      nodes: Collection<ExecutableProcessNode.Builder> = emptyList(),
      predecessors: Collection<Identified> = emptyList(),
      override var condition: String? = null,
      successors: Collection<Identified> = emptyList(),
      override var label: String? = null,
      imports: Collection<IXmlResultType> = emptyList(),
      defines: Collection<IXmlDefineType> = emptyList(),
      exports: Collection<IXmlDefineType> = emptyList(),
      results: Collection<IXmlResultType> = emptyList(),
      override var x: Double = Double.NaN, override var y: Double = Double.NaN) : ExecutableChildModel.Builder(rootBuilder, childId, nodes, imports, exports), Activity.ChildModelBuilder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableModelCommon.Builder {

    override var predecessors: MutableSet<Identified> = predecessors.toMutableArraySet()
      set(value) { field.replaceBy(value) }

    override var successors: MutableSet<Identified> = successors.toMutableArraySet()
      set(value) { field.replaceBy(value) }

    override var defines: MutableCollection<IXmlDefineType> = java.util.ArrayList(defines)
      set(value) {field.replaceBy(value)}

    override var results: MutableCollection<IXmlResultType> = java.util.ArrayList(results)
      set(value) {field.replaceBy(value)}

    override fun buildModel(ownerModel: RootProcessModel<ExecutableProcessNode, ExecutableModelCommon>, pedantic: Boolean): ExecutableChildModel {
      return ExecutableChildModel(this, ownerModel.asM.rootModel, pedantic)
    }

    override fun buildActivity(childModel: ChildProcessModel<ExecutableProcessNode, ExecutableModelCommon>): Activity<ExecutableProcessNode, ExecutableModelCommon> {
      return ExecutableActivity(this, childModel as ExecutableChildModel)
    }
  }

  override val childModel: ExecutableChildModel? get() = super.childModel?.let { it as ExecutableChildModel }
  private var _condition: ExecutableCondition?

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override var condition: String?
    get() = _condition?.condition
    set(value) {
      _condition = condition?.let(::ExecutableCondition)
    }


  override fun builder() = Builder(node=this)

  /**
   * Determine whether the process can start.
   */
  override fun condition(engineData: ProcessEngineDataAccess, instance: ProcessNodeInstance): ConditionResult {
    return _condition?.run { eval(engineData, instance) } ?: ConditionResult.TRUE
  }

  override fun createOrReuseInstance(data: ProcessEngineDataAccess,
                                     processInstance: ProcessInstance,
                                     predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>): ProcessNodeInstance {
    return processInstance.getNodeInstance(this) ?: if(childModel==null) ProcessNodeInstance(this, predecessor, processInstance) else CompositeInstance(this, predecessor, processInstance)
  }

  /**
   * This will actually take the message element, and send it through the
   * message service.
   *
   * @param engineData The data needed
   *
   * @param instance The processInstance that represents the actual activity
   *           instance that the message responds to.
   *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  override fun provideTask(engineData: ProcessEngineDataAccess,
                           processInstance: ProcessInstance, instance: ProcessNodeInstance): Boolean {

    return childModel.let { when (it) {
      null -> false // The instance will take care of this
      else -> true // Let the instance create the process
    }}

  }

  /**
   * Take the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically taken.
   *
   * @return `false`
   */
  override fun takeTask(instance: ProcessNodeInstance) = false

  /**
   * Start the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically started.
   *
   * @return `false`
   */
  override fun startTask(instance: ProcessNodeInstance) = false

  @Throws(XmlException::class)
  override fun serializeCondition(out: XmlWriter) {
    out.writeChild(_condition)
  }

  companion object {

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableActivity {
      return ExecutableActivity.Builder().deserializeHelper(reader).build(ownerModel)
    }

  }

}