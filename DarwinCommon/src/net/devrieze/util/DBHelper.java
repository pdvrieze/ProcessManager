package net.devrieze.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;


public class DBHelper {
  
  private static final String LOGGER_NAME = "DBHelper";


  private static class DataSourceWrapper {
    DataSource aDataSource;
    ConcurrentHashMap<Object, Connection> aConnectionMap;
    
    DataSourceWrapper(DataSource pDataSource) {
      aDataSource = pDataSource;
      aConnectionMap = new ConcurrentHashMap<Object, Connection>(5);
    }
  }
  
  
  
  public interface DBStatement {

    DBStatement addParam(int pColumn, String pValue);
    DBStatement addParam(int pColumn, int pValue);
    DBStatement addParam(int pColumn, long pValue);

    boolean exec();

    boolean execCommit();

    void close() throws SQLException;
  }


  public String aErrorMsg;
  private Connection aConnection;
  private Object aKey;
  public boolean aValid;
  
  private static Object aShareLock = new Object();
  private static final boolean SHARE_CONNECTIONS = true;
  private volatile static Map<String, DataSourceWrapper> aSourceMap;


  private class DBStatementImpl implements DBStatement {
    
    public PreparedStatement aSQL;
    
    public DBStatementImpl() {
      // Dud that doesn't do anything
    }

    DBStatementImpl(String pSQL, String pErrorMsg) throws SQLException {
      if (DBHelper.this.aConnection==null) {
        if (SHARE_CONNECTIONS) {
          DBHelper.this.aConnection = aDataSource.aConnectionMap.get(DBHelper.this.aKey);
        }
        if (DBHelper.this.aConnection==null) {
          DBHelper.this.aConnection = aDataSource.aDataSource.getConnection();
          DBHelper.this.aConnection.setAutoCommit(false);
          aDataSource.aConnectionMap.put(DBHelper.this.aKey, DBHelper.this.aConnection);
        }
      }
      aSQL=aConnection.prepareStatement(pSQL);
      logWarnings("Preparing statement", DBHelper.this.aConnection.getWarnings());
      DBHelper.this.aErrorMsg=pErrorMsg;
    }

    @Override
    public DBStatement addParam(int pColumn, String pValue) {
      checkValid();
      if (aSQL!=null) {
        try {
          aSQL.setString(pColumn, pValue);
        } catch (SQLException e) {
          logException("Failure to create prepared statement", e);
          aSQL=null;
        }
      }
      return this;
    }

    @Override
    public DBStatement addParam(int pColumn, int pValue) {
      checkValid();
      if (aSQL!=null) {
        try {
          aSQL.setInt(pColumn, pValue);
        } catch (SQLException e) {
          logException("Failure to create prepared statement", e);
          aSQL=null;
        }
      }
      return this;
    }

    @Override
    public DBStatement addParam(int pColumn, long pValue) {
      checkValid();
      if (aSQL!=null) {
        try {
          aSQL.setLong(pColumn, pValue);
        } catch (SQLException e) {
          logException("Failure to create prepared statement", e);
          aSQL=null;
        }
      }
      return this;
    }

    @Override
    public boolean exec() {
      checkValid();
      DBHelper.this.aValid = false;
      try {
        if (aSQL==null) {
          logException("No prepared statement available", new NullPointerException());
          return false;
        }
        aSQL.execute();
        logWarnings("Executing prepared statement", aSQL.getWarnings());
        return true;
      } catch (SQLException e) {
        logException(aErrorMsg, e);
        try {
          aConnection.rollback();
          logWarnings("Rolling back database statements", aConnection.getWarnings());
        } catch (SQLException e1) {
          logException( "Rollback failed", e);
        }
        return false;
      }
    }

    @Override
    public boolean execCommit() {
      boolean result = exec();
      if (result) {
        try {
          aConnection.commit();
          logWarnings("Committing database statements", aConnection.getWarnings());
        } catch (SQLException e) {
          logException("Commit failed", e);
          try {
            aConnection.rollback();
            logWarnings("Rolling back database statements", aConnection.getWarnings());
          } catch (SQLException e1) {
            logException("Rollback failed after commit failed", e1);
          }
          return false;
        }
      }
      return result;
    }

