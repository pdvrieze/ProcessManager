/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.StringUtil
import java.util.*
import javax.xml.bind.annotation.XmlRootElement

/**
 * Enumeration representing the various states a task can be in.

 * @author Paul de Vrieze
 */
@XmlRootElement(name = "taskState", namespace = "http://adaptivity.nl/userMessageHandler")
enum class NodeInstanceState constructor(val isFinal: Boolean, val isActive:Boolean, val isCommitted:Boolean) {
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
   * Signifies that the task has been cancelled directly (but not through a failure).
   */
  Cancelled(true, false, false),
  /** Signifies that the task has been skipped because a predecessor was cancelled */
  SkippedCancel(true, false, false),
  /** Signifies that the task has been skipped because a predecessor failed */
  SkippedFail(true, false, false)
  ;

  val lcname = name.toLowerCase(Locale.ENGLISH)

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