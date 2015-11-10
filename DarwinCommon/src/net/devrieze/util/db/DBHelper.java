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
    final DataSource mDataSource;

    @Nullable
    final ConcurrentHashMap<Object, Connection> mConnectionMap;

    DataSourceWrapper(@NotNull final DataSource dataSource) {
      mDataSource = dataSource;
      mConnectionMap = CACHE ? new ConcurrentHashMap<Object, Connection>(5) : null;
    }
  }

  private class DBStatementImpl implements DBStatement {

    @Nullable
    PreparedStatement mSQLStatement;

    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(@NotNull final String sQL, @Nullable final String errorMsg) throws SQLException {
      StringBuilder debug = new StringBuilder();
      Connection connection = mConnection;
      boolean connectionValid = false;
      connection = mConnection;
      if (connection != null && (! connection.isClosed())) {
        debug.append("Connection not closed.");
        try {
          connectionValid = connection.isValid(1);
        } catch (final AbstractMethodError e) {
          logWarning("We should use jdbc 4, not 3. isValid is missing");
          try {
            connection.close();
          } catch (SQLException ex) { /* Ignore problems closing connection */ }
          mConnection = connection = null;
        }
      } else {
        debug.append("Connection closed.");
      }
      if (! connectionValid) {
        connection = null;
        debug.append(" Connection not valid.");
      }

      if (mDataSource!=null) {
        debug.append(" mDataSource set.");
        final DataSourceWrapper dataSource = mDataSource;
        if (!connectionValid) {
          if (DBHelper.CACHE) {
            assert dataSource.mConnectionMap!=null;
            connection = dataSource.mConnectionMap.get(DBHelper.this.mKey);
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
            if ((mConnection == null) && (connection != null) && connectionValid) {
              mConnection = connection;
            } else {
              assert dataSource.mConnectionMap!=null;
              dataSource.mConnectionMap.remove(DBHelper.this.mKey);
              mConnection = connection = null;
            }
          }
        }
        if (connection == null) {
          connection = dataSource.mDataSource.getConnection();
          connection.setAutoCommit(false);
          if (CACHE) {
            assert dataSource.mConnectionMap!=null;
            dataSource.mConnectionMap.put(DBHelper.this.mKey, connection);
          }
          DBHelper.this.mConnection = connection;
        }
      }
      if (connection!=null) {
        try {
          mSQLStatement = connection.prepareStatement(sQL);
          logWarnings("Preparing statement", connection);
        } catch (final SQLException e) {
          logWarnings("Preparing statement", connection);
          e.addSuppressed(new RuntimeException(debug.toString()));
          DBHelper.this.close();
          throw e;
        }
      }

      DBHelper.this.mErrorMsg = errorMsg;
    }

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
    public boolean exec() {
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
    public StringCache getStringCache() {
      return mStringCache;
    }

    @Override
    public void close() {
      if (mSQLStatement != null) {
        try {
          mSQLStatement.close();
        } catch (SQLException e) {
          logException("Error closing statement", e);
          mSQLStatement = null;
          throw new RuntimeException(e);
        }
        mSQLStatement = null;
      }
    }

    @Override
    public void closeHelper() {
      DBHelper.this.close();
    }

  }

  private class DBQueryImpl extends DBStatementImpl implements DBQuery {
    List<ResultSet> mResultSets;

    public DBQueryImpl() {
      super();
    }

    public DBQueryImpl(@NotNull final String sQL, final String errorMsg) throws SQLException {
      super(sQL, errorMsg);
      mResultSets = new ArrayList<>();
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
        logWarning("The query " + mSQLStatement + " does not return 1 element");
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
      mResultSets.add(rs);
      return rs;
    }

    @Override
    public void close() {
      if (mResultSets!=null) {
        for (ResultSet rs:mResultSets) {
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
  public String mErrorMsg;

  @Nullable
  private Connection mConnection;

  @Nullable
  private final Object mKey;

  @NotNull
  private static Object mShareLock = new Object();

  @Nullable
  private volatile static Map<String, DataSourceWrapper> mSourceMap;

  @Nullable
  private final DataSourceWrapper mDataSource;

  @NotNull
  private List<DBStatement> mStatements;

  @NotNull
  private StringCache mStringCache;

  private DBHelper(@Nullable final DataSourceWrapper dataSource, @Nullable final Object key) {
    mDataSource = dataSource;
    mKey = key != null ? key : new Object();
    mStatements = new ArrayList<>();
    mStringCache = StringCache.NOPCACHE;
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
    if (mSourceMap == null) {
      synchronized (mShareLock) {
        if (mSourceMap == null) {
          mSourceMap = new ConcurrentHashMap<>();
        }
      }
    }
    DataSourceWrapper dataSource = mSourceMap.get(resourceName);
    if (dataSource == null) {
      try {
        final InitialContext initialContext = new InitialContext();
        dataSource = new DataSourceWrapper((DataSource) Objects.requireNonNull(initialContext.lookup(resourceName)));
        mSourceMap.put(resourceName, dataSource);
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
        assert dataSource.mConnectionMap!=null;
        final int size = dataSource.mConnectionMap.size();
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
      if (mDataSource != null) {
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
    mStatements.add(statement);
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
      if (mDataSource != null) {
        return recordStatement(new DBInsertImpl(sQL, errorMsg));
      }
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return recordStatement(new DBInsertImpl());
  }

  public void commit() {
    try {
      mConnection.commit();
      logWarnings("Committing database connection", mConnection);
    } catch (final SQLException e) {
      logException("Failure to commit statement", e);
      rollback();
    }
  }

  public void rollback() {
    try {
      mConnection.rollback();
      logWarnings("Rolling back database connection", mConnection);
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
    getLogger().log(DETAIL_LOG_LEVEL, "Closing connection for key " + mKey);
    for (DBStatement statement:mStatements) {
      try {
        statement.close();
      } catch (RuntimeException e) {
        logException("Failure to close database statements", e);
        if (error==null) { error = e; } else {
          error.addSuppressed(e);
        }
      }
    }
    mStatements=new ArrayList<>();
    try {
      Connection connection = mConnection;
      if (connection != null) {
        if (!connection.isClosed()) {
          connection.close();
        }
      } else if (mDataSource != null) {
        if (CACHE) {
          assert mDataSource.mConnectionMap!=null;
          try (Connection connection2 = mDataSource.mConnectionMap.remove(mKey)) {
            // The code comes for free
          }
        }
      }
    } catch (SQLException e) {
      if (error==null) { error=e; } else { error.addSuppressed(e); }
      logException("Failure to close database connection", e);
    } finally {
      mConnection = null;
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
    synchronized (mShareLock) {
      if (mSourceMap!=null) {
        for (final DataSourceWrapper dataSource : mSourceMap.values()) {
          assert dataSource.mConnectionMap!=null;
          try(final Connection conn = dataSource.mConnectionMap.remove(reference)) {
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
    synchronized (mShareLock) {
      if (mSourceMap!=null) {
        final DataSourceWrapper wrapper = mSourceMap.get(dbResource);
        if (wrapper != null) {
          assert wrapper.mConnectionMap!=null;
          for (final Connection connection : wrapper.mConnectionMap.values()) {
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
          mSourceMap.remove(dbResource);
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
    mStringCache = stringCache;
  }

}