    @Override
    public void close() throws SQLException {
      if (aSQL!=null) {
        aSQL.close();
        logWarnings("Closing prepared statement", aSQL.getWarnings());
        aSQL=null;
      }
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

    public DBQueryImpl() {
      super();
    }

    public DBQueryImpl(String pSQL, String pErrorMsg) throws SQLException {
      super(pSQL, pErrorMsg);
    }

    @Override
    public DBQuery addParam(int pColumn, String pValue) {
      return (DBQuery) super.addParam(pColumn, pValue);
    }

    @Override
    public DBQuery addParam(int pColumn, int pValue) {
      return (DBQuery) super.addParam(pColumn, pValue);
    }

    @Override
    public DBQuery addParam(int pColumn, long pValue) {
      return (DBQuery) super.addParam(pColumn, pValue);
    }

    @Override
    public ResultSet execQuery() {
      checkValid();
      DBHelper.this.aValid = false;
      try {
        if (aSQL==null) { return null; }
        ResultSet result = aSQL.executeQuery();
        logWarnings("Prepared statement "+ aSQL.toString(), aSQL.getWarnings());
        
        return result;
      } catch (SQLException e) {
        logException(aErrorMsg, e);
      }
      return null;
    }
    
    @Override
    public boolean execQueryNotEmpty() {
      ResultSet rs = execQuery();
      try {
        if (rs==null) { return false; }
        boolean result = rs.next();
        logWarnings("execQueryNotEmpty resultset", rs.getWarnings());
        return result;
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return false;
      }
    }
    
    @Override
    public boolean execQueryEmpty() {
      ResultSet rs = execQuery();
      try {
        if (rs==null) { return true; }
        boolean result = ! rs.next();
        logWarnings("execQueryNotEmpty resultset", rs.getWarnings());
        return result;
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return true;
      }
    }

    @Override
    public Integer intQuery() {
      try {
        ResultSet rs = getSingleHelper();
        return rs==null ? null : rs.getInt(1);
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return null;
      }
    }

    @Override
    public Long longQuery() {
      try {
        ResultSet rs = getSingleHelper();
        return rs==null ? null : rs.getLong(1);
      } catch (SQLException e) {
        logException("Error processing result set", e);
        return null;
      }
    }

    private ResultSet getSingleHelper() throws SQLException {
      ResultSet rs = execQuery();
      if (rs==null) { return null; }
      if (rs.getMetaData().getColumnCount()!=1) {
        logWarning("The query "+aSQL+ " does not return 1 element");
        return null;
      }
      logWarnings("getSingleHelper resultset", rs.getWarnings());
      if (! rs.next()) {
        logWarnings("getSingleHelper resultset", rs.getWarnings());
        return null; // No result, that is allowed, no warning
      }
      logWarnings("getSingleHelper resultset", rs.getWarnings());
      if (rs.getObject(1)==null) { 
        logWarnings("getSingleHelper resultset", rs.getWarnings());
        return null; 
      }
      logWarnings("getSingleHelper resultset", rs.getWarnings());
      return rs;
    }
    
  }
  
  public interface DBInsert extends DBStatement {
    @Override
    DBInsert addParam(int pColumn, String pValue);
  
  }


  private class DBInsertImpl extends DBStatementImpl implements DBInsert {


    public DBInsertImpl(String pSQL, String pErrorMsg) throws SQLException {
      super(pSQL, pErrorMsg);
    }

    public DBInsertImpl() {
      // Dud constructor that doesn't do anything
    }

    @Override
    public DBInsert addParam(int pColumn, String pValue) {
      return (DBInsert) super.addParam(pColumn, pValue);
    }
    
  }


  private final DataSourceWrapper aDataSource;

  private DBHelper(DataSourceWrapper pDataSource, Object pKey) {
    aDataSource = pDataSource;
    aKey = pKey!=null ? pKey : new Object();
    aValid=true;
  }
  
  public static DBHelper dbHelper(String pResourceName, Object pKey) {
    if (aSourceMap==null) {
      synchronized (aShareLock) {
        if (aSourceMap==null) {
          aSourceMap = new ConcurrentHashMap<String, DataSourceWrapper>();
        }
      }
    }
    DataSourceWrapper dataSource = aSourceMap.get(pResourceName); 
    if (dataSource==null) {
      try {
        InitialContext initialContext = new InitialContext();
        dataSource = new DataSourceWrapper((DataSource) initialContext.lookup(pResourceName));
        aSourceMap.put(pResourceName, dataSource);
      } catch (NamingException e) {
        logException("Failure to register access permission in database", e);
        return new DBHelper(null, pKey); // Return an empty helper to ensure building doesn't fail stuff
      }
    }
    
    return new DBHelper(dataSource, pKey);
    
  }
  
