package nl.adaptivity.process.engine;

import java.io.IOException;
import java.io.Reader;
import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.StringCache;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.process.processModel.XmlProcessModel;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.util.activation.Sources;


public class ProcessModelMap extends CachingDBHandleMap<ProcessModelImpl> {


  private static final String TABLE = "processmodels";

  private static final String COL_HANDLE = "pmhandle";

  private static final String COL_OWNER = "owner";

  private static final String COL_MODEL = "model";

  static class ProcessModelFactory extends AbstractElementFactory<ProcessModelImpl> {

    private static boolean _supports_set_character_stream = true;
    private int aColNoOwner;
    private int aColNoModel;
    private int aColNoHandle;
    private final StringCache aStringCache;

    ProcessModelFactory(StringCache stringCache) {
      aStringCache = stringCache;
    }

    @Override
    public CharSequence getTableName() {
      return TABLE;
    }

    @Override
    public String getFilterExpression() {
      // TODO implement more carefully
      return null;
//      if (aFilterUser==null) {
//        return null;
//      } else {
//
//      }
    }

    @Override
    public int setFilterParams(PreparedStatement statement, int offset) {
      // No where clauses are used.
      return 0;
    }

    @Override
    public void initResultSet(ResultSetMetaData metaData) throws SQLException {
      final int columnCount = metaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = metaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          aColNoHandle = i;
        } else if (COL_OWNER.equals(colName)) {
          aColNoOwner = i;
        } else if (COL_MODEL.equals(colName)) {
          aColNoModel = i;
        } // ignore other columns
      }
    }

    @Override
    public ProcessModelImpl create(DBTransaction connection, ResultSet row) throws SQLException {
      Principal owner = new SimplePrincipal(aStringCache.lookup(row.getString(aColNoOwner)));
      try(Reader modelReader = row.getCharacterStream(aColNoModel)) {
        long handle = row.getLong(aColNoHandle);

        XmlProcessModel xmlModel = JAXB.unmarshal(modelReader, XmlProcessModel.class);
        ProcessModelImpl result = xmlModel.toProcessModel();

        result.setHandle(handle);
        result.cacheStrings(aStringCache);
        if (result.getOwner()==null) {
          result.setOwner(owner);
        }
        return result;
      } catch (IOException e) {
        throw new SQLException(e);
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessModelImpl object) {
      return getHandleCondition(object.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement statement, ProcessModelImpl element, int offset) throws SQLException {
      return setHandleParams(statement, element.getHandle(), offset);
    }

    @Override
    public ProcessModelImpl asInstance(Object object) {
      if (object instanceof ProcessModelImpl) {
        return (ProcessModelImpl) object;
      } else {
        return null;
      }
    }

    @Override
    public CharSequence getCreateColumns() {
      return COL_HANDLE+", "+COL_OWNER + ", " + COL_MODEL;
    }

    @Override
    public List<CharSequence> getStoreParamHolders() {
      return Arrays.<CharSequence>asList("?","?");
    }

    @Override
    public List<CharSequence> getStoreColumns() {
      return Arrays.<CharSequence>asList(COL_OWNER,COL_MODEL);
    }

    @Override
    public int setStoreParams(PreparedStatement statement, ProcessModelImpl element, int offset) throws SQLException {
      statement.setString(offset, element.getOwner().getName());

      JAXBSource jbs;
      try {
        JAXBContext jbc = JAXBContext.newInstance(XmlProcessModel.class);
        XmlProcessModel xmlModel = new XmlProcessModel(element);
        jbs = new JAXBSource(jbc, xmlModel);
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      }
      if (_supports_set_character_stream) {
        try {
          statement.setCharacterStream(offset + 1, Sources.toReader(jbs));
          return 2;
        } catch (AbstractMethodError|UnsupportedOperationException e) {
          _supports_set_character_stream =false;
        }
      }
      statement.setString(offset + 1, Sources.toString(jbs));
      return 2;
    }

    @Override
    public CharSequence getHandleCondition(long handle) {
      return COL_HANDLE + " = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement statement, long handle, int offset) throws SQLException {
      statement.setLong(offset, handle);
      return 1;
    }

    @Override
    public void preRemove(DBTransaction connection, ResultSet elementSource) throws SQLException {
      // Ignore. Don't even use the default implementation
    }

  }

  public ProcessModelMap(TransactionFactory<?> transactionFactory, StringCache stringCache) {
    super(transactionFactory, new ProcessModelFactory(stringCache));
  }

}
