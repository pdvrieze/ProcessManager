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
import io.github.pdvrieze.kotlinsql.dml.Insert
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._MaybeWhere
import io.github.pdvrieze.kotlinsql.monadic.DBReceiver
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import io.github.pdvrieze.kotlinsql.monadic.actions.InsertAction
import io.github.pdvrieze.kotlinsql.monadic.actions.InsertActionCommon
import io.github.pdvrieze.kotlinsql.monadic.actions.mapSeq
import net.devrieze.util.*
import nl.adaptivity.util.net.devrieze.util.ForEachContextImpl
import nl.adaptivity.util.net.devrieze.util.HasForEach
import nl.adaptivity.util.net.devrieze.util.MutableHasForEach
import java.io.Closeable
import java.sql.SQLException
import javax.naming.Context
import javax.naming.InitialContext
import javax.naming.NamingException
import javax.sql.DataSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class DbSet<TMP, T : Any, TR : MonadicDBTransaction<DB>, DB : Database>(
    protected val transactionFactory: DBTransactionFactory<TR, DB>,
    protected open val elementFactory: ElementFactory<TMP, T, TR, DB>,
    val handleAssigner: (T, Handle<T>) -> T? = ::HANDLE_AWARE_ASSIGNER
) : MutableHasForEach<T> {


    /**
     * Iterable that automatically closes
     * @author pdvrieze
     */
    inner class ClosingIterable : Iterable<T>, AutoCloseable, Closeable {

        override fun close() {}

        override fun iterator(): Iterator<T> {
            @Suppress("DEPRECATION")
            return this@DbSet.iterator()
        }

    }

    @Deprecated("Don't use")
    fun closingIterable(): ClosingIterable {
        return ClosingIterable()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Deprecated("Use a Kotlin safe wrapper")
    @SuppressWarnings("resource")
    fun unsafeIterator(readOnly: Boolean): Iterator<T> {
        val columns = elementFactory.createColumns

        return withTransaction {
            SELECT(columns)
                .maybeWHERE { elementFactory.filter(this) }
                .flatMapEach { row ->
                    sequence {
                        yield(elementFactory.createBuilder(this@withTransaction, row))
                    }.asIterable()
                }.flatMap { builders ->
                    builders.map { builder ->
                        elementFactory.createFromBuilder(this@withTransaction, SetAccessImpl(), builder)
                    }
                }.commit()
        }.iterator()
    }

    open fun iterator(): MutableIterator<T> {
        return withTransaction {
            iterator(this).commit()
        }
    }

    @SuppressWarnings("resource")
    @Throws(SQLException::class)
    open fun iterator(dbReceiver: DBReceiver<DB>): DBAction<DB, MutableIterator<T>> {
        val columns = elementFactory.createColumns

        return dbReceiver.transaction {
            val tr = this
            SELECT(columns)
                .maybeWHERE { elementFactory.filter(this) }
                .flatMapEach { rs ->
                    sequence {
                        yield(elementFactory.createBuilder(tr, rs))
                    }.asIterable()
                }.flatMap { builders ->
                    builders.map { builder ->
                        elementFactory.createFromBuilder(tr, SetAccessImpl(), builder)
                    }
                }.map { MutableDbSetIterator(it) }
        }
    }

    override fun forEach(body: HasForEach.ForEachReceiver<T>) {
        withTransaction { forEach(this, body) }
    }

    override fun forEach(body: MutableHasForEach.ForEachReceiver<T>) {
        withTransaction { forEach(this, body) }
    }

    fun forEach(dbReceiver: DBReceiver<DB>, body: MutableHasForEach.ForEachReceiver<T>) {
        dbReceiver.transaction {
            val tr = this
            val toDelete = mutableListOf<T>()
            SELECT(elementFactory.createColumns)
                .maybeWHERE { elementFactory.filter(this) }
                .map { rs ->
                    lateinit var value: T
                    val context = ForEachContextImpl { toDelete.add(value) }
                    while (context.continueIteration && rs.next()) {
                        val builder = elementFactory.createBuilder(tr, rs.rowData)
                        value = builder.then { b -> elementFactory.createFromBuilder(tr, SetAccessImpl(), b) }.evaluateNow()
                        with (body) {
                            eval(context, value)
                        }
                    }
                }.evaluateNow()
            if (toDelete.isNotEmpty()) {
                tr.transaction {
                    for(item in toDelete) {
                        remove(item)
                    }
                }
            }

        }
    }

    val size: Int
        get() = withTransaction {
            size(this).commit()
        }

    @Throws(SQLException::class)
    fun size(dbReceiver: DBReceiver<DB>): DBAction<DB, Int> {
        val column = elementFactory.createColumns[0]
        return with(dbReceiver) {
            SELECT(COUNT(column))
                .maybeWHERE { elementFactory.filter(this) }
                .mapSeq { it.single()!! }
        }
    }

    operator fun contains(element: Any): Boolean {
        return withTransaction {
            contains(this, element).commit()
        }
    }

    @Throws(SQLException::class)
    open fun contains(dbReceiver: DBReceiver<DB>, element: Any): DBAction<DB, Boolean> {
        val instance = elementFactory.asInstance(element) ?: return dbReceiver.value(false)

        return with(dbReceiver) {
            SELECT(COUNT(elementFactory.createColumns[0]))
                .maybeWHERE { elementFactory.getPrimaryKeyCondition(this, instance) AND elementFactory.filter(this) }
                .mapSeq { it.single()!! > 0 }
        }
    }

    fun add(elem: T): Boolean {
        return withTransaction {
            add(this, elem).commit()
        }
    }

    fun add(dbReceiver: DBReceiver<DB>, elem: T): DBAction<DB, Boolean> {
        return dbReceiver.transaction {
            elementFactory.insertStatement(this, elem)
                .keys(elementFactory.keyColumn)
                .then {
                    when (val handle = it.singleOrNull()) {
                        null -> value(false)
                        else -> {
                            val newElem = handleAssigner(elem, handle) ?: elem // No assignment we keep the element
                            elementFactory.postStore(this@transaction, handle, null, newElem)
                        }
                    }
                }
        }
    }

    fun addAll(collection: Collection<T>): Boolean {
        return withTransaction {
            addAll(this, collection).commit()
        }
    }

    @Throws(SQLException::class)
    fun addAll(dbReceiver: DBReceiver<DB>, collection: Collection<T>): DBAction<DB, Boolean> {
        if (collection.isEmpty()) return dbReceiver.value(false)
        return dbReceiver.transaction {
            val tr = this
            val insert = elementFactory.insertStatement(this)
            (collection.fold<T, InsertActionCommon<DB, Insert>>(insert) { i, value ->
                elementFactory.insertValues(tr, i, value)
            } as InsertAction<DB, Insert>)
                .keys(elementFactory.keyColumn)
                .flatMap { handles ->
                    collection.zip(handles).map { (elem, handle) ->
                        when (handle) {
                            null -> value(false)
                            else -> {
                                val newElem = handleAssigner(elem, handle) ?: elem // No assignment we keep the element
                                elementFactory.postStore(tr, handle, null, newElem)
                            }
                        }
                        value(true)
                    }
                }
                .map { it.any() }
        }
    }

    fun remove(value: Any): Boolean {
        return withTransaction {
            remove(this, value).commit()
        }
    }

    fun remove(dbReceiver: DBReceiver<DB>, value: Any): DBAction<DB, Boolean> {
        dbReceiver.transaction {
            val elem = elementFactory.asInstance(value) ?: return value(false)
            return elementFactory.preRemove(this, elem)
                .then(DELETE_FROM(elementFactory.table)
                    .maybeWHERE { elementFactory.getPrimaryKeyCondition(this, elem) }
                ).map { it > 0 }
        }
    }

    fun clear() {
        withTransaction {
            clear(this).commit()
        }
    }

    @Throws(SQLException::class)
    fun clear(dbReceiver: DBReceiver<DB>): DBAction<DB, Int> {
        dbReceiver.transaction {
            return elementFactory.preClear(this)
                .then(DELETE_FROM(elementFactory.table))
        }
    }

    @Throws(SQLException::class)
    fun isEmpty(dbReceiver: DBReceiver<DB>): DBAction<DB, Boolean> {
        return size(dbReceiver).map { it == 0 }
    }

    fun removeAll(selection: _MaybeWhere.() -> WhereClause?): Boolean {
        return withTransaction { removeAll(this, selection).commit() }
    }

    @Throws(SQLException::class)
    fun removeAll(dbReceiver: DBReceiver<DB>, selection: _MaybeWhere.() -> WhereClause?): DBAction<DB, Boolean> {
        return dbReceiver.transaction {
            SELECT(elementFactory.createColumns)
                .maybeWHERE(selection)
                .mapEachRS { valueRow ->
                    val values = columns.mapIndexed { i, col ->
                        valueRow.value(col, i + 1)
//                        col.type.fromResultSet(valueRow.rawResultSet, i + 1)
                    }
                    elementFactory.preRemove(this@transaction, columns, values)
                }.then(
                    DELETE_FROM(elementFactory.table)
                        .maybeWHERE(selection)
                        .map { it > 0 }
                )
        }
    }

    @Throws(SQLException::class)
    protected fun <W : T> addWithKey(dbReceiver: DBReceiver<DB>, elem: W): DBAction<DB, Handle<W>?> {
        dbReceiver.transaction {
            val tr = this
            val stmt = elementFactory.insertStatement(tr, elem)
            return stmt.keys(elementFactory.keyColumn)
                .then { handles ->
                    @Suppress("UNCHECKED_CAST")
                    when (val handle = handles.singleOrNull() as Handle<W>?) {
                        null -> dbReceiver.value(null)
                        else -> {
                            val newElem = handleAssigner(elem, handle) ?: elem
                            elementFactory.postStore(tr, handle, null, newElem)
                                .then(dbReceiver.value(handle))
                        }
                    }
                }
        }
    }

    override fun toString(): String {
        return "DbSet <SELECT * FROM `" + elementFactory.table._name + "`>"
    }

    @OptIn(ExperimentalContracts::class)
    protected inline fun <R> withTransaction(action: TR.() -> R): R {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        val tr = transactionFactory.startTransaction()
        try {
            return tr.action()
        } finally {
            tr.close()
        }
    }

    @OptIn(ExperimentalContracts::class)
    protected inline fun <R> DBReceiver<DB>.transaction(action: TR.() -> R): R {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }

        return transactionFactory.asTransaction(this).action()
    }

    companion object {

        @Throws(SQLException::class)
        internal fun commitIfTrue(pTransaction: Transaction, pValue: Boolean): Boolean {
            if (pValue) {
                pTransaction.commit()
            }
            return pValue
        }

        @Deprecated("Kotlin does it better", ReplaceWith("pList.joinToString(pSeparator)"), DeprecationLevel.ERROR)
        fun join(pList: List<CharSequence>?, pSeparator: CharSequence): CharSequence? {
            return pList?.joinToString(pSeparator)
        }

        @JvmName("joinNotNull")
        @Deprecated("Kotlin does it better", ReplaceWith("list.joinToString(separator)"), DeprecationLevel.ERROR)
        fun join(list: List<CharSequence>, separator: CharSequence): CharSequence {
            return list.joinToString(separator)
        }

        @JvmStatic
        fun join(
            pList1: List<CharSequence>,
            pList2: List<CharSequence>,
            pOuterSeparator: CharSequence,
            pInnerSeparator: CharSequence
        ): CharSequence {
            if (pList1.size != pList2.size) {
                throw IllegalArgumentException("List sizes must match")
            }
            if (pList1.isEmpty()) {
                return ""
            }
            val result = StringBuilder()
            val it1 = pList1.iterator()
            val it2 = pList2.iterator()

            result.append(it1.next()).append(pInnerSeparator).append(it2.next())
            while (it1.hasNext()) {
                result.append(pOuterSeparator).append(it1.next()).append(pInnerSeparator).append(it2.next())
            }
            return result
        }

        @JvmStatic
        @Deprecated(
            "use {@link #resourceNameToDataSource(Context, String)}, that is more reliable as the context can be gained earlier",
            ReplaceWith("resourceNameToDataSource(context, pResourceName)")
        )
        fun resourceNameToDataSource2(pResourceName: String): DataSource {
            try {
                return InitialContext().lookup(pResourceName) as DataSource
            } catch (e: NamingException) {
                throw RuntimeException(e)
            }

        }

        @JvmStatic
        fun resourceNameToDataSource(pContext: Context?, pDbresourcename: String): DataSource {
            return when (pContext) {
                null ->
                    try {
                        InitialContext().lookup(pDbresourcename) as DataSource
                    } catch (e: NamingException) {
                        InitialContext().lookup("java:/comp/env/" + pDbresourcename) as DataSource
                    }

                else -> pContext.lookup(pDbresourcename) as DataSource
            }
        }
    }

    interface DBSetAccess<TMP> {

    }

    private inner class SetAccessImpl<TMP> : DBSetAccess<TMP>

    private inner class MutableDbSetIterator(private val data: List<T>) : MutableIterator<T> {
        private var nextPos = 0

        override fun hasNext(): Boolean {
            return nextPos < data.size
        }

        override fun next(): T {
            return data[nextPos++]
        }

        override fun remove() {
            remove(data[nextPos - 1]) // As the iterator uses a copy of the list this is valid.
        }
    }
}
