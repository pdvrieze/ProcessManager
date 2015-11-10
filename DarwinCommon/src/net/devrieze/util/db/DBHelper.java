package net.devrieze.util.db;

import net.devrieze.util.CompoundException;
import net.devrieze.util.StringCache;
import net.devrieze.util.db.DBConnection.DBInsert;
import net.devrieze.util.db.DBConnection.DBQuery;
import net.devrieze.util.db.DBConnection.DBStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//import net.devrieze.util.StringCacheImpl;

/**
 *
 * @author Paul de Vrieze
 * @deprecated In favour of {@link DBConnection} which does not aim to replicate
 * the general jdbc connection pooling work.
 */
@Deprecated
public class DBHelper implements AutoCloseable{

  private static final Level DETAIL_LOG_LEVEL = Level.FINE;

  private static final String LOGGER_NAME = "DBHelper";

  private static final boolean CACHE=false;

  private static class DataSourceWrapper {

    @NotNull
    final DataSource aDataSource;

    @Nullable
    final ConcurrentHashMap<Object, Connection> aConnectionMap;

    DataSourceWrapper(@NotNull final DataSource dataSource) {
      aDataSource = dataSource;
      aConnectionMap = CACHE ? new ConcurrentHashMap<Object, Connection>(5) : null;
    }
  }

  private class DBStatementImpl implements DBStatement {

    @Nullable
    PreparedStatement aSQLStatement;

    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(@NotNull final String sQL, @Nullable final String errorMsg) throws SQLException {
      StringBuilder debug = new StringBuilder();
      Connection connection = aConnection;
      boolean connectionValid = false;
      connection = aConnection;
      if (connection != null && (! connection.isClosed())) {
        debug.append("Connection not closed.");
        try {
          connectionValid = connection.isValid(1);
        } catch (final AbstractMethodError e) {
          logWarning("We should use jdbc 4, not 3. isValid is missing");
          try {
            connection.close();
          } catch (SQLException ex) { /* Ignore problems closing connection */ }
          aConnection = connection = null;
        }
      } else {
        debug.append("Connection closed.");
      }
      if (! connectionValid) {
        connection = null;
        debug.append(" Connection not valid.");
      }

      if (aDataSource!=null) {
        debug.append(" aDataSource set.");
        final DataSourceWrapper dataSource = aDataSource;
        if (!connectionValid) {
          if (DBHelper.CACHE) {
            assert dataSource.aConnectionMap!=null;
            connection = dataSource.aConnectionMap.get(DBHelper.this.aKey);
            if (connection!=null) {
              try {
                connectionValid = connection.isValid(1);
              } catch (final AbstractMethodError e2) {
                logWarning("We should use jdbc 4, not 3. isValid is missing");
                connectionValid = false;
                try {
                  connection.close();
                } catch (SQLException ex) { /* Ignore problems closing connection */ }
              }
            } else {
              connectionValid = false;
            }
            if ((aConnection == null) && (connection != null) && connectionValid) {
              aConnection = connection;
            } else {
              assert dataSource.aConnectionMap!=null;
              dataSource.aConnectionMap.remove(DBHelper.this.aKey);
              aConnection = connection = null;
            }
          }
        }
        if (connection == null) {
          connection = dataSource.aDataSource.getConnection();
          connection.setAutoCommit(false);
          if (CACHE) {
            assert dataSource.aConnectionMap!=null;
            dataSource.aConnectionMap.put(DBHelper.this.aKey, connection);
          }
          DBHelper.this.aConnection = connection;
        }
      }
      if (connection!=null) {
        try {
          aSQLStatement = connection.prepareStatement(sQL);
          logWarnings("Preparing statement", connection);
        } catch (final SQLException e) {
          logWarnings("Preparing statement", connection);
          e.addSuppressed(new RuntimeException(debug.toString()));
          DBHelper.this.close();
          throw e;
        }
      }

      DBHelper.this.aErrorMsg = errorMsg;
    }

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
    public boolean exec() {
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
    public boolean execCommit() {
      final boolean result = exec();
      if (result) {
        commit();
        close();
      }
      return result;
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
      DBHelper.this.close();
    }

  }

  private class DBQueryImpl extends DBStatementImpl implements DBQuery {
    List<ResultSet> aResultSets;

    public DBQueryImpl() {
      super();
    }

    public DBQueryImpl(@NotNull final String sQL, final String errorMsg) throws SQLException {
      super(sQL, errorMsg);
      aResultSets = new ArrayList<>();
    }

    @Override
    @NotNull
    public DBQuery addParam(final int column, final String value) {
      super.addParam(column, value);
      return this;
    }

