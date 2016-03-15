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

package uk.ac.bournemouth.util.kotlin.sql

import java.sql.*
import java.util.*
import java.util.concurrent.Executor
import javax.sql.DataSource

/**
 * Created by pdvrieze on 13/03/16.
 */

public inline fun <R> DataSource.connection(block: (ConnectionHelper) -> R) = getConnection().use { ConnectionHelper(it).use(block) }

class ConnectionHelper constructor (val connection: Connection) {

    inline fun <R> raw (block: (Connection)->R):R = block(connection)
    inline fun <R> use (block: (ConnectionHelper)->R):R = block(this)

    /**
     * Creates a `PreparedStatement` object for sending
     * parameterized SQL statements to the database.
     *
     * A SQL statement with or without IN parameters can be
     * pre-compiled and stored in a `PreparedStatement` object. This
     * object can then be used to efficiently execute this statement
     * multiple times.

     * Note: This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation,
     * the method `prepareStatement` will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the `PreparedStatement`
     * object is executed.  This has no direct effect on users; however, it does
     * affect which methods throw certain `SQLException` objects.
     *
     * Result sets created using the returned `PreparedStatement`
     * object will by default be type `TYPE_FORWARD_ONLY`
     * and have a concurrency level of `CONCUR_READ_ONLY`.
     * The holdability of the created result sets can be determined by
     * calling [.getHoldability].

     * @param sql an SQL statement that may contain one or more '?' IN
     * * parameter placeholders
     * *
     * @return a new default `PreparedStatement` object containing the
     * * pre-compiled SQL statement
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     */
    inline fun <R> prepareStatement (sql: String, block: StatementHelper.() -> R): R = connection.prepareStatement(sql).use { block(StatementHelper(it)) }


    /**
     * Makes all changes made since the previous
     * commit/rollback permanent and releases any database locks
     * currently held by this `Connection` object.
     * This method should be
     * used only when auto-commit mode has been disabled.

     * @exception SQLException if a database access error occurs,
     * * this method is called while participating in a distributed transaction,
     * * if this method is called on a closed connection or this
     * *            `Connection` object is in auto-commit mode
     * *
     * @see .setAutoCommit
     */
    inline fun commit() = connection.commit()

    /**
     * Retrieves a `DatabaseMetaData` object that contains
     * metadata about the database to which this
     * `Connection` object represents a connection.
     * The metadata includes information about the database's
     * tables, its supported SQL grammar, its stored
     * procedures, the capabilities of this connection, and so on.

     * @return a `DatabaseMetaData` object for this
     * *         `Connection` object
     * *
     * @exception  SQLException if a database access error occurs
     * * or this method is called on a closed connection
     */
    inline fun getMetaData() = connection.getMetaData()

    /**
     * Creates a `CallableStatement` object for calling
     * database stored procedures.
     * The `CallableStatement` object provides
     * methods for setting up its IN and OUT parameters, and
     * methods for executing the call to a stored procedure.

     * Note: This method is optimized for handling stored
     * procedure call statements. Some drivers may send the call
     * statement to the database when the method `prepareCall`
     * is done; others
     * may wait until the `CallableStatement` object
     * is executed. This has no
     * direct effect on users; however, it does affect which method
     * throws certain SQLExceptions.
     *
     * Result sets created using the returned `CallableStatement`
     * object will by default be type `TYPE_FORWARD_ONLY`
     * and have a concurrency level of `CONCUR_READ_ONLY`.
     * The holdability of the created result sets can be determined by
     * calling [.getHoldability].

     * @param sql an SQL statement that may contain one or more '?'
     * * parameter placeholders. Typically this statement is specified using JDBC
     * * call escape syntax.
     * *
     * @return a new default `CallableStatement` object containing the
     * * pre-compiled SQL statement
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     */
    private inline fun prepareCall(sql: String) = connection.prepareCall(sql)

    /**
     * Converts the given SQL statement into the system's native SQL grammar.
     * A driver may convert the JDBC SQL grammar into its system's
     * native SQL grammar prior to sending it. This method returns the
     * native form of the statement that the driver would have sent.

     * @param sql an SQL statement that may contain one or more '?'
     * * parameter placeholders
     * *
     * @return the native form of this statement
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     */
    @Throws(SQLException::class)
    inline fun nativeSQL(sql: String): String = connection.nativeSQL(sql)

    /**
     * Retrieves the current auto-commit mode for this `Connection`
     * object.

     * @return the current state of this `Connection` object's
     * *         auto-commit mode
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @see .setAutoCommit
     */
    @Throws(SQLException::class)
    inline fun getAutoCommit(): Boolean = connection.getAutoCommit()
    /**
     * Undoes all changes made in the current transaction
     * and releases any database locks currently held
     * by this `Connection` object. This method should be
     * used only when auto-commit mode has been disabled.

     * @exception SQLException if a database access error occurs,
     * * this method is called while participating in a distributed transaction,
     * * this method is called on a closed connection or this
     * *            `Connection` object is in auto-commit mode
     * *
     * @see .setAutoCommit
     */
    @Throws(SQLException::class)
    inline fun rollback() = connection.rollback()

    /**
     * Retrieves whether this `Connection` object has been
     * closed.  A connection is closed if the method `close`
     * has been called on it or if certain fatal errors have occurred.
     * This method is guaranteed to return `true` only when
     * it is called after the method `Connection.close` has
     * been called.
     *
     * This method generally cannot be called to determine whether a
     * connection to a database is valid or invalid.  A typical client
     * can determine that a connection is invalid by catching any
     * exceptions that might be thrown when an operation is attempted.

     * @return `true` if this `Connection` object
     * *         is closed; `false` if it is still open
     * *
     * @exception SQLException if a database access error occurs
     */
    @Throws(SQLException::class)
    inline fun isClosed(): Boolean = connection.isClosed

    //======================================================================
    // Advanced features:

    /**
     * Puts this connection in read-only mode as a hint to the driver to enable
     * database optimizations.

     * Note: This method cannot be called during a transaction.

     * @param readOnly `true` enables read-only mode;
     * *        `false` disables it
     * *
     * @exception SQLException if a database access error occurs, this
     * *  method is called on a closed connection or this
     * *            method is called during a transaction
     */
    @Throws(SQLException::class)
    inline fun setReadOnly(readOnly: Boolean) = connection.setReadOnly(readOnly)

