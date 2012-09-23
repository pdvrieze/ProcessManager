package net.devrieze.util;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.devrieze.util.DBHelper.DBStatement;


public class StringAdapter extends ResultSetAdapter<String> {

  
  public class StringAdapterIterator extends SingletonAdapterIterator<String> {

    public StringAdapterIterator(DBStatement pStatement, ResultSet pResultSet) {
      super(pStatement, pResultSet);
    }

    public StringAdapterIterator(DBStatement pStatement, ResultSet pResultSet, boolean pAutoClose) {
      super(pStatement, pResultSet, pAutoClose);
    }

    @Override
    protected String doCreateElem(ResultSet pResultSet) throws SQLException {
      final String result;
      if (getStringCache()==null) {
        result = pResultSet.getString(1);
      } else {
        result = getStringCache().lookup(pResultSet.getString(1));
      }
      DBHelper.logWarnings("Reading string out of resultset", pResultSet.getWarnings());
      return result;
    }

  }

  private boolean aAutoClose;

  public StringAdapter(DBStatement pStatement, ResultSet pResultSet) {
    this(pStatement, pResultSet, false);
  }

  public StringAdapter(DBStatement pStatement, ResultSet pResultSet, boolean pAutoClose) {
    super(pStatement, pResultSet);
    aAutoClose = pAutoClose;
  }

  @Override
  public StringAdapterIterator iterator() {
    
    return new StringAdapterIterator(aStatement, aResultSet, aAutoClose);
  }

}
