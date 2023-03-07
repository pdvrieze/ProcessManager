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

package nl.adaptivity.process

import net.devrieze.util.*
import nl.adaptivity.util.net.devrieze.util.HasForEach
import nl.adaptivity.util.net.devrieze.util.MutableHasForEach

import java.sql.SQLException


/**
 * Created by pdvrieze on 09/12/15.
 */
open class MemTransactionedHandleMap<T : Any, TR : StubTransaction>(private val assigner: Assigner<T, TR>) :
    MemHandleMap<T>(handleAssigner = { t: T, h: Handle<T> -> assigner(t, h) }), MutableTransactionedHandleMap<T, TR> {

    @JvmOverloads
    constructor(
        handleAssigner: (TR, T, Handle<T>) -> T? = { transaction, value, handle ->
            HANDLE_AWARE_ASSIGNER(transaction, value, handle)
        }
    ) : this(Assigner(handleAssigner))

    class Assigner<T, TR : StubTransaction>(private val base: (TR, T, Handle<T>) -> T?) {
        lateinit var transaction: TR
        operator fun invoke(t: T, h: Handle<T>): T? {
            return base(transaction, t, h)
        }
    }

    interface TestTransactionFactory<TR : StubTransaction> {
        fun newTransaction(): TR
    }

    private class IteratorWrapper<T>(private val delegate: Iterator<T>, private val readOnly: Boolean) :
        MutableIterator<T> {

        override fun remove() {
            if (readOnly) throw UnsupportedOperationException("The iterator is read-only")
        }

        override fun next(): T {
            return delegate.next()
        }

        override fun hasNext(): Boolean {
            return delegate.hasNext()
        }
    }

    @Throws(SQLException::class)
    override fun <W : T> put(transaction: TR, value: W): Handle<W> {
        assigner.transaction = transaction
        return put(value)
    }

    @Throws(SQLException::class)
    override fun get(transaction: TR, handle: Handle<T>): T? {
        return get(handle)
    }

    @Throws(SQLException::class)
    override fun castOrGet(transaction: TR, handle: Handle<T>): T? {
        return get(handle)
    }

    @Throws(SQLException::class)
    override fun set(transaction: TR, handle: Handle<T>, value: T): T? {
        assigner.transaction = transaction
        return set(handle, value)
    }

    /*
     * Direct usage of this function is unsafe, but it is used to implement the transactioned version.
     */
    @Deprecated("Use the transactioned version")
    override fun forEach(body: HasForEach.ForEachReceiver<T>) {
        super<MemHandleMap>.forEach(body)
    }

    override fun forEach(transaction: TR, body: HasForEach.ForEachReceiver<T>) {
        forEach(transaction, body as MutableHasForEach.ForEachReceiver<T>)
    }

    override fun forEach(transaction: TR, body: MutableHasForEach.ForEachReceiver<T>) {
        assigner.transaction = transaction
        forEach(body)
    }

    @Deprecated("Unsafe as it does not guarantee closing the transaction")
    override fun iterable(transaction: TR): MutableIterable<T> {
        assigner.transaction = transaction
        return this
    }

    @Deprecated("Unsafe as it does not guarantee closing the transaction")
    override fun iterator(transaction: TR, readOnly: Boolean): MutableIterator<T> {
        assigner.transaction = transaction
        return IteratorWrapper(iterator(), readOnly)
    }

    @Throws(SQLException::class)
    override fun containsAll(transaction: TR, c: Collection<*>) =
        c.all { it != null && contains(it) }

    @Throws(SQLException::class)
    override fun containsElement(transaction: TR, element: Any): Boolean {
        return contains(element)
    }

    @Throws(SQLException::class)
    override fun contains(transaction: TR, handle: Handle<T>): Boolean {
        return contains(handle)
    }

    @Throws(SQLException::class)
    override fun remove(transaction: TR, handle: Handle<T>): Boolean {
        assigner.transaction = transaction
        return remove(handle)
    }

    override fun invalidateCache(handle: Handle<T>) { /* No-op */
    }

    override fun invalidateCache() { /* No-op */
    }

    @Throws(SQLException::class)
    override fun clear(transaction: TR) {
        assigner.transaction = transaction
        clear()
    }

    override fun withTransaction(transaction: TR): MutableHandleMap<T> {
        assigner.transaction = transaction
        return MutableHandleMapForwarder(transaction, this)
    }
}
