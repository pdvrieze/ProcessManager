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

package net.devrieze.util.db;

import net.devrieze.util.StringCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class that helps with handling database operations and closing up afterwards. It keeps track of opened statements
 * so they can be automatically closed.
 */
@SuppressWarnings("deprecation")
public class DBConnection implements AutoCloseable {

  private interface Warnable {

    boolean isClosed() throws SQLException;

    SQLWarning getWarnings() throws SQLException;
  }

  /**
   * The base class for any statement. It wraps a prepared statement.
   */
  public interface DBStatement extends AutoCloseable {

    /**
     * Add a parameter to the statement.
     * @param column The column index to add it to
     * @param value The value to set
     * @return This object, to allow for method chaining.
     * @see PreparedStatement#setString(int, String)
     */
    @NotNull
    DBStatement addParam(int column, String value);

    /**
     * Add a parameter to the statement.
     * @param column The column index to add it to
     * @param value The value to set
     * @return This object, to allow for method chaining.
     * @see PreparedStatement#setInt(int, int)
     */
    @NotNull
    DBStatement addParam(int column, int value);

    /**
     * Add a parameter to the statement.
     * @param column The column index to add it to
     * @param value The value to set
     * @return This object, to allow for method chaining.
     * @see PreparedStatement#setLong(int, long)
     */
    @NotNull
    DBStatement addParam(int column, long value);

    /**
     * Execute the query.
     * @return The result of executing it.
     * @throws SQLException Any thrown exception.
     * @see PreparedStatement#execute()
     */
    boolean exec() throws SQLException;

    /**
     * Execute the query, commmitting if there was no exception.
     * @return The result of executing it.
     * @throws SQLException Any thrown exception.
     * @see PreparedStatement#execute
     */
    boolean execCommit() throws SQLException;

    /**
     * Close the statement
     * @see PreparedStatement#close()
     */
    @Override
    void close();

    /**
     * Close the underlying connection.
     */
    void closeConnection();

    // Property accessors start

    /**
     * Get the string cache to use for this statement.
     * @return The string cache.
     */
    @NotNull
    StringCache getStringCache();
// Property acccessors end
  }

  /**
   * A {@link DBStatement} representing an sql query (not manipulation etc.)
   */
  public interface DBQuery extends DBStatement {

    /**
     * Execute the query.
     *
     * @return <code>true</code> if the query does not have any results, <code>false</code> if the query gives one or
     * more results.
     */
    boolean execQueryEmpty();

    /**
     * Add a parameter to the query.
     *
     * @param column The column number to add the parameter
     * @param value  The value
     * @return the value of <code>this</code>
     */
    @NotNull
    @Override
    DBQuery addParam(int column, String value);

    /**
     * Execute the query.
     *
     * @return <code>false</code> if the query does not have any results, <code>true</code> if the query gives one or
     * more results.
     */
    boolean execQueryNotEmpty();

    /**
     * Execute the query and get the result set.
     *
     * @return The resultset.
     */
    ResultSet execQuery();

    /**
     * Execute the query and return the integer value
     *
     * @return The resultset.
     */
    Integer intQuery();

    /**
     * Execute the query and return the long value
     *
     * @return The resultset.
     */
    Long longQuery();

    @NotNull
    @Override
    DBQuery addParam(int column, int value);


    @NotNull
    @Override
    DBQuery addParam(int column, long value);


  }

  /**
   * A {@link DBStatement} representing an insert sql statement.
   */
  public interface DBInsert extends DBStatement {

    @NotNull
    @Override
    DBInsert addParam(int column, String value);

  }

  private class DBStatementImpl implements DBStatement {

    @Nullable
    PreparedStatement mSQLStatement;

    // Object Initialization
    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(@NotNull final String sql, @Nullable final String errorMsg) throws SQLException {
      final Connection connection = mConnection;
      if (connection == null) {
        throw new SQLException("Committing closed connection");
      }
      try {
        mSQLStatement = connection.prepareStatement(sql);
      } finally {
        logWarnings("Preparing statement", connection);
      }

      DBConnection.this.mErrorMsg = errorMsg;
    }
// Object Initialization end