    /**
     * Retrieves whether this `Connection`
     * object is in read-only mode.

     * @return `true` if this `Connection` object
     * *         is read-only; `false` otherwise
     * *
     * @exception SQLException SQLException if a database access error occurs
     * * or this method is called on a closed connection
     */
    @Throws(SQLException::class)
    inline fun isReadOnly(): Boolean = connection.isReadOnly()

    /**
     * Sets the given catalog name in order to select
     * a subspace of this `Connection` object's database
     * in which to work.
     *
     * If the driver does not support catalogs, it will
     * silently ignore this request.
     *
     *
     * Calling `setCatalog` has no effect on previously created or prepared
     * `Statement` objects. It is implementation defined whether a DBMS
     * prepare operation takes place immediately when the `Connection`
     * method `prepareStatement` or `prepareCall` is invoked.
     * For maximum portability, `setCatalog` should be called before a
     * `Statement` is created or prepared.

     * @param catalog the name of a catalog (subspace in this
     * *        `Connection` object's database) in which to work
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @see .getCatalog
     */
    @Throws(SQLException::class)
    inline fun setCatalog(catalog: String) = connection.setCatalog(catalog)

    /**
     * Retrieves this `Connection` object's current catalog name.

     * @return the current catalog name or `null` if there is none
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @see .setCatalog
     */
    @Throws(SQLException::class)
    inline fun getCatalog(): String = connection.getCatalog()

    /**
     * A constant indicating that transactions are not supported.
     */
    @Deprecated("Don't use this, just use Connection's version", replaceWith = ReplaceWith("Connection.TRANSACTION_NONE", "java.sql.Connection"))
    val TRANSACTION_NONE = Connection.TRANSACTION_NONE

    /**
     * A constant indicating that
     * dirty reads, non-repeatable reads and phantom reads can occur.
     * This level allows a row changed by one transaction to be read
     * by another transaction before any changes in that row have been
     * committed (a "dirty read").  If any of the changes are rolled back,
     * the second transaction will have retrieved an invalid row.
     */
    @Deprecated("Don't use this, just use Connection's version", replaceWith = ReplaceWith("Connection.TRANSACTION_READ_UNCOMMITTED", "java.sql.Connection"))
    val TRANSACTION_READ_UNCOMMITTED = Connection.TRANSACTION_READ_UNCOMMITTED

    /**
     * A constant indicating that
     * dirty reads are prevented; non-repeatable reads and phantom
     * reads can occur.  This level only prohibits a transaction
     * from reading a row with uncommitted changes in it.
     */
    @Deprecated("Don't use this, just use Connection's version", replaceWith = ReplaceWith("Connection.TRANSACTION_READ_COMMITTED", "java.sql.Connection"))
    val TRANSACTION_READ_COMMITTED = Connection.TRANSACTION_READ_COMMITTED

    /**
     * A constant indicating that
     * dirty reads and non-repeatable reads are prevented; phantom
     * reads can occur.  This level prohibits a transaction from
     * reading a row with uncommitted changes in it, and it also
     * prohibits the situation where one transaction reads a row,
     * a second transaction alters the row, and the first transaction
     * rereads the row, getting different values the second time
     * (a "non-repeatable read").
     */
    @Deprecated("Don't use this, just use Connection's version", replaceWith = ReplaceWith("Connection.TRANSACTION_REPEATABLE_READ", "java.sql.Connection"))
    val TRANSACTION_REPEATABLE_READ = Connection.TRANSACTION_REPEATABLE_READ

    /**
     * A constant indicating that
     * dirty reads, non-repeatable reads and phantom reads are prevented.
     * This level includes the prohibitions in
     * `TRANSACTION_REPEATABLE_READ` and further prohibits the
     * situation where one transaction reads all rows that satisfy
     * a `WHERE` condition, a second transaction inserts a row that
     * satisfies that `WHERE` condition, and the first transaction
     * rereads for the same condition, retrieving the additional
     * "phantom" row in the second read.
     */
    @Deprecated("Don't use this, just use Connection's version", replaceWith = ReplaceWith("Connection.TRANSACTION_SERIALIZABLE", "java.sql.Connection"))
    val TRANSACTION_SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE

    /**
     * Attempts to change the transaction isolation level for this
     * `Connection` object to the one given.
     * The constants defined in the interface `Connection`
     * are the possible transaction isolation levels.
     *
     * Note: If this method is called during a transaction, the result
     * is implementation-defined.

     * @param level one of the following `Connection` constants:
     * *        `Connection.TRANSACTION_READ_UNCOMMITTED`,
     * *        `Connection.TRANSACTION_READ_COMMITTED`,
     * *        `Connection.TRANSACTION_REPEATABLE_READ`, or
     * *        `Connection.TRANSACTION_SERIALIZABLE`.
     * *        (Note that `Connection.TRANSACTION_NONE` cannot be used
     * *        because it specifies that transactions are not supported.)
     * *
     * @exception SQLException if a database access error occurs, this
     * * method is called on a closed connection
     * *            or the given parameter is not one of the `Connection`
     * *            constants
     * *
     * @see DatabaseMetaData.supportsTransactionIsolationLevel

     * @see .getTransactionIsolation
     */
    @Throws(SQLException::class)
    inline fun setTransactionIsolation(level: Int) = connection.setTransactionIsolation(level)

    /**
     * Retrieves this `Connection` object's current
     * transaction isolation level.

     * @return the current transaction isolation level, which will be one
     * *         of the following constants:
     * *        `Connection.TRANSACTION_READ_UNCOMMITTED`,
     * *        `Connection.TRANSACTION_READ_COMMITTED`,
     * *        `Connection.TRANSACTION_REPEATABLE_READ`,
     * *        `Connection.TRANSACTION_SERIALIZABLE`, or
     * *        `Connection.TRANSACTION_NONE`.
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @see .setTransactionIsolation
     */
    @Throws(SQLException::class)
    inline fun getTransactionIsolation(): Int = connection.transactionIsolation

