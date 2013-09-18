package net.devrieze.util.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import net.devrieze.util.CompoundException;
import net.devrieze.util.StringCache;


public class DBHelper implements AutoCloseable{

  private static final Level DETAIL_LOG_LEVEL = Level.INFO;

  private static final String LOGGER_NAME = "DBHelper";


  private static class DataSourceWrapper {

    DataSource aDataSource;

    ConcurrentHashMap<Object, Connection> aConnectionMap;

    DataSourceWrapper(final DataSource pDataSource) {
      aDataSource = pDataSource;
      aConnectionMap = new ConcurrentHashMap<Object, Connection>(5);
    }
  }


  public interface DBStatement extends AutoCloseable {

    DBStatement addParam(int pColumn, String pValue);

    DBStatement addParam(int pColumn, int pValue);

    DBStatement addParam(int pColumn, long pValue);

    boolean exec();

    boolean execCommit();

    @Override
    void close();

    StringCache getStringCache();

    void closeHelper();
  }


  public String aErrorMsg;

  private Connection aConnection;

  private final Object aKey;

  private static Object aShareLock = new Object();

  private static final boolean SHARE_CONNECTIONS = false;

  private volatile static Map<String, DataSourceWrapper> aSourceMap;


  private class DBStatementImpl implements DBStatement {

    public PreparedStatement aSQLStatement;

    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(final String pSQL, final String pErrorMsg) throws SQLException {
      boolean connectionValid = false;
      if (DBHelper.this.aConnection != null) {
        try {
          connectionValid = aConnection.isValid(1);
        } catch (final AbstractMethodError e) {
          logWarning("We should use jdbc 4, not 3. isValid is missing");
          try {
            aConnection.close();
          } catch (SQLException ex) { /* Ignore problems closing connection */ }
          aConnection = null;
        }
      }
      if (!connectionValid) {
        if (SHARE_CONNECTIONS) {
          final Connection connection = aDataSource.aConnectionMap.get(DBHelper.this.aKey);
          if (connection!=null) {
            try {
              connectionValid = connection.isValid(1);
            } catch (final AbstractMethodError e) {
              logWarning("We should use jdbc 4, not 3. isValid is missing");
              connectionValid = false;
              try {
                connection.close();
              } catch (SQLException ex) { /* Ignore problems closing connection */ }
            }
          } else {
            connectionValid = false;
          }
          if ((DBHelper.this.aConnection == null) && (connection != null) && connectionValid) {
            DBHelper.this.aConnection = connection;
          } else {
            aDataSource.aConnectionMap.remove(DBHelper.this.aKey);
            DBHelper.this.aConnection = null;
          }
        }
        if (DBHelper.this.aConnection == null) {
          DBHelper.this.aConnection = aDataSource.aDataSource.getConnection();
          DBHelper.this.aConnection.setAutoCommit(false);
          aDataSource.aConnectionMap.put(DBHelper.this.aKey, DBHelper.this.aConnection);
        }
      }
      try {
        aSQLStatement = aConnection.prepareStatement(pSQL);
      } catch (final SQLException e) {
        DBHelper.this.close();
        throw e;
      }

      logWarnings("Preparing statement", DBHelper.this.aConnection.getWarnings());
      DBHelper.this.aErrorMsg = pErrorMsg;
    }

    @Override
    public DBStatement addParam(final int pColumn, final String pValue) {
      if (aSQLStatement != null) {
        checkValid();
        try {
          aSQLStatement.setString(pColumn, pValue);
        } catch (final SQLException e) {
          logException("Failure to set parameter on prepared statement", e);
          DBStatementImpl.this.close();
          aSQLStatement = null;
        }
      }
      return this;
    }

    @Override
    public DBStatement addParam(final int pColumn, final int pValue) {
      if (aSQLStatement != null) {
        checkValid();
        try {
          aSQLStatement.setInt(pColumn, pValue);
        } catch (final SQLException e) {
          logException("Failure to create prepared statement", e);
          DBStatementImpl.this.close();
          aSQLStatement = null;
        }
      }
      return this;
    }

    @Override
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
      try {
        if (aSQLStatement == null) {
          logException("No prepared statement available", new NullPointerException());
          return false;
        }
        aSQLStatement.execute();
        logWarnings("Executing prepared statement", aSQLStatement.getWarnings());
        return true;
      } catch (final SQLException e) {
        logException(aErrorMsg, e);
        try {
          aSQLStatement.close();
        } catch (final SQLException e2) {
          logWarning("Error closing prepared statement after error", e2);
        }

        aSQLStatement = null;
        try {
          aConnection.rollback();
          logWarnings("Rolling back database statements", aConnection.getWarnings());
        } catch (final SQLException e1) {
          logException("Rollback failed", e);
        }
        return false;
      }
    }

