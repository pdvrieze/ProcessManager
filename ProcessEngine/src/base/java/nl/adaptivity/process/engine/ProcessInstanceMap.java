package nl.adaptivity.process.engine;

import java.io.IOException;
import java.io.StringReader;
import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.devrieze.util.TransactionFactory;
import nl.adaptivity.process.processModel.ProcessModel;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.Handles;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.engine.ProcessInstance.State;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstanceMap;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;


public class ProcessInstanceMap extends CachingDBHandleMap<ProcessInstance> {

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

  static class ProcessInstanceElementFactory extends AbstractElementFactory<ProcessInstance> {

    private static final String QUERY_DATA = "SELECT `"+COL_NAME+"`, `"+COL_DATA+"`, `"+COL_ISOUTPUT+"` FROM `"+TABLE_INSTANCEDATA+"` WHERE `"+COL_HANDLE+"` = ?;";

    private static final String QUERY_GET_NODEINSTHANDLES_FROM_PROCINSTANCE = "SELECT "+ProcessNodeInstanceMap.COL_HANDLE+
    " FROM "+ProcessNodeInstanceMap.TABLE+
    " WHERE "+ProcessNodeInstanceMap.COL_HPROCESSINSTANCE +" = ? ;";
//    AND "+
//    ProcessNodeInstanceMap.COL_HANDLE+" NOT IN ( SELECT "+ProcessNodeInstanceMap.COL_PREDECESSOR+" FROM "+ProcessNodeInstanceMap.TABLE_PREDECESSORS+" );";
    private int aColNoHandle;
    private int aColNoOwner;
    private int aColNoHProcessModel;
    private int aColNoName;
    private final ProcessEngine aProcessEngine;
    private int aColNoState;
    private int aColNoUuid;

    public ProcessInstanceElementFactory(ProcessEngine pProcessEngine) {
      aProcessEngine = pProcessEngine;
    }

    @Override
    public void initResultSet(ResultSetMetaData pMetaData) throws SQLException {
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
        } else if (COL_STATE.equals(colName)) {
          aColNoState = i;
        } else if (COL_UUID.equals(colName)) {
          aColNoUuid = i;
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
      return TABLE_INSTANCES;
    }

