/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.util.db;

import net.devrieze.util.db.DBConnection.DBStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;


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
    protected String doCreateElem(@NotNull final ResultSet resultSet) throws SQLException {
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