    @Override
    @NotNull
    public DBStatement addParam(final int column, final String value) {
      if (mSQLStatement != null) {
        checkValid();
        try {
          mSQLStatement.setString(column, value);
        } catch (final SQLException e) {
          logException("Failure to set parameter on prepared statement", e);
          DBStatementImpl.this.close();
          mSQLStatement = null;
        }
      }
      return this;
    }

    protected final void checkValid() {
      try {
        if (mSQLStatement == null) {
          throw new IllegalStateException("No underlying statement");
        }
        if (mSQLStatement.isClosed()) {
          throw new IllegalStateException("Trying to use a closed prepared statement");
        }
      } catch (final SQLException e) {
        logException("Failure to check whether prepared statement is closed", e);
        throw new RuntimeException(e);
      }
    }

    @Override
    @NotNull
    public DBStatement addParam(final int column, final int value) {
      if (mSQLStatement != null) {
        checkValid();
        try {
          mSQLStatement.setInt(column, value);
        } catch (final SQLException e) {
          logException("Failure to create prepared statement", e);
          DBStatementImpl.this.close();
          mSQLStatement = null;
        }
      }
      return this;
    }

    @Override
    @NotNull
    public DBStatement addParam(final int column, final long value) {
      checkValid();
      if (mSQLStatement != null) {
        try {
          mSQLStatement.setLong(column, value);
        } catch (final SQLException e) {
          logException("Failure to create prepared statement", e);
          DBStatementImpl.this.close();
          mSQLStatement = null;
        }
      }
      return this;
    }

    @Override
    public boolean exec() throws SQLException {
      checkValid();
      final PreparedStatement sqlStatement = this.mSQLStatement;
      if (sqlStatement == null) {
        logException("No prepared statement available", new NullPointerException());
        return false;
      }
      try {
        sqlStatement.execute();
        logWarnings("Executing prepared statement", sqlStatement);
        return true;
      } catch (final SQLException e) {
        logException(mErrorMsg, e);
        try {
          sqlStatement.close();
        } catch (final SQLException e2) {
          logWarning("Error closing prepared statement after error", e2);
          e.addSuppressed(e2);
        }

        mSQLStatement = null;
        rollback();
        return false;
      }
    }

    @Override
    public boolean execCommit() throws SQLException {
      final boolean result = exec();
      if (result) {
        commit();
        close();
      }
      return result;
    }


    @Override
    @NotNull
    public StringCache getStringCache() {
      return _stringCache;
    }

    @Override
    public void close() {
      if (mSQLStatement != null) {
        try {
          mSQLStatement.close();
        } catch (final SQLException e) {
          logException("Error closing statement", e);
          mSQLStatement = null;
          throw new RuntimeException(e);
        }
        mSQLStatement = null;
      }
    }

    @Override
    public void closeConnection() {
      DBConnection.this.close();
    }

  }

  private class DBQueryImpl extends DBStatementImpl implements DBQuery {

    List<ResultSet> mResultSets;

    // Object Initialization
    public DBQueryImpl() {
      super();
    }

    public DBQueryImpl(@NotNull final String sql, final String errorMsg) throws SQLException {
      super(sql, errorMsg);
      mResultSets = new ArrayList<>();
    }
// Object Initialization end

    @Override
    @NotNull
    public DBQuery addParam(final int column, final String value) {
      //noinspection resource
      super.addParam(column, value);
      return this;
    }

    @Override
    public boolean execQueryEmpty() {
      try (final ResultSet rs = execQuery()) {
        if (rs == null) {
          return true;
        }
        final boolean result = !rs.next();
        logWarnings("execQueryNotEmpty resultset", rs);
        return result;
      } catch (final SQLException e) {
        logException("Error processing result set", e);
        return true;
      }
    }

