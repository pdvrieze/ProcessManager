package nl.adaptivity.process.userMessageHandler.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMSource;

import org.w3.soapEnvelope.Body;
import org.w3.soapEnvelope.Envelope;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.client.ServletProcessEngineClient;
import nl.adaptivity.process.exec.XmlProcessNodeInstance;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.activation.Sources;


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
        Future<XmlProcessNodeInstance> future = ServletProcessEngineClient.getProcessNodeInstance(remoteHandle, SecurityProvider.SYSTEMPRINCIPAL, null, XmlTask.class, Envelope.class);
        instance = future.get(TASK_LOOKUP_TIMEOUT_MILIS, TimeUnit.MILLISECONDS);
      } catch (JAXBException | InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }
      for(Object bodyElem: instance.getBody().getAny()) {
        if (bodyElem instanceof JAXBElement<?>) {
          bodyElem = ((JAXBElement<?>) bodyElem).getValue();
        }
        if (bodyElem instanceof Envelope) {
          Envelope envelope = (Envelope) bodyElem;

          Body body = envelope.getBody();
          Element operation = (Element) body.getAny().get(0);
          for(Node elem=operation.getFirstChild(); elem!=null; elem=elem.getNextSibling()) {
            if (Constants.USER_MESSAGE_HANDLER_NS.equals(elem.getNamespaceURI()) &&
                "taskParam".equals(elem.getLocalName())) {

              XmlTask result = JAXB.unmarshal(Sources.toReader(new DOMSource(elem.getFirstChild().cloneNode(true))), XmlTask.class);
              result.setInstanceHandle(instance.getProcessinstance());
              result.setHandle(handle);
              return result;
            }
          }
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
      return getHandleCondition(pObject.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement pStatement, UserTask<?> pObject, int pOffset) throws SQLException {
      return setHandleParams(pStatement, pObject.getHandle(), pOffset);
    }

    @Override
    public UserTask<?> asInstance(Object pO) {
      if (pO instanceof UserTask<?>) {
        return (UserTask<?>) pO;
      }
      return null;
    }

    @Override
    public List<CharSequence> getStoreColumns() {
      return Arrays.<CharSequence>asList(COL_REMOTEHANDLE);
    }

    @Override
    public List<CharSequence> getStoreParamHolders() {
      return Arrays.<CharSequence>asList("?");
    }

    @Override
    public int setStoreParams(PreparedStatement pStatement, UserTask<?> pElement, int pOffset) throws SQLException {
      pStatement.setLong(pOffset, pElement.getRemoteHandle());
      return 1;
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

  }

  public UserTaskMap(DataSource pConnectionProvider) {
    super(pConnectionProvider, new UserTaskFactory());
  }

}
