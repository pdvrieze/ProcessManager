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

package nl.adaptivity.process.userMessageHandler.server

import net.devrieze.util.Handle
import net.devrieze.util.MutableTransactionedHandleMap
import net.devrieze.util.Transaction
import net.devrieze.util.TransactionedHandleMap

import java.sql.SQLException


/**
 * Interface with some specific bets for user tasks
 * Created by pdvrieze on 02/05/16.
 */
interface IUserTaskMap<T : Transaction> : TransactionedHandleMap<XmlTask, T> {

  @Throws(SQLException::class)
  fun containsRemoteHandle(transaction: T, remoteHandle: Long): Handle<XmlTask>?
}

interface IMutableUserTaskMap<T : Transaction> : IUserTaskMap<T>, MutableTransactionedHandleMap<XmlTask, T>