    @Override
    public boolean execQueryNotEmpty() {

      try (final ResultSet rs = execQuery()) {
        if (rs == null) {
          return false;
        }
        final boolean result = rs.next();
        logWarnings("execQueryNotEmpty resultset", rs);
        return result;
      } catch (final SQLException e) {
        logException("Error processing result set", e);
        return false;
      }
    }

    @Override
    @Nullable
    public ResultSet execQuery() {
      checkValid();
      try {
        final PreparedStatement sqlStatement = this.mSQLStatement;
        if (sqlStatement == null) {
          return null;
        }
        final ResultSet result = sqlStatement.executeQuery();
        logWarnings("Prepared statement " + sqlStatement.toString(), sqlStatement);
        mResultSets.add(result);
        return result;
      } catch (final SQLException e) {
        logException(mErrorMsg, e);
      }
      return null;
    }

    @SuppressWarnings("UnnecessaryBoxing")
    @Override
    @Nullable
    public Integer intQuery() {
      try {
        try (ResultSet rs = getSingleHelper()) {
          return rs == null ? null : rs.first() ? Integer.valueOf(rs.getInt(1)) : null;
        } catch (final SQLException e) {
          logException("Error processing result set", e);
          return null;
        }
      } finally {
        close();
      }
    }

    @SuppressWarnings("UnnecessaryBoxing")
    @Override
    @Nullable
    public Long longQuery() {
      try {
        try (ResultSet rs = getSingleHelper()) {
          return rs == null ? null : rs.first() ? Long.valueOf(rs.getLong(1)) : null;
        } catch (final SQLException e) {
          logException("Error processing result set", e);
          return null;
        }
      } finally {
        close();
      }
    }

    @Nullable
    private ResultSet getSingleHelper() throws SQLException {
      @SuppressWarnings("resource")
      final ResultSet rs = execQuery();
      if (rs == null) {
        return null;
      }
      if (rs.getMetaData().getColumnCount() != 1) {
        logWarning("The query " + mSQLStatement + " does not return 1 element");
        try {
          rs.close();
        } catch (final SQLException e) {
          logException("Failure closing resultset", e);
        }
        return null;
      }
      logWarnings("getSingleHelper resultset", rs);
      if (!rs.next()) {
        logWarnings("getSingleHelper resultset", rs);
        try {
          rs.close();
        } catch (final SQLException e) {
          logException("Failure closing resultset", e);
        }
        return null; // No result, that is allowed, no warning
      }
      logWarnings("getSingleHelper resultset", rs);
      if (rs.getObject(1) == null) {
        logWarnings("getSingleHelper resultset", rs);
        try {
          rs.close();
        } catch (final SQLException e) {
          logException("Failure closing resultset", e);
        }
        return null;
      }
      logWarnings("getSingleHelper resultset", rs);
      mResultSets.add(rs);
      return rs;
    }

    @Override
    @NotNull
    public DBQuery addParam(final int column, final int value) {
      //noinspection resource
      super.addParam(column, value);
      return this;
    }




    @Override
    @NotNull
    public DBQuery addParam(final int column, final long value) {
      //noinspection resource
      super.addParam(column, value);
      return this;
    }


    @Override
    public void close() {
      if (mResultSets != null) {
        for (final ResultSet rs : mResultSets) {
          try {
            rs.close();
          } catch (final SQLException e) {
            logException("Failure closing down resultsets", e);
          }
        }
      }
      super.close();
    }

  }

  private class DBInsertImpl extends DBStatementImpl implements DBInsert {


    // Object Initialization
    public DBInsertImpl(@NotNull final String sql, final String errorMsg) throws SQLException {
      super(sql, errorMsg);
    }
// Object Initialization end

    @Override
    @NotNull
    public DBInsert addParam(final int column, final String value) {
      return (DBInsert) super.addParam(column, value);
    }

  }

  private static final boolean DEBUG = DBConnection.class.desiredAssertionStatus();
  private static final Level DETAIL_LOG_LEVEL = Level.FINE;
  private static final String LOGGER_NAME = "DBHelper";
  @NotNull
  private static StringCache _stringCache = StringCache.NOPCACHE;
  @Nullable
  private String mErrorMsg;
  @Nullable
  private Connection mConnection;
  @NotNull
  private List<DBStatement> mStatements;

