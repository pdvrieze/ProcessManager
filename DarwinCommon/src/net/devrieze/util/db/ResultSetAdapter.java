package net.devrieze.util.db;

import net.devrieze.util.AutoCloseableIterator;
import net.devrieze.util.StringCache;
import net.devrieze.util.db.DBConnection.DBStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.NoSuchElementException;


public abstract class ResultSetAdapter<T> implements DBIterable<T>/*, Iterable<T>*/ {


  public abstract static class ResultSetAdapterIterator<T> implements AutoCloseableIterator<T> {

    @Nullable
    private ResultSet mResultSet;

    @Nullable
    private DBStatement mStatement;

    private final boolean mAutoClose;

    private boolean mPeeked = false;

    private boolean mInitialized = false;

    @NotNull
    private final StringCache mStringCache;

    public ResultSetAdapterIterator(@NotNull final DBStatement statement, @NotNull final ResultSet resultSet) {
      this(statement, resultSet, false);
    }

    public ResultSetAdapterIterator(@NotNull final DBStatement statement, @NotNull final ResultSet resultSet, final boolean autoClose) {
      mResultSet = resultSet;
      mStatement = statement;
      mAutoClose = autoClose;
      mStringCache = statement.getStringCache();
    }

    private void init() {
      if (mResultSet != null) {
        try {
          mResultSet.beforeFirst();
          DBConnection.logWarnings("Resetting resultset for AdapterIterator", mResultSet);
          final ResultSetMetaData metadata = mResultSet.getMetaData();
          DBConnection.logWarnings("Getting resultset metadata for AdapterIterator", mResultSet);
          for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            doRegisterColumn(i, metadata.getColumnName(i));
          }
          mInitialized = true;
        } catch (final SQLException e) {
          DBConnection.logException("Initializing resultset iterator", e);
          closeStatement();
          throw new RuntimeException(e);
        }
      }
    }

    /**
     * Register the given column at the given location. Implementations override this method to be able to store this
     * column information for later object creation.
     * @param i The index of the column.
     * @param columnName The name of that column.
     */
    protected abstract void doRegisterColumn(int i, @SuppressWarnings("UnusedParameters") @NotNull String columnName);

    abstract protected T doCreateElem(@NotNull ResultSet resultSet) throws SQLException;

    @Override
    public final boolean hasNext() {
      if (!mInitialized) {
        init();
      }
      final ResultSet resultSet = this.mResultSet;
      if (resultSet == null) {
        return false;
      }
      try {
        mPeeked = resultSet.next();
        DBConnection.logWarnings("Getting a peek at next row in resultset", resultSet);
        if (mAutoClose && !mPeeked) {
          closeStatement();
        }
        return mPeeked;
      } catch (final SQLException e) {
        DBConnection.logException("Initializing resultset iterator", e);
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @SuppressWarnings("IteratorNextCanNotThrowNoSuchElementException")
    @Override
    @Nullable
    public final T next() throws NoSuchElementException {
      final ResultSet resultSet = this.mResultSet;
      if (resultSet == null) {
        throw new IllegalStateException("Trying to access a null resultset");
      }
      if (!mInitialized) {
        init();
      }
      try {
        if (!mPeeked) {
          if (!resultSet.next()) {
            closeStatement();
            DBConnection.logWarnings("Getting the next resultset in ResultSetAdapter", resultSet);
            throw new NoSuchElementException("Trying to go beyond the last element");
          }
          DBConnection.logWarnings("Getting the next resultset in ResultSetAdapter", resultSet);
        }
        mPeeked = false;

        return doCreateElem(resultSet);

      } catch (final SQLException e) {
        closeStatement();
        throw new RuntimeException(e);
      }
    }

    @Override
    public final void remove() {
      final ResultSet resultSet = this.mResultSet;
      if ((!mInitialized) || (resultSet==null)) {
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
      if (mResultSet != null) {
        try {
          DBConnection.logWarnings("Closing resultset in ResultSetAdapter", mResultSet);
          mResultSet.close();
        } catch (final SQLException e) {
          DBConnection.logException("Error closing resultset", e);
        }
        mResultSet = null;
      }
    }

    public void closeStatement() {
      final ResultSet resultSet = this.mResultSet;
      //noinspection Duplicates
      if (resultSet != null) {
        try {
          try {
            resultSet.close();
          } finally {
            if (mStatement != null) {
              mStatement.close();
            }
          }
        } catch (final SQLException e) {
          DBConnection.logException("Error closing owning iterator for resultset", e);
        }
        mResultSet = null;
        mStatement = null;
      }
    }

    @NotNull
    protected StringCache getStringCache() {
      return mStringCache;
    }

  }

  public abstract static class SingletonAdapterIterator<T> extends ResultSetAdapterIterator<T> {

    public SingletonAdapterIterator(final DBStatement statement, final ResultSet resultSet) {
      super(statement, resultSet);
    }

    public SingletonAdapterIterator(final DBStatement statement, final ResultSet resultSet, final boolean autoClose) {
      super(statement, resultSet, autoClose);
    }

    @Override
    protected void doRegisterColumn(final int index, @NotNull final String columnName) {
      if (index != 1) {
        throw new IllegalArgumentException("Singleton adapters can not be created for result sets with more than one columns");
      }
    }

  }

  @Override
  public void close() {
    closeStatement();
  }

  public void closeStatement() {
    final ResultSet resultSet = this.mResultSet;
    //noinspection Duplicates
    if (resultSet != null) {
      try {
        try {
          resultSet.close();
        } finally {
          if (mStatement != null) {
            mStatement.close();
          }
        }
      } catch (final SQLException e) {
        DBConnection.logException("Error closing owning iterator for resultset", e);
      }
      mResultSet = null;
      mStatement = null;
    }
  }

  public void closeAll() {
    final DBStatement statement = mStatement; //needed as closeStatement nulls this
    if (statement!=null) {
      statement.closeHelper();
    }
    closeStatement();
  }

  @Nullable
  protected ResultSet mResultSet;

  @Nullable
  protected DBStatement mStatement;

  @SuppressWarnings("NullableProblems")
  protected ResultSetAdapter(@NotNull final DBStatement statement, final ResultSet resultSet) {
    mResultSet = resultSet;
    mStatement = statement;
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