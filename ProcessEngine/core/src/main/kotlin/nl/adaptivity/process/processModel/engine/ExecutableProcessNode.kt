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

import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.DefaultProcessNodeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import java.sql.SQLException


/**
 * Base type for any process node that can be executed
 */
interface ExecutableProcessNode : ProcessNode<ExecutableProcessNode, ExecutableModelCommon>, Identified {

  interface Builder : ProcessNode.IBuilder<ExecutableProcessNode, ExecutableModelCommon> {
    override fun build(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>): ProcessNode<ExecutableProcessNode, ExecutableModelCommon>

    override fun predecessors(vararg values: Identifiable) {
      values.forEach {
        predecessors.add(it.identifier ?: throw NullPointerException("Missing identifier for predecessor ${it}"))
      }
    }

    override fun result(builder: XmlResultType.Builder.() -> Unit) {
      results.add(XmlResultType.Builder().apply(builder).build())
    }

  }

  override val ownerModel: ExecutableModelCommon

  override val identifier: Identifier
    get() = Identifier(id)

  override val results: List<XmlResultType>

  override val defines: List<XmlDefineType>

  override fun builder(): ExecutableProcessNode.Builder

  /**
   * Create an instance of the node or return it if it already exist.
   *
   * TODO handle failRetry nodes
   */
  fun createOrReuseInstance(data: MutableProcessEngineDataAccess,
                            processInstanceBuilder: ProcessInstance.Builder,
                            predecessor: IProcessNodeInstance,
                            entryNo: Int): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
    processInstanceBuilder.getChild(this, entryNo)?.let { return it }
    if (!isMultiInstance && entryNo>1) {
      processInstanceBuilder.allChildren { it.node == this && it.entryNo!=entryNo }.forEach {
        processInstanceBuilder.updateChild(it) {
          invalidateTask(data)
        }
      }
    }
    return DefaultProcessNodeInstance.BaseBuilder(this, listOf(predecessor.handle()),
                                                  processInstanceBuilder,
                                                  processInstanceBuilder.owner, entryNo)
                            }


  /**
   * Should this node be able to be provided?
   * @param engineData
   *
   * @param instance The instance against which the condition should be evaluated.
   *
   * @return `true` if the node can be started, `false` if
   *          not.
   */
  fun condition(engineData: ProcessEngineDataAccess, instance: ProcessNodeInstance<*>): ConditionResult = ConditionResult.TRUE

  /**
   * Take action to make task available
   *
   * @param engineData
   *
   * @param instance The processnode instance involved.
   *
   * @return `true` if the task can/must be automatically taken
   */
  @Throws(SQLException::class)
  @Deprecated("Use the version that takes a builder")
  fun provideTask(engineData: ProcessEngineDataAccess,
                  processInstance: ProcessInstance, instance: ProcessNodeInstance<*>): Boolean = true

  /**
   * Take action to make task available
   *
   * @param engineData
   *
   * @param instanceBuilder The processnode instance involved.
   *
   * @return `true` if the task can/must be automatically taken
   */
  @Throws(SQLException::class)
  fun provideTask(engineData: ProcessEngineDataAccess, instanceBuilder: ProcessNodeInstance.Builder<*, *>): Boolean
    = true

  /**
   * Take action to accept the task (but not start it yet)
   *
   * @param instance The processnode instance involved.
   *
   * @return `true` if the task can/must be automatically started
   */
  @Deprecated("Use the version that takes a builder")
  fun takeTask(instance: ProcessNodeInstance<*>): Boolean = true

  fun takeTask(instance: ProcessNodeInstance.Builder<*, *>): Boolean = true

  @Deprecated("Use the version that takes a builder")
  fun startTask(instance: ProcessNodeInstance<*>): Boolean = true

  fun startTask(instance: ProcessNodeInstance.Builder<*, *>): Boolean = true

  private fun preceeds(node: ExecutableProcessNode, reference: ExecutableProcessNode, seenIds: MutableSet<String>):Boolean {
    if (node in reference.predecessors) return true
    seenIds+=id
    for (predecessorId in reference.predecessors) {
      if (predecessorId.id !in seenIds) {
        val predecessor = ownerModel.getNode(predecessorId)!!
        return preceeds(node, predecessor, seenIds)
      }
    }
    return false
  }

  /**
   * Determine whether this node is a "possible" predecessor of the reference node.
   */
  infix fun  preceeds(reference: ExecutableProcessNode): Boolean {
    if (this===reference) return false
    return preceeds(this, reference, HashSet<String>())
  }

  /**
   * Determine whether this node is a "possible" successor of the reference node.
   */
  infix fun succceeds(reference: ExecutableProcessNode): Boolean {
    if (this===reference) return false
    return preceeds(reference, this, HashSet<String>())
  }
}