  // Object Initialization
  private DBConnection(@NotNull final DataSource dataSource) throws SQLException {
    mConnection = dataSource.getConnection();
    mConnection.setAutoCommit(false);
    mStatements = new ArrayList<>();
  }

  /**
   * @deprecated Use {@link #newInstance(DataSource)}
   */
  @Deprecated
  public static DBConnection getDbHelper(final String dbresource) {
    return newInstance(dbresource);
  }

  /**
   * @deprecated Use {@link #newInstance(DataSource)}
   */
  @Deprecated
  public static DBConnection newInstance(final String dbresource) {
    try {
      return newInstance(getDataSource(dbresource));
    } catch (final NamingException e) {
      logException("Failure to get data source: " + dbresource, e);
      return null;
    }
  }

  /**
   * Create a new connection object. This could have included caching, but that is better handled at lower level.
   * There are many jdbc connection pooling libraries.
   * @param dataSource The datasource to based the connection upon.
   * @return The connection, or <code>null</code> if none.
   */
  public static DBConnection newInstance(final DataSource dataSource) {
    try {
      return new DBConnection(dataSource);
    } catch (SQLException e) {
      logException("Failure to get database connection", e);
      return null;
    }
  }

  /**
   * Set a string cache to reset strings from.
   *
   * @param stringCache The string cache.
   */
  public static void setStringCache(@NotNull final StringCache stringCache) {
    _stringCache = stringCache;
  }

  /**
   * Helper method to get a datasource for the given string that describes it.
   * @param dbresource The string resource to look for.
   * @return The DataSource.
   * @throws NamingException When the resource could not be found
   * @see InitialContext#lookup(String)
   */
  public static DataSource getDataSource(final String dbresource) throws NamingException {
    final InitialContext initialContext = new InitialContext();
    return (DataSource) Objects.requireNonNull(initialContext.lookup(dbresource));
  }
// Object Initialization end

  /**
   * Create a new query.
   * @param sql The query string
   * @return The object wrapping the prepared statement.
   */
  @NotNull
  public DBQuery makeQuery(@NotNull final String sql) {
    return makeQuery(sql, null);
  }


  /**
   * Create a new query.
   *
   * @param sql      The query string
   * @param errorMsg The message to use if the query fails (normally after this method returned).
   * @return The object wrapping the prepared statement.
   */
  @NotNull
  public DBQuery makeQuery(@NotNull final String sql, @Nullable final String errorMsg) {
    try {
      return recordStatement(new DBQueryImpl(sql, errorMsg));
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
      throw new RuntimeException(e);
    }
  }

  static void logException(@Nullable final String msg, @NotNull final Throwable e) {
    getLogger().log(Level.SEVERE, msg, e);
  }

  @NotNull
  private <T extends DBStatement> T recordStatement(@NotNull final T statement) {
    mStatements.add(statement);
    return statement;
  }


  /**
   * Create a new insert statement.
   *
   * @param sql      The query string
   * @return The object wrapping the prepared statement.
   * @throws SQLException When the creation fails
   */
  @NotNull
  public DBInsert makeInsert(@NotNull final String sql) throws SQLException {
    return makeInsert(sql, null);
  }

  /**
   * Create a new insert statement.
   *
   * @param sql      The query string
   * @param errorMsg The message to use if the insert fails (normally after this method returned).
   * @return The object wrapping the prepared statement.
   * @throws SQLException When the creation fails
   */
  @NotNull
  public DBInsert makeInsert(@NotNull final String sql, @Nullable final String errorMsg) throws SQLException {
    try {
      return recordStatement(new DBInsertImpl(sql, errorMsg));
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
      throw e;
    }
  }

  /**
   * Commit the current transaction
   * @throws SQLException If the commit fails.
   * @see Connection#commit()
   */
  public void commit() throws SQLException {
    final Connection connection = this.mConnection;
    if (connection == null) {
      throw new SQLException("Committing closed connection");
    }
    try {
      connection.commit();
      logWarnings("Committing database connection", connection);
    } catch (final SQLException e) {
      logException("Failure to commit statement", e);
      rollback();
    }
  }

