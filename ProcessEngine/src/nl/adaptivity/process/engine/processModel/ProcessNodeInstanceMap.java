package nl.adaptivity.process.engine.processModel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.Handles;
import net.devrieze.util.StringCache;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;


public class ProcessNodeInstanceMap extends CachingDBHandleMap<ProcessNodeInstance> {

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

  static class ProcessNodeInstanceFactory extends AbstractElementFactory<ProcessNodeInstance> {

    private static final String QUERY_DATA = "SELECT `"+COL_NAME+"`, `"+COL_DATA+"` FROM `"+TABLE_NODEDATA+"` WHERE `"+COL_HANDLE+"` = ?;";
    private static final String QUERY_PREDECESSOR = "SELECT `"+COL_PREDECESSOR+"` FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` = ?;";
    private int aColNoHandle;
    private int aColNoHProcessInstance;
    private int aColNoNodeId;
    private int aColNoState;

    private final StringCache aStringCache;
    private final ProcessEngine aProcessEngine;

    public ProcessNodeInstanceFactory(ProcessEngine pProcessEngine, StringCache pStringCache) {
      aProcessEngine = pProcessEngine;
      aStringCache = pStringCache;
    }

    @Override
    public void initResultSet(ResultSetMetaData pMetaData) throws SQLException {
      final int columnCount = pMetaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = pMetaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          aColNoHandle = i;
        } else if (COL_NODEID.equals(colName)) {
          aColNoNodeId = i;
        } else if (COL_STATE.equals(colName)) {
          aColNoState = i;
        } else if (COL_HPROCESSINSTANCE.equals(colName)) {
          aColNoHProcessInstance = i;
        } // ignore other columns
      }
    }

    @Override
    public CharSequence getHandleCondition(long pElement) {
      return COL_HANDLE+" = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement pStatement, long pHandle, int pOffset) throws SQLException {
      pStatement.setLong(pOffset, pHandle);
      return 1;
    }

    @Override
    public CharSequence getTableName() {
      return TABLE;
    }

    @Override
    public int setFilterParams(PreparedStatement pStatement, int pOffset) throws SQLException {
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ProcessNodeInstance create(DBTransaction pConnection, ResultSet pRow) throws SQLException {
      long hProcessInstance = pRow.getLong(aColNoHProcessInstance);
      ProcessInstance processInstance = aProcessEngine.getAllProcessInstances(SecurityProvider.SYSTEMPRINCIPAL).get(hProcessInstance);

      String nodeId = aStringCache.lookup(pRow.getString(aColNoNodeId));
      ProcessNodeImpl node = processInstance.getProcessModel().getNode(nodeId);

      long handle = pRow.getLong(aColNoHandle);

      final String sState = pRow.getString(aColNoState);
      TaskState state = sState==null ? null : TaskState.valueOf(sState);

      ProcessNodeInstance result;
      if (node instanceof JoinImpl) {
        result =  new JoinInstance((JoinImpl) node, processInstance, state);
      } else {
        result = new ProcessNodeInstance(node, processInstance, state);
      }
      result.setHandle(handle);
      return result;
    }



    @Override
    public void postCreate(DBTransaction pConnection, ProcessNodeInstance pElement) throws SQLException {
      {
        List<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>();
        try (PreparedStatement statement = pConnection.prepareStatement(QUERY_PREDECESSOR)) {
          statement.setLong(1, pElement.getHandle());
          if(statement.execute()) {
            try (ResultSet resultset = statement.getResultSet()){
              while(resultset.next()) {
                Handle<? extends ProcessNodeInstance> predecessor = Handles.handle(resultset.getLong(1));
                predecessors.add(predecessor);
              }
            }
          }
        }
        pElement.getDirectPredecessors().clear();
        pElement.getDirectPredecessors().addAll(predecessors);
      }
      {
        List<ProcessData> data = new ArrayList<>();
        try (PreparedStatement statement = pConnection.prepareStatement(QUERY_DATA)) {
          statement.setLong(1, pElement.getHandle());
          if(statement.execute()) {
            try (ResultSet resultset = statement.getResultSet()){
              while(resultset.next()) {
                data.add(new ProcessData(resultset.getString(1), resultset.getString(1)));
              }
            }
          }
        }
        pElement.setResult(data);
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessNodeInstance pObject) {
      return getHandleCondition(pObject.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement pStatement, ProcessNodeInstance pObject, int pOffset) throws SQLException {
      return setHandleParams(pStatement, pObject.getHandle(), pOffset);
    }

    @Override
    public ProcessNodeInstance asInstance(Object pO) {
      if (pO instanceof ProcessNodeInstance) {
        return (ProcessNodeInstance) pO;
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
    public int setStoreParams(PreparedStatement pStatement, ProcessNodeInstance pElement, int pOffset) throws SQLException {
      pStatement.setString(pOffset, pElement.getNode().getId());
      pStatement.setLong(pOffset+1, pElement.getProcessInstance().getHandle());
      pStatement.setString(pOffset+2, pElement.getState()==null ? null : pElement.getState().name());
      return 3;
    }

    @Override
    public void postStore(DBTransaction pConnection, long pHandle, ProcessNodeInstance pElement) throws SQLException {
      try (PreparedStatement statement = pConnection.prepareStatement("INSERT INTO `"+TABLE_PREDECESSORS+"` (`"+COL_HANDLE+"`,`"+COL_PREDECESSOR+"`) VALUES ( ?, ? );")) {
        final Collection<net.devrieze.util.HandleMap.Handle<? extends ProcessNodeInstance>> directPredecessors = pElement.getDirectPredecessors();
        for(Handle<? extends ProcessNodeInstance> predecessor:directPredecessors) {
          statement.setLong(1, pHandle);
          if (predecessor==null) {
            statement.setNull(2, Types.BIGINT);
          } else {
            statement.setLong(2, predecessor.getHandle());
          }
          statement.addBatch();
        }
        statement.executeBatch();
      }
    }

    @Override
    public void preRemove(DBTransaction pConnection, long pHandle) throws SQLException {
      try (PreparedStatement statement = pConnection.prepareStatement("DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` = ?;")) {
        statement.setLong(1, pHandle);
        statement.executeUpdate();
      }
      try (PreparedStatement statement = pConnection.prepareStatement("DELETE FROM `"+TABLE_NODEDATA+"` WHERE `"+COL_HANDLE+"` ?;")) {
        statement.setLong(1, pHandle);
        statement.executeUpdate();
      }
    }

    @Override
    public void preRemove(DBTransaction pConnection, ProcessNodeInstance pElement) throws SQLException {
      preRemove(pConnection, pElement.getHandle());
    }

    @Override
    public void preRemove(DBTransaction pConnection, ResultSet pElementSource) throws SQLException {
      preRemove(pConnection, pElementSource.getLong(aColNoHandle));
    }

    @Override
    public void preClear(DBTransaction pConnection) throws SQLException {
      CharSequence filter = getFilterExpression();
      {
        final String sql;
        if (filter==null || filter.length()==0) {
          sql= "DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE+"`);";
        } else {
          sql= "DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE+"` WHERE "+filter+");";
        }
        try (PreparedStatement statement = pConnection.prepareStatement(sql)) {
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
        try (PreparedStatement statement = pConnection.prepareStatement(sql)) {
          setFilterParams(statement, 1);
          statement.executeUpdate();
        }
      }
    }

  }

  public ProcessNodeInstanceMap(DataSource pDBResource, ProcessEngine pProcessEngine, StringCache pStringCache) {
    super(pDBResource, new ProcessNodeInstanceFactory(pProcessEngine, pStringCache));
  }

}
