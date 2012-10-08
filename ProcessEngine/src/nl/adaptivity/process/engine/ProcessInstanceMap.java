package nl.adaptivity.process.engine;

import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import net.devrieze.util.DBHandleMap;
import net.devrieze.util.DBHandleMap.ElementFactory;
import net.devrieze.util.MemHandleMap;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.process.processModel.ProcessModel;


public class ProcessInstanceMap extends DBHandleMap<ProcessInstance> implements ElementFactory<ProcessInstance> {

  private static final String TABLE = "processinstances";
  private static final String COL_HANDLE = "pihandle";
  private static final String COL_OWNER = "owner";
  private static final String COL_NAME = "name";
  private static final String COL_HPROCESSMODEL = "pmhandle";
  private int aColNoHandle;
  private int aColNoOwner;
  private int aColNoHProcessModel;
  private int aColNoName;
  private final ProcessEngine aProcessEngine;

  public ProcessInstanceMap(ProcessEngine pProcessEngine, String pResourceName) {
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
  public ProcessInstance create(ResultSet pRow) throws SQLException {
    Principal owner = new SimplePrincipal(pRow.getString(aColNoOwner));
    Handle<ProcessModel> hProcessModel = MemHandleMap.handle(pRow.getLong(aColNoHProcessModel));
    ProcessModel processModel = aProcessEngine.getProcessModel(hProcessModel, SecurityProvider.SYSTEMPRINCIPAL);
    String instancename = pRow.getString(aColNoName);
    long piHandle = pRow.getLong(aColNoHandle);

    final ProcessInstance result = new ProcessInstance(owner, processModel, instancename, aProcessEngine);
    result.setHandle(piHandle);
    return result;

  }

  @Override
  public void init(ResultSetMetaData pMetaData) {
    try {
      final int columnCount = pMetaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = pMetaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          aColNoHandle = i;
        } else if (COL_OWNER.equals(colName)) {
          aColNoOwner = i;
        } else if (COL_HPROCESSMODEL.equals(colName)) {
          aColNoHProcessModel = i;
        } else if (COL_NAME.equals(colName)) {
          aColNoName = i;
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
    return Arrays.asList(COL_OWNER, COL_HPROCESSMODEL);
  }

  @Override
  public void insertColumnValues(PreparedStatement pStatement, ProcessInstance pElement) throws SQLException {
    pStatement.setString(1, pElement.getOwner().getName());
    pStatement.setLong(2, pElement.getProcessModel().getHandle());
  }

}