    /**
     * Retrieves the first warning reported by calls on this
     * `Connection` object.  If there is more than one
     * warning, subsequent warnings will be chained to the first one
     * and can be retrieved by calling the method
     * `SQLWarning.getNextWarning` on the warning
     * that was retrieved previously.
     *
     * This method may not be
     * called on a closed connection; doing so will cause an
     * `SQLException` to be thrown.

     * Note: Subsequent warnings will be chained to this
     * SQLWarning.

     * @return the first `SQLWarning` object or `null`
     * *         if there are none
     * *
     * @exception SQLException if a database access error occurs or
     * *            this method is called on a closed connection
     * *
     * @see SQLWarning
     */
    val warningsIt: Iterator<SQLWarning> get() = object: AbstractIterator<SQLWarning>() {
        override fun computeNext() {
            val w = connection.warnings
            if (w!=null) {
                setNext(w)
            } else {
                done()
            }
        }
    }


    /**
     * Clears all warnings reported for this `Connection` object.
     * After a call to this method, the method `getWarnings`
     * returns `null` until a new warning is
     * reported for this `Connection` object.

     * @exception SQLException SQLException if a database access error occurs
     * * or this method is called on a closed connection
     */
    @Throws(SQLException::class)
    inline fun clearWarnings() = connection.clearWarnings()


    //--------------------------JDBC 2.0-----------------------------