    @Override
    public int setFilterParams(PreparedStatement pStatement, int pOffset) throws SQLException {
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ProcessInstance create(DBTransaction pConnection, ResultSet pRow) throws SQLException {
      Principal owner = new SimplePrincipal(pRow.getString(aColNoOwner));
      Handle<ProcessModel> hProcessModel = Handles.handle(pRow.getLong(aColNoHProcessModel));
      ProcessModelImpl processModel = aProcessEngine.getProcessModel(pConnection, hProcessModel, SecurityProvider.SYSTEMPRINCIPAL);
      String instancename = pRow.getString(aColNoName);
      long piHandle = pRow.getLong(aColNoHandle);
      ProcessInstance.State state = toState(pRow.getString(aColNoState));
      UUID uuid = toUUID(pRow.getString(aColNoUuid));

      final ProcessInstance result = new ProcessInstance(piHandle, owner, processModel, instancename, uuid, state, aProcessEngine);
      return result;
    }

    private UUID toUUID(String pString) {
      if (pString==null) { return null; }
      return UUID.fromString(pString);
    }

    private static State toState(String pString) {
      if (pString==null) { return null; }
      return State.valueOf(pString);
    }

    @Override
    public void postCreate(DBTransaction pConnection, ProcessInstance pElement) throws SQLException {
      {
        try (PreparedStatement statement = pConnection.prepareStatement(QUERY_GET_NODEINSTHANDLES_FROM_PROCINSTANCE)) {
          statement.setLong(1, pElement.getHandle());

          List<Handle<ProcessNodeInstance>> handles = new ArrayList<>();
          if (statement.execute()) {
            try (ResultSet resultset = statement.getResultSet()){
              while (resultset.next()) {
                handles.add(Handles.<ProcessNodeInstance>handle(resultset.getLong(1)));
              }
            }
          }
          pElement.setChildren(pConnection, handles);
        }
      }
      {
        try (PreparedStatement statement = pConnection.prepareStatement(QUERY_DATA)) {
          statement.setLong(1, pElement.getHandle());

          List<ProcessData> inputs = new ArrayList<>();
          List<ProcessData> outputs = new ArrayList<>();

          if (statement.execute()) {
            try (ResultSet resultset = statement.getResultSet()){
              DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
              DocumentBuilder db = dbf.newDocumentBuilder();
              while (resultset.next()) {
                ProcessData data = new ProcessData(resultset.getString(1), db.parse(new InputSource(new StringReader(resultset.getString(2)))));
                if (resultset.getBoolean(3)) {
                  outputs.add(data);
                } else {
                  inputs.add(data);
                }
              }
            } catch (SAXException | IOException | ParserConfigurationException e) {
              throw new RuntimeException(e);
            }
          }
          pElement.setInputs(inputs);
          pElement.setOutputs(outputs);
        }
      }
      pElement.reinitialize(pConnection);
    }

    @Override
    public void preRemove(DBTransaction pConnection, long pHandle) throws SQLException {
      try (PreparedStatement statement = pConnection.prepareStatement("DELETE FROM `"+TABLE_INSTANCEDATA+"` WHERE `"+COL_HANDLE+"` = ?;")) {
        statement.setLong(1, pHandle);
        statement.executeUpdate();
      }
      try (PreparedStatement statement = pConnection.prepareStatement("DELETE FROM `"+ProcessNodeInstanceMap.TABLE+"` where `"+ ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+" = ?;")) {
        statement.setLong(1, pHandle);
        statement.executeUpdate();
      }
    }

    @Override
    public void preRemove(DBTransaction pConnection, ProcessInstance pElement) throws SQLException {
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
          sql= "DELETE FROM `"+TABLE_INSTANCEDATA+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE_INSTANCES+"`);";
        } else {
          sql= "DELETE FROM `"+TABLE_INSTANCEDATA+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE_INSTANCES+"` WHERE "+filter+");";
        }
        try (PreparedStatement statement = pConnection.prepareStatement(sql)) {
          setFilterParams(statement, 1);
          statement.executeUpdate();
        }
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessInstance pObject) {
      return getHandleCondition(pObject.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement pStatement, ProcessInstance pObject, int pOffset) throws SQLException {
      return setHandleParams(pStatement, pObject.getHandle(), pOffset);
    }

    @Override
    public ProcessInstance asInstance(Object pO) {
      if (pO instanceof ProcessInstance) {
        return (ProcessInstance) pO;
      }
      return null;
    }

    @Override
    public CharSequence getCreateColumns() {
      return COL_HANDLE+", "+COL_HPROCESSMODEL+", "+COL_NAME+", "+COL_OWNER+", "+COL_STATE+", "+COL_UUID;
    }

    @Override
    public List<CharSequence> getStoreColumns() {
      return Arrays.<CharSequence>asList(COL_HPROCESSMODEL, COL_NAME, COL_OWNER, COL_STATE, COL_UUID);
    }

    @Override
    public List<CharSequence> getStoreParamHolders() {
      return Arrays.<CharSequence>asList("?","?","?", "?", "?");
    }

    @Override
    public int setStoreParams(PreparedStatement pStatement, ProcessInstance pElement, int pOffset) throws SQLException {
      pStatement.setLong(pOffset, pElement.getProcessModel().getHandle());
      pStatement.setString(pOffset+1, pElement.getName());
      pStatement.setString(pOffset+2, pElement.getOwner().getName());
      pStatement.setString(pOffset+3, pElement.getState()==null? null : pElement.getState().name());
      pStatement.setString(pOffset+4, pElement.getUUID()==null? null : pElement.getUUID().toString());
      return 5;
    }

  }

  public ProcessInstanceMap(TransactionFactory<?> pTransactionFactory, ProcessEngine pProcessEngine) {
    super(pTransactionFactory, new ProcessInstanceElementFactory(pProcessEngine));
  }

}