    @Override
    public boolean execCommit() {
      final boolean result = exec();
      if (result) {
        try {
          aConnection.commit();
          logWarnings("Committing database statements", aConnection.getWarnings());
        } catch (final SQLException e) {
          logException("Commit failed", e);
          try {
            aConnection.rollback();
            logWarnings("Rolling back database statements", aConnection.getWarnings());
          } catch (final SQLException e1) {
            logException("Rollback failed after commit failed", e1);
          }
          return false;
        }
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
    public StringCache getStringCache() {
      return aStringCache;
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

    public DBQueryImpl(final String pSQL, final String pErrorMsg) throws SQLException {
      super(pSQL, pErrorMsg);
      aResultSets = new ArrayList<ResultSet>();
    }

    @Override
    public DBQuery addParam(final int pColumn, final String pValue) {
      super.addParam(pColumn, pValue);
      return this;
    }

    @Override
    public DBQuery addParam(final int pColumn, final int pValue) {
      super.addParam(pColumn, pValue);
      return this;
    }

    @Override
    public DBQuery addParam(final int pColumn, final long pValue) {
      super.addParam(pColumn, pValue);
      return this;
    }

    @Override
    public ResultSet execQuery() {
      checkValid();
      try {
        if (aSQLStatement == null) {
          return null;
        }
        final ResultSet result = aSQLStatement.executeQuery();
        logWarnings("Prepared statement " + aSQLStatement.toString(), aSQLStatement.getWarnings());
        aResultSets.add(result);
        return result;
      } catch (final SQLException e) {
        logException(aErrorMsg, e);
      }
      return null;
    }

    @Override
    public boolean execQueryNotEmpty() {
      final ResultSet rs = execQuery();
      try {
        if (rs == null) {
          return false;
        }
        final boolean result = rs.next();
        logWarnings("execQueryNotEmpty resultset", rs.getWarnings());
        return result;
      } catch (final SQLException e) {
        logException("Error processing result set", e);
        return false;
      } finally {
        if (rs!=null) {
          try {
            rs.close();
          } catch (SQLException e) {
            logException("Failure closing resultset", e);
          }
        }
      }
    }

    @Override
    public boolean execQueryEmpty() {
      final ResultSet rs = execQuery();
      try {
        if (rs == null) {
          return true;
        }
        final boolean result = !rs.next();
        logWarnings("execQueryNotEmpty resultset", rs.getWarnings());
        return result;
      } catch (final SQLException e) {
        logException("Error processing result set", e);
        return true;
      } finally {
        if (rs!=null) {
          try {
            rs.close();
          } catch (SQLException e) {
            logException("Failure closing resultset", e);
          }
        }
      }
    }

    @Override
    public Integer intQuery() {
      try {
        ResultSet rs=null;
        try {
           rs = getSingleHelper();
          return rs == null ? null : rs.getInt(1);
        } catch (final SQLException e) {
          logException("Error processing result set", e);
          return null;
        } finally {
          if (rs!=null) {
            try {
              rs.close();
            } catch (SQLException e) {
              logException("Failure closing resultset", e);
            }
          }
        }
      } finally {
        close();
      }
    }

    @Override
    public Long longQuery() {
      try {
        ResultSet rs = null;
        try {
          rs = getSingleHelper();
          return rs == null ? null : rs.getLong(1);
        } catch (final SQLException e) {
          logException("Error processing result set", e);
          return null;
        } finally {
          if (rs!=null) {
            try {
              rs.close();
            } catch (SQLException e) {
              logException("Failure closing resultset", e);
            }
          }
        }
      } finally {
        close();
      }
    }

    private ResultSet getSingleHelper() throws SQLException {
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
      logWarnings("getSingleHelper resultset", rs.getWarnings());
      if (!rs.next()) {
        logWarnings("getSingleHelper resultset", rs.getWarnings());
        try {
          rs.close();
        } catch (SQLException e) {
          logException("Failure closing resultset", e);
        }
        return null; // No result, that is allowed, no warning
      }
      logWarnings("getSingleHelper resultset", rs.getWarnings());
      if (rs.getObject(1) == null) {
        logWarnings("getSingleHelper resultset", rs.getWarnings());
        try {
          rs.close();
        } catch (SQLException e) {
          logException("Failure closing resultset", e);
        }
        return null;
      }
      logWarnings("getSingleHelper resultset", rs.getWarnings());
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


    public DBInsertImpl(final String pSQL, final String pErrorMsg) throws SQLException {
      super(pSQL, pErrorMsg);
    }

    public DBInsertImpl() {
      // Dud constructor that doesn't do anything
    }

    @Override
    public DBInsert addParam(final int pColumn, final String pValue) {
      return (DBInsert) super.addParam(pColumn, pValue);
    }

  }


  private final DataSourceWrapper aDataSource;

  private StringCache aStringCache;

  private List<DBStatement> aStatements;

  private DBHelper(final DataSourceWrapper pDataSource, final Object pKey) {
    aDataSource = pDataSource;
    aKey = pKey != null ? pKey : new Object();
    aStatements = new ArrayList<DBStatement>();
  }

  /**
   * @deprecated use {@link #getDbHelper(String, Object)}
   */
  @Deprecated
  public static DBHelper dbHelper(final String pResourceName, final Object pKey) {
    return getDbHelper(pResourceName, pKey);
  }

  public static DBHelper getDbHelper(final String pResourceName, final Object pKey) {
    if (aSourceMap == null) {
      synchronized (aShareLock) {
        if (aSourceMap == null) {
          aSourceMap = new ConcurrentHashMap<String, DataSourceWrapper>();
        }
      }
    }
    DataSourceWrapper dataSource = aSourceMap.get(pResourceName);
    if (dataSource == null) {
      try {
        final InitialContext initialContext = new InitialContext();
        dataSource = new DataSourceWrapper((DataSource) initialContext.lookup(pResourceName));
        aSourceMap.put(pResourceName, dataSource);
      } catch (final NamingException e) {
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

  private static Logger getLogger() {
    return Logger.getLogger(LOGGER_NAME);
  }

  public static void logWarning(final String pMsg) {
    getLogger().log(Level.WARNING, pMsg);
  }

  public static void logWarning(final String pMsg, final Throwable pException) {
    getLogger().log(Level.WARNING, pMsg, pException);
  }

  static void logWarnings(final String pString, final SQLWarning pWarnings) {
    if (pWarnings != null) {
      getLogger().log(Level.WARNING, pString, pWarnings);
      logWarnings(pString, pWarnings.getNextWarning());
    }
  }

  public static void logException(final String pMsg, final Throwable pE) {
    getLogger().log(Level.SEVERE, pMsg, pE);
  }

  public DBQuery makeQuery(final String pSQL) {
    return makeQuery(pSQL, null);
  }

  public DBQuery makeQuery(final String pSQL, final String pErrorMsg) {
    try {
      if (aDataSource != null) {
        return recordStatement(new DBQueryImpl(pSQL, pErrorMsg));
      }
    } catch (final SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return recordStatement(new DBQueryImpl());
  }

  private <T extends DBStatement> T recordStatement(T statement) {
    if (aStatements==null) {aStatements = new ArrayList<DBHelper.DBStatement>(); }
    aStatements.add(statement);
    return statement;
  }

  public DBInsert makeInsert(final String pSQL) {
    return makeInsert(pSQL, null);
  }

  public DBInsert makeInsert(final String pSQL, final String pErrorMsg) {
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
      aConnection.commit();
      logWarnings("Committing database connection", aConnection.getWarnings());
    } catch (final SQLException e) {
      logException("Failure to commit statement", e);
      try {
        aConnection.rollback();
        logWarnings("Rolling back database connection", aConnection.getWarnings());
      } catch (final SQLException f) {
        logException("Failure to rollback after failed commit", f);
      }
    }
  }

  public void rollback() {
    try {
      aConnection.rollback();
      logWarnings("Rolling back database connection", aConnection.getWarnings());
    } catch (final SQLException e) {
      logException("Failure to roll back statement", e);
    }
  }

  /**
   * Close the underlying connection for this helper. A new connection will
   * automatically be established when needed.
   */
  @Override
  public void close() {
    getLogger().log(DETAIL_LOG_LEVEL, "Closing connection for key " + aKey);
    try {
      if (aStatements!=null) {
        for (DBStatement statement:aStatements) {
          statement.close();
        }
      }
    } catch (RuntimeException e) {
      logException("Failure to close database statements", e);
    }
    aStatements = null;
    try {
      if (aConnection != null) {
        if (!aConnection.isClosed()) {
          aConnection.close();
        }
      } else if (aDataSource != null) {
        aConnection = aDataSource.aConnectionMap.get(aKey);
        if (aConnection != null) {
          if (!aConnection.isClosed()) {
            aConnection.close();
          }
        }
      }
      aConnection = null;
      if (aDataSource != null) {
        aDataSource.aConnectionMap.remove(aKey);
      }

    } catch (SQLException e) {
      logException("Failure to close database connection", e);
    }
  }

  public static void closeConnections(final Object pReference) {
    int count = 0;
    synchronized (aShareLock) {
      for (final DataSourceWrapper dataSource : aSourceMap.values()) {
        final Connection conn = dataSource.aConnectionMap.get(pReference);
        if (conn != null) {
          ++count;
          try {
            if (!conn.isClosed()) {
              conn.close();
            }
          } catch (final SQLException e) {
            logException("Failure to close connection", e);
          }
          dataSource.aConnectionMap.remove(pReference);
        }
      }
    }
    getLogger().log(DETAIL_LOG_LEVEL, "Closed " + count + " connections for key " + pReference);
  }

  public static void closeAllConnections(final String pDbResource) {
    ArrayList<SQLException> exceptions = null;
    int count = 0;
    synchronized (aShareLock) {
      final DataSourceWrapper wrapper = aSourceMap.get(pDbResource);
      if (wrapper != null) {
        for (final Connection connection : wrapper.aConnectionMap.values()) {
          try {
            ++count;
            connection.close();
          } catch (final SQLException e) {
            if (exceptions == null) {
              exceptions = new ArrayList<SQLException>();
            }
            exceptions.add(e);
          }
        }
        aSourceMap.remove(pDbResource);
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
  public void setStringCache(final StringCache pStringCache) {
    aStringCache = pStringCache;
  }

}