    /**

     * Creates a `PreparedStatement` object that will generate
     * `ResultSet` objects with the given type and concurrency.
     * This method is the same as the `prepareStatement` method
     * above, but it allows the default result set
     * type and concurrency to be overridden.
     * The holdability of the created result sets can be determined by
     * calling [.getHoldability].

     * @param sql a `String` object that is the SQL statement to
     * *            be sent to the database; may contain one or more '?' IN
     * *            parameters
     * *
     * @param resultSetType a result set type; one of
     * *         `ResultSet.TYPE_FORWARD_ONLY`,
     * *         `ResultSet.TYPE_SCROLL_INSENSITIVE`, or
     * *         `ResultSet.TYPE_SCROLL_SENSITIVE`
     * *
     * @param resultSetConcurrency a concurrency type; one of
     * *         `ResultSet.CONCUR_READ_ONLY` or
     * *         `ResultSet.CONCUR_UPDATABLE`
     * *
     * @return a new PreparedStatement object containing the
     * * pre-compiled SQL statement that will produce `ResultSet`
     * * objects with the given type and concurrency
     * *
     * @exception SQLException if a database access error occurs, this
     * * method is called on a closed connection
     * *         or the given parameters are not `ResultSet`
     * *         constants indicating type and concurrency
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method or this method is not supported for the specified result
     * * set type and result set concurrency.
     * *
     * @since 1.2
     */
    @Throws(SQLException::class)
    inline fun <R> prepareStatement(sql: String, resultSetType: Int,
                                  resultSetConcurrency: Int, block: (StatementHelper) -> R): R {
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency).use { block(StatementHelper(it)) }
    }

    /**
     * Retrieves the `Map` object associated with this
     * `Connection` object.
     * Unless the application has added an entry, the type map returned
     * will be empty.
     *
     *
     * You must invoke `setTypeMap` after making changes to the
     * `Map` object returned from
     * `getTypeMap` as a JDBC driver may create an internal
     * copy of the `Map` object passed to `setTypeMap`:

     *
     * Map&lt;String,Class&lt;?&gt;&gt; myMap = con.getTypeMap();
     * myMap.put("mySchemaName.ATHLETES", Athletes.class);
     * con.setTypeMap(myMap);
     *
     * @return the `java.util.Map` object associated
     * *         with this `Connection` object
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * @since 1.2
     * *
     * @see .setTypeMap
     */
    @Throws(SQLException::class)
    inline fun getTypeMap() = connection.typeMap

    /**
     * Installs the given `TypeMap` object as the type map for
     * this `Connection` object.  The type map will be used for the
     * custom mapping of SQL structured types and distinct types.
     *
     *
     * You must set the the values for the `TypeMap` prior to
     * callng `setMap` as a JDBC driver may create an internal copy
     * of the `TypeMap`:

     *
     * Map myMap&lt;String,Class&lt;?&gt;&gt; = new HashMap&lt;String,Class&lt;?&gt;&gt;();
     * myMap.put("mySchemaName.ATHLETES", Athletes.class);
     * con.setTypeMap(myMap);
     *
     * @param map the `java.util.Map` object to install
     * *        as the replacement for this `Connection`
     * *        object's default type map
     * *
     * @exception SQLException if a database access error occurs, this
     * * method is called on a closed connection or
     * *        the given parameter is not a `java.util.Map`
     * *        object
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * @since 1.2
     * *
     * @see .getTypeMap
     */
    @Throws(SQLException::class)
    inline fun setTypeMap(map: Map<String, Class<*>>) = connection.setTypeMap(map)

    //--------------------------JDBC 3.0-----------------------------


    /**
     * Changes the default holdability of `ResultSet` objects
     * created using this `Connection` object to the given
     * holdability.  The default holdability of `ResultSet` objects
     * can be be determined by invoking
     * [DatabaseMetaData.getResultSetHoldability].

     * @param holdability a `ResultSet` holdability constant; one of
     * *        `ResultSet.HOLD_CURSORS_OVER_COMMIT` or
     * *        `ResultSet.CLOSE_CURSORS_AT_COMMIT`
     * *
     * @throws SQLException if a database access occurs, this method is called
     * * on a closed connection, or the given parameter
     * *         is not a `ResultSet` constant indicating holdability
     * *
     * @exception SQLFeatureNotSupportedException if the given holdability is not supported
     * *
     * @see .getHoldability

     * @see DatabaseMetaData.getResultSetHoldability

     * @see ResultSet

     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun setHoldability(holdability: Int) = connection.setHoldability(holdability)

    /**
     * Retrieves the current holdability of `ResultSet` objects
     * created using this `Connection` object.

     * @return the holdability, one of
     * *        `ResultSet.HOLD_CURSORS_OVER_COMMIT` or
     * *        `ResultSet.CLOSE_CURSORS_AT_COMMIT`
     * *
     * @throws SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @see .setHoldability

     * @see DatabaseMetaData.getResultSetHoldability

     * @see ResultSet

     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun getHoldability() = connection.holdability

    /**
     * Creates an unnamed savepoint in the current transaction and
     * returns the new `Savepoint` object that represents it.

     *
     *  if setSavepoint is invoked outside of an active transaction, a transaction will be started at this newly created
     * savepoint.

     * @return the new `Savepoint` object
     * *
     * @exception SQLException if a database access error occurs,
     * * this method is called while participating in a distributed transaction,
     * * this method is called on a closed connection
     * *            or this `Connection` object is currently in
     * *            auto-commit mode
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * @see Savepoint

     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun setSavepoint(): Savepoint = connection.setSavepoint()

    /**
     * Creates a savepoint with the given name in the current transaction
     * and returns the new `Savepoint` object that represents it.

     *
     *  if setSavepoint is invoked outside of an active transaction, a transaction will be started at this newly created
     * savepoint.

     * @param name a `String` containing the name of the savepoint
     * *
     * @return the new `Savepoint` object
     * *
     * @exception SQLException if a database access error occurs,
     * * this method is called while participating in a distributed transaction,
     * * this method is called on a closed connection
     * *            or this `Connection` object is currently in
     * *            auto-commit mode
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * @see Savepoint

     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun setSavepoint(name: String): Savepoint = connection.setSavepoint(name)

    /**
     * Undoes all changes made after the given `Savepoint` object
     * was set.
     *
     * This method should be used only when auto-commit has been disabled.

     * @param savepoint the `Savepoint` object to roll back to
     * *
     * @exception SQLException if a database access error occurs,
     * * this method is called while participating in a distributed transaction,
     * * this method is called on a closed connection,
     * *            the `Savepoint` object is no longer valid,
     * *            or this `Connection` object is currently in
     * *            auto-commit mode
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * @see Savepoint

     * @see .rollback

     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun rollback(savepoint: Savepoint) = connection.rollback(savepoint)

    /**
     * Removes the specified `Savepoint`  and subsequent `Savepoint` objects from the current
     * transaction. Any reference to the savepoint after it have been removed
     * will cause an `SQLException` to be thrown.

     * @param savepoint the `Savepoint` object to be removed
     * *
     * @exception SQLException if a database access error occurs, this
     * *  method is called on a closed connection or
     * *            the given `Savepoint` object is not a valid
     * *            savepoint in the current transaction
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun releaseSavepoint(savepoint: Savepoint) = connection.releaseSavepoint(savepoint)

    /**
     * Creates a `PreparedStatement` object that will generate
     * `ResultSet` objects with the given type, concurrency,
     * and holdability.
     *
     * This method is the same as the `prepareStatement` method
     * above, but it allows the default result set
     * type, concurrency, and holdability to be overridden.

     * @param sql a `String` object that is the SQL statement to
     * *            be sent to the database; may contain one or more '?' IN
     * *            parameters
     * *
     * @param resultSetType one of the following `ResultSet`
     * *        constants:
     * *         `ResultSet.TYPE_FORWARD_ONLY`,
     * *         `ResultSet.TYPE_SCROLL_INSENSITIVE`, or
     * *         `ResultSet.TYPE_SCROLL_SENSITIVE`
     * *
     * @param resultSetConcurrency one of the following `ResultSet`
     * *        constants:
     * *         `ResultSet.CONCUR_READ_ONLY` or
     * *         `ResultSet.CONCUR_UPDATABLE`
     * *
     * @param resultSetHoldability one of the following `ResultSet`
     * *        constants:
     * *         `ResultSet.HOLD_CURSORS_OVER_COMMIT` or
     * *         `ResultSet.CLOSE_CURSORS_AT_COMMIT`
     * *
     * @return a new `PreparedStatement` object, containing the
     * *         pre-compiled SQL statement, that will generate
     * *         `ResultSet` objects with the given type,
     * *         concurrency, and holdability
     * *
     * @exception SQLException if a database access error occurs, this
     * * method is called on a closed connection
     * *            or the given parameters are not `ResultSet`
     * *            constants indicating type, concurrency, and holdability
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method or this method is not supported for the specified result
     * * set type, result set holdability and result set concurrency.
     * *
     * @see ResultSet

     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun <R> prepareStatement(sql: String, resultSetType: Int,
                                  resultSetConcurrency: Int, resultSetHoldability: Int, block: (StatementHelper) -> R): R {
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability).use { block(StatementHelper(it)) }
    }

    /**
     * Creates a default `PreparedStatement` object that has
     * the capability to retrieve auto-generated keys. The given constant
     * tells the driver whether it should make auto-generated keys
     * available for retrieval.  This parameter is ignored if the SQL statement
     * is not an `INSERT` statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     *
     * Note: This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation,
     * the method `prepareStatement` will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the `PreparedStatement`
     * object is executed.  This has no direct effect on users; however, it does
     * affect which methods throw certain SQLExceptions.
     *
     * Result sets created using the returned `PreparedStatement`
     * object will by default be type `TYPE_FORWARD_ONLY`
     * and have a concurrency level of `CONCUR_READ_ONLY`.
     * The holdability of the created result sets can be determined by
     * calling [.getHoldability].

     * @param sql an SQL statement that may contain one or more '?' IN
     * *        parameter placeholders
     * *
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     * *        should be returned; one of
     * *        `Statement.RETURN_GENERATED_KEYS` or
     * *        `Statement.NO_GENERATED_KEYS`
     * *
     * @return a new `PreparedStatement` object, containing the
     * *         pre-compiled SQL statement, that will have the capability of
     * *         returning auto-generated keys
     * *
     * @exception SQLException if a database access error occurs, this
     * *  method is called on a closed connection
     * *         or the given parameter is not a `Statement`
     * *         constant indicating whether auto-generated keys should be
     * *         returned
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method with a constant of Statement.RETURN_GENERATED_KEYS
     * *
     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun <R> prepareStatement(sql: String, autoGeneratedKeys: Int, block: (StatementHelper) -> R): R {
        return connection.prepareStatement(sql, autoGeneratedKeys).use { block(StatementHelper(it)) }
    }

    /**
     * Creates a default `PreparedStatement` object capable
     * of returning the auto-generated keys designated by the given array.
     * This array contains the indexes of the columns in the target
     * table that contain the auto-generated keys that should be made
     * available.  The driver will ignore the array if the SQL statement
     * is not an `INSERT` statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     *
     *
     * An SQL statement with or without IN parameters can be
     * pre-compiled and stored in a `PreparedStatement` object. This
     * object can then be used to efficiently execute this statement
     * multiple times.
     *
     * Note: This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation,
     * the method `prepareStatement` will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the `PreparedStatement`
     * object is executed.  This has no direct effect on users; however, it does
     * affect which methods throw certain SQLExceptions.
     *
     * Result sets created using the returned `PreparedStatement`
     * object will by default be type `TYPE_FORWARD_ONLY`
     * and have a concurrency level of `CONCUR_READ_ONLY`.
     * The holdability of the created result sets can be determined by
     * calling [.getHoldability].

     * @param sql an SQL statement that may contain one or more '?' IN
     * *        parameter placeholders
     * *
     * @param columnIndexes an array of column indexes indicating the columns
     * *        that should be returned from the inserted row or rows
     * *
     * @return a new `PreparedStatement` object, containing the
     * *         pre-compiled statement, that is capable of returning the
     * *         auto-generated keys designated by the given array of column
     * *         indexes
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * *
     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun <R> prepareStatement(sql: String, columnIndexes: IntArray, block: (StatementHelper) -> R): R {
        return connection.prepareStatement(sql, columnIndexes).use { block(StatementHelper(it)) }
    }

    /**
     * Creates a default `PreparedStatement` object capable
     * of returning the auto-generated keys designated by the given array.
     * This array contains the names of the columns in the target
     * table that contain the auto-generated keys that should be returned.
     * The driver will ignore the array if the SQL statement
     * is not an `INSERT` statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     *
     * An SQL statement with or without IN parameters can be
     * pre-compiled and stored in a `PreparedStatement` object. This
     * object can then be used to efficiently execute this statement
     * multiple times.
     *
     * Note: This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation,
     * the method `prepareStatement` will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the `PreparedStatement`
     * object is executed.  This has no direct effect on users; however, it does
     * affect which methods throw certain SQLExceptions.
     *
     * Result sets created using the returned `PreparedStatement`
     * object will by default be type `TYPE_FORWARD_ONLY`
     * and have a concurrency level of `CONCUR_READ_ONLY`.
     * The holdability of the created result sets can be determined by
     * calling [.getHoldability].

     * @param sql an SQL statement that may contain one or more '?' IN
     * *        parameter placeholders
     * *
     * @param columnNames an array of column names indicating the columns
     * *        that should be returned from the inserted row or rows
     * *
     * @return a new `PreparedStatement` object, containing the
     * *         pre-compiled statement, that is capable of returning the
     * *         auto-generated keys designated by the given array of column
     * *         names
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * *
     * @since 1.4
     */
    @Throws(SQLException::class)
    inline fun <R> prepareStatement(sql: String, columnNames: Array<out String>, block: (StatementHelper) -> R): R {
        return connection.prepareStatement(sql, columnNames).use { block(StatementHelper(it)) }
    }

    /**
     * Constructs an object that implements the `Clob` interface. The object
     * returned initially contains no data.  The `setAsciiStream`,
     * `setCharacterStream` and `setString` methods of
     * the `Clob` interface may be used to add data to the `Clob`.
     * @return An object that implements the `Clob` interface
     * *
     * @throws SQLException if an object that implements the
     * * `Clob` interface can not be constructed, this method is
     * * called on a closed connection or a database access error occurs.
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this data type
     * *
     * *
     * @since 1.6
     */
    @Throws(SQLException::class)
    inline  fun createClob(): Clob = connection.createClob()

    /**
     * Constructs an object that implements the `Blob` interface. The object
     * returned initially contains no data.  The `setBinaryStream` and
     * `setBytes` methods of the `Blob` interface may be used to add data to
     * the `Blob`.
     * @return  An object that implements the `Blob` interface
     * *
     * @throws SQLException if an object that implements the
     * * `Blob` interface can not be constructed, this method is
     * * called on a closed connection or a database access error occurs.
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this data type
     * *
     * *
     * @since 1.6
     */
    @Throws(SQLException::class)
    inline fun createBlob(): Blob = connection.createBlob()

    /**
     * Constructs an object that implements the `NClob` interface. The object
     * returned initially contains no data.  The `setAsciiStream`,
     * `setCharacterStream` and `setString` methods of the `NClob` interface may
     * be used to add data to the `NClob`.
     * @return An object that implements the `NClob` interface
     * *
     * @throws SQLException if an object that implements the
     * * `NClob` interface can not be constructed, this method is
     * * called on a closed connection or a database access error occurs.
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this data type
     * *
     * *
     * @since 1.6
     */
    @Throws(SQLException::class)
    inline fun createNClob(): NClob = connection.createNClob()

    /**
     * Constructs an object that implements the `SQLXML` interface. The object
     * returned initially contains no data. The `createXmlStreamWriter` object and
     * `setString` method of the `SQLXML` interface may be used to add data to the `SQLXML`
     * object.
     * @return An object that implements the `SQLXML` interface
     * *
     * @throws SQLException if an object that implements the `SQLXML` interface can not
     * * be constructed, this method is
     * * called on a closed connection or a database access error occurs.
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this data type
     * *
     * @since 1.6
     */
    @Throws(SQLException::class)
    inline fun createSQLXML(): SQLXML = connection.createSQLXML()

    /**
     * Returns true if the connection has not been closed and is still valid.
     * The driver shall submit a query on the connection or use some other
     * mechanism that positively verifies the connection is still valid when
     * this method is called.
     *
     *
     * The query submitted by the driver to validate the connection shall be
     * executed in the context of the current transaction.

     * @param timeout -             The time in seconds to wait for the database operation
     * *                                              used to validate the connection to complete.  If
     * *                                              the timeout period expires before the operation
     * *                                              completes, this method returns false.  A value of
     * *                                              0 indicates a timeout is not applied to the
     * *                                              database operation.
     * *
     *
     *
     * *
     * @return true if the connection is valid, false otherwise
     * *
     * @exception SQLException if the value supplied for `timeout`
     * * is less then 0
     * *
     * @since 1.6
     * *
     * *
     * @see java.sql.DatabaseMetaData.getClientInfoProperties
     */
    @Throws(SQLException::class)
    inline fun isValid(timeout: Int): Boolean = connection.isValid(timeout)

    /**
     * Sets the value of the client info property specified by name to the
     * value specified by value.
     *
     *
     * Applications may use the `DatabaseMetaData.getClientInfoProperties`
     * method to determine the client info properties supported by the driver
     * and the maximum length that may be specified for each property.
     *
     *
     * The driver stores the value specified in a suitable location in the
     * database.  For example in a special register, session parameter, or
     * system table column.  For efficiency the driver may defer setting the
     * value in the database until the next time a statement is executed or
     * prepared.  Other than storing the client information in the appropriate
     * place in the database, these methods shall not alter the behavior of
     * the connection in anyway.  The values supplied to these methods are
     * used for accounting, diagnostics and debugging purposes only.
     *
     *
     * The driver shall generate a warning if the client info name specified
     * is not recognized by the driver.
     *
     *
     * If the value specified to this method is greater than the maximum
     * length for the property the driver may either truncate the value and
     * generate a warning or generate a `SQLClientInfoException`.  If the driver
     * generates a `SQLClientInfoException`, the value specified was not set on the
     * connection.
     *
     *
     * The following are standard client info properties.  Drivers are not
     * required to support these properties however if the driver supports a
     * client info property that can be described by one of the standard
     * properties, the standard property name should be used.

     *
     *  * ApplicationName  -       The name of the application currently utilizing
     * the connection
     *  * ClientUser               -       The name of the user that the application using
     * the connection is performing work for.  This may
     * not be the same as the user name that was used
     * in establishing the connection.
     *  * ClientHostname   -       The hostname of the computer the application
     * using the connection is running on.
     *
     *
     *
     * @param name          The name of the client info property to set
     * *
     * @param value         The value to set the client info property to.  If the
     * *                                      value is null, the current value of the specified
     * *                                      property is cleared.
     * *
     *
     *
     * *
     * @throws      SQLClientInfoException if the database server returns an error while
     * *                      setting the client info value on the database server or this method
     * * is called on a closed connection
     * *
     *
     *
     * *
     * @since 1.6
     */
    @Throws(SQLClientInfoException::class)
    inline fun setClientInfo(name: String, value: String) = connection.setClientInfo(name, value)

    /**
     * Sets the value of the connection's client info properties.  The
     * `Properties` object contains the names and values of the client info
     * properties to be set.  The set of client info properties contained in
     * the properties list replaces the current set of client info properties
     * on the connection.  If a property that is currently set on the
     * connection is not present in the properties list, that property is
     * cleared.  Specifying an empty properties list will clear all of the
     * properties on the connection.  See `setClientInfo (String, String)` for
     * more information.
     *
     *
     * If an error occurs in setting any of the client info properties, a
     * `SQLClientInfoException` is thrown. The `SQLClientInfoException`
     * contains information indicating which client info properties were not set.
     * The state of the client information is unknown because
     * some databases do not allow multiple client info properties to be set
     * atomically.  For those databases, one or more properties may have been
     * set before the error occurred.
     *
     *

     * @param properties                the list of client info properties to set
     * *
     *
     *
     * *
     * @see java.sql.Connection.setClientInfo
     * @since 1.6
     * *
     *
     *
     * *
     * @throws SQLClientInfoException if the database server returns an error while
     * *                  setting the clientInfo values on the database server or this method
     * * is called on a closed connection
     */
    @Throws(SQLClientInfoException::class)
    inline fun setClientInfo(properties: Properties) = connection.setClientInfo(properties)

    /**
     * Returns the value of the client info property specified by name.  This
     * method may return null if the specified client info property has not
     * been set and does not have a default value.  This method will also
     * return null if the specified client info property name is not supported
     * by the driver.
     *
     *
     * Applications may use the `DatabaseMetaData.getClientInfoProperties`
     * method to determine the client info properties supported by the driver.
     *
     *
     * @param name          The name of the client info property to retrieve
     * *
     *
     *
     * *
     * @return                      The value of the client info property specified
     * *
     *
     *
     * *
     * @throws SQLException         if the database server returns an error when
     * *                                                      fetching the client info value from the database
     * *or this method is called on a closed connection
     * *
     *
     *
     * *
     * @since 1.6
     * *
     * *
     * @see java.sql.DatabaseMetaData.getClientInfoProperties
     */
    @Throws(SQLException::class)
    inline fun getClientInfo(name: String): String = connection.getClientInfo(name)

    /**
     * Returns a list containing the name and current value of each client info
     * property supported by the driver.  The value of a client info property
     * may be null if the property has not been set and does not have a
     * default value.
     *
     *
     * @return      A `Properties` object that contains the name and current value of
     * *                      each of the client info properties supported by the driver.
     * *
     *
     *
     * *
     * @throws      SQLException if the database server returns an error when
     * *                      fetching the client info values from the database
     * * or this method is called on a closed connection
     * *
     *
     *
     * *
     * @since 1.6
     */
    @Throws(SQLException::class)
    inline fun getClientInfo() = connection.clientInfo

    /**
     * Factory method for creating Array objects.
     *
     *
     * **Note: **When `createArrayOf` is used to create an array object
     * that maps to a primitive data type, then it is implementation-defined
     * whether the `Array` object is an array of that primitive
     * data type or an array of `Object`.
     *
     *
     * **Note: **The JDBC driver is responsible for mapping the elements
     * `Object` array to the default JDBC SQL type defined in
     * java.sql.Types for the given class of `Object`. The default
     * mapping is specified in Appendix B of the JDBC specification.  If the
     * resulting JDBC type is not the appropriate type for the given typeName then
     * it is implementation defined whether an `SQLException` is
     * thrown or the driver supports the resulting conversion.

     * @param typeName the SQL name of the type the elements of the array map to. The typeName is a
     * * database-specific name which may be the name of a built-in type, a user-defined type or a standard  SQL type supported by this database. This
     * *  is the value returned by `Array.getBaseTypeName`
     * *
     * @param elements the elements that populate the returned object
     * *
     * @return an Array object whose elements map to the specified SQL type
     * *
     * @throws SQLException if a database error occurs, the JDBC type is not
     * *  appropriate for the typeName and the conversion is not supported, the typeName is null or this method is called on a closed connection
     * *
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this data type
     * *
     * @since 1.6
     */
    @Throws(SQLException::class)
    inline fun createArrayOf(typeName: String, elements: Array<Any>) = connection.createArrayOf(typeName, elements)

    /**
     * Factory method for creating Struct objects.

     * @param typeName the SQL type name of the SQL structured type that this `Struct`
     * * object maps to. The typeName is the name of  a user-defined type that
     * * has been defined for this database. It is the value returned by
     * * `Struct.getSQLTypeName`.

     * *
     * @param attributes the attributes that populate the returned object
     * *
     * @return a Struct object that maps to the given SQL type and is populated with the given attributes
     * *
     * @throws SQLException if a database error occurs, the typeName is null or this method is called on a closed connection
     * *
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this data type
     * *
     * @since 1.6
     */
    @Throws(SQLException::class)
    inline fun createStruct(typeName: String, attributes: Array<Any>): Struct = connection.createStruct(typeName, attributes)

    //--------------------------JDBC 4.1 -----------------------------

    /**
     * Sets the given schema name to access.
     *
     * If the driver does not support schemas, it will
     * silently ignore this request.
     *
     *
     * Calling `setSchema` has no effect on previously created or prepared
     * `Statement` objects. It is implementation defined whether a DBMS
     * prepare operation takes place immediately when the `Connection`
     * method `prepareStatement` or `prepareCall` is invoked.
     * For maximum portability, `setSchema` should be called before a
     * `Statement` is created or prepared.

     * @param schema the name of a schema  in which to work
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @see .getSchema

     * @since 1.7
     */
    @Throws(SQLException::class)
    inline fun setSchema(schema: String) = connection.setSchema(schema)

    /**
     * Retrieves this `Connection` object's current schema name.

     * @return the current schema name or `null` if there is none
     * *
     * @exception SQLException if a database access error occurs
     * * or this method is called on a closed connection
     * *
     * @see .setSchema

     * @since 1.7
     */
    @Throws(SQLException::class)
    inline fun getSchema(): String = connection.schema

    /**
     * Terminates an open connection.  Calling `abort` results in:
     *
     *  * The connection marked as closed
     *  * Closes any physical connection to the database
     *  * Releases resources used by the connection
     *  * Insures that any thread that is currently accessing the connection
     * will either progress to completion or throw an `SQLException`.
     *
     *
     *
     * Calling `abort` marks the connection closed and releases any
     * resources. Calling `abort` on a closed connection is a
     * no-op.
     *
     *
     * It is possible that the aborting and releasing of the resources that are
     * held by the connection can take an extended period of time.  When the
     * `abort` method returns, the connection will have been marked as
     * closed and the `Executor` that was passed as a parameter to abort
     * may still be executing tasks to release resources.
     *
     *
     * This method checks to see that there is an `SQLPermission`
     * object before allowing the method to proceed.  If a
     * `SecurityManager` exists and its
     * `checkPermission` method denies calling `abort`,
     * this method throws a
     * `java.lang.SecurityException`.
     * @param executor  The `Executor`  implementation which will
     * * be used by `abort`.
     * *
     * @throws java.sql.SQLException if a database access error occurs or
     * * the `executor` is `null`,
     * *
     * @throws java.lang.SecurityException if a security manager exists and its
     * *    `checkPermission` method denies calling `abort`
     * *
     * @see SecurityManager.checkPermission

     * @see Executor

     * @since 1.7
     */
    @Throws(SQLException::class)
    inline fun abort(executor: Executor) = connection.abort(executor)

    /**

     * Sets the maximum period a `Connection` or
     * objects created from the `Connection`
     * will wait for the database to reply to any one request. If any
     * request remains unanswered, the waiting method will
     * return with a `SQLException`, and the `Connection`
     * or objects created from the `Connection`  will be marked as
     * closed. Any subsequent use of
     * the objects, with the exception of the `close`,
     * `isClosed` or `Connection.isValid`
     * methods, will result in  a `SQLException`.
     *
     *
     * **Note**: This method is intended to address a rare but serious
     * condition where network partitions can cause threads issuing JDBC calls
     * to hang uninterruptedly in socket reads, until the OS TCP-TIMEOUT
     * (typically 10 minutes). This method is related to the
     * [abort() ][.abort] method which provides an administrator
     * thread a means to free any such threads in cases where the
     * JDBC connection is accessible to the administrator thread.
     * The `setNetworkTimeout` method will cover cases where
     * there is no administrator thread, or it has no access to the
     * connection. This method is severe in it's effects, and should be
     * given a high enough value so it is never triggered before any more
     * normal timeouts, such as transaction timeouts.
     *
     *
     * JDBC driver implementations  may also choose to support the
     * `setNetworkTimeout` method to impose a limit on database
     * response time, in environments where no network is present.
     *
     *
     * Drivers may internally implement some or all of their API calls with
     * multiple internal driver-database transmissions, and it is left to the
     * driver implementation to determine whether the limit will be
     * applied always to the response to the API call, or to any
     * single  request made during the API call.
     *
     *

     * This method can be invoked more than once, such as to set a limit for an
     * area of JDBC code, and to reset to the default on exit from this area.
     * Invocation of this method has no impact on already outstanding
     * requests.
     *
     *
     * The `Statement.setQueryTimeout()` timeout value is independent of the
     * timeout value specified in `setNetworkTimeout`. If the query timeout
     * expires  before the network timeout then the
     * statement execution will be canceled. If the network is still
     * active the result will be that both the statement and connection
     * are still usable. However if the network timeout expires before
     * the query timeout or if the statement timeout fails due to network
     * problems, the connection will be marked as closed, any resources held by
     * the connection will be released and both the connection and
     * statement will be unusable.
     *
     *
     * When the driver determines that the `setNetworkTimeout` timeout
     * value has expired, the JDBC driver marks the connection
     * closed and releases any resources held by the connection.
     *
     *

     * This method checks to see that there is an `SQLPermission`
     * object before allowing the method to proceed.  If a
     * `SecurityManager` exists and its
     * `checkPermission` method denies calling
     * `setNetworkTimeout`, this method throws a
     * `java.lang.SecurityException`.

     * @param executor  The `Executor`  implementation which will
     * * be used by `setNetworkTimeout`.
     * *
     * @param milliseconds The time in milliseconds to wait for the database
     * * operation
     * *  to complete.  If the JDBC driver does not support milliseconds, the
     * * JDBC driver will round the value up to the nearest second.  If the
     * * timeout period expires before the operation
     * * completes, a SQLException will be thrown.
     * * A value of 0 indicates that there is not timeout for database operations.
     * *
     * @throws java.sql.SQLException if a database access error occurs, this
     * * method is called on a closed connection,
     * * the `executor` is `null`,
     * * or the value specified for `seconds` is less than 0.
     * *
     * @throws java.lang.SecurityException if a security manager exists and its
     * *    `checkPermission` method denies calling
     * * `setNetworkTimeout`.
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * @see SecurityManager.checkPermission

     * @see Statement.setQueryTimeout

     * @see .getNetworkTimeout

     * @see .abort

     * @see Executor

     * @since 1.7
     */
    @Throws(SQLException::class)
    inline  fun setNetworkTimeout(executor: Executor, milliseconds: Int) = connection.setNetworkTimeout(executor, milliseconds)


    /**
     * Retrieves the number of milliseconds the driver will
     * wait for a database request to complete.
     * If the limit is exceeded, a
     * `SQLException` is thrown.

     * @return the current timeout limit in milliseconds; zero means there is
     * *         no limit
     * *
     * @throws SQLException if a database access error occurs or
     * * this method is called on a closed `Connection`
     * *
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * * this method
     * *
     * @see .setNetworkTimeout

     * @since 1.7
     */
    @Throws(SQLException::class)
    inline fun getNetworkTimeout(): Int = connection.networkTimeout


}

