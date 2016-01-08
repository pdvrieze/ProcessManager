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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.devrieze.util.StringCache;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
//import net.devrieze.util.StringCacheImpl;


@SuppressWarnings("deprecation")
public class DBConnection implements AutoCloseable {

  public static class DBHelper {

    @NotNull
    private final DataSource mDataSource;
    private StringCache mStringCache;

    // Object Initialization
    public DBHelper(@NotNull final DataSource dataSource) {
      mDataSource = dataSource;
    }

    @NotNull
    public static DBHelper getDbHelper(final String resourceName) throws SQLException {
      final DataSource dataSource;
      try {
        dataSource = getDataSource(resourceName);
      } catch (final NamingException e) {
        throw new SQLException("Failure to link to database", e);
      }
      if (DBConnection.getLogger()
                      .isLoggable(DBConnection.DETAIL_LOG_LEVEL)) { // Do this only when we log this is going to be output
        final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        final StringBuilder message = new StringBuilder();
        if (DEBUG) {
          message.append("dbHelper invoked for ").append(resourceName).append(" from ");
          for (int i = 2; (i < stackTraceElements.length) && (i < 7); ++i) {
            if (i > 2) {
              message.append(" -> ");
            }
            message.append(stackTraceElements[i]);
          }
          DBConnection.getLogger().log(DBConnection.DETAIL_LOG_LEVEL, message.toString());
        }
      }
      return new DBHelper(dataSource);
    }
// Object Initialization end

    // Property accessors start
    @NotNull
    public DBConnection getConnection() throws SQLException {
      final DBConnection dbConnection = new DBConnection(mDataSource);
      final StringCache mStringCache2 = mStringCache;
      if (mStringCache2 != null) {
        DBConnection.setStringCache(mStringCache2);
      }
      return dbConnection;
    }

    public void setStringCache(final StringCache stringCache) {
      mStringCache = stringCache;
    }
// Property acccessors end

  }

  @Deprecated
  static class DataSourceWrapper {

    @NotNull
    final DataSource mDataSource;

    // Object Initialization
    DataSourceWrapper(@NotNull final DataSource dataSource) {
      mDataSource = dataSource;
    }
// Object Initialization end
  }

  private interface Warnable {

    boolean isClosed() throws SQLException;

    SQLWarning getWarnings() throws SQLException;
  }

  public interface DBStatement extends AutoCloseable {

    @NotNull
    DBStatement addParam(int column, String value);

    @NotNull
    DBStatement addParam(int column, int value);

    @NotNull
    DBStatement addParam(int column, long value);

    boolean exec() throws SQLException;

    boolean execCommit() throws SQLException;

    @Override
    void close();

    void closeHelper();

    // Property accessors start
    @NotNull
    StringCache getStringCache();
// Property acccessors end
  }

  public interface DBQuery extends DBStatement {

    boolean execQueryEmpty();

    /**
     * Add a parameter to the query.
     * @param column The column number to add the parameter
     * @param value The value
     * @return the value of <code>this</code>
     */
    @NotNull
    @Override
    DBQuery addParam(int column, String value);

    boolean execQueryNotEmpty();

    /** Execute the query and get the result set. */
    ResultSet execQuery();

    /** Execute the query and return the integer value */
    Integer intQuery();

    /** Execute the query and return the long value */
    Long longQuery();

    @NotNull
    @Override
    DBQuery addParam(int column, int value);


    @NotNull
    @Override
    DBQuery addParam(int column, long value);


  }

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

    DBStatementImpl(@NotNull final String sQL, @Nullable final String errorMsg) throws SQLException {
      final Connection connection = mConnection;
      if (connection == null) {
        throw new SQLException("Committing closed connection");
      }
      try {
        mSQLStatement = connection.prepareStatement(sQL);
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
    public void closeHelper() {
      DBConnection.this.close();
    }

  }

  public class DBQueryImpl extends DBStatementImpl implements DBQuery {

    List<ResultSet> mResultSets;

    // Object Initialization
    public DBQueryImpl() {
      super();
    }

    public DBQueryImpl(@NotNull final String sQL, final String errorMsg) throws SQLException {
      super(sQL, errorMsg);
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
    public DBInsertImpl(@NotNull final String sQL, final String errorMsg) throws SQLException {
      super(sQL, errorMsg);
    }
// Object Initialization end

    @Override
    @NotNull
    public DBInsert addParam(final int column, final String value) {
      return (DBInsert) super.addParam(column, value);
    }

  }

  public static final boolean DEBUG = DBConnection.class.desiredAssertionStatus();
  private static final Level DETAIL_LOG_LEVEL = Level.FINE;
  private static final String LOGGER_NAME = "DBHelper";
  @NotNull
  private static StringCache _stringCache = StringCache.NOPCACHE;
  @Nullable
  public String mErrorMsg;
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

  public static DataSource getDataSource(final String dbresource) throws NamingException {
    final InitialContext initialContext = new InitialContext();
    return (DataSource) Objects.requireNonNull(initialContext.lookup(dbresource));
  }
// Object Initialization end

  @NotNull
  public DBQuery makeQuery(@NotNull final String sQL) {
    return makeQuery(sQL, null);
  }

  @NotNull
  public DBQuery makeQuery(@NotNull final String sQL, @Nullable final String errorMsg) {
    try {
      return recordStatement(new DBQueryImpl(sQL, errorMsg));
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
      throw new RuntimeException(e);
    }
  }

  public static void logException(@Nullable final String msg, @NotNull final Throwable e) {
    getLogger().log(Level.SEVERE, msg, e);
  }

  @NotNull
  private <T extends DBStatement> T recordStatement(@NotNull final T statement) {
    mStatements.add(statement);
    return statement;
  }

  @NotNull
  public DBInsert makeInsert(@NotNull final String sQL) throws SQLException {
    return makeInsert(sQL, null);
  }

  @NotNull
  public DBInsert makeInsert(@NotNull final String sQL, @Nullable final String errorMsg) throws SQLException {
    try {
      return recordStatement(new DBInsertImpl(sQL, errorMsg));
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
      throw e;
    }
  }

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

  public static void logWarnings(@NotNull final String message, @Nullable final Connection connection) {
    logWarnings(message, "connection", warnable(connection));
  }

  public static void logWarnings(@NotNull final String message, @Nullable final Statement statement) {
    logWarnings(message, "statement", warnable(statement));
  }

  public static void logWarnings(@NotNull final String message, @Nullable final ResultSet resultSet) {
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
