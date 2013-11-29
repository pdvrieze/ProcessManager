package net.devrieze.util.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;

import static net.devrieze.util.Annotations.*;
import net.devrieze.annotations.NotNull;
import net.devrieze.annotations.Nullable;
import net.devrieze.util.AutoCloseableIterator;
import net.devrieze.util.StringCache;
import net.devrieze.util.db.DBConnection.DBStatement;


public abstract class ResultSetAdapter<T> implements DBIterable<T>/*, Iterable<T>*/ {


  public abstract static class ResultSetAdapterIterator<T> implements AutoCloseableIterator<T> {

    @Nullable
    private ResultSet aResultSet;

    @Nullable
    private DBStatement aStatement;

    private final boolean aAutoClose;

    private boolean aPeeked = false;

    private boolean aInitialized = false;

    @NotNull
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
          DBConnection.logWarnings("Resetting resultset for AdapterIterator", aResultSet);
          final ResultSetMetaData metadata = aResultSet.getMetaData();
          DBConnection.logWarnings("Getting resultset metadata for AdapterIterator", aResultSet);
          for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            doRegisterColumn(i, notNull(metadata.getColumnName(i)));
          }
          aInitialized = true;
        } catch (final SQLException e) {
          DBConnection.logException("Initializing resultset iterator", e);
          closeStatement();
          throw new RuntimeException(e);
        }
      }
    }

    protected abstract void doRegisterColumn(int pI, @NotNull String pColumnName);

    abstract protected T doCreateElem(@NotNull ResultSet pResultSet) throws SQLException;

    @Override
    public final boolean hasNext() {
      if (!aInitialized) {
        init();
      }
      final ResultSet resultSet = this.aResultSet;
      if (resultSet == null) {
        return false;
      }
      try {
        aPeeked = resultSet.next();
        DBConnection.logWarnings("Getting a peek at next row in resultset", resultSet);
        if (aAutoClose && !aPeeked) {
          closeStatement();
        }
        return aPeeked;
      } catch (final SQLException e) {
        DBConnection.logException("Initializing resultset iterator", e);
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @Override
    @Nullable
    public final T next() {
      final ResultSet resultSet = this.aResultSet;
      if (resultSet == null) {
        throw new IllegalStateException("Trying to access a null resultset");
      }
      if (!aInitialized) {
        init();
      }
      try {
        if (!aPeeked) {
          if (!resultSet.next()) {
            closeStatement();
            DBConnection.logWarnings("Getting the next resultset in ResultSetAdapter", resultSet);
            throw new IllegalStateException("Trying to go beyond the last element");
          }
          DBConnection.logWarnings("Getting the next resultset in ResultSetAdapter", resultSet);
        }
        aPeeked = false;

        return doCreateElem(resultSet);

      } catch (final SQLException e) {
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @Override
    public final void remove() {
      final ResultSet resultSet = this.aResultSet;
      if ((!aInitialized) || (resultSet==null)) {
        throw new IllegalStateException("Trying to remove an element before reading the iterator");
      }
      try {
        resultSet.deleteRow();
        DBConnection.logWarnings("Deleting a row in ResultSetAdapter", resultSet);
      } catch (final SQLFeatureNotSupportedException e) {
        throw new UnsupportedOperationException(e);
      } catch (final SQLException e) {
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @Override
    public void close() {
      if (aResultSet != null) {
        try {
          DBConnection.logWarnings("Closing resultset in ResultSetAdapter", aResultSet);
          aResultSet.close();
        } catch (final SQLException e) {
          DBConnection.logException("Error closing resultset", e);
        }
        aResultSet = null;
      }
    }

    public void closeStatement() {
      final ResultSet resultSet = this.aResultSet;
      if (resultSet != null) {
        try {
          try {
            resultSet.close();
          } finally {
            if (aStatement != null) {
              aStatement.close();
            }
          }
        } catch (final SQLException e) {
          DBConnection.logException("Error closing owning iterator for resultset", e);
        }
        aResultSet = null;
        aStatement = null;
      }
    }

    @NotNull
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
    protected void doRegisterColumn(final int pIndex, @NotNull final String pColumnName) {
      if (pIndex != 1) {
        throw new IllegalArgumentException("Singleton adapters can not be created for result sets with more than one columns");
      }
    }

  }

  @Override
  public void close() {
    closeStatement();
  }

  public void closeStatement() {
    final ResultSet resultSet = this.aResultSet;
    if (resultSet != null) {
      try {
        try {
          resultSet.close();
        } finally {
          if (aStatement != null) {
            aStatement.close();
          }
        }
      } catch (final SQLException e) {
        DBConnection.logException("Error closing owning iterator for resultset", e);
      }
      aResultSet = null;
      aStatement = null;
    }
  }

  public void closeAll() {
    final DBStatement statement = aStatement; //needed as closeStatement nulls this
    if (statement!=null) {
      statement.closeHelper();
    }
    closeStatement();
  }

  @Nullable
  protected ResultSet aResultSet;

  @Nullable
  protected DBStatement aStatement;

  protected ResultSetAdapter(final DBStatement pStatement, final ResultSet pResultSet) {
    aResultSet = pResultSet;
    aStatement = pStatement;
  }

  @Override
  @NotNull
  public abstract ResultSetAdapterIterator<T> iterator();

  @Override
  @NotNull
  public Iterable<T> all() {
    return new Iterable<T>() {

      @Override
      public Iterator<T> iterator() {
        return ResultSetAdapter.this.iterator();
      }};
  }
}