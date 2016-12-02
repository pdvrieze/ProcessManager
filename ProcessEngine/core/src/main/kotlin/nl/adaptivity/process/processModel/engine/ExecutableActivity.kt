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

import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessTransaction
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xml.*
import java.sql.SQLException


/**
 * Created by pdvrieze on 27/11/16.
 */
class ExecutableActivity : ActivityBase<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode {

  class Builder : ActivityBase.Builder<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode.Builder {
    constructor(predecessor: Identifiable?, successor: Identifiable?, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>, message: XmlMessage?, condition: String? = null, name: String?) : super(predecessor, successor, id, label, x, y, defines, results, message, condition, name)
    constructor(node: Activity<*, *>) : super(node)

    override fun build(newOwner: ExecutableProcessModel) = ExecutableActivity(this, newOwner)
  }

  private var _condition: ExecutableCondition?

  override var condition: String?
    get() = _condition?.condition
    set(value) {
      _condition = condition?.let { ExecutableCondition(it) }
    }

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

  constructor(orig: Activity<*, *>, newOwner: ExecutableProcessModel?): super(orig, newOwner) {
    _condition = orig.condition?.let { ExecutableCondition(it) }
  }

  constructor(builder: Activity.Builder<*, *>, newOwnerModel: ExecutableProcessModel) : super(builder, newOwnerModel) {
    _condition = builder.condition?.let { ExecutableCondition(it) }
  }


  override fun builder() = Builder(this)

  /**
   * Determine whether the process can start.
   */
  override fun <T : ProcessTransaction> condition(transaction: T, instance: IExecutableProcessNodeInstance<*>): Boolean {
    return _condition?.run { eval(transaction, instance) } ?: true
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
  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> provideTask(transaction: T,
                                                                                              messageService: IMessageService<V, T, in U>,
                                                                                              instance: U): Boolean {
    // TODO handle imports
    val message = messageService.createMessage(message)
    try {
      if (!messageService.sendMessage(transaction, message, instance)) {
        instance.failTaskCreation(transaction, MessagingException("Failure to send message"))
      }
    } catch (e: RuntimeException) {
      instance.failTaskCreation(transaction, e)
      throw e
    }

    return false
  }

  /**
   * Take the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically taken.

   * @return `false`
   */
  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> takeTask(messageService: IMessageService<V, T, in U>,
                                                                                           instance: U): Boolean {
    return false
  }

  /**
   * Start the task. Tasks are either process aware or finished when a reply is
   * received. In either case they should not be automatically started.

   * @return `false`
   */
  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> startTask(messageService: IMessageService<V, T, in U>,
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