    @Override
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
    public boolean execQueryEmpty() {
      try (final ResultSet rs = execQuery()){
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
    @Nullable
    public Integer intQuery() {
      try {
        try (ResultSet rs=getSingleHelper()){
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
        try (ResultSet rs = getSingleHelper()){
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
    }

    @Override
    public void close() {
      if (aResultSets!=null) {
        for (ResultSet rs:aResultSets) {
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


    public DBInsertImpl(@NotNull final String sQL, final String errorMsg) throws SQLException {
      super(sQL, errorMsg);
    }

    public DBInsertImpl() {
      // Dud constructor that doesn't do anything
    }

    @Override
    @NotNull
    public DBInsert addParam(final int column, final String value) {
      return (DBInsert) super.addParam(column, value);
    }

  }

  @Nullable
  public String aErrorMsg;

  @Nullable
  private Connection aConnection;

  @Nullable
  private final Object aKey;

  @NotNull
  private static Object aShareLock = new Object();

  @Nullable
  private volatile static Map<String, DataSourceWrapper> aSourceMap;

  @Nullable
  private final DataSourceWrapper aDataSource;

  @NotNull
  private List<DBStatement> aStatements;

  @NotNull
  private StringCache aStringCache;

  private DBHelper(@Nullable final DataSourceWrapper dataSource, @Nullable final Object key) {
    aDataSource = dataSource;
    aKey = key != null ? key : new Object();
    aStatements = new ArrayList<>();
    aStringCache = StringCache.NOPCACHE;
  }

  /**
   * @deprecated use {@link #getDbHelper(String, Object)}
   */
  @Deprecated
  @NotNull
  public static DBHelper dbHelper(final String resourceName, final Object key) {
    return getDbHelper(resourceName, key);
  }

  @NotNull
  public static DBHelper getDbHelper(final String resourceName, final Object key) {
    if (aSourceMap == null) {
      synchronized (aShareLock) {
        if (aSourceMap == null) {
          aSourceMap = new ConcurrentHashMap<>();
        }
      }
    }
    DataSourceWrapper dataSource = aSourceMap.get(resourceName);
    if (dataSource == null) {
      try {
        final InitialContext initialContext = new InitialContext();
        dataSource = new DataSourceWrapper((DataSource) Objects.requireNonNull(initialContext.lookup(resourceName)));
        aSourceMap.put(resourceName, dataSource);
      } catch (final NamingException|NullPointerException e) {
        logException("Failure to register access permission in database", e);
        return new DBHelper(null, key); // Return an empty helper to ensure building doesn't fail stuff
      }
    }
    if (getLogger().isLoggable(DETAIL_LOG_LEVEL)) { // Do this only when we log this is going to be output
      final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
      final StringBuilder message = new StringBuilder();
      message.append("dbHelper invoked for ").append(resourceName).append(" with key [").append(key).append("] from ");
      for (int i = 2; (i < stackTraceElements.length) && (i < 7); ++i) {
        if (i > 2) {
          message.append(" -> ");
        }
        message.append(stackTraceElements[i]);
      }
      if (CACHE) {
        assert dataSource.aConnectionMap!=null;
        final int size = dataSource.aConnectionMap.size();
        message.append("\n  ").append(size).append(" outstanding helpers on this resource.");
      }
      getLogger().log(DETAIL_LOG_LEVEL, message.toString());
    }
    return new DBHelper(dataSource, key);
  }

  @NotNull
  private static Logger getLogger() {
    return Logger.getLogger(LOGGER_NAME);
  }

  public static void logWarning(@NotNull final String msg) {
    getLogger().log(Level.WARNING, msg);
  }

  public static void logWarning(@Nullable final String msg, @NotNull final Throwable exception) {
    getLogger().log(Level.WARNING, msg, exception);
  }

  static void logWarnings(@Nullable final String string, @Nullable final Connection connection) {
    if (connection != null) {
      try {
        if (connection.isClosed()) {
          getLogger().log(Level.INFO, "Logging warnings on closed connection");
        } else {
          try {
            SQLWarning warning = connection.getWarnings();
            while (warning!=null) {
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

  static void logWarnings(@Nullable final String string, @Nullable final Statement statement) {
    if (statement != null) {
      try {
        if (statement.isClosed()) {
          getLogger().log(Level.INFO, "Logging warnings on closed statement");
        } else {
          try {
            SQLWarning warning = statement.getWarnings();
            while (warning!=null) {
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
        while (warning!=null) {
          getLogger().log(Level.WARNING, string, resultSet);
          warning = warning.getNextWarning();
        }
      } catch (SQLException e) {
        getLogger().log(Level.WARNING, "Error processing warnings", e);
      }
    }
  }

  public static void logException(@Nullable final String msg, @NotNull final Throwable e) {
    getLogger().log(Level.SEVERE, msg, e);
  }

  @NotNull
  public DBQuery makeQuery(@NotNull final String sQL) {
    return makeQuery(sQL, null);
  }

  @SuppressWarnings("resource")
  @NotNull
  public DBQuery makeQuery(@NotNull final String sQL, @Nullable final String errorMsg) {
    try {
      if (aDataSource != null) {
        return recordStatement(new DBQueryImpl(sQL, errorMsg));
      }
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
      throw new RuntimeException(e);
    }
    return recordStatement(new DBQueryImpl());
  }

  @NotNull
  private <T extends DBStatement> T recordStatement(@NotNull T statement) {
    aStatements.add(statement);
    return statement;
  }

  @NotNull
  public DBInsert makeInsert(@NotNull final String sQL) {
    return makeInsert(sQL, null);
  }

  @SuppressWarnings("resource")
  @NotNull
  public DBInsert makeInsert(@NotNull final String sQL, @Nullable final String errorMsg) {
    try {
      if (aDataSource != null) {
        return recordStatement(new DBInsertImpl(sQL, errorMsg));
      }
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return recordStatement(new DBInsertImpl());
  }

  public void commit() {
    try {
      aConnection.commit();
      logWarnings("Committing database connection", aConnection);
    } catch (final SQLException e) {
      logException("Failure to commit statement", e);
      rollback();
    }
  }

  public void rollback() {
    try {
      aConnection.rollback();
      logWarnings("Rolling back database connection", aConnection);
    } catch (final SQLException|NullPointerException e) {
      logException("Failure to roll back statement", e);
    }
  }

  /**
   * Close the underlying connection for this helper. A new connection will
   * automatically be established when needed.
   */
  @Override
  public void close() {
    Exception error = null;
    getLogger().log(DETAIL_LOG_LEVEL, "Closing connection for key " + aKey);
    for (DBStatement statement:aStatements) {
      try {
        statement.close();
      } catch (RuntimeException e) {
        logException("Failure to close database statements", e);
        if (error==null) { error = e; } else {
          error.addSuppressed(e);
        }
      }
    }
    aStatements=new ArrayList<>();
    try {
      Connection connection = aConnection;
      if (connection != null) {
        if (!connection.isClosed()) {
          connection.close();
        }
      } else if (aDataSource != null) {
        if (CACHE) {
          assert aDataSource.aConnectionMap!=null;
          try (Connection connection2 = aDataSource.aConnectionMap.remove(aKey)) {
            // The code comes for free
          }
        }
      }
    } catch (SQLException e) {
      if (error==null) { error=e; } else { error.addSuppressed(e); }
      logException("Failure to close database connection", e);
    } finally {
      aConnection = null;
    }
    if (error!=null) {
      if (error instanceof RuntimeException) {
        throw (RuntimeException) error;
      } else {
        throw new RuntimeException(error);
      }
    }
  }

  public static void closeConnections(@NotNull final Object reference) {
    if (!CACHE) { return; }
    Exception error = null;
    int count = 0;
    synchronized (aShareLock) {
      if (aSourceMap!=null) {
        for (final DataSourceWrapper dataSource : aSourceMap.values()) {
          assert dataSource.aConnectionMap!=null;
          try(final Connection conn = dataSource.aConnectionMap.remove(reference)) {
            if (conn != null) {
              ++count;
            }
          } catch (SQLException e) {
            if (error==null) { error=e; } else { error.addSuppressed(e); }
          }
        }
      }
    }
    getLogger().log(DETAIL_LOG_LEVEL, "Closed " + count + " connections for key " + reference);
    if (error!=null) {
      if (error instanceof RuntimeException) {
        throw (RuntimeException) error;
      } else {
        throw new RuntimeException(error);
      }
    }
  }

  public static void closeAllConnections(@NotNull final String dbResource) {
    if (! CACHE) { return; }
    ArrayList<SQLException> exceptions = null;
    int count = 0;
    synchronized (aShareLock) {
      if (aSourceMap!=null) {
        final DataSourceWrapper wrapper = aSourceMap.get(dbResource);
        if (wrapper != null) {
          assert wrapper.aConnectionMap!=null;
          for (final Connection connection : wrapper.aConnectionMap.values()) {
            try {
              ++count;
              connection.close();
            } catch (final SQLException e) {
              if (exceptions == null) {
                exceptions = new ArrayList<>();
              }
              exceptions.add(e);
            }
          }
          aSourceMap.remove(dbResource);
        }
      }
    }
    getLogger().log(DETAIL_LOG_LEVEL, "Closed " + count + " connections for resource " + dbResource);
    if (exceptions != null) {
      if (exceptions.size() == 1) {
        throw new RuntimeException(exceptions.get(0));
      } else {
        throw new CompoundException(exceptions);
      }
    }
  }

  /**
   * Set a string cache to reset strings from.
   *
   * @param stringCache The string cache.
   */
  public void setStringCache(@NotNull final StringCache stringCache) {
    aStringCache = stringCache;
  }

}
