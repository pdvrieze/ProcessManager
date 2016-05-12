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

package nl.adaptivity.process.engine;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.StringCache;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl.Factory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlStreaming;

import java.io.IOException;
import java.io.Reader;
import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class ProcessModelMap extends CachingDBHandleMap<ProcessModelImpl> implements IProcessModelMap<DBTransaction> {


  private static final String TABLE = "processmodels";

  private static final String COL_HANDLE = "pmhandle";

  private static final String COL_OWNER = "owner";

  private static final String COL_MODEL = "model";

  static class ProcessModelFactory extends AbstractElementFactory<ProcessModelImpl> {

    private static boolean _supports_set_character_stream = true;
    private int mColNoOwner;
    private int mColNoModel;
    private int mColNoHandle;
    private final StringCache mStringCache;

    ProcessModelFactory(StringCache stringCache) {
      mStringCache = stringCache;
    }

    @Override
    public CharSequence getTableName() {
      return TABLE;
    }

    @Override
    public String getFilterExpression() {
      // TODO implement more carefully
      return null;
//      if (mFilterUser==null) {
//        return null;
//      } else {
//
//      }
    }

    @Override
    public int setFilterParams(PreparedStatement statement, int offset) {
      // No where clauses are used.
      return 0;
    }

    @Override
    public void initResultSet(ResultSetMetaData metaData) throws SQLException {
      final int columnCount = metaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = metaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          mColNoHandle = i;
        } else if (COL_OWNER.equals(colName)) {
          mColNoOwner = i;
        } else if (COL_MODEL.equals(colName)) {
          mColNoModel = i;
        } // ignore other columns
      }
    }

    @Override
    public ProcessModelImpl create(DBTransaction connection, ResultSet row) throws SQLException {
      Principal owner = new SimplePrincipal(mStringCache.lookup(row.getString(mColNoOwner)));
      try(Reader modelReader = row.getCharacterStream(mColNoModel)) {
        long handle = row.getLong(mColNoHandle);

        ProcessModelImpl result = ProcessModelImpl.deserialize(new Factory(), XmlStreaming.newReader(modelReader));

        result.setHandle(handle);
        result.cacheStrings(mStringCache);
        if (result.getOwner()==null) {
          result.setOwner(owner);
        }
        return result;
      } catch (IOException e) {
        throw new SQLException(e);
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessModelImpl object) {
      return getHandleCondition(object);
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement statement, ProcessModelImpl element, int offset) throws SQLException {
      return setHandleParams(statement, element, offset);
    }

    @Override
    public ProcessModelImpl asInstance(Object object) {
      if (object instanceof ProcessModelImpl) {
        return (ProcessModelImpl) object;
      } else {
        return null;
      }
    }

    @Override
    public CharSequence getCreateColumns() {
      return COL_HANDLE+", "+COL_OWNER + ", " + COL_MODEL;
    }

    @Override
    public List<CharSequence> getStoreParamHolders() {
      return Arrays.<CharSequence>asList("?","?");
    }

    @Override
    public List<CharSequence> getStoreColumns() {
      return Arrays.<CharSequence>asList(COL_OWNER,COL_MODEL);
    }

    @Override
    public int setStoreParams(PreparedStatement statement, ProcessModelImpl element, int offset) throws SQLException {
      statement.setString(offset, element.getOwner().getName());

      if (_supports_set_character_stream) {
        try {
          statement.setCharacterStream(offset + 1, nl.adaptivity.xml.XmlUtil.toReader(element));
          return 2;
        } catch (AbstractMethodError|UnsupportedOperationException e) {
          _supports_set_character_stream =false;
        } catch (XmlException e) {
          throw new RuntimeException(e);
        }
      }
      statement.setString(offset + 1, nl.adaptivity.xml.XmlUtil.toString(element));
      return 2;
    }

    @Override
    public CharSequence getHandleCondition(Handle<? extends ProcessModelImpl> handle) {
      return COL_HANDLE + " = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement statement, Handle<? extends ProcessModelImpl> handle, int offset) throws SQLException {
      statement.setLong(offset, handle.getHandle());
      return 1;
    }

    @Override
    public void preRemove(DBTransaction connection, ResultSet elementSource) throws SQLException {
      // Ignore. Don't even use the default implementation
    }

  }

  @Override
  public ProcessModelImpl getModelWithUuid(final DBTransaction transaction, final UUID uuid) throws SQLException {
    List<Long> candidates = new ArrayList<>();
    try(PreparedStatement statement = transaction.prepareStatement("SELECT "+COL_HANDLE+" FROM "+TABLE+" WHERE "+COL_MODEL+" LIKE ?")){
      statement.setString(1, '%'+uuid.toString()+'%');
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          candidates.add(rs.getLong(1));
        }
      }
    }
    for(long candidateHandle: candidates) {
      ProcessModelImpl candidate = get(transaction, candidateHandle);
      if (uuid.equals(candidate.getUuid())) {
        return candidate;
      }
    }
    return null;
  }

  public ProcessModelMap(TransactionFactory transactionFactory, StringCache stringCache) {
    super(transactionFactory, new ProcessModelFactory(stringCache));
  }

}
