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

package net.devrieze.util.db

import io.github.pdvrieze.kotlinsql.ddl.Database
import io.github.pdvrieze.kotlinsql.monadic.DBReceiver
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import io.github.pdvrieze.kotlinsql.monadic.actions.mapSeq
import net.devrieze.util.*
import nl.adaptivity.util.net.devrieze.util.HasForEach
import nl.adaptivity.util.net.devrieze.util.MutableHasForEach
import java.sql.SQLException
import java.util.*

open class DBHandleMap<TMP, V : Any, TR : MonadicDBTransaction<DB>, DB : Database>(
    transactionFactory: DBTransactionFactory<TR, DB>,
    elementFactory: HMElementFactory<TMP, V, TR, DB>,
    handleAssigner: (V, Handle<V>) -> V? = ::HANDLE_AWARE_ASSIGNER
) : DbSet<TMP, V, TR, DB>(transactionFactory, elementFactory, handleAssigner),
    MutableTransactionedHandleMap<V, TR> {

    override val elementFactory: HMElementFactory<TMP, V, TR, DB>
        get() = super.elementFactory as HMElementFactory<TMP, V, TR, DB>

    private val pendingCreates = TreeMap<Handle<V>, TMP>()

    protected fun isPending(handle: Handle<V>): Boolean {
        return pendingCreates.containsKey(handle)
    }


    fun pendingValue(handle: Handle<V>): TMP? {
        return pendingCreates[handle]
    }

    fun <W : V> put(value: W): Handle<W> = withDB { dbReceiver ->
        put(dbReceiver, value)
    }

    fun <W : V> put(receiver: DBReceiver<DB>, value: W): DBAction<DB, Handle<W>> {
        return addWithKey(receiver, value).map { it ?: throw RuntimeException("Adding element $value failed") }
    }

    @Deprecated("Use monadic function")
    override fun <W : V> put(transaction: TR, value: W): Handle<W> =
        with(transaction) {
            put(receiver = this, value = value).evaluateNow()
        }

    fun get(handle: Handle<V>): V? = withDB { dbReceiver ->
        get(dbReceiver, handle)
    }

    override fun get(transaction: TR, handle: Handle<V>): V? {
        return with(transaction) {
            get(dbReceiver = transaction, handle).evaluateNow()
        }
    }

    fun get(dbReceiver: DBReceiver<DB>, handle: Handle<V>): DBAction<DB, V?> {
        dbReceiver.transaction {
            val tr = this
            if (pendingCreates.containsKey(handle)) {
                error("Pending create")
            }

            val factory = elementFactory

            return SELECT(factory.createColumns)
                .WHERE { factory.getHandleCondition(this, handle) AND factory.filter(this) }
                .flatMapEach { rowData ->
                    sequence {
                        yield(elementFactory.createBuilder(tr, rowData))
                    }.asIterable()
                }.then {
                    it.singleOrNull()?.let { result ->
                        factory.createFromBuilder(tr, FactoryAccess(), result as TMP)
                    } ?: value(null)
                }
        }
    }

    fun castOrGet(handle: Handle<V>): V? = withDB { dbReceiver ->
        castOrGet(dbReceiver, handle)
    }

    override fun castOrGet(transaction: TR, handle: Handle<V>): V? {
        return with(transaction) { castOrGet(dbReceiver = this, handle).evaluateNow() }
    }

    fun castOrGet(dbReceiver: DBReceiver<DB>, handle: Handle<V>): DBAction<DB, V?> {
        val element = elementFactory.asInstance(handle)
        if (element != null) {
            return dbReceiver.value(element)
        } // If the element is it's own handle, don't bother looking it up.
        return get(dbReceiver, handle)
    }

    fun set(handle: Handle<V>, value: V): V? = withDB { dbReceiver ->
        set(dbReceiver, handle, value)
    }

    @Throws(SQLException::class)
    override fun set(transaction: TR, handle: Handle<V>, value: V): V? {

        return with(transaction) {
            get(dbReceiver = transaction, handle).then { oldValue ->
                set(transaction, handle, oldValue, value)
            }.evaluateNow()
        }
    }

    fun set(dbReceiver: DBReceiver<DB>, handle: Handle<V>, value: V): DBAction<DB, V?> {
        return get(dbReceiver, handle).then { oldValue ->
            set(dbReceiver, handle, oldValue, value)
        }
    }


    @Throws(SQLException::class)
    protected operator fun set(
        dbReceiver: DBReceiver<DB>,
        handle: Handle<V>,
        oldValue: V?,
        newValue: V
    ): DBAction<DB, V?> {
        if (elementFactory.isEqualForStorage(oldValue, newValue)) {
            return dbReceiver.value(newValue)
        }

        val newValueWithHandle = handleAssigner(newValue, handle) ?: newValue

        return dbReceiver.transaction {
            val tr = this
            UPDATE { elementFactory.store(this, newValueWithHandle) }
                .WHERE { elementFactory.filter(this) AND elementFactory.getHandleCondition(this, handle) }
                .then {
                    elementFactory.postStore(tr, handle, oldValue, newValueWithHandle)
                        .then(value(oldValue))

                }
        }
    }

    override fun forEach(transaction: TR, body: MutableHasForEach.ForEachReceiver<V>) {
        super<DbSet>.forEach(transaction, body)
    }

    override fun forEach(transaction: TR, body: HasForEach.ForEachReceiver<V>) {
        super<DbSet>.forEach(transaction, body)
    }

    @Deprecated("Unsafe as it does not guarantee closing the transaction")
    override fun iterator(transaction: TR, readOnly: Boolean): MutableIterator<V> {
        return with(transaction) {
            super.iterator(dbReceiver = transaction).evaluateNow()
        }
    }

    /*
    override fun iterator2(transaction: TR, readOnly: Boolean): MutableAutoCloseableIterator<V> {
        try {
            return super.iterator(transaction, readOnly)
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }

    }
*/

    @Deprecated("Unsafe as it does not guarantee closing the transaction")
    override fun iterable(transaction: TR): MutableIterable<V> {
        return with(transaction) {
            iterable(dbReceiver = this).evaluateNow()
        }
    }

    fun iterable(dbReceiver: DBReceiver<DB>): DBAction<DB, MutableIterable<V>> {
        return with(dbReceiver) {
            iterator(this).map {
                object : MutableIterable<V> {
                    override fun iterator(): MutableIterator<V> = it
                }
            }
        }
    }

    fun containsElement(element: Any): Boolean {
        if (element is Handle<*>) {
            return containsElement(element.handleValue)
        }
        return super.contains(element)
    }

    override fun containsElement(transaction: TR, element: Any): Boolean {
        return with(transaction) { containsElement(dbReceiver = this, element).evaluateNow() }
    }

    fun containsElement(dbReceiver: DBReceiver<DB>, element: Any): DBAction<DB, Boolean> {
        if (element is Handle<*>) {
            return containsElement(dbReceiver, element.handleValue)
        }
        return super.contains(dbReceiver, element)
    }

    override fun contains(transaction: TR, handle: Handle<V>): Boolean {
        return with(transaction) { contains(dbReceiver = this, handle).evaluateNow() }
    }

    override fun contains(dbReceiver: DBReceiver<DB>, element: Any): DBAction<DB, Boolean> {
        @Suppress("UNCHECKED_CAST")
        return when (element) {
            is Handle<*> -> contains(dbReceiver, handle = element as Handle<V>)
            else -> super.contains(dbReceiver, element)
        }
    }

    fun contains(dbReceiver: DBReceiver<DB>, handle: Handle<V>): DBAction<DB, Boolean> {
        return with(dbReceiver) {
            SELECT(COUNT(elementFactory.createColumns[0]))
                .WHERE { elementFactory.getHandleCondition(this, handle) AND elementFactory.filter(this) }
                .mapSeq { it.single()!! > 0 }
        }
    }

    /*
        @Throws(SQLException::class)
        override fun contains2(transaction: TR, handle: Handle<V>): Boolean {
            val query = database
                .SELECT(database.COUNT(elementFactory.createColumns[0]))
                .WHERE { elementFactory.getHandleCondition(this, handle) AND elementFactory.filter(this) }

            try {
                return query.getSingleList(transaction.connection) { _, data ->
                    data[0] as Int > 0
                }
            } catch (e: RuntimeException) {
                return false
            }
        }
    */

    fun containsAll(c: Collection<*>): Boolean = withDB { dbReceiver ->
        containsAll(dbReceiver, c)
    }

    override fun containsAll(transaction: TR, c: Collection<*>): Boolean {
        return with(transaction) { containsAll(dbReceiver = this, c).evaluateNow() }
    }

    fun containsAll(dbReceiver: DBReceiver<DB>, c: Collection<*>): DBAction<DB, Boolean> {
        return with(dbReceiver) {
            c.fold(value(true)) { i: DBAction<DB, Boolean>, elem: Any? ->
                when (elem) {
                    null -> value(false)
                    else -> i.then { acc ->
                        when (acc) {
                            true -> contains(dbReceiver = dbReceiver, elem)
                            else -> value(false)
                        }
                    }
                }
            }
        }
    }

    fun remove(handle: Handle<V>): Boolean {
        return withTransaction { remove(dbReceiver = this, handle).commit() }
    }

    override fun remove(transaction: TR, handle: Handle<V>): Boolean {
        return with(transaction) { remove(dbReceiver = this, handle).evaluateNow() }
    }

    fun remove(dbReceiver: DBReceiver<DB>, handle: Handle<V>): DBAction<DB, Boolean> {
        return dbReceiver.transaction {
            elementFactory.preRemove(this, handle)
                .then {
                    DELETE_FROM(elementFactory.table)
                        .WHERE { elementFactory.getHandleCondition(this, handle) AND elementFactory.filter(this) }
                        .map { it > 0 }
                }
        }
    }

    override fun clear(transaction: TR) {
        return with(transaction) { clear(dbReceiver = this).evaluateNow() }
    }

    override fun invalidateCache(handle: Handle<V>) =// No-op, there is no cache
        Unit

    override fun invalidateCache() {
        /* No-op, no cache */
    }

    private inline fun <R> withDB(crossinline action: (DBReceiver<DB>) -> DBAction<DB, R>): R {
        return withTransaction { action(this).commit() }
    }

    private inner class FactoryAccess() : DBSetAccess<TMP> {

    }

}
