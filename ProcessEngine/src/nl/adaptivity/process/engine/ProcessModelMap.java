package nl.adaptivity.process.engine;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.Reader;
import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.xml.bind.JAXB;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.StringCache;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.db.DbSet;
import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.XmlProcessModel;


public class ProcessModelMap extends CachingDBHandleMap<ProcessModel> {


  private static final String TABLE = "processmodels";

  private static final String COL_HANDLE = "pmhandle";

  private static final String COL_OWNER = "owner";

  private static final String COL_MODEL = "model";

  static class ProcessModelFactory extends AbstractElementFactory<ProcessModel> {

    private int aColNoOwner;
    private int aColNoModel;
    private int aColNoHandle;
    private final StringCache aStringCache;

    ProcessModelFactory(StringCache pStringCache) {
      aStringCache = pStringCache;
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
    public int setFilterParams(PreparedStatement pStatement, int pOffset) {
      throw new UnsupportedOperationException(); // this should not be called
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
        } else if (COL_MODEL.equals(colName)) {
          aColNoModel = i;
        } // ignore other columns
      }
    }

    @Override
    public ProcessModel create(DataSource pConnectionProvider, ResultSet pRow) throws SQLException {
      Principal owner = new SimplePrincipal(aStringCache.lookup(pRow.getString(aColNoOwner)));
      Reader modelReader = pRow.getCharacterStream(aColNoModel);
      long handle = pRow.getLong(aColNoHandle);

      XmlProcessModel xmlModel = JAXB.unmarshal(modelReader, XmlProcessModel.class);
      ProcessModel result = xmlModel.toProcessModel();

      result.setHandle(handle);
      result.cacheStrings(aStringCache);
      result.setOwner(owner);
      return result;
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessModel pObject) {
      return getHandleCondition(pObject.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement pStatement, ProcessModel pElement, int pOffset) throws SQLException {
      return setHandleParams(pStatement, pElement.getHandle(), pOffset);
    }

    @Override
    public ProcessModel asInstance(Object pObject) {
      if (pObject instanceof ProcessModel) {
        return (ProcessModel) pObject;
      } else {
        return null;
      }
    }

    @Override
    public CharSequence getCreateColumns() {
      return COL_HANDLE+", "+COL_OWNER + ", " + COL_MODEL;
    }

    @Override
    public CharSequence getStoreParamHolders() {
      return "?, ?";
    }

    @Override
    public CharSequence getStoreColumns() {
      return COL_OWNER + ", " + COL_MODEL;
    }

    @Override
    public int setStoreParams(PreparedStatement pStatement, ProcessModel pElement, int pOffset) throws SQLException {
      pStatement.setString(pOffset, pElement.getOwner().getName());

      // TODO see if this can be done in a streaming way.
      XmlProcessModel xmlModel = new XmlProcessModel(pElement);
      CharArrayWriter out = new CharArrayWriter();
      JAXB.marshal(xmlModel, out);
      pStatement.setCharacterStream(pOffset+1, new CharArrayReader(out.toCharArray()));
      return 2;
    }

    @Override
    public CharSequence getHandleCondition(long pHandle) {
      return aColNoHandle + " = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement pStatement, long pHandle, int pOffset) throws SQLException {
      pStatement.setLong(pOffset, pHandle);
      return 1;
    }

  }

  public ProcessModelMap(String pResourceName, StringCache pStringCache) {
    super(DbSet.resourceNameToDataSource(pResourceName), new ProcessModelFactory(pStringCache));
  }

}
