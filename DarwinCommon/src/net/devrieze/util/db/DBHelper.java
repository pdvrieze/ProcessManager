package net.devrieze.util.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static net.devrieze.util.Annotations.*;

import net.devrieze.annotations.NotNull;
import net.devrieze.annotations.Nullable;
import net.devrieze.util.CompoundException;
import net.devrieze.util.StringCache;
//import net.devrieze.util.StringCacheImpl;


public class DBHelper implements AutoCloseable{

  private static final Level DETAIL_LOG_LEVEL = Level.FINE;

  private static final String LOGGER_NAME = "DBHelper";


  private static class DataSourceWrapper {

    @NotNull
    final DataSource aDataSource;

    @NotNull
    final ConcurrentHashMap<Object, Connection> aConnectionMap;

    DataSourceWrapper(@NotNull final DataSource pDataSource) {
      aDataSource = pDataSource;
      aConnectionMap = new ConcurrentHashMap<>(5);
    }
  }


  public interface DBStatement extends AutoCloseable {

    @NotNull
    DBStatement addParam(int pColumn, String pValue);

    @NotNull
    DBStatement addParam(int pColumn, int pValue);

    @NotNull
    DBStatement addParam(int pColumn, long pValue);

    boolean exec();

    boolean execCommit();

    @Override
    void close();

    void closeHelper();

    @NotNull
    StringCache getStringCache();
  }


  private class DBStatementImpl implements DBStatement {

    @Nullable
    PreparedStatement aSQLStatement;

    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(@NotNull final String pSQL, @Nullable final String pErrorMsg) throws SQLException {
      Connection connection = aConnection;
      boolean connectionValid = false;
      connection = aConnection;
      if (connection != null && (! connection.isClosed())) {
        try {
          connectionValid = connection.isValid(1);
        } catch (final AbstractMethodError e) {
          logWarning("We should use jdbc 4, not 3. isValid is missing");
          try {
            connection.close();
          } catch (SQLException ex) { /* Ignore problems closing connection */ }
          aConnection = connection = null;
        }
      }
      if (! connectionValid) { connection = null; }

      if (aDataSource!=null) {
        final DataSourceWrapper dataSource = notNull(aDataSource);
        if (!connectionValid) {
          if (SHARE_CONNECTIONS) {
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
              dataSource.aConnectionMap.remove(DBHelper.this.aKey);
              aConnection = connection = null;
            }
          }
        }
        if (connection == null) {
          connection = dataSource.aDataSource.getConnection();
          connection.setAutoCommit(false);
          dataSource.aConnectionMap.put(DBHelper.this.aKey, connection);
          DBHelper.this.aConnection = connection;
        }
      }
      if (connection!=null) {
        try {
          aSQLStatement = connection.prepareStatement(pSQL);
          logWarnings("Preparing statement", connection);
        } catch (final SQLException e) {
          DBHelper.this.close();
          throw e;
        }
      }

      DBHelper.this.aErrorMsg = pErrorMsg;
    }