  /**
   * Roll the transaction back.
   * @throws SQLException When the rollback fails
   * @see Connection#rollback()
   */
  public void rollback() throws SQLException {
    final Connection connection = this.mConnection;
    if (connection == null) {
      throw new SQLException("Committing closed connection");
    }
    try {
      connection.rollback();
      logWarnings("Rolling back database connection", connection);
    } catch (final SQLException | NullPointerException e) {
      logException("Failure to roll back statement", e);
    }
  }

  private static void logWarning(@Nullable final String msg, @NotNull final Throwable exception) {
    getLogger().log(Level.WARNING, msg, exception);
  }

  @NotNull
  private static Logger getLogger() {
    return Logger.getLogger(LOGGER_NAME);
  }

  private static void logWarning(@NotNull final String msg) {
    getLogger().log(Level.WARNING, msg);
  }

  private static void logWarnings(@NotNull final String message, @Nullable final Connection connection) {
    logWarnings(message, "connection", warnable(connection));
  }

  private static void logWarnings(@NotNull final String message, @Nullable final Statement statement) {
    logWarnings(message, "statement", warnable(statement));
  }

  static void logWarnings(@NotNull final String message, @Nullable final ResultSet resultSet) {
    logWarnings(message, "resultSet", warnable(resultSet));
  }

  private static void logWarnings(@NotNull final String message, final String typeName, @Nullable final Warnable warnable) {
    if (warnable != null) {
      try {
        if (warnable.isClosed()) {
          getLogger().log(Level.INFO, "Logging warnings on closed "+typeName);
        } else {
          try {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            SQLWarning warning = warnable.getWarnings();
            while (warning != null) {
              getLogger().log(Level.WARNING, message, warnable);
              warning = warning.getNextWarning();
            }
          } catch (SQLException e) {
            getLogger().log(Level.WARNING, "Error processing warnings", e);
          }
        }
      } catch (SQLException e) {
        getLogger().log(Level.WARNING, "Error processing warnings", e);
      }
    }
  }

  /**
   * Close the underlying connection for this helper. A new connection will
   * automatically be established when needed.
   */
  @Override
  public void close() {
    final Connection connection = this.mConnection;
    if (connection == null) {
      return;
    }
    Exception error = null;
    getLogger().log(DETAIL_LOG_LEVEL, "Closing connection");
    for (final DBStatement statement : mStatements) {
      try {
        statement.close();
      } catch (RuntimeException e) {
        logException("Failure to close database statements", e);
        if (error == null) {
          error = e;
        } else {
          error.addSuppressed(e);
        }
      }
    }
    mStatements = new ArrayList<>();
    try {
      connection.close();
    } catch (SQLException e) {
      if (error == null) {
        error = e;
      } else {
        error.addSuppressed(e);
      }
      logException("Failure to close database connection", e);
    } finally {
      mConnection = null;
    }
    if (error != null) {
      if (error instanceof RuntimeException) {
        throw (RuntimeException) error;
      } else {
        throw new RuntimeException(error);
      }
    }
  }

  private static Warnable warnable(final Connection connection) {
    return new Warnable() {
      @Override
      public boolean isClosed() throws SQLException {
        return connection.isClosed();
      }

      @Override
      public SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
      }
    };
  }

  private static Warnable warnable(final Statement statement) {
    return new Warnable() {
      @Override
      public boolean isClosed() throws SQLException {
        return statement.isClosed();
      }

      @Override
      public SQLWarning getWarnings() throws SQLException {
        return statement.getWarnings();
      }
    };
  }

  private static Warnable warnable(final ResultSet resultSet) {
    return new Warnable() {
      @Override
      public boolean isClosed() throws SQLException {
        return resultSet.isClosed();
      }

      @Override
      public SQLWarning getWarnings() throws SQLException {
        return resultSet.getWarnings();
      }
    };
  }


}
