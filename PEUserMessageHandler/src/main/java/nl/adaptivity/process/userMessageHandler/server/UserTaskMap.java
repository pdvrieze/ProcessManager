package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.client.ServletProcessEngineClient;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.engine.processModel.XmlProcessNodeInstance;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.w3.soapEnvelope.Envelope;

import javax.xml.bind.JAXBException;

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


public class UserTaskMap extends CachingDBHandleMap<XmlTask> {


  public static final String TABLE = "usertasks";
  public static final String TABLEDATA = "nodedata";
  public static final String COL_HANDLE = "taskhandle";
  public static final String COL_REMOTEHANDLE = "remotehandle";
  public static final String COL_NAME = "name";
  public static final String COL_DATA = "data";



  private static class PostTaskFactory implements nl.adaptivity.util.xml.XmlDeserializerFactory<XmlTask> {

    @Override
    public XmlTask deserialize(final XmlReader in) throws XmlException {
      XmlUtil.skipPreamble(in);

      in.require(EventType.START_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, "postTask");

      XmlTask result = null;
      while (in.hasNext() && in.next() != EventType.END_ELEMENT) {
        switch (in.getEventType()) {
          case START_ELEMENT:
            if ("taskParam".equals(in.getLocalName())) {
              in.next(); // Go to the contents
              result = XmlTask.deserialize(in);
              in.nextTag();
              in.require(EventType.END_ELEMENT, null, "taskParam");
            } else {
              XmlUtil.skipElement(in);
              in.require(EventType.END_ELEMENT, null, null);
            }
            break;
          default:
            XmlUtil.unhandledEvent(in);
        }
      }
      in.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, "postTask");

      return result;
    }
  }


  private static final class UserTaskFactory extends AbstractElementFactory<XmlTask> {


    private static final int TASK_LOOKUP_TIMEOUT_MILIS = 1;
    private static final String QUERY_GET_DATA_FOR_TASK = "SELECT "+COL_NAME+", "+COL_DATA+" FROM "+TABLEDATA+" WHERE "+COL_HANDLE+" = ?";
    private int mColNoHandle;
    private int mColNoRemoteHandle;

    @Override
    public void initResultSet(ResultSetMetaData metaData) throws SQLException {
      final int columnCount = metaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = metaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          mColNoHandle = i;
        } else if (COL_REMOTEHANDLE.equals(colName)) {
          mColNoRemoteHandle = i;
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
    public XmlTask create(DBTransaction connection, ResultSet resultSet) throws SQLException {
      long handle = resultSet.getLong(mColNoHandle);
      long remoteHandle = resultSet.getLong(mColNoRemoteHandle);

      XmlProcessNodeInstance instance;
      try {
        try{
          Future<XmlProcessNodeInstance> future = ServletProcessEngineClient.getProcessNodeInstance(remoteHandle, SecurityProvider.SYSTEMPRINCIPAL, null, XmlTask.class, Envelope.class);
          instance = future.get(TASK_LOOKUP_TIMEOUT_MILIS, TimeUnit.MILLISECONDS);
          if (instance==null || instance.getState()==TaskState.Complete) {
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
            throw (RuntimeException) f.getCause();
          } else if (f instanceof ExecutionException) {
            throw (ExecutionException) f;
          } else if (f instanceof MessagingException) {
            throw (MessagingException) f;
          }
          throw e;
        }

        if (instance.getBody()!=null) {
          XmlReader reader = XMLFragmentStreamReader.from(instance.getBody());
          Envelope<XmlTask> env = Envelope.deserialize(reader, new PostTaskFactory());
          XmlTask task = env.getBody().getBodyContent();
          task.setHandle(handle);
          task.setRemoteHandle(remoteHandle);
          return task;
        }
      } catch (JAXBException | InterruptedException | ExecutionException | TimeoutException | XmlException e) {
        throw new RuntimeException(e);
      }
      return null;
    }

    @Override
    public void postCreate(DBTransaction connection, XmlTask element) throws SQLException {
      try(PreparedStatement statement = connection.prepareStatement(QUERY_GET_DATA_FOR_TASK)) {
        statement.setLong(1, element.getHandle());
        if(statement.execute()) {
          try (ResultSet resultset = statement.getResultSet()) {
            while(resultset.next()) {
              String name = resultset.getString(1);
              XmlItem item = element.getItem(name);
              if (item!=null) {
                item.setValue(resultset.getString(2));
              }
            }
          }
        }
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(XmlTask object) {
      return getHandleCondition(object.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement statement, XmlTask object, int offset) throws SQLException {
      return setHandleParams(statement, object.getHandle(), offset);
    }

    @Override
    public XmlTask asInstance(Object o) {
      if (o instanceof XmlTask) {
        return (XmlTask) o;
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
    public int setStoreParams(PreparedStatement statement, XmlTask element, int offset) throws SQLException {
      statement.setLong(offset, element.getRemoteHandle());
      return 1;
    }

    @Override
    public void postStore(DBTransaction connection, long handle, XmlTask oldValue, XmlTask newValue) throws SQLException {
      try (PreparedStatement statement = connection.prepareStatement("INSERT INTO `"+TABLEDATA+"` (`"+COL_HANDLE+"`, `"+COL_NAME+"`, `"+COL_DATA+"`) VALUES ( ?, ?, ? ) ON DUPLICATE KEY UPDATE `"+COL_DATA+"`= VALUES(`"+COL_DATA+"`);")) {
        final List<XmlItem> items = newValue.getItems();
        for(XmlItem item:items) {
          if (item!=null && item.getName()!=null) {
            XmlItem oldItem = oldValue==null ? null : oldValue.getItem(item.getName());
            if (oldItem==null ||
                oldItem.getValue()==null ||
                (! oldItem.getValue().equals(item.getValue()))) {
              if (! ((oldItem==null || oldItem.getValue()==null) && item.getValue()==null)) {
                statement.setLong(1, handle);
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
    public CharSequence getHandleCondition(long element) {
      return COL_HANDLE+" = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement statement, long handle, int offset) throws SQLException {
      statement.setLong(offset, handle);
      return 1;
    }

    @Override
    public void preClear(DBTransaction connection) throws SQLException {
      CharSequence filter = getFilterExpression();
      final String sql;
      if (filter==null) {
        sql = "DELETE FROM "+TABLEDATA;
      } else {
        sql= "DELETE FROM `"+TABLEDATA+"` WHERE `"+COL_HANDLE+"` IN (SELECT `"+COL_HANDLE+"` FROM `"+TABLE+"` WHERE "+filter+");";
      }
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        setFilterParams(statement, 1);
        statement.execute();
      }
    }

    @Override
    public void preRemove(DBTransaction connection, long handle) throws SQLException {
      final String sql = "DELETE FROM "+TABLEDATA+" WHERE `"+COL_HANDLE+"` = ?";
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, handle);
        statement.execute();
      }
    }

    @Override
    public void preRemove(DBTransaction connection, XmlTask element) throws SQLException {
      preRemove(connection, element.getHandle());
    }

    @Override
    public void preRemove(DBTransaction connection, ResultSet elementSource) throws SQLException {
      preRemove(connection, elementSource.getLong(mColNoHandle));
    }

  }

  public UserTaskMap(TransactionFactory<? extends DBTransaction> connectionProvider) {
    super(connectionProvider, new UserTaskFactory());
  }

}
