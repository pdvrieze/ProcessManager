package net.devrieze.util.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.devrieze.util.db.DBConnection.DBStatement;


public class StringAdapter extends ResultSetAdapter<String> {


  public class StringAdapterIterator extends SingletonAdapterIterator<String> {

    public StringAdapterIterator(final DBStatement statement, final ResultSet resultSet) {
      super(statement, resultSet);
    }

    public StringAdapterIterator(final DBStatement statement, final ResultSet resultSet, final boolean autoClose) {
      super(statement, resultSet, autoClose);
    }

    @Override
    @Nullable
    protected String doCreateElem(final ResultSet resultSet) throws SQLException {
      final String result = getStringCache().lookup(resultSet.getString(1));
      DBConnection.logWarnings("Reading string out of resultset", resultSet);
      return result;
    }

  }

  private final boolean mAutoClose;

  public StringAdapter(final DBStatement statement, final ResultSet resultSet) {
    this(statement, resultSet, false);
  }

  public StringAdapter(final DBStatement statement, final ResultSet resultSet, final boolean autoClose) {
    super(statement, resultSet);
    mAutoClose = autoClose;
  }

  @Override
  @NotNull
  public StringAdapterIterator iterator() {

    return new StringAdapterIterator(mStatement, mResultSet, mAutoClose);
  }

}
