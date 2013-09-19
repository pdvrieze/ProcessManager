package net.devrieze.util.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.devrieze.annotations.NotNull;
import net.devrieze.annotations.Nullable;
import net.devrieze.util.db.DBHelper.DBStatement;


public class StringAdapter extends ResultSetAdapter<String> {


  public class StringAdapterIterator extends SingletonAdapterIterator<String> {

    public StringAdapterIterator(final DBStatement pStatement, final ResultSet pResultSet) {
      super(pStatement, pResultSet);
    }

    public StringAdapterIterator(final DBStatement pStatement, final ResultSet pResultSet, final boolean pAutoClose) {
      super(pStatement, pResultSet, pAutoClose);
    }

    @Override
    @Nullable
    protected String doCreateElem(final ResultSet pResultSet) throws SQLException {
      final String result = getStringCache().lookup(pResultSet.getString(1));
      DBHelper.logWarnings("Reading string out of resultset", pResultSet.getWarnings());
      return result;
    }

  }

  private final boolean aAutoClose;

  public StringAdapter(final DBStatement pStatement, final ResultSet pResultSet) {
    this(pStatement, pResultSet, false);
  }

  public StringAdapter(final DBStatement pStatement, final ResultSet pResultSet, final boolean pAutoClose) {
    super(pStatement, pResultSet);
    aAutoClose = pAutoClose;
  }

  @Override
  @NotNull
  public StringAdapterIterator iterator() {

    return new StringAdapterIterator(aStatement, aResultSet, aAutoClose);
  }

}
