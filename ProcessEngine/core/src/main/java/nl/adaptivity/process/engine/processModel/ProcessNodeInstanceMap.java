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

package nl.adaptivity.process.engine.processModel;

import net.devrieze.util.Handles;
import net.devrieze.util.StringCache;
import net.devrieze.util.Transaction;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBHandleMap;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.util.xml.CompactFragment;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class ProcessNodeInstanceMap extends DBHandleMap<ProcessNodeInstance<DBTransaction>> {

  public static final String TABLE = "processnodeinstances";
  public static final String COL_HANDLE = "pnihandle";
  public static final String COL_NODEID = "nodeid";
  public static final String COL_STATE = "state";
  public static final String COL_PREDECESSOR = "predecessor";
  public static final String COL_HPROCESSINSTANCE = "pihandle";
  public static final String COL_NAME = "name";
  public static final String COL_DATA = "data";
  public static final String TABLE_PREDECESSORS = "pnipredecessors";
  public static final String TABLE_NODEDATA = "nodedata";

  static class ProcessNodeInstanceFactory extends AbstractElementFactory<ProcessNodeInstance<DBTransaction>> {

    private static final String QUERY_DATA = "SELECT `"+COL_NAME+"`, `"+COL_DATA+"` FROM `"+TABLE_NODEDATA+"` WHERE `"+COL_HANDLE+"` = ?;";
    private static final String QUERY_PREDECESSOR = "SELECT `"+COL_PREDECESSOR+"` FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` = ?;";
    private int mColNoHandle;
    private int mColNoHProcessInstance;
    private int mColNoNodeId;
    private int mColNoState;

    private final StringCache mStringCache;
    private final ProcessEngine mProcessEngine;

    public ProcessNodeInstanceFactory(ProcessEngine processEngine, StringCache stringCache) {
      mProcessEngine = processEngine;
      mStringCache = stringCache;
    }

    @Override
    public void initResultSet(ResultSetMetaData metaData) throws SQLException {
      final int columnCount = metaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = metaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          mColNoHandle = i;
        } else if (COL_NODEID.equals(colName)) {
          mColNoNodeId = i;
        } else if (COL_STATE.equals(colName)) {
          mColNoState = i;
        } else if (COL_HPROCESSINSTANCE.equals(colName)) {
          mColNoHProcessInstance = i;
        } // ignore other columns
      }
    }

    @Override
    public CharSequence getHandleCondition(Handle<? extends ProcessNodeInstance<DBTransaction>> element) {
      return COL_HANDLE+" = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement statement, Handle<? extends ProcessNodeInstance<DBTransaction>> handle, int offset) throws SQLException {
      statement.setLong(offset, handle.getHandle());
      return 1;
    }

    @Override
    public CharSequence getTableName() {
      return TABLE;
    }

    @Override
    public int setFilterParams(PreparedStatement statement, int offset) throws SQLException {
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ProcessNodeInstance create(DBTransaction transaction, ResultSet row) throws SQLException {


      Handle<ProcessInstance<? extends Transaction>> hProcessInstance = Handles.handle(row.getLong(mColNoHProcessInstance));
      ProcessInstance processInstance = mProcessEngine.getProcessInstance(transaction, hProcessInstance, SecurityProvider.SYSTEMPRINCIPAL);

      String nodeId = mStringCache.lookup(row.getString(mColNoNodeId));
      ExecutableProcessNode node = processInstance.getProcessModel().getNode(nodeId);

      long handle = row.getLong(mColNoHandle);

      final String sState = row.getString(mColNoState);
      NodeInstanceState state = sState == null ? null : NodeInstanceState.valueOf(sState);

      List<Handle<ProcessNodeInstance<DBTransaction>>> predecessors = new ArrayList<>();

      try(PreparedStatement preparedStatement = transaction.prepareStatement("SELECT "+COL_PREDECESSOR+" FROM "+TABLE_PREDECESSORS+" WHERE "+COL_HANDLE+" = ?")) {
        preparedStatement.setLong(1, handle);
        try(ResultSet rs = preparedStatement.executeQuery()) {
          while(rs.next()) {
            predecessors.add(Handles.<ProcessNodeInstance<DBTransaction>>handle(rs.getLong(1)));
          }
        }
      }

      ProcessNodeInstance result;
      if (node instanceof JoinImpl) {
        result =  new JoinInstance((JoinImpl) node, predecessors, processInstance, state);
      } else {
        result = new ProcessNodeInstance(node, predecessors, processInstance, state);
      }
      result.setHandle(handle);
      return result;
    }



    @Override
    public void postCreate(DBTransaction connection, ProcessNodeInstance<DBTransaction> element) throws SQLException {
      {
        for (Handle<? extends ProcessNodeInstance<?>> handle: element.getDirectPredecessors()) {
          element.ensurePredecessor(handle);
        }
      }
      {
        List<ProcessData> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(QUERY_DATA)) {
          statement.setLong(1, element.getHandle());
          if(statement.execute()) {
            try (ResultSet resultset = statement.getResultSet()){
              while(resultset.next()) {
                String name = resultset.getString(1);
                String data = resultset.getString(2);
                if (FAILURE_CAUSE.equals(name) && (element.getState() == NodeInstanceState.Failed || element.getState() == NodeInstanceState.FailRetry)) {
                  element.setFailureCause(data);
                } else {
                  results.add(new ProcessData(name, new CompactFragment(data)));
                }
              }
            }
          }
        }
        element.setResult(results);
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessNodeInstance<DBTransaction> object) {
      return getHandleCondition(object);
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement statement, ProcessNodeInstance<DBTransaction> object, int offset) throws SQLException {
      return setHandleParams(statement, object, offset);
    }

    @Override
    public ProcessNodeInstance asInstance(Object o) {
      if (o instanceof ProcessNodeInstance) {
        return (ProcessNodeInstance) o;
      }
      return null;
    }

    @Override
    public CharSequence getCreateColumns() {
      return COL_HANDLE+", "+COL_NODEID+", "+COL_HPROCESSINSTANCE+", "+COL_STATE;
    }

    @Override
    public List<CharSequence> getStoreColumns() {
      return Arrays.<CharSequence>asList(COL_NODEID, COL_HPROCESSINSTANCE,COL_STATE);
    }

    @Override
    public List<CharSequence> getStoreParamHolders() {
      return Arrays.<CharSequence>asList("?","?", "?");
    }

    @Override
    public int setStoreParams(PreparedStatement statement, ProcessNodeInstance element, int offset) throws SQLException {
      statement.setString(offset, element.getNode().getId());
      statement.setLong(offset+1, element.getProcessInstance().getHandle());
      statement.setString(offset+2, element.getState()==null ? null : element.getState().name());
      return 3;
    }

    @Override
    public void postStore(DBTransaction connection, Handle<? extends ProcessNodeInstance<DBTransaction>> handle, ProcessNodeInstance<DBTransaction> oldValue, ProcessNodeInstance<DBTransaction> element) throws SQLException {
      ProcessNodeInstanceMap.postStore(connection, handle, oldValue, element);
    }

    @Override
    public void preRemove(DBTransaction connection, Handle<? extends ProcessNodeInstance<DBTransaction>> handle) throws SQLException {
      ProcessNodeInstanceMap.preRemove(connection, handle);
    }

    @Override
    public void preRemove(DBTransaction connection, ProcessNodeInstance<DBTransaction> element) throws SQLException {
      preRemove(connection, element);
    }

    @Override
    public void preRemove(DBTransaction connection, ResultSet elementSource) throws SQLException {
      preRemove(connection, Handles.<ProcessNodeInstance<DBTransaction>>handle(elementSource.getLong(mColNoHandle)));
    }

    @Override
    public void preClear(DBTransaction connection) throws SQLException {
      CharSequence filter = getFilterExpression();
      {
        final String sql;
        if (filter==null || filter.length()==0) {
          sql= "DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE+"`);";
        } else {
          sql= "DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE+"` WHERE "+filter+");";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
          setFilterParams(statement, 1);
          statement.executeUpdate();
        }
      }
      {
        final String sql;
        if (filter==null || filter.length()==0) {
          sql= "DELETE FROM `"+TABLE_NODEDATA+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE+"`);";
        } else {
          sql= "DELETE FROM `"+TABLE_NODEDATA+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE+"` WHERE "+filter+");";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
          setFilterParams(statement, 1);
          statement.executeUpdate();
        }
      }
    }

  }

  private static final String FAILURE_CAUSE = "failureCause";

  static void postStore(final DBTransaction connection, final Handle<? extends ProcessNodeInstance<DBTransaction>> handle, final ProcessNodeInstance oldValue, final ProcessNodeInstance<DBTransaction> element) throws
          SQLException {
    if (oldValue!=null) { // update
      try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` = ?;")) {
        statement.setLong(1, handle.getHandle());
        statement.executeUpdate();
      }
    }
    // TODO allow for updating/storing node data
    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO `"+TABLE_PREDECESSORS+"` (`"+COL_HANDLE+"`,`"+COL_PREDECESSOR+"`) VALUES ( ?, ? );")) {
      final Collection<Handle<? extends ProcessNodeInstance<?>>> directPredecessors = element.getDirectPredecessors();
      for(Handle<? extends ProcessNodeInstance> predecessor:directPredecessors) {
        statement.setLong(1, handle.getHandle());
        if (predecessor==null) {
          statement.setNull(2, Types.BIGINT);
        } else {
          statement.setLong(2, predecessor.getHandle());
        }
        statement.addBatch();
      }
      statement.executeBatch();
    }

    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + TABLE_NODEDATA + "` (`" + COL_HANDLE + "`, `" + COL_NAME + "`, `" + COL_DATA + "`) VALUES ( ?, ?, ?)" +
                                                                              "ON DUPLICATE KEY UPDATE `" + COL_DATA + "` = VALUES(`"+COL_DATA+"`)")) {
      for(ProcessData data: element.getResults()) {
        statement.setLong(1, handle.getHandle());
        statement.setString(2, data.getName());
        String value = new String(data.getContent().getContent());
        statement.setString(3, value);

        statement.addBatch();
      }
      if ((element.getState() == NodeInstanceState.Failed || element.getState() == NodeInstanceState.FailRetry) && element.getFailureCause() != null) {
        statement.setLong(1, handle.getHandle());
        statement.setString(2, FAILURE_CAUSE);
        statement.setString(3, element.getFailureCause().getLocalizedMessage());
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  static void preRemove(final DBTransaction connection, final Handle<? extends ProcessNodeInstance<DBTransaction>> handle) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` = ?;")) {
      statement.setLong(1, handle.getHandle());
      statement.executeUpdate();
    }
    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+TABLE_NODEDATA+"` WHERE `"+COL_HANDLE+"` = ?;")) {
      statement.setLong(1, handle.getHandle());
      statement.executeUpdate();
    }
  }

  public ProcessNodeInstanceMap(TransactionFactory transactionFactory, ProcessEngine processEngine, StringCache stringCache) {
    super(transactionFactory, new ProcessNodeInstanceFactory(processEngine, stringCache));
  }

}
