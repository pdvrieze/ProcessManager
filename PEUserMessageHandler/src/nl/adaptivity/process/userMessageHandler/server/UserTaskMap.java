package nl.adaptivity.process.userMessageHandler.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;

import nl.adaptivity.process.client.ServletProcessEngineClient;
import nl.adaptivity.process.exec.XmlProcessNodeInstance;
import nl.adaptivity.process.userMessageHandler.server.InternalEndpoint.XmlTask;


public class UserTaskMap extends CachingDBHandleMap<UserTask<?>> {


  public static final String TABLE = "usertasks";
  public static final String COL_HANDLE = "taskhandle";
  public static final String COL_REMOTEHANDLE = "remotehandle";

  private static final class UserTaskFactory extends AbstractElementFactory<UserTask<?>> {

    private static final int TASK_LOOKUP_TIMEOUT_MILIS = 1;
    private int aColNoHandle;
    private int aColNoRemoteHandle;

    @Override
    public void initResultSet(ResultSetMetaData pMetaData) throws SQLException {
      final int columnCount = pMetaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = pMetaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          aColNoHandle = i;
        } else if (COL_REMOTEHANDLE.equals(colName)) {
          aColNoRemoteHandle = i;
        } // ignore other columns
      }
    }

    @Override
    public CharSequence getTableName() {
      return TABLE;
    }

    @Override
    public CharSequence getCreateColumns() {
      return COL_HANDLE+", "+COL_REMOTEHANDLE;
    }

    @Override
    public UserTask<?> create(DBTransaction pConnection, ResultSet pResultSet) throws SQLException {
      long handle = pResultSet.getLong(aColNoHandle);
      long remoteHandle = pResultSet.getLong(aColNoRemoteHandle);

      XmlProcessNodeInstance instance;
      try {
        Future<XmlProcessNodeInstance> future = ServletProcessEngineClient.getProcessNodeInstance(remoteHandle, SecurityProvider.SYSTEMPRINCIPAL, null, XmlTask.class);
        instance = future.get(TASK_LOOKUP_TIMEOUT_MILIS, TimeUnit.MILLISECONDS);
      } catch (JAXBException | InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }
      for(Object bodyElem: instance.getBody().getAny()) {
        if (bodyElem instanceof UserTask<?>) {
          UserTask<?> result = (UserTask<?>) bodyElem;
          result.setHandle(handle);
          return result;
        }
      }
//
//      instance.getBody().getAny().iterator().next();
//
//      XmlTask result = new XmlTask();
//      result.setHandle(handle);
//      result.setRemoteHandle(remoteHandle);
//      result.setEndpoint(instance.getEndpoint());


      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public CharSequence getPrimaryKeyCondition(UserTask<?> pObject) {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement pStatement, UserTask<?> pObject, int pOffset) throws SQLException {
      // TODO Auto-generated method stub
      // return 0;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public UserTask<?> asInstance(Object pO) {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<CharSequence> getStoreColumns() {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<CharSequence> getStoreParamHolders() {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int setStoreParams(PreparedStatement pStatement, UserTask<?> pElement, int pOffset) throws SQLException {
      // TODO Auto-generated method stub
      // return 0;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public CharSequence getHandleCondition(long pElement) {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int setHandleParams(PreparedStatement pStatement, long pHandle, int pOffset) throws SQLException {
      // TODO Auto-generated method stub
      // return 0;
      throw new UnsupportedOperationException("Not yet implemented");
    }

  }

  public UserTaskMap(DataSource pConnectionProvider) {
    super(pConnectionProvider, new UserTaskFactory());
    // TODO Auto-generated constructor stub
  }

}
