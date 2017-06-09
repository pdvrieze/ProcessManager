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
enum class NodeInstanceState {
  /**
   * Initial task state. The instance has been created, but has not been successfully sent to a receiver.
   */
  Pending {
    override val isActive: Boolean get() = true
  },
  /**
   * The task is skipped due to split conditions
   */
  Skipped {
    override val isFinal: Boolean get() = true
    override val isSkipped: Boolean get() = true
  },
  /**
   * Signifies that the task has failed to be created, a new attempt should be made.
   */
  FailRetry,
  /**
   * Indicates that the task has been communicated to a
   * handler, but receipt has not been acknowledged.
   */
  Sent {
    override val isActive: Boolean get() = true
  },
  /**
   * State acknowledging reception of the task. Note that this is generally
   * only used by process aware services. It signifies that a task has been
   * received, but processing has not started yet.
   */
  Acknowledged {
    override val isActive: Boolean get() = true
  },
  /**
   * Some tasks allow for alternatives (different users). Taken signifies that
   * the task has been claimed and others can not claim it anymore (unless
   * released again).
   */
  Taken {
    override val isActive: Boolean get() = true
    override val isCommitted: Boolean get() = true
  },
  /**
   * Signifies that work on the task has actually started.
   */
  Started {
    override val isActive: Boolean get() = true
    override val isCommitted: Boolean get() = true
  },
  /**
   * Signifies that the task is complete. This generally is the end state of a
   * task.
   */
  Complete {
    override val isFinal: Boolean get() = true
    override val isCommitted: Boolean get() = true
  },
  /**
   * Signifies that the task has failed for some reason.
   */
  Failed {
    override val isFinal: Boolean get() = true
    override val isCommitted: Boolean get() = true
  },
  /**
   * Signifies that the task has been cancelled directly (but not through a failure).
   */
  Cancelled {
    override val isFinal: Boolean get() = true
  },
  /** Signifies that the task has been skipped because a predecessor was cancelled */
  SkippedCancel {
    override val isFinal: Boolean get() = true
    override val isSkipped: Boolean get() = true
  },
  /** Signifies that the task has been skipped because a predecessor failed */
  SkippedFail {
    override val isFinal: Boolean get() = true
    override val isSkipped: Boolean get() = true
  },
  /** Signifies that the task is no longer valid, but overridden by an instance with higher entryno. */
  SkippedInvalidated {
    override val isFinal: Boolean get() = true
    override val isSkipped: Boolean get() = true
  }
  ;

  open val isSkipped: Boolean get() = false
  open val isFinal: Boolean get() = false
  open val isActive: Boolean get() = false
  open val isCommitted: Boolean get() = false
  val canRestart get()= this==FailRetry || this==Pending

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