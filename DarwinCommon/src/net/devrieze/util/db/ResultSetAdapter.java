package net.devrieze.util.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;

import net.devrieze.util.StringCache;
import net.devrieze.util.db.DBHelper.DBStatement;


public abstract class ResultSetAdapter<T> implements DBIterable<T>/*, Iterable<T>*/ {


  public abstract static class ResultSetAdapterIterator<T> implements Iterator<T> {

    private ResultSet aResultSet;

    private DBStatement aStatement;

    private final boolean aAutoClose;

    private boolean aPeeked = false;

    private boolean aInitialized = false;

    private StringCache aStringCache;

    public ResultSetAdapterIterator(final DBStatement pStatement, final ResultSet pResultSet) {
      this(pStatement, pResultSet, false);
    }

    public ResultSetAdapterIterator(final DBStatement pStatement, final ResultSet pResultSet, final boolean pAutoClose) {
      aResultSet = pResultSet;
      aStatement = pStatement;
      aAutoClose = pAutoClose;
      aStringCache = pStatement.getStringCache();
    }

    private void init() {
      if (aResultSet != null) {
        try {
          aResultSet.beforeFirst();
          DBHelper.logWarnings("Resetting resultset for AdapterIterator", aResultSet.getWarnings());
          final ResultSetMetaData metadata = aResultSet.getMetaData();
          DBHelper.logWarnings("Getting resultset metadata for AdapterIterator", aResultSet.getWarnings());
          for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            doRegisterColumn(i, metadata.getColumnName(i));
          }
          aInitialized = true;
        } catch (final SQLException e) {
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
      if (!aInitialized) {
        init();
      }
      if (aResultSet == null) {
        return false;
      }
      try {
        aPeeked = aResultSet.next();
        DBHelper.logWarnings("Getting a peek at next row in resultset", aResultSet.getWarnings());
        if (aAutoClose && !aPeeked) {
          closeStatement();
        }
        return aPeeked;
      } catch (final SQLException e) {
        DBHelper.logException("Initializing resultset iterator", e);
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @Override
    public final T next() {
      if (aResultSet == null) {
        throw new IllegalStateException("Trying to access a null resultset");
      }
      if (!aInitialized) {
        init();
      }
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

      } catch (final SQLException e) {
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @Override
    public final void remove() {
      if (!aInitialized) {
        throw new IllegalStateException("Trying to remove an element before reading the iterator");
      }
      try {
        aResultSet.deleteRow();
        DBHelper.logWarnings("Deleting a row in ResultSetAdapter", aResultSet.getWarnings());
      } catch (final SQLFeatureNotSupportedException e) {
        throw new UnsupportedOperationException(e);
      } catch (final SQLException e) {
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    public void close() {
      if (aResultSet != null) {
        try {
          aResultSet.close();
          DBHelper.logWarnings("Closing resultset in ResultSetAdapter", aResultSet.getWarnings());
        } catch (final SQLException e) {
          DBHelper.logException("Error closing resultset", e);
        }
        aResultSet = null;
      }
    }

    public void closeStatement() {
      if (aResultSet != null) {
        try {
          try {
            aResultSet.close();
          } finally {
            if (aStatement != null) {
              aStatement.close();
            }
          }
        } catch (final SQLException e) {
          DBHelper.logException("Error closing owning iterator for resultset", e);
        }
        aResultSet = null;
        aStatement = null;
      }
    }

    protected StringCache getStringCache() {
      return aStringCache;
    }

  }

  public abstract static class SingletonAdapterIterator<T> extends ResultSetAdapterIterator<T> {

    public SingletonAdapterIterator(final DBStatement pStatement, final ResultSet pResultSet) {
      super(pStatement, pResultSet);
    }

    public SingletonAdapterIterator(final DBStatement pStatement, final ResultSet pResultSet, final boolean pAutoClose) {
      super(pStatement, pResultSet, pAutoClose);
    }

    @Override
    protected void doRegisterColumn(final int pIndex, final String pColumnName) {
      if (pIndex != 1) {
        throw new IllegalArgumentException("Singleton adapters can not be created for result sets with more than one columns");
      }
    }

  }

  public void close() {
//    if (aResultSet != null) {
//      try {
//        aResultSet.close();
//        DBHelper.logWarnings("Closing resultset in ResultSetAdapter", aResultSet.getWarnings());
//      } catch (final SQLException e) {
//        DBHelper.logException("Error closing resultset", e);
//      }
//      aResultSet = null;
//    }
    closeStatement();
  }

  public void closeStatement() {
    if (aResultSet != null) {
      try {
        try {
          aResultSet.close();
        } finally {
          if (aStatement != null) {
            aStatement.close();
          }
        }
      } catch (final SQLException e) {
        DBHelper.logException("Error closing owning iterator for resultset", e);
      }
      aResultSet = null;
      aStatement = null;
    }
  }
  
  public void closeAll() {
    DBStatement statement = aStatement; //needed as closeStatement nulls this
    closeStatement();
    statement.closeHelper();
  }

  protected ResultSet aResultSet;

  protected DBStatement aStatement;

  protected ResultSetAdapter(final DBStatement pStatement, final ResultSet pResultSet) {
    aResultSet = pResultSet;
    aStatement = pStatement;
  }

  @Override
  public abstract ResultSetAdapterIterator<T> iterator();
  
  public Iterable<T> all() {
    return new Iterable<T>() {

      @Override
      public Iterator<T> iterator() {
        return ResultSetAdapter.this.iterator();
      }};
  }
}