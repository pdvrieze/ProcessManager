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

import net.devrieze.util.Handles;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBHandleMap;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.engine.ProcessInstance.State;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstanceMap;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.util.xml.CompactFragment;

import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class ProcessInstanceMap extends DBHandleMap<ProcessInstance<DBTransaction>> {
/*
  private static class NodeInstanceData implements Comparable<NodeInstanceData> {
    long handle;
    long[] predecessors;

    public NodeInstanceData(final long pHandle, final long[] pPredecessors) {
      handle = pHandle;
      predecessors = pPredecessors;
    }

    public NodeInstanceData(final long pHandle, final long pPredecessor) {
      handle = pHandle;
      predecessors = new long[] {pPredecessor};
    }

    public NodeInstanceData(final long pNodeHandle, final List<Long> pPredecessors) {
      handle = pNodeHandle;
      predecessors = new long[pPredecessors.size()];
      for(int i=predecessors.length-1; i>=0; --i) {
        predecessors[i] = pPredecessors.get(i).longValue();
      }
    }

    @Override
    public int compareTo(final NodeInstanceData o) {
      return 0;
    }
  }
*/

  public static final String TABLE_INSTANCES = "processinstances";
  public static final String TABLE_INSTANCEDATA = "instancedata";
  public static final String COL_HANDLE = "pihandle";
  public static final String COL_OWNER = "owner";
  public static final String COL_NAME = "name";
  public static final String COL_STATE = "state";
  public static final String COL_UUID = "uuid";
  public static final String COL_HPROCESSMODEL = "pmhandle";
  public static final String COL_DATA = "data";
  public static final String COL_ISOUTPUT = "isoutput";

  static class ProcessInstanceElementFactory extends AbstractElementFactory<ProcessInstance<DBTransaction>> {

    private static final String QUERY_DATA = "SELECT `"+COL_NAME+"`, `"+COL_DATA+"`, `"+COL_ISOUTPUT+"` FROM `"+TABLE_INSTANCEDATA+"` WHERE `"+COL_HANDLE+"` = ?;";

    private static final String QUERY_GET_NODEINSTHANDLES_FROM_PROCINSTANCE = "SELECT "+ProcessNodeInstanceMap.COL_HANDLE+
    " FROM "+ProcessNodeInstanceMap.TABLE+
    " WHERE "+ProcessNodeInstanceMap.COL_HPROCESSINSTANCE +" = ? ;";

    public static final List<CharSequence> STORE_COLUMNS = Arrays.<CharSequence>asList(COL_HPROCESSMODEL, COL_NAME, COL_OWNER, COL_STATE, COL_UUID);
    public static final List<CharSequence> STORE_PARAMS = Arrays.<CharSequence>asList("?", "?", "?", "?", "?");
    //    AND "+
//    ProcessNodeInstanceMap.COL_HANDLE+" NOT IN ( SELECT "+ProcessNodeInstanceMap.COL_PREDECESSOR+" FROM "+ProcessNodeInstanceMap.TABLE_PREDECESSORS+" );";
    private int mColNoHandle;
    private int mColNoOwner;
    private int mColNoHProcessModel;
    private int mColNoName;
    private final ProcessEngine mProcessEngine;
    private int mColNoState;
    private int mColNoUuid;

    public ProcessInstanceElementFactory(ProcessEngine processEngine) {
      mProcessEngine = processEngine;
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
        } else if (COL_HPROCESSMODEL.equals(colName)) {
          mColNoHProcessModel = i;
        } else if (COL_NAME.equals(colName)) {
          mColNoName = i;
        } else if (COL_STATE.equals(colName)) {
          mColNoState = i;
        } else if (COL_UUID.equals(colName)) {
          mColNoUuid = i;
        } // ignore other columns
      }
    }

    @Override
    public CharSequence getHandleCondition(Handle<? extends ProcessInstance<DBTransaction>> element) {
      return COL_HANDLE+" = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement statement, Handle<? extends ProcessInstance<DBTransaction>> handle, int offset) throws SQLException {
      statement.setLong(offset, handle.getHandle());
      return 1;
    }

    @Override
    public CharSequence getTableName() {
      return TABLE_INSTANCES;
    }

    @Override
    public int setFilterParams(PreparedStatement statement, int offset) throws SQLException {
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ProcessInstance create(DBTransaction transaction, ResultSet row) throws SQLException {
      Principal owner = new SimplePrincipal(row.getString(mColNoOwner));
      Handle<ProcessModelImpl> hProcessModel = Handles.handle(row.getLong(mColNoHProcessModel));
      ProcessModelImpl processModel = mProcessEngine.getProcessModel(transaction, hProcessModel, SecurityProvider.SYSTEMPRINCIPAL);
      String instancename = row.getString(mColNoName);
      long piHandle = row.getLong(mColNoHandle);
      ProcessInstance.State state = toState(row.getString(mColNoState));
      UUID uuid = toUUID(row.getString(mColNoUuid));

      final ProcessInstance result = new ProcessInstance(piHandle, owner, processModel, instancename, uuid, state, mProcessEngine);
      return result;
    }

    private UUID toUUID(String string) {
      if (string==null) { return null; }
      return UUID.fromString(string);
    }

    private static State toState(String string) {
      if (string==null) { return null; }
      return State.valueOf(string);
    }

    @Override
    public void postCreate(DBTransaction connection, ProcessInstance element) throws SQLException {
      {
        try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_NODEINSTHANDLES_FROM_PROCINSTANCE)) {
          statement.setLong(1, element.getHandle());

          List<Handle<ProcessNodeInstance>> handles = new ArrayList<>();
          if (statement.execute()) {
            try (ResultSet resultset = statement.getResultSet()){
              while (resultset.next()) {
                handles.add(Handles.<ProcessNodeInstance>handle(resultset.getLong(1)));
              }
            }
          }
          element.setChildren(connection, handles);
        }
      }
      {
        try (PreparedStatement statement = connection.prepareStatement(QUERY_DATA)) {
          statement.setLong(1, element.getHandle());

          List<ProcessData> inputs = new ArrayList<>();
          List<ProcessData> outputs = new ArrayList<>();

          if (statement.execute()) {
            try (ResultSet resultset = statement.getResultSet()){
              while (resultset.next()) {
                ProcessData data = new ProcessData(resultset.getString(1), new CompactFragment(resultset.getString(2)));
                if (resultset.getBoolean(3)) {
                  outputs.add(data);
                } else {
                  inputs.add(data);
                }
              }
            }
          }
          element.setInputs(inputs);
          element.setOutputs(outputs);
        }
      }
      element.reinitialize(connection);
    }

    @Override
    public void preRemove(DBTransaction connection, Handle<? extends ProcessInstance<DBTransaction>> handle) throws SQLException {
      try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+TABLE_INSTANCEDATA+"` WHERE `"+COL_HANDLE+"` = ?;")) {
        statement.setLong(1, handle.getHandle());
        statement.executeUpdate();
      }
      try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+ProcessNodeInstanceMap.TABLE_PREDECESSORS+"` where `"+ ProcessNodeInstanceMap.COL_HANDLE+
                                                                              "` in (SELECT `"+ProcessNodeInstanceMap.COL_HANDLE+"` FROM `"+ ProcessNodeInstanceMap.TABLE+"` WHERE `"+ ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+"` = ?);")) {
        statement.setLong(1, handle.getHandle());
        statement.executeUpdate();
      }
      try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+ProcessNodeInstanceMap.TABLE_NODEDATA+"` where `"+ ProcessNodeInstanceMap.COL_HANDLE+
                                                                              "` in (SELECT `"+ProcessNodeInstanceMap.COL_HANDLE+"` FROM `"+ ProcessNodeInstanceMap.TABLE+"` WHERE `"+ ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+"` = ?);")) {
        statement.setLong(1, handle.getHandle());
        statement.executeUpdate();
      }

      try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+ProcessNodeInstanceMap.TABLE+"` where `"+ ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+"` = ?;")) {
        statement.setLong(1, handle.getHandle());
        statement.executeUpdate();
      }
    }

    @Override
    public void preRemove(DBTransaction connection, ProcessInstance<DBTransaction> element) throws SQLException {
      preRemove(connection, element);
    }

    @Override
    public void preRemove(DBTransaction connection, ResultSet elementSource) throws SQLException {
      preRemove(connection, Handles.<ProcessInstance<DBTransaction>>handle(elementSource.getLong(mColNoHandle)));
    }

    @Override
    public void preClear(DBTransaction connection) throws SQLException {
      CharSequence filter = getFilterExpression();
      {
        final String sql;
        if (filter==null || filter.length()==0) {
          sql= "DELETE FROM `"+TABLE_INSTANCEDATA+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE_INSTANCES+"`);";
        } else {
          sql= "DELETE FROM `"+TABLE_INSTANCEDATA+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE_INSTANCES+"` WHERE "+filter+");";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
          setFilterParams(statement, 1);
          statement.executeUpdate();
        }
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessInstance<DBTransaction> object) {
      return getHandleCondition(object);
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement statement, ProcessInstance<DBTransaction> object, int offset) throws SQLException {
      return setHandleParams(statement, object, offset);
    }

    @Override
    public ProcessInstance asInstance(Object o) {
      if (o instanceof ProcessInstance) {
        return (ProcessInstance) o;
      }
      return null;
    }

    @Override
    public CharSequence getCreateColumns() {
      return COL_HANDLE+", "+COL_HPROCESSMODEL+", "+COL_NAME+", "+COL_OWNER+", "+COL_STATE+", "+COL_UUID;
    }

    @Override
    public List<CharSequence> getStoreColumns() {
      return STORE_COLUMNS;
    }

    @Override
    public List<CharSequence> getStoreParamHolders() {
      return STORE_PARAMS;
    }

    @Override
    public int setStoreParams(PreparedStatement statement, ProcessInstance element, int offset) throws SQLException {
      statement.setLong(offset, element.getProcessModel().getHandle());
      statement.setString(offset+1, element.getName());
      statement.setString(offset+2, element.getOwner().getName());
      statement.setString(offset+3, element.getState()==null? null : element.getState().name());
      statement.setString(offset+4, element.getUUID()==null? null : element.getUUID().toString());
      return 5;
    }

  }

  public ProcessInstanceMap(TransactionFactory<DBTransaction> transactionFactory, ProcessEngine<DBTransaction> processEngine) {
    super(transactionFactory, new ProcessInstanceElementFactory(processEngine));
  }

}