  private static Logger getLogger() {
    return Logger.getLogger(LOGGER_NAME);
  }

  public static void logWarning(String pMsg) {
    getLogger().log(Level.WARNING, pMsg);
  }

  static void logWarnings(String pString, SQLWarning pWarnings) {
    if (pWarnings!=null) {
      getLogger().log(Level.WARNING, pString, pWarnings);
      logWarnings(pString, pWarnings.getNextWarning());
    }
  }

  public static void logException(final String pMsg, Throwable pE) {
    getLogger().log(Level.SEVERE, pMsg, pE);
  }
  
  private void checkValid() {
    if (!aValid) {
      throw new IllegalStateException("DBHelpers can not be reused");
    }
  }
  
  public DBQuery makeQuery(String pSQL) {
    return makeQuery(pSQL, null);
  }

  public DBQuery makeQuery(String pSQL, String pErrorMsg) {
    aValid=true;
    try {
      if (aDataSource!=null) {
        return new DBQueryImpl(pSQL, pErrorMsg);
      }
    } catch (SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return new DBQueryImpl();
  }

  public DBInsert makeInsert(String pSQL) {
    return makeInsert(pSQL, null);
  }

  public DBInsert makeInsert(String pSQL, String pErrorMsg) {
    aValid=true;
    try {
      if (aDataSource!=null) {
        return new DBInsertImpl(pSQL, pErrorMsg);
      }
    } catch (SQLException e) {
      logException("Failure to create prepared statement", e);
    }
    return new DBInsertImpl();
  }

  public void commit() {
    try {
      aConnection.commit();
      logWarnings("Committing database connection", aConnection.getWarnings());
    } catch (SQLException e) {
      logException("Failure to commit statement", e);
      try {
        aConnection.rollback();
        logWarnings("Rolling back database connection", aConnection.getWarnings());
      } catch (SQLException f) {
        logException("Failure to rollback after failed commit", f);
      }
    }
  }

  public void rollback() {
    try {
      aConnection.rollback();
      logWarnings("Rolling back database connection", aConnection.getWarnings());
    } catch (SQLException e) {
      logException("Failure to roll back statement", e);
    }
  }

  public void close() throws SQLException {
    if (aConnection!=null) { 
      aConnection.close();
      logWarnings("Closing database connection", aConnection.getWarnings());
    }
    else if (aDataSource!=null) {
      aConnection = aDataSource.aConnectionMap.get(aKey);
      if (aConnection!=null) { 
        aConnection.close(); 
        logWarnings("Closing database connection", aConnection.getWarnings());
      }
    }
    aConnection = null;
    if (aDataSource !=null) {
      aDataSource.aConnectionMap.remove(aKey);
    }
  }

  public static void closeConnections(Object pReference) {
    synchronized (aShareLock) {
      for(DataSourceWrapper dataSource: aSourceMap.values()) {
        Connection conn = dataSource.aConnectionMap.get(pReference);
        if (conn !=null) {
          try {
            conn.close();
            logWarnings("Closing database connection", conn.getWarnings());
          } catch (SQLException e) {
            logException("Failure to close connection", e);
          }
          dataSource.aConnectionMap.remove(pReference);
        } 
      }
    }
  }

  public static void closeAllConnections(String pDbResource) {
    ArrayList<SQLException> exceptions = null;
    synchronized (aShareLock) {
      DataSourceWrapper wrapper = aSourceMap.get(pDbResource);
      if (wrapper!=null) {
        for( Connection connection:wrapper.aConnectionMap.values()) {
          try {
            connection.close();
          } catch (SQLException e) {
            if (exceptions==null) { exceptions = new ArrayList<SQLException>(); }
            exceptions.add(e);
          }
        }
        aSourceMap.remove(pDbResource);
      }
    }
    if (exceptions!=null) {
      if (exceptions.size()==1) {
        throw new RuntimeException(exceptions.get(0));
      } else {
        throw new CompoundException(exceptions);
      }
    }
  }

}