class StatementHelper constructor (val statement: PreparedStatement) : PreparedStatement by statement {
    inline fun <R> raw (block: (PreparedStatement)->R):R = block(statement)

    @Deprecated("Use withResultSet")
    override fun getResultSet(): ResultSet? {
        throw UnsupportedOperationException()
    }

    @Deprecated("Use withGeneratedKeys")
    override fun getGeneratedKeys(): ResultSet? {
        throw UnsupportedOperationException()
    }

    @Deprecated("Use Execute with lambda instead", replaceWith = ReplaceWith("execute"))
    override fun executeQuery(p0: String?): ResultSet? {
        throw UnsupportedOperationException()
    }

    val warningsIt: Iterator<SQLWarning>
        get()= object: AbstractIterator<SQLWarning>() {
        override fun computeNext() {
            val w = statement.warnings
            if (w!=null) {
                setNext(w)
            } else {
                done()
            }
        }
    }


    inline fun <reified T> setParam_(index:Int, value :T) = when (value) {
        is Int -> setParam(index, value)
        is Long -> setParam(index, value)
        is String -> setParam(index, value)
        is Boolean -> setParam(index, value)
        is Byte -> setParam(index, value)
        is Short -> setParam(index, value)
        else -> throw UnsupportedOperationException("Not possible to set this value")
    }



