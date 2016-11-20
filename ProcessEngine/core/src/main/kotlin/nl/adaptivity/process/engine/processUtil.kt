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

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import net.devrieze.util.Transaction
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import java.io.FileNotFoundException

/**
 * Utilities for handling process things
 */

/**
 * Verify that the node instance exists. If it doesn't exist this is an internal error
 * @return The node
 * @throws IllegalStateException If it doesn't
 */
fun <T: Transaction, N:ProcessNodeInstance<T>> N?.mustExist(handle: Handle<out ProcessNodeInstance<T>>): N = this ?: throw IllegalStateException("Node instance missing: $handle")

/**
 * Verify that the node exists. Non-existance could be user errror.
 * @return The node
 * @throws FileNotFoundException If it doesn't.
 */
fun <T: Transaction, N:ProcessNodeInstance<T>> N?.shouldExist(handle: Handle<out ProcessNodeInstance<T>>): N = this ?: throw FileNotFoundException("Node instance missing: $handle")

/**
 * Verify that the node instance exists. If it doesn't exist this is an internal error
 * @return The node
 * @throws IllegalStateException If it doesn't
 */
fun <T: Transaction, I:ProcessInstance<T>> I?.mustExist(handle: Handle<out ProcessInstance<T>>): I = this ?: throw IllegalStateException("Node instance missing: $handle")

/**
 * Verify that the node exists. Non-existance could be user errror.
 * @return The node
 * @throws FileNotFoundException If it doesn't.
 */
fun <T: Transaction, I:ProcessInstance<T>> I?.shouldExist(handle: Handle<out ProcessInstance<T>>): I = this ?: throw FileNotFoundException("Node instance missing: $handle")

/**
 * Verify that the node instance exists. If it doesn't exist this is an internal error
 * @return The node
 * @throws IllegalStateException If it doesn't
 */
fun <N: ProcessNode<N, M>, M: ProcessModel<N,M>> M?.mustExist(handle: Handle<out ProcessModel<N,M>>): M = this ?: throw IllegalStateException("Node instance missing: $handle")

/**
 * Verify that the node exists. Non-existance could be user errror.
 * @return The node
 * @throws FileNotFoundException If it doesn't.
 */
fun <N: ProcessNode<N, M>, M: ProcessModel<N,M>> M?.shouldExist(handle: Handle<out ProcessModel<N,M>>): M = this ?: throw FileNotFoundException("Node instance missing: $handle")
