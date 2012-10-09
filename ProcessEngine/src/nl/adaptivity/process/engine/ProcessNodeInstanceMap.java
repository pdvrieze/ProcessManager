package nl.adaptivity.process.engine;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.DBHandleMap.ElementFactory;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;


public class ProcessNodeInstanceMap extends CachingDBHandleMap<ProcessNodeInstance> implements ElementFactory<ProcessNodeInstance> {

  private static final String TABLE = "processnodeinstances";
  private static final String COL_HANDLE = "pnihandle";
  private static final String COL_NODEID = "nodeid";
  private static final String COL_HPROCESSINSTANCE = "pihandle";
  private int aColNoHandle;
  private int aColNoHPredecessor;
  private int aColNoHProcessInstance;
  private int aColNoNodeId;
  private final ProcessEngine aProcessEngine;

  public ProcessNodeInstanceMap(ProcessEngine pProcessEngine, String pResourceName) {
    super(resourceNameToDataSource(pResourceName), null, TABLE, COL_HANDLE);
    aProcessEngine = pProcessEngine;
  }

  private static DataSource resourceNameToDataSource(String pResourceName) {
    try {
      return (DataSource) new InitialContext().lookup(pResourceName);
    } catch (NamingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ProcessNodeInstance create(ResultSet pRow) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
//    Principal owner = new SimplePrincipal(pRow.getString(aColNoOwner));
//    Handle<ProcessModel> hProcessModel = MemHandleMap.handle(pRow.getLong(aColNoHProcessModel));
//    ProcessModel processModel = aProcessEngine.getProcessModel(hProcessModel, SecurityProvider.SYSTEMPRINCIPAL);
//    String instancename = pRow.getString(aColNoName);
//    long piHandle = pRow.getLong(aColNoHandle);
//
//    final ProcessNodeInstance result = new ProcessNodeInstance(owner, processModel, instancename, aProcessEngine);
//    result.setHandle(piHandle);
//    return result;

  }

  @Override
  public void init(ResultSetMetaData pMetaData) {
    try {
      final int columnCount = pMetaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = pMetaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          aColNoHandle = i;
        } else if (COL_NODEID.equals(colName)) {
          aColNoNodeId = i;
        } else if (COL_HPROCESSINSTANCE.equals(colName)) {
          aColNoHProcessInstance = i;
        } // ignore other columns
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isInstance(Object pObject) {
    return pObject instanceof ProcessInstance;
  }

  @Override
  public Iterable<String> getInsertColumns() {
    return Arrays.asList(COL_NODEID, COL_HPROCESSINSTANCE);
  }

  @Override
  public void insertColumnValues(PreparedStatement pStatement, ProcessNodeInstance pElement) throws SQLException {
    pStatement.setString(1, pElement.getNode().getId());
    pStatement.setLong(2, pElement.getProcessInstance().getHandle());
  }

}
