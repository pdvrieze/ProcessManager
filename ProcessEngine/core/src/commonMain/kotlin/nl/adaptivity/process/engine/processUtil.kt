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

package nl.adaptivity.process.engine

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.HandleNotFoundException
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier

/**
 * Utilities for handling process things
 */

fun <N: ProcessNode> N?.mustExist(id:Identifiable): N = this ?: throw ProcessException("The node with id $id is missing")

@Suppress("NOTHING_TO_INLINE")
inline fun <N: ProcessNode> N?.mustExist(id:String): N = mustExist(Identifier(id))

fun <T: ProcessNode> ProcessModel<T>.requireNode(id:Identifiable):T = getNode(id).mustExist(id)

@Suppress("NOTHING_TO_INLINE")
inline fun <T: ProcessNode> ProcessModel<T>.requireNode(id:String):T = requireNode(Identifier(id))

/**
 * Verify that the node instance exists. If it doesn't exist this is an internal error
 * @return The node
 * @throws IllegalStateException If it doesn't
 */
fun <T: ProcessTransaction, N: ProcessNodeInstance<*>> N?.mustExist(handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>): N = this ?: throw IllegalStateException("Node instance missing: $handle")

/**
 * Verify that the node exists. Non-existance could be user errror.
 * @return The node
 * @throws HandleNotFoundException If it doesn't.
 */
fun <T: ProcessTransaction, N: ProcessNodeInstance<*>> N?.shouldExist(handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>): N = this ?: throw HandleNotFoundException("Node instance missing: $handle")

/**
 * Verify that the object instance exists. If it doesn't exist this is an internal error
 * @return The node
 * @throws IllegalStateException If it doesn't
 */
fun <N:SecureObject<V>, V:Any> N?.mustExist(handle: Handle<SecureObject<V>>): N = this ?: throw IllegalStateException("Process engine element missing: $handle")

/**
 * Verify that the object exists. If it doesn't exist this is an internal error
 * @return The node
 * @throws IllegalStateException If it doesn't
 */
fun <N:SecureObject<V>, V:Any> N?.shouldExist(handle: Handle<SecureObject<V>>): N = this ?: throw HandleNotFoundException("Process engine element missing: $handle")

/**
 * Verify that the node instance exists. If it doesn't exist this is an internal error
 * @return The node
 * @throws IllegalStateException If it doesn't
 */
fun <T: ProcessTransaction> ProcessInstance?.mustExist(handle: ComparableHandle<SecureObject<ProcessInstance>>): ProcessInstance = this ?: throw IllegalStateException("Node instance missing: $handle")

/**
 * Verify that the node exists. Non-existance could be user errror.
 * @return The node
 * @throws HandleNotFoundException If it doesn't.
 */
fun <T: ProcessTransaction> ProcessInstance?.shouldExist(handle: ComparableHandle<SecureObject<ProcessInstance>>): ProcessInstance = this ?: throw HandleNotFoundException("Node instance missing: $handle")

/**
 * Verify that the node instance exists. If it doesn't exist this is an internal error
 * @return The node
 * @throws IllegalStateException If it doesn't
 */
fun <N: ProcessNode, M: RootProcessModel<N>> M?.mustExist(handle: Handle<RootProcessModel<N>>): M = this ?: throw IllegalStateException("Node instance missing: $handle")

/**
 * Verify that the node exists. Non-existance could be user errror.
 * @return The node
 * @throws HandleNotFoundException If it doesn't.
 */
fun <N: ProcessNode, M: RootProcessModel<N>> M?.shouldExist(handle: Handle<RootProcessModel<N>>): M = this ?: throw HandleNotFoundException("Node instance missing: $handle")
