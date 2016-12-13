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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.HandleMap
import net.devrieze.util.ReadableHandleAware
import net.devrieze.util.StringUtil
import net.devrieze.util.Transaction
import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter
import org.w3c.dom.Node
import java.sql.SQLException
import javax.xml.bind.annotation.XmlRootElement


/**
 * Class representing the instantiation of an executable process node.

 * @author Paul de Vrieze
 * *
 * @param <V> The actual type of the implementing class.
</V> */
interface IProcessNodeInstance<V : IProcessNodeInstance<V>> : ReadableHandleAware<SecureObject<V>> {

  /**
   * Enumeration representing the various states a task can be in.

   * @author Paul de Vrieze
   */
  @XmlRootElement(name = "taskState", namespace = "http://adaptivity.nl/userMessageHandler")
  enum class NodeInstanceState private constructor(val isFinal: Boolean, val isActive:Boolean, val isCommitted:Boolean) {
    /**
     * Initial task state. The instance has been created, but has not been successfully sent to a receiver.
     */
    Pending(false, true, false),
    /**
     * The task is skipped due to split conditions
     */
    Skipped(true, false, false),
    /**
     * Signifies that the task has failed to be created, a new attempt should be made.
     */
    FailRetry(false, false, false),
    /**
     * Indicates that the task has been communicated to a
     * handler, but receipt has not been acknowledged.
     */
    Sent(false, true, false),
    /**
     * State acknowledging reception of the task. Note that this is generally
     * only used by process aware services. It signifies that a task has been
     * received, but processing has not started yet.
     */
    Acknowledged(false, true, false),
    /**
     * Some tasks allow for alternatives (different users). Taken signifies that
     * the task has been claimed and others can not claim it anymore (unless
     * released again).
     */
    Taken(false, true, true),
    /**
     * Signifies that work on the task has actually started.
     */
    Started(false, true, true),
    /**
     * Signifies that the task is complete. This generally is the end state of a
     * task.
     */
    Complete(true, false, true),
    /**
     * Signifies that the task has failed for some reason.
     */
    Failed(true, false, true),
    /**
     * Signifies that the task has been cancelled (but not through a failure).
     */
    Cancelled(true, false, false);


    companion object {

      @JvmStatic
      fun fromString(string: CharSequence): NodeInstanceState? {
        val lowerCase = StringUtil.toLowerCase(string)
        for (candidate in values()) {
          if (lowerCase == candidate.name.toLowerCase()) {
            return candidate
          }
        }
        return null
      }
    }

  }

  /**
   * Get the state of the task.

   * @return the state.
   */
  val state: NodeInstanceState
}
