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

import net.devrieze.util.*
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.Database.WhereClause
import uk.ac.bournemouth.kotlinsql.Database._Where
import uk.ac.bournemouth.kotlinsql.getSingleList
import uk.ac.bournemouth.util.kotlin.sql.StatementHelper
import java.io.Closeable
import java.sql.*
import java.util.*
import javax.naming.Context
import javax.naming.InitialContext
import javax.naming.NamingException
import javax.sql.DataSource

open class DbSet<TMP, T : Any, TR : DBTransaction>(
    protected val transactionFactory: TransactionFactory<out TR>,
    val database: Database,
    protected open val elementFactory: ElementFactory<TMP, T, TR>,
    val handleAssigner: (T, Handle<T>) -> T? = ::HANDLE_AWARE_ASSIGNER) : AutoCloseable, Closeable {


    /**
     * Iterable that automatically closes
     * @author pdvrieze
     */
    inner class ClosingIterable : Iterable<T>, AutoCloseable, Closeable {

        override fun close() {
            this@DbSet.close()
        }

        override fun iterator(): MutableIterator<T> {
            @Suppress("DEPRECATION")
            return this@DbSet.unsafeIterator(false)
        }

    }

    private inner class ResultSetIterator @Throws(SQLException::class)
    constructor(private val transaction: TR,
                private val statement: PreparedStatement,
                private val columns: List<Column<*, *, *>>,
                private val resultSet: ResultSet,
                private val readOnly: Boolean=false) : MutableAutoCloseableIterator<T> {
        private var nextElem: T? = null
        private var isFinished = false
        private val doCloseOnFinish: Boolean= false

        init {
            resultSet.metaData
        }

        @Throws(SQLException::class)
        fun size(): Int {
            val pos = resultSet.row
            try {
                resultSet.last()
                return resultSet.row
            } finally {
                resultSet.absolute(pos)
            }
        }

        override fun hasNext(): Boolean {
            if (isFinished) {
                return false
            }
            if (nextElem != null) {
                return true
            }

            try {

                val nextTmp = if (resultSet.next()) {
                    val values = columns.mapIndexed { i, column -> column.type.fromResultSet(resultSet, i + 1) }
                    elementFactory.create(transaction, columns, values)
                } else {
                    isFinished = true
                    transaction.commit()
                    if (doCloseOnFinish) {
                        closeResultSet(transaction, statement, resultSet)
                    } else {
                        closeResultSet(null, statement, resultSet)
                    }
                    return false
                }
                nextElem = elementFactory.postCreate(transaction, nextTmp)
                return true
            } catch (ex: SQLException) {
                closeResultSet(transaction, statement, resultSet)
                throw RuntimeException(ex)
            }

        }

        override fun next(): T {
            val currentElem = nextElem
            nextElem = null

            if (currentElem != null) return currentElem

            if (!hasNext()) { // hasNext will actually update mNextElem;
                throw IllegalStateException("Reading beyond iterator")
            }
            return nextElem!!
        }

        override fun remove() {
            if (readOnly) throw UnsupportedOperationException("The iterator is read only")
            try {
                resultSet.deleteRow()
            } catch (ex: SQLException) {
                closeResultSet(transaction, statement, resultSet)
                throw ex
            }

        }

        override fun close() {
            try {
                try {
                    resultSet.close()
                } finally {
                    statement.close()
                }
            } finally {
                iterators.remove(this)
                if (iterators.isEmpty()) {
                    this@DbSet.close()
                }
            }

        }
    }


    private val iterators = ArrayList<ResultSetIterator>()

    fun closingIterable(): ClosingIterable {
        return ClosingIterable()
    }

    @Deprecated("Use a Kotlin safe wrapper")
    @SuppressWarnings("resource")
    fun unsafeIterator(readOnly: Boolean): MutableAutoCloseableIterator<T> {
        val transaction: TR
        var statement: StatementHelper? = null
        try {
            transaction = transactionFactory.startTransaction()
            //      connection = mTransactionFactory.getConnection();
            //      connection.setAutoCommit(false);
            val columns = elementFactory.createColumns

            val query = database.SELECT(columns).WHERE { elementFactory.filter(this) }

            statement = query.toSQL().let { sql: String ->
                StatementHelper(transaction.connection.rawConnection.prepareStatement(sql), sql)
            }

            query.setParams(statement)

            val rs = statement.statement.executeQuery()
            val it = ResultSetIterator(transaction, statement.statement, columns, rs, readOnly)
            iterators.add(it)
            return it
        } catch (e: Exception) {
            try {
                if (statement != null) {
                    statement.close()
                }
            } catch (ex: SQLException) {
                val runtimeException = RuntimeException(ex)
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (runtimeException as java.lang.Throwable).addSuppressed(e)
                throw runtimeException
            }

            if (e is RuntimeException) {
                throw e
            }
            throw RuntimeException(e)
        }

    }

    @SuppressWarnings("resource")
    @Throws(SQLException::class)
    open fun iterator(transaction: TR, readOnly: Boolean): MutableAutoCloseableIterator<T> {
        try {
            val columns = elementFactory.createColumns

            val query = database.SELECT(columns).WHERE { elementFactory.filter(this) }

            val statement = query.toSQL().let { sql: String ->
                StatementHelper(transaction.connection.rawConnection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
                                                                                      if (readOnly) ResultSet.CONCUR_READ_ONLY else ResultSet.CONCUR_UPDATABLE),
                                sql)
            }

            query.setParams(statement)

            val rs = statement.statement.executeQuery()
            val it = ResultSetIterator(transaction, statement.statement, columns, rs)
            iterators.add(it)
            return it
        } catch (e: RuntimeException) {
            rollbackConnection(transaction, e)
            close()
            throw e
        } catch (e: SQLException) {
            close()
            throw e
        }

    }

    @Throws(SQLException::class)
    fun size(connection: DBTransaction): Int {
        val column = elementFactory.createColumns[0]
        val select = database.SELECT(database.COUNT(column))
        val filter = elementFactory.filter(_Where())
        val query = if (filter == null) select else select.WHERE { filter }
        return query.getSingleList(connection.connection) { _, values -> values[0] as Int }
    }

    @Throws(SQLException::class)
    open fun contains(transaction: DBTransaction, element: Any): Boolean {
        val instance = elementFactory.asInstance(element) ?: return false

        val query = database
            .SELECT(database.COUNT(elementFactory.createColumns[0]))
            .WHERE { elementFactory.getPrimaryKeyCondition(this, instance) AND elementFactory.filter(this) }

        try {
            return query.getSingleList(transaction.connection) { _, data ->
                data[0] as Int > 0
            }
        } catch (e: RuntimeException) {
            return false
        }
    }

    fun add(transaction: DBTransaction, elem: T): Boolean {
        assert(transactionFactory.isValidTransaction(transaction))

        val stmt = elementFactory.insertStatement(elem)
        return stmt.execute(transaction.connection, elementFactory.keyColumn) { handle ->
            if (handle != null) {
                val newElem = handleAssigner(elem, handle) ?: elem // No assignment we keep the element
                elementFactory.postStore(transaction.connection, handle, null, newElem)
                true
            } else false
        }.let { it.isNotEmpty() && it.all { it } }
    }

    @Throws(SQLException::class)
    fun addAll(transaction: DBTransaction, collection: Collection<T>): Boolean {
        return collection
            .asSequence()
            .map { elem ->
                add(transaction, elem)
            }.reduce { b1, b2 -> b1 || b2 }
    }

    @Throws(SQLException::class)
    fun remove(transaction: TR, value: Any): Boolean {
        elementFactory.asInstance(value)?.let { elem ->
            elementFactory.preRemove(transaction, elem)
            val delete = database.DELETE_FROM(elementFactory.table).WHERE {
                elementFactory.getPrimaryKeyCondition(this, elem)
            }
            return delete.executeUpdate(transaction.connection) > 0
        } ?: return false
    }

    @Throws(SQLException::class)
    fun clear(transaction: TR) {
        elementFactory.preClear(transaction)
        val delete = database
            .DELETE_FROM(elementFactory.table)
            .WHERE { elementFactory.filter(this) }

        delete.executeUpdate(transaction.connection)
    }

    override fun close() {
        var errors: MutableList<RuntimeException>? = null
        for (iterator in iterators) {
            try {
                iterator.close()
            } catch (e: RuntimeException) {
                if (errors == null) {
                    errors = ArrayList<RuntimeException>()
                }
                errors.add(e)
            }

        }
        if (errors != null) {
            val it = errors.iterator()
            val ex = it.next()
            while (it.hasNext()) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (ex as java.lang.Throwable).addSuppressed(it.next())
            }
            throw ex
        }
    }

    @Throws(SQLException::class)
    fun isEmpty(pTransaction: TR): Boolean {
        @Suppress("UNCHECKED_CAST")
        (iterator(pTransaction, true) as DbSet<TMP, T, TR>.ResultSetIterator).use { it -> return it.hasNext() }
    }

    @Throws(SQLException::class)
    fun removeAll(transaction: TR, selection: _Where.() -> WhereClause?): Boolean {
        run {
            database
                .SELECT(elementFactory.createColumns)
                .WHERE { elementFactory.filter(this) AND this.selection() }
                .executeList(transaction.connection) { columns, values ->
                    elementFactory.preRemove(transaction, columns, values)
                }
        }

        run {
            return database
                       .DELETE_FROM(elementFactory.table)
                       .WHERE { elementFactory.filter(this) AND this.selection() }
                       .executeUpdate(transaction.connection) > 0
        }
    }

    @Throws(SQLException::class)
    protected fun <W : T> addWithKey(transaction: TR, elem: W): ComparableHandle<W>? {
        val stmt = elementFactory.insertStatement(elem)
        return stmt.execute(transaction.connection, elementFactory.keyColumn) {
            it?.let { handle ->
                val newElem = handleAssigner(elem, handle) ?: elem
                handle<W>(handle= handle.handleValue).apply {
                    elementFactory.postStore(transaction.connection, this, null, newElem)
                }
            }
        }.firstOrNull()
    }

    override fun toString(): String {
        return "DbSet <SELECT * FROM `" + elementFactory.table._name + "`>"
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
        fun join(pList1: List<CharSequence>,
                 pList2: List<CharSequence>,
                 pOuterSeparator: CharSequence,
                 pInnerSeparator: CharSequence): CharSequence {
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
        protected fun rollbackConnection(pConnection: DBTransaction, pCause: Throwable) {
            rollbackConnection(pConnection, null, pCause)
        }

        private fun rollbackConnection(pConnection: DBTransaction, savepoint: Savepoint?, pCause: Throwable?) {
            try {
                if (savepoint == null) {
                    pConnection.rollback()
                } else {
                    pConnection.rollback(savepoint)
                }
            } catch (ex: SQLException) {
                if (pCause != null) {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (pCause as java.lang.Throwable).addSuppressed(ex)
                } else {
                    throw RuntimeException(ex)
                }
            }

            if (pCause is RuntimeException) {
                throw pCause
            }
            throw RuntimeException(pCause)
        }

        @JvmStatic
        internal fun rollbackConnection(pConnection: Connection, pSavepoint: Savepoint?, pCause: Throwable?) {
            try {
                if (pSavepoint == null) {
                    pConnection.rollback()
                } else {
                    pConnection.rollback(pSavepoint)
                }
            } catch (ex: SQLException) {
                if (pCause != null) {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (pCause as java.lang.Throwable).addSuppressed(ex)
                } else {
                    throw RuntimeException(ex)
                }
            }

            if (pCause is RuntimeException) {
                throw pCause
            }
            throw RuntimeException(pCause)
        }


        @JvmStatic
        @Deprecated("Use try-with")
        protected fun closeConnection(pConnection: Connection?, e: Exception) {
            try {
                pConnection?.close()
            } catch (ex: Exception) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (e as java.lang.Throwable).addSuppressed(ex)
            }

        }

        @JvmStatic
        protected fun closeResultSet(pConnection: DBTransaction?,
                                     pStatement: PreparedStatement,
                                     pResultSet: ResultSet) {
            try {
                try {
                    try {
                        try {
                            pResultSet.close()
                        } finally {
                            pStatement.close()
                        }
                    } finally {
                        if (pConnection != null) {
                            if (!pConnection.isClosed) {
                                pConnection.rollback()
                            }
                        }
                    }
                } finally {
                    pConnection?.close()
                }
            } catch (e: SQLException) {
                throw RuntimeException(e)
            }

        }


        @JvmStatic
        @Deprecated(
            "use {@link #resourceNameToDataSource(Context, String)}, that is more reliable as the context can be gained earlier",
            ReplaceWith("resourceNameToDataSource(context, pResourceName)"))
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
}