    @Override
    @NotNull
    public DBStatement addParam(final int pColumn, final String pValue) {
      if (aSQLStatement != null) {
        checkValid();
        try {
          notNull(aSQLStatement).setString(pColumn, pValue);
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
    public DBStatement addParam(final int pColumn, final int pValue) {
      if (aSQLStatement != null) {
        checkValid();
        try {
          notNull(aSQLStatement).setInt(pColumn, pValue);
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
    public DBStatement addParam(final int pColumn, final long pValue) {
      checkValid();
      if (aSQLStatement != null) {
        try {
          aSQLStatement.setLong(pColumn, pValue);
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
        if (notNull(aSQLStatement).isClosed()) {
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


  public interface DBQuery extends DBStatement {

    @Override
    DBQuery addParam(int pColumn, String pValue);

    @Override
    DBQuery addParam(int pColumn, int pValue);

    @Override
    DBQuery addParam(int pColumn, long pValue);

    boolean execQueryEmpty();

    boolean execQueryNotEmpty();

    /** Execute the query and get the result set. */
    ResultSet execQuery();

    /** Execute the query and return the integer value */
    Integer intQuery();

    /** Execute the query and return the long value */
    Long longQuery();
  }

  public class DBQueryImpl extends DBStatementImpl implements DBQuery {
    List<ResultSet> aResultSets;

    public DBQueryImpl() {
      super();
    }

    public DBQueryImpl(@NotNull final String pSQL, final String pErrorMsg) throws SQLException {
      super(pSQL, pErrorMsg);
      aResultSets = new ArrayList<>();
    }

    @Override
    @NotNull
    public DBQuery addParam(final int pColumn, final String pValue) {
      super.addParam(pColumn, pValue);
      return this;
    }

    @Override
    @NotNull
    public DBQuery addParam(final int pColumn, final int pValue) {
      super.addParam(pColumn, pValue);
      return this;
    }

    @Override
    @NotNull
    public DBQuery addParam(final int pColumn, final long pValue) {
      super.addParam(pColumn, pValue);
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

  public interface DBInsert extends DBStatement {

    @Override
    DBInsert addParam(int pColumn, String pValue);

  }


  private class DBInsertImpl extends DBStatementImpl implements DBInsert {


    public DBInsertImpl(@NotNull final String pSQL, final String pErrorMsg) throws SQLException {
      super(pSQL, pErrorMsg);
    }

    public DBInsertImpl() {
      // Dud constructor that doesn't do anything
    }

    @Override
    @NotNull
    public DBInsert addParam(final int pColumn, final String pValue) {
      return (DBInsert) super.addParam(pColumn, pValue);
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

  private static final boolean SHARE_CONNECTIONS = false;

  @Nullable
  private volatile static Map<String, DataSourceWrapper> aSourceMap;

  @Nullable
  private final DataSourceWrapper aDataSource;

  @NotNull
  private List<DBStatement> aStatements;

  @NotNull
  private StringCache aStringCache;

  private DBHelper(@Nullable final DataSourceWrapper pDataSource, @Nullable final Object pKey) {
    aDataSource = pDataSource;
    aKey = pKey != null ? pKey : new Object();
    aStatements = new ArrayList<>();
    aStringCache = StringCache.NOPCACHE;
  }

  /**
   * @deprecated use {@link #getDbHelper(String, Object)}
   */
  @Deprecated
  @NotNull
  public static DBHelper dbHelper(final String pResourceName, final Object pKey) {
    return getDbHelper(pResourceName, pKey);
  }

  @NotNull
  public static DBHelper getDbHelper(final String pResourceName, final Object pKey) {
    if (aSourceMap == null) {
      synchronized (aShareLock) {
        if (aSourceMap == null) {
          aSourceMap = new ConcurrentHashMap<>();
        }
      }
    }
    DataSourceWrapper dataSource = notNull(aSourceMap).get(pResourceName);
    if (dataSource == null) {
      try {
        final InitialContext initialContext = new InitialContext();
        dataSource = new DataSourceWrapper((DataSource)notNull(Objects.requireNonNull(initialContext.lookup(pResourceName))));
        notNull(aSourceMap).put(pResourceName, dataSource);
      } catch (final NamingException|NullPointerException e) {
        logException("Failure to register access permission in database", e);
        return new DBHelper(null, pKey); // Return an empty helper to ensure building doesn't fail stuff
      }
    }
    if (getLogger().isLoggable(DETAIL_LOG_LEVEL)) { // Do this only when we log this is going to be output
      final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
      final StringBuilder message = new StringBuilder();
      message.append("dbHelper invoked for ").append(pResourceName).append(" with key [").append(pKey).append("] from ");
      for (int i = 2; (i < stackTraceElements.length) && (i < 7); ++i) {
        if (i > 2) {
          message.append(" -> ");
        }
        message.append(stackTraceElements[i]);
      }
      message.append("\n  ").append(dataSource.aConnectionMap.size()).append(" outstanding helpers on this resource.");
      getLogger().log(DETAIL_LOG_LEVEL, message.toString());
    }
    return new DBHelper(dataSource, pKey);
  }

  @NotNull
  private static Logger getLogger() {
    return Logger.getLogger(LOGGER_NAME);
  }

  public static void logWarning(@NotNull final String pMsg) {
    getLogger().log(Level.WARNING, pMsg);
  }

  public static void logWarning(@Nullable final String pMsg, @NotNull final Throwable pException) {
    getLogger().log(Level.WARNING, pMsg, pException);
  }

  static void logWarnings(@Nullable final String pString, @Nullable final Connection pConnection) {
    if (pConnection != null) {
      try {
        if (pConnection.isClosed()) {
          getLogger().log(Level.INFO, "Logging warnings on closed connection");
        } else {
          try {
            SQLWarning warning = pConnection.getWarnings();
            while (warning!=null) {
              getLogger().log(Level.WARNING, pString, pConnection);
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

  static void logWarnings(@Nullable final String pString, @Nullable final Statement pStatement) {
    if (pStatement != null) {
      try {
        if (pStatement.isClosed()) {
          getLogger().log(Level.INFO, "Logging warnings on closed statement");
        } else {
          try {
            SQLWarning warning = pStatement.getWarnings();
            while (warning!=null) {
              getLogger().log(Level.WARNING, pString, pStatement);
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

  static void logWarnings(@Nullable final String pString, @Nullable final ResultSet pResultSet) {
    if (pResultSet != null) {
      try {
        if (pResultSet.isClosed()) {
          getLogger().log(Level.INFO, "Logging warnings on closed resultset");
        } else {
          try {
            SQLWarning warning = pResultSet.getWarnings();
            while (warning!=null) {
              getLogger().log(Level.WARNING, pString, pResultSet);
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

  public static void logException(@Nullable final String pMsg, @NotNull final Throwable pE) {
    getLogger().log(Level.SEVERE, pMsg, pE);
  }

  @NotNull
  public DBQuery makeQuery(@NotNull final String pSQL) {
    return makeQuery(pSQL, null);
  }

  @SuppressWarnings("resource")
  @NotNull
  public DBQuery makeQuery(@NotNull final String pSQL, @Nullable final String pErrorMsg) {
    try {
      if (aDataSource != null) {
        return recordStatement(new DBQueryImpl(pSQL, pErrorMsg));
      }
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return recordStatement(new DBQueryImpl());
  }

  @NotNull
  private <T extends DBStatement> T recordStatement(@NotNull T statement) {
    aStatements.add(statement);
    return statement;
  }

  @NotNull
  public DBInsert makeInsert(@NotNull final String pSQL) {
    return makeInsert(pSQL, null);
  }

  @SuppressWarnings("resource")
  @NotNull
  public DBInsert makeInsert(@NotNull final String pSQL, @Nullable final String pErrorMsg) {
    try {
      if (aDataSource != null) {
        return recordStatement(new DBInsertImpl(pSQL, pErrorMsg));
      }
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return recordStatement(new DBInsertImpl());
  }

  public void commit() {
    try {
      notNull(aConnection).commit();
      logWarnings("Committing database connection", aConnection);
    } catch (final SQLException e) {
      logException("Failure to commit statement", e);
      rollback();
    }
  }

  public void rollback() {
    try {
      notNull(aConnection).rollback();
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
        try (Connection connection2 = notNull(aDataSource).aConnectionMap.remove(aKey)) {
          // The code comes for free
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

  public static void closeConnections(@NotNull final Object pReference) {
    Exception error = null;
    int count = 0;
    synchronized (aShareLock) {
      if (aSourceMap!=null) {
        for (final DataSourceWrapper dataSource : aSourceMap.values()) {
          try(final Connection conn = dataSource.aConnectionMap.remove(pReference)) {
            if (conn != null) {
              ++count;
            }
          } catch (SQLException e) {
            if (error==null) { error=e; } else { error.addSuppressed(e); }
          }
        }
      }
    }
    getLogger().log(DETAIL_LOG_LEVEL, "Closed " + count + " connections for key " + pReference);
    if (error!=null) {
      if (error instanceof RuntimeException) {
        throw (RuntimeException) error;
      } else {
        throw new RuntimeException(error);
      }
    }
  }

  public static void closeAllConnections(@NotNull final String pDbResource) {
    ArrayList<SQLException> exceptions = null;
    int count = 0;
    synchronized (aShareLock) {
      if (aSourceMap!=null) {
        final DataSourceWrapper wrapper = aSourceMap.get(pDbResource);
        if (wrapper != null) {
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
          notNull(aSourceMap).remove(pDbResource);
        }
      }
    }
    getLogger().log(DETAIL_LOG_LEVEL, "Closed " + count + " connections for resource " + pDbResource);
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
   * @param pStringCache The string cache.
   */
  public void setStringCache(@NotNull final StringCache pStringCache) {
    aStringCache = pStringCache;
  }

}
