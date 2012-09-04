package net.devrieze.util;

import java.sql.*;
import java.util.Iterator;

import net.devrieze.util.DBHelper.DBStatement;


public abstract class ResultSetAdapter<T> implements Iterable<T> {


  public abstract static class ResultSetAdapterIterator<T> implements Iterator<T> {

    private ResultSet aResultSet;
    private DBStatement aStatement;
    private final boolean aAutoClose;

    private boolean aPeeked = false;
    
    private boolean aInitialized = false;

    public ResultSetAdapterIterator(DBStatement pStatement, ResultSet pResultSet) {
      this(pStatement, pResultSet, false);
    }
    public ResultSetAdapterIterator(DBStatement pStatement, ResultSet pResultSet, boolean pAutoClose) {
      aResultSet = pResultSet;
      aStatement = pStatement;
      aAutoClose = pAutoClose;
    }

    private void init() {
      if (aResultSet!=null) {
        try {
          aResultSet.beforeFirst();
          DBHelper.logWarnings("Resetting resultset for AdapterIterator", aResultSet.getWarnings());
          ResultSetMetaData metadata = aResultSet.getMetaData();
          DBHelper.logWarnings("Getting resultset metadata for AdapterIterator", aResultSet.getWarnings());
          for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            doRegisterColumn(i, metadata.getColumnName(i));
          }
          aInitialized = true;
        } catch (SQLException e) {
          DBHelper.logException("Initializing resultset iterator", e);
          closeStatement();
          throw new RuntimeException(e);
        }
      }
    }

    protected abstract void doRegisterColumn(int pI, String pColumnName);

    abstract protected T doCreateElem(ResultSet pResultSet) throws SQLException;

    @Override
    public final boolean hasNext() {
      if (! aInitialized ) { init(); }
      if (aResultSet==null) { return false; }
      try {
        aPeeked = aResultSet.next();
        DBHelper.logWarnings("Getting a peek at next row in resultset", aResultSet.getWarnings());
        if (aAutoClose && ! aPeeked) { closeStatement(); }
        return aPeeked;
      } catch (SQLException e) {
        DBHelper.logException("Initializing resultset iterator", e);
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @Override
    public final T next() {
      if (aResultSet==null) { throw new IllegalStateException("Trying to access a null resultset"); }
      if (! aInitialized ) { init(); }
      try {
        if (!aPeeked) {
          if (!aResultSet.next()) {
            closeStatement();
            DBHelper.logWarnings("Getting the next resultset in ResultSetAdapter", aResultSet.getWarnings());
            throw new IllegalStateException("Trying to go beyond the last element");
          }
          DBHelper.logWarnings("Getting the next resultset in ResultSetAdapter", aResultSet.getWarnings());
        }
        aPeeked = false;

        return doCreateElem(aResultSet);

      } catch (SQLException e) {
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @Override
    public final void remove() {
      if (! aInitialized ) {throw new IllegalStateException("Trying to remove an element before reading the iterator");}
      try {
        aResultSet.deleteRow();
        DBHelper.logWarnings("Deleting a row in ResultSetAdapter", aResultSet.getWarnings());
      } catch (SQLFeatureNotSupportedException e) {
        throw new UnsupportedOperationException(e);
      } catch (SQLException e) {
        closeStatement();
        throw new RuntimeException(e);
      }
    }
    
    public void close() {
      if (aResultSet!=null) {
        try {
          aResultSet.close();
          DBHelper.logWarnings("Closing resultset in ResultSetAdapter", aResultSet.getWarnings());
        } catch (SQLException e) {
          DBHelper.logException("Error closing resultset", e);
        }
        aResultSet = null;
      }
    }
    
    public void closeStatement() {
      if (aResultSet!=null) {
        try {
          try {
            aResultSet.close();
          } finally {
            if (aStatement!=null) {
              aStatement.close();
            }
          }
        } catch (SQLException e) {
          DBHelper.logException("Error closing owning iterator for resultset", e);
        }
        aResultSet = null;
        aStatement = null;
      }
    }
    
  }

  public abstract static class SingletonAdapterIterator<T> extends ResultSetAdapterIterator<T> {

    public SingletonAdapterIterator(DBStatement pStatement, ResultSet pResultSet) {
      super(pStatement, pResultSet);
    }

    public SingletonAdapterIterator(DBStatement pStatement, ResultSet pResultSet, boolean pAutoClose) {
      super(pStatement, pResultSet, pAutoClose);
    }

    @Override
    protected void doRegisterColumn(int pIndex, String pColumnName) {
      if (pIndex!=1) {
        throw new IllegalArgumentException("Singleton adapters can not be created for result sets with more than one columns");
      }
    }
    
  }

  public void close() {
    if (aResultSet!=null) {
      try {
        aResultSet.close();
        DBHelper.logWarnings("Closing resultset in ResultSetAdapter", aResultSet.getWarnings());
      } catch (SQLException e) {
        DBHelper.logException("Error closing resultset", e);
      }
      aResultSet=null;
    }
  }
  
  public void closeStatement() {
    if (aResultSet!=null) {
      try {
        try {
          aResultSet.close();
        } finally {
          if (aStatement!=null) {
            aStatement.close();
          }
        }
      } catch (SQLException e) {
        DBHelper.logException("Error closing owning iterator for resultset", e);
      }
      aResultSet = null;
      aStatement = null;
    }
  }
  
  protected ResultSet aResultSet;
  
  protected DBStatement aStatement;

  protected ResultSetAdapter(DBStatement pStatement, ResultSet pResultSet) {
    aResultSet = pResultSet;
    aStatement = pStatement;
  }

  @Override
  public abstract ResultSetAdapterIterator<T> iterator();
}