/*
 * Copyright (c) 2018.
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

package nl.adaptivity.process.userMessageHandler.server

import net.devrieze.util.Handle
import net.devrieze.util.MutableHandleAware
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.util.security.Principal


interface UserTask<T : UserTask<T>> : MutableHandleAware<T> {


  interface TaskItem {

    val options: List<String>

    val value: String?

    val type: String?

    val name: String?

    val params: String?

    val label: String?

  }

  val state: NodeInstanceState?

  fun setState(newState: NodeInstanceState, user: Principal)

  fun setEndpoint(endPoint: EndpointDescriptorImpl)

  val owner: Principal?

  val items: List<TaskItem>

  val remoteHandle: Handle<*>

  val instanceHandle: Handle<*>

  val handleValue: Long

  val summary: String?

}
