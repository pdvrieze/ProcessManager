package nl.adaptivity.process.engine.processModel;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.Handles;
import net.devrieze.util.StringCache;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


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

    public ProcessNodeInstanceFactory(ProcessEngine processEngine, StringCache stringCache) {
      aProcessEngine = processEngine;
      aStringCache = stringCache;
    }

    @Override
    public void initResultSet(ResultSetMetaData metaData) throws SQLException {
      final int columnCount = metaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = metaData.getColumnName(i);
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
    public CharSequence getHandleCondition(long element) {
      return COL_HANDLE+" = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement statement, long handle, int offset) throws SQLException {
      statement.setLong(offset, handle);
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
    public ProcessNodeInstance create(DBTransaction connection, ResultSet row) throws SQLException {
      HProcessInstance hProcessInstance = new HProcessInstance(row.getLong(aColNoHProcessInstance));
      ProcessInstance processInstance = aProcessEngine.getProcessInstance(connection, hProcessInstance, SecurityProvider.SYSTEMPRINCIPAL);

      String nodeId = aStringCache.lookup(row.getString(aColNoNodeId));
      ProcessNodeImpl node = processInstance.getProcessModel().getNode(nodeId);

      long handle = row.getLong(aColNoHandle);

      final String sState = row.getString(aColNoState);
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
    public void postCreate(DBTransaction connection, ProcessNodeInstance element) throws SQLException {
      {
        List<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(QUERY_PREDECESSOR)) {
          statement.setLong(1, element.getHandle());
          if(statement.execute()) {
            try (ResultSet resultset = statement.getResultSet()){
              while(resultset.next()) {
                Handle<? extends ProcessNodeInstance> predecessor = Handles.handle(resultset.getLong(1));
                predecessors.add(predecessor);
              }
            }
          }
        }
        element.setDirectPredecessors(predecessors);
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
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader("<value>"+ data +"</value>")));
                Node value;
                if (doc.getDocumentElement().getChildNodes().getLength()==1) {
                  value = doc.getDocumentElement().getFirstChild();
                } else {
                  value = doc.createDocumentFragment();
                  for(Node n = doc.getDocumentElement().getFirstChild(); n!=null; n=n.getNextSibling()) {
                    value.appendChild(n);
                  }
                }
                results.add(new ProcessData(name, value));
              }
            } catch (SAXException | IOException | ParserConfigurationException e) {
              throw new RuntimeException(e);
            }
          }
        }
        element.setResult(results);
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessNodeInstance object) {
      return getHandleCondition(object.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement statement, ProcessNodeInstance object, int offset) throws SQLException {
      return setHandleParams(statement, object.getHandle(), offset);
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
    public void postStore(DBTransaction connection, long handle, ProcessNodeInstance oldValue, ProcessNodeInstance element) throws SQLException {
      ProcessNodeInstanceMap.postStore(connection, handle, oldValue, element);
    }

    @Override
    public void preRemove(DBTransaction connection, long handle) throws SQLException {
      ProcessNodeInstanceMap.preRemove(connection, handle);
    }

    @Override
    public void preRemove(DBTransaction connection, ProcessNodeInstance element) throws SQLException {
      preRemove(connection, element.getHandle());
    }

    @Override
    public void preRemove(DBTransaction connection, ResultSet elementSource) throws SQLException {
      preRemove(connection, elementSource.getLong(aColNoHandle));
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

  static void postStore(final DBTransaction connection, final long handle, final ProcessNodeInstance oldValue, final ProcessNodeInstance element) throws
          SQLException {
    if (oldValue!=null) { // update
      try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` = ?;")) {
        statement.setLong(1, handle);
        statement.executeUpdate();
      }
    }
    // TODO allow for updating/storing node data
    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO `"+TABLE_PREDECESSORS+"` (`"+COL_HANDLE+"`,`"+COL_PREDECESSOR+"`) VALUES ( ?, ? );")) {
      final Collection<Handle<? extends ProcessNodeInstance>> directPredecessors = element.getDirectPredecessors();
      for(Handle<? extends ProcessNodeInstance> predecessor:directPredecessors) {
        statement.setLong(1, handle);
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
        statement.setLong(1, handle);
        statement.setString(2, data.getName());
        String value = new String(data.getContent().getContent());
        statement.setString(3, value);

        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  static void preRemove(final DBTransaction connection, final long handle) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+TABLE_PREDECESSORS+"` WHERE `"+COL_HANDLE+"` = ?;")) {
      statement.setLong(1, handle);
      statement.executeUpdate();
    }
    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+TABLE_NODEDATA+"` WHERE `"+COL_HANDLE+"` = ?;")) {
      statement.setLong(1, handle);
      statement.executeUpdate();
    }
  }

  public ProcessNodeInstanceMap(TransactionFactory transactionFactory, ProcessEngine processEngine, StringCache stringCache) {
    super(transactionFactory, new ProcessNodeInstanceFactory(processEngine, stringCache));
  }

}