    inline fun setParam(index:Int, value:Int) = setInt(index, value)
    inline fun setParam(index:Int, value:Long) = setLong(index, value)
    inline fun setParam(index:Int, value:String) = setString(index, value)
    inline fun setParam(index:Int, value:Boolean) = setBoolean(index, value)
    inline fun setParam(index:Int, value:Byte) = setByte(index, value)
    inline fun setParam(index:Int, value:Short) = setShort(index, value)

    class ParamHelper_(val sh: StatementHelper) {
        var index = 1
        inline operator fun Int.unaryPlus():Unit = sh.setParam(index++, this)
        inline operator fun Long.unaryPlus():Unit = sh.setParam(index++, this)
        inline operator fun String.unaryPlus():Unit = sh.setParam(index++, this)
        inline operator fun Boolean.unaryPlus():Unit = sh.setParam(index++, this)
        inline operator fun Byte.unaryPlus():Unit = sh.setParam(index++, this)
        inline operator fun Short.unaryPlus():Unit = sh.setParam(index++, this)

        inline operator fun <T> T.plus(value:Int):Unit = sh.setParam(index++, value)
        inline operator fun <T> T.plus(value:Long):Unit = sh.setParam(index++, value)
        inline operator fun <T> T.plus(value:String):Unit = sh.setParam(index++, value)
        inline operator fun <T> T.plus(value:Boolean):Unit = sh.setParam(index++, value)
        inline operator fun <T> T.plus(value:Byte):Unit = sh.setParam(index++, value)
        inline operator fun <T> T.plus(value:Short):Unit = sh.setParam(index++, value)
    }

