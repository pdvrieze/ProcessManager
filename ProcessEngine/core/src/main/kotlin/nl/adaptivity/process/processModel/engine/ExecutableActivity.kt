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
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.*
import java.sql.SQLException


/**
 * Created by pdvrieze on 27/11/16.
 */
class ExecutableActivity : ActivityBase<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode {

  class Builder : ActivityBase.Builder<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode.Builder {

    constructor(): this(predecessor=null)
    constructor(predecessor: Identified? = null,
                successor: Identified? = null,
                id: String? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                message: XmlMessage? = null,
                condition: String? = null,
                name: String? = null) : super(predecessor, successor, id, label, x, y, defines, results, message, condition, name)

    constructor(node: Activity<*, *>) : super(node)

    override fun build(newOwner: ExecutableProcessModel) = ExecutableActivity(this, newOwner)
  }

  private var _condition: ExecutableCondition?

  override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

  override var condition: String?
    get() = _condition?.condition
    set(value) {
      _condition = condition?.let { ExecutableCondition(it) }
    }

  override val ownerModel: ExecutableProcessModel
    get() = super.ownerModel!!

  constructor(ownerModel: ExecutableProcessModel, condition: ExecutableCondition? = null) : super(ownerModel) {
    this._condition = condition
  }

  /**
   * Create a new Activity. Note that activities can only have a a single
   * predecessor.

   * @param predecessor The process node that starts immediately precedes this
   * *          activity.
   */
  @Deprecated("Don't use")
  constructor(ownerModel: ExecutableProcessModel, predecessor: ExecutableProcessNode?): this(ownerModel) {
    setPredecessors(listOfNotNull(predecessor))
  }

  constructor(orig: Activity<*, *>, newOwner: ExecutableProcessModel): super(orig, newOwner) {
    _condition = orig.condition?.let { ExecutableCondition(it) }
  }

  constructor(builder: Activity.Builder<*, *>, newOwnerModel: ExecutableProcessModel) : super(builder, newOwnerModel) {
    _condition = builder.condition?.let { ExecutableCondition(it) }
  }


  override fun builder() = Builder(node=this)

  override fun createOrReuseInstance(data: ProcessEngineDataAccess, processInstance: ProcessInstance, predecessor: ComparableHandle<out SecureObject<out ProcessNodeInstance>>): ProcessNodeInstance {
    return ProcessNodeInstance(this, predecessor, processInstance)
  }

  /**
   * Determine whether the process can start.
   */
  override fun condition(engineData: ProcessEngineDataAccess, instance: IExecutableProcessNodeInstance<*>): Boolean {
    return _condition?.run { eval(engineData, instance) } ?: true
  }

  /**
   * This will actually take the message element, and send it through the
   * message service.
   *
   * @param transaction
   *
   * @param messageService The message service to use to send the message.
   *
   * @param instance The processInstance that represents the actual activity
   *           instance that the message responds to.
   *
   * @throws SQLException
   *
   * @todo handle imports.
   */
  @Throws(SQLException::class)
  override fun <V, U : IExecutableProcessNodeInstance<U>> provideTask(engineData: MutableProcessEngineDataAccess,
                                                                                              messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                              processInstance: ProcessInstance, instance: U): Boolean {
    // TODO handle imports
    val message = messageService.createMessage(message)
    try {
      if (!messageService.sendMessage(engineData, message, instance)) {
        instance.failTaskCreation(engineData, processInstance, MessagingException("Failure to send message"))
      }
    } catch (e: RuntimeException) {
      instance.failTaskCreation(engineData, processInstance, e)
      throw e
    }

    return false
  }

  /**
   * Take the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically taken.

   * @return `false`
   */
  override fun <V, U : IExecutableProcessNodeInstance<U>> takeTask(messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                           instance: U): Boolean {
    return false
  }

  /**
   * Start the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically started.

   * @return `false`
   */
  override fun <V, U : IExecutableProcessNodeInstance<U>> startTask(messageService: IMessageService<V, MutableProcessEngineDataAccess, in U>,
                                                                                            instance: U): Boolean {
    return false
  }

  @Throws(XmlException::class)
  override fun serializeCondition(out: XmlWriter) {
    out.writeChild(_condition)
  }

  @Throws(XmlException::class)
  override fun deserializeCondition(reader: XmlReader) {
    _condition = ExecutableCondition.deserialize(reader)
  }

  companion object {

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel, reader: XmlReader): ExecutableActivity {
      return ExecutableActivity(ownerModel).deserializeHelper(reader)
    }

  }

}