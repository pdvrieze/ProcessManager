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
    private DataSource aDataSource;
    private StringCache aStringCache;

    // Object Initialization
    public DBHelper(@NotNull DataSource dataSource) {
      aDataSource = dataSource;
    }

    @NotNull
    public static DBHelper getDbHelper(final String resourceName) throws SQLException {
      DataSource dataSource;
      if (true) {
        try {
          dataSource = getDataSource(resourceName);
        } catch (final NamingException e) {
          throw new SQLException("Failure to link to database", e);
        }
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
      final DBConnection dbConnection = new DBConnection(aDataSource);
      final StringCache aStringCache2 = aStringCache;
      if (aStringCache2 != null) {
        dbConnection.setStringCache(aStringCache2);
      }
      return dbConnection;
    }

    public void setStringCache(StringCache stringCache) {
      aStringCache = stringCache;
    }
// Property acccessors end

  }

  @Deprecated
  static class DataSourceWrapper {

    @NotNull
    final DataSource aDataSource;

    // Object Initialization
    DataSourceWrapper(@NotNull final DataSource dataSource) {
      aDataSource = dataSource;
    }
// Object Initialization end
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

    @Override
    DBQuery addParam(int column, String value);

    boolean execQueryNotEmpty();

    /** Execute the query and get the result set. */
    ResultSet execQuery();

    /** Execute the query and return the integer value */
    Integer intQuery();

    /** Execute the query and return the long value */
    Long longQuery();

    @Override
    DBQuery addParam(int column, int value);


    @Override
    DBQuery addParam(int column, long value);


  }

  public interface DBInsert extends DBStatement {

    @Override
    DBInsert addParam(int column, String value);

  }

  private class DBStatementImpl implements DBStatement {

    @Nullable
    PreparedStatement aSQLStatement;

    // Object Initialization
    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(@NotNull final String sQL, @Nullable final String errorMsg) throws SQLException {
      final Connection connection = aConnection;
      if (connection == null) {
        throw new SQLException("Committing closed connection");
      }
      try {
        aSQLStatement = connection.prepareStatement(sQL);
      } catch (final SQLException e) {
        throw e;
      } finally {
        logWarnings("Preparing statement", connection);
      }

      DBConnection.this.aErrorMsg = errorMsg;
    }
// Object Initialization end

    @Override
    @NotNull
    public DBStatement addParam(final int column, final String value) {
      if (aSQLStatement != null) {
        checkValid();
        try {
          aSQLStatement.setString(column, value);
        } catch (final SQLException e) {
          logException("Failure to set parameter on prepared statement", e);
          DBStatementImpl.this.close();
          aSQLStatement = null;
        }
      }
      return this;
    }

    protected final void checkValid() {
      try {
        if (aSQLStatement == null) {
          throw new IllegalStateException("No underlying statement");
        }
        if (aSQLStatement.isClosed()) {
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
      if (aSQLStatement != null) {
        checkValid();
        try {
          aSQLStatement.setInt(column, value);
        } catch (final SQLException e) {
          logException("Failure to create prepared statement", e);
          DBStatementImpl.this.close();
          aSQLStatement = null;
        }
      }
      return this;
    }

    @Override
    @NotNull
    public DBStatement addParam(final int column, final long value) {
      checkValid();
      if (aSQLStatement != null) {
        try {
          aSQLStatement.setLong(column, value);
        } catch (final SQLException e) {
          logException("Failure to create prepared statement", e);
          DBStatementImpl.this.close();
          aSQLStatement = null;
        }
      }
      return this;
    }

    @Override
    public boolean exec() throws SQLException {
      checkValid();
      final PreparedStatement sqlStatement = this.aSQLStatement;
      if (sqlStatement == null) {
        logException("No prepared statement available", new NullPointerException());
        return false;
      }
      try {
        sqlStatement.execute();
        logWarnings("Executing prepared statement", sqlStatement);
        return true;
      } catch (final SQLException e) {
        logException(aErrorMsg, e);
        try {
          sqlStatement.close();
        } catch (final SQLException e2) {
          logWarning("Error closing prepared statement after error", e2);
          e.addSuppressed(e2);
        }

        aSQLStatement = null;
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
      return aStringCache;
    }

    @Override
    public void close() {
      if (aSQLStatement != null) {
        try {
          aSQLStatement.close();
        } catch (SQLException e) {
          logException("Error closing statement", e);
          aSQLStatement = null;
          throw new RuntimeException(e);
        }
        aSQLStatement = null;
      }
    }

    @Override
    public void closeHelper() {
      DBConnection.this.close();
    }

  }

  public class DBQueryImpl extends DBStatementImpl implements DBQuery {

    List<ResultSet> aResultSets;

    // Object Initialization
    public DBQueryImpl() {
      super();
    }

    public DBQueryImpl(@NotNull final String sQL, final String errorMsg) throws SQLException {
      super(sQL, errorMsg);
      aResultSets = new ArrayList<>();
    }
// Object Initialization end

    @Override
    @NotNull
    public DBQuery addParam(final int column, final String value) {
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
        final PreparedStatement sqlStatement = this.aSQLStatement;
        if (sqlStatement == null) {
          return null;
        }
        final ResultSet result = sqlStatement.executeQuery();
        logWarnings("Prepared statement " + sqlStatement.toString(), sqlStatement);
        aResultSets.add(result);
        return result;
      } catch (final SQLException e) {
        logException(aErrorMsg, e);
      }
      return null;
    }

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
        logWarning("The query " + aSQLStatement + " does not return 1 element");
        try {
          rs.close();
        } catch (SQLException e) {
          logException("Failure closing resultset", e);
        }
        return null;
      }
      logWarnings("getSingleHelper resultset", rs);
      if (!rs.next()) {
        logWarnings("getSingleHelper resultset", rs);
        try {
          rs.close();
        } catch (SQLException e) {
          logException("Failure closing resultset", e);
        }
        return null; // No result, that is allowed, no warning
      }
      logWarnings("getSingleHelper resultset", rs);
      if (rs.getObject(1) == null) {
        logWarnings("getSingleHelper resultset", rs);
        try {
          rs.close();
        } catch (SQLException e) {
          logException("Failure closing resultset", e);
        }
        return null;
      }
      logWarnings("getSingleHelper resultset", rs);
      aResultSets.add(rs);
      return rs;
    }    @Override
    @NotNull
    public DBQuery addParam(final int column, final int value) {
      super.addParam(column, value);
      return this;
    }




    @Override
    @NotNull
    public DBQuery addParam(final int column, final long value) {
      super.addParam(column, value);
      return this;
    }


    @Override
    public void close() {
      if (aResultSets != null) {
        for (ResultSet rs : aResultSets) {
          try {
            rs.close();
          } catch (SQLException e) {
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
  private static StringCache aStringCache;
  @Nullable
  public String aErrorMsg;
  @Nullable
  private Connection aConnection;
  @NotNull
  private List<DBStatement> aStatements;

  // Object Initialization
  private DBConnection(@NotNull final DataSource dataSource) throws SQLException {
    aConnection = dataSource.getConnection();
    aConnection.setAutoCommit(false);
    aStatements = new ArrayList<>();
    aStringCache = StringCache.NOPCACHE;
  }

  /**
   * @deprecated Use {@link #newInstance(DataSource)}
   */
  @Deprecated
  public static DBConnection getDbHelper(String dbresource, Object key) {
    return newInstance(dbresource);
  }

  /**
   * @deprecated Use {@link #newInstance(DataSource)}
   */
  @Deprecated
  public static DBConnection newInstance(String dbresource) {
    try {
      return newInstance(getDataSource(dbresource));
    } catch (NamingException e) {
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
    aStringCache = stringCache;
  }

  public static DataSource getDataSource(String dbresource) throws NamingException {
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
  private <T extends DBStatement> T recordStatement(@NotNull T statement) {
    aStatements.add(statement);
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
    final Connection connection = this.aConnection;
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

  static void logWarnings(@Nullable final String string, @Nullable final Connection connection) {
    if (connection != null) {
      try {
        if (connection.isClosed()) {
          getLogger().log(Level.INFO, "Logging warnings on closed connection");
        } else {
          try {
            SQLWarning warning = connection.getWarnings();
            while (warning != null) {
              getLogger().log(Level.WARNING, string, connection);
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

  public void rollback() throws SQLException {
    final Connection connection = this.aConnection;
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

  static void logWarnings(@Nullable final String string, @Nullable final Statement statement) {
    if (statement != null) {
      try {
        if (statement.isClosed()) {
          getLogger().log(Level.INFO, "Logging warnings on closed statement");
        } else {
          try {
            SQLWarning warning = statement.getWarnings();
            while (warning != null) {
              getLogger().log(Level.WARNING, string, statement);
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

  static void logWarnings(@Nullable final String string, @Nullable final ResultSet resultSet) {
    if (resultSet != null) {
      try {
        SQLWarning warning = resultSet.getWarnings();
        while (warning != null) {
          getLogger().log(Level.WARNING, string, resultSet);
          warning = warning.getNextWarning();
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
    final Connection connection = this.aConnection;
    if (connection == null) {
      return;
    }
    Exception error = null;
    getLogger().log(DETAIL_LOG_LEVEL, "Closing connection");
    for (DBStatement statement : aStatements) {
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
    aStatements = new ArrayList<>();
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
      aConnection = null;
    }
    if (error != null) {
      if (error instanceof RuntimeException) {
        throw (RuntimeException) error;
      } else {
        throw new RuntimeException(error);
      }
    }
  }


}