    inline fun <R> params(block:ParamHelper_.() -> R) =ParamHelper_(this).block()

    inline fun <R> withResultSet(block: (ResultSet)-> R) = statement.getResultSet().use(block)

    inline fun <R> withGeneratedKeys(block: (ResultSet) -> R) = statement.generatedKeys.use(block)

    inline fun <R> execute(block: (ResultSet)-> R ) = statement.executeQuery().use(block)

    inline fun executeHasRows(): Boolean = execute() && withResultSet { it.next() }
}

fun String.appendWarnings(warnings: Iterator<SQLWarning>):String {
    val result = StringBuilder().append(this).append(" - \n    ")
    warnings.asSequence().map { "${it.errorCode}: ${it.message}"  }.joinTo(result, ",\n    ")
    return result.toString()
}

/**
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception
 * is thrown or not.
 *
 * @param block a function to process this closable resource.
 * @return the result of [block] function on this closable resource.
 */
public inline fun <T : Connection, R> T.use(block: (T) -> R) = useHelper({ it.close()}, block)
public inline fun <T : Statement, R> T.use(block: (T) -> R) = useHelper({ it.close()}, block)
public inline fun <T : ResultSet, R> T.use(block: (T) -> R) = useHelper({ it.close()}, block)

public inline fun <T : Connection, R> T.useTransacted(block: (T) -> R) {
    useHelper({ it.close()}) {
        it.autoCommit=false
        try {
            block(it)
        } catch (e: Exception) {
            it.rollback()
            throw e
        }

    }
}


public inline fun  <T, R> T.useHelper(close: (T)->Unit, block: (T) -> R) : R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            close(this)
        } catch (closeException: Exception) {
            // drop for now.
        }
        throw e
    } finally {
        if (!closed) {
            close(this)
        }
    }
}