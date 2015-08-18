package nl.adaptivity.process.userMessageHandler.server;

import java.io.FileNotFoundException;
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

import net.devrieze.util.TransactionFactory;
import org.w3.soapEnvelope.Body;
import org.w3.soapEnvelope.Envelope;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;

import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.client.ServletProcessEngineClient;
import nl.adaptivity.process.exec.XmlProcessNodeInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.activation.Sources;


public class UserTaskMap extends CachingDBHandleMap<XmlTask> {


  public static final String TABLE = "usertasks";
  public static final String TABLEDATA = "nodedata";
  public static final String COL_HANDLE = "taskhandle";
  public static final String COL_REMOTEHANDLE = "remotehandle";
  public static final String COL_NAME = "name";
  public static final String COL_DATA = "data";



  private static final class UserTaskFactory extends AbstractElementFactory<XmlTask> {

    private static final int TASK_LOOKUP_TIMEOUT_MILIS = 1;
    private static final String QUERY_GET_DATA_FOR_TASK = "SELECT "+COL_NAME+", "+COL_DATA+" FROM "+TABLEDATA+" WHERE "+COL_HANDLE+" = ?";
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
    public XmlTask create(DBTransaction pConnection, ResultSet pResultSet) throws SQLException {
      long handle = pResultSet.getLong(aColNoHandle);
      long remoteHandle = pResultSet.getLong(aColNoRemoteHandle);

      XmlProcessNodeInstance instance;
      try {
        try{
          Future<XmlProcessNodeInstance> future = ServletProcessEngineClient.getProcessNodeInstance(remoteHandle, SecurityProvider.SYSTEMPRINCIPAL, null, XmlTask.class, Envelope.class);
          instance = future.get(TASK_LOOKUP_TIMEOUT_MILIS, TimeUnit.MILLISECONDS);
          if (instance.getState()==TaskState.Complete) {
            return null; // Delete from the database, this can't be redone.
          }
        } catch (ExecutionException|MessagingException e) {

          Throwable f = e;
          while (f.getCause()!=null && ((f.getCause() instanceof ExecutionException)||(f.getCause() instanceof MessagingException))) {
            f = f.getCause();
          }
          if (f.getCause() instanceof FileNotFoundException) {
            return null; // No such task exists
          } else if (f.getCause() instanceof RuntimeException){
            throw (RuntimeException) f;
          } else if (f instanceof ExecutionException) {
            throw (ExecutionException) f;
          } else if (f instanceof MessagingException) {
            throw (MessagingException) f;
          }
          throw e;
        }
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
              result.setState(instance.getState());
              return result;
            }
          }
        }
      }
      return null;
    }

    @Override
    public void postCreate(DBTransaction pConnection, XmlTask pElement) throws SQLException {
      try(PreparedStatement statement = pConnection.prepareStatement(QUERY_GET_DATA_FOR_TASK)) {
        statement.setLong(1, pElement.getHandle());
        if(statement.execute()) {
          try (ResultSet resultset = statement.getResultSet()) {
            while(resultset.next()) {
              String name = resultset.getString(1);
              XmlItem item = pElement.getItem(name);
              if (item!=null) {
                item.setValue(resultset.getString(2));
              }
            }
          }
        }
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(XmlTask pObject) {
      return getHandleCondition(pObject.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement pStatement, XmlTask pObject, int pOffset) throws SQLException {
      return setHandleParams(pStatement, pObject.getHandle(), pOffset);
    }

    @Override
    public XmlTask asInstance(Object pO) {
      if (pO instanceof XmlTask) {
        return (XmlTask) pO;
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
    public int setStoreParams(PreparedStatement pStatement, XmlTask pElement, int pOffset) throws SQLException {
      pStatement.setLong(pOffset, pElement.getRemoteHandle());
      return 1;
    }

    @Override
    public void postStore(DBTransaction pConnection, long pHandle, XmlTask pOldValue, XmlTask pNewValue) throws SQLException {
      try (PreparedStatement statement = pConnection.prepareStatement("INSERT INTO `"+TABLEDATA+"` (`"+COL_HANDLE+"`, `"+COL_NAME+"`, `"+COL_DATA+"`) VALUES ( ?, ?, ? ) ON DUPLICATE KEY UPDATE `"+COL_DATA+"`= VALUES(`"+COL_DATA+"`);")) {
        final List<XmlItem> items = pNewValue.getItems();
        for(XmlItem item:items) {
          if (item!=null && item.getName()!=null) {
            XmlItem oldItem = pOldValue==null ? null : pOldValue.getItem(item.getName());
            if (oldItem==null ||
                oldItem.getValue()==null ||
                (! oldItem.getValue().equals(item.getValue()))) {
              if (! ((oldItem==null || oldItem.getValue()==null) && item.getValue()==null)) {
                statement.setLong(1, pHandle);
                statement.setString(2, item.getName());
                statement.setString(3, item.getValue());
                statement.addBatch();
              }
            }
          }
        }
        statement.executeBatch();
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
    public void preClear(DBTransaction pConnection) throws SQLException {
      CharSequence filter = getFilterExpression();
      final String sql;
      if (filter==null) {
        sql = "DELETE FROM "+TABLEDATA;
      } else {
        sql= "DELETE FROM `"+TABLEDATA+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE+"` WHERE "+filter+");";
      }
      try (PreparedStatement statement = pConnection.prepareStatement(sql)) {
        setFilterParams(statement, 1);
        statement.execute();
      }
    }

    @Override
    public void preRemove(DBTransaction pConnection, long pHandle) throws SQLException {
      final String sql = "DELETE FROM "+TABLEDATA+" WHERE `"+COL_HANDLE+"` = ?";
      try (PreparedStatement statement = pConnection.prepareStatement(sql)) {
        statement.setLong(1, pHandle);
        statement.execute();
      }
    }

    @Override
    public void preRemove(DBTransaction pConnection, XmlTask pElement) throws SQLException {
      preRemove(pConnection, pElement.getHandle());
    }

    @Override
    public void preRemove(DBTransaction pConnection, ResultSet pElementSource) throws SQLException {
      preRemove(pConnection, pElementSource.getLong(aColNoHandle));
    }

  }

  public UserTaskMap(TransactionFactory<? extends DBTransaction> pConnectionProvider) {
    super(pConnectionProvider, new UserTaskFactory());
  }

}
