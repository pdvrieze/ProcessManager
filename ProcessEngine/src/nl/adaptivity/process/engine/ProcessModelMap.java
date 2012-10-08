package nl.adaptivity.process.engine;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.Reader;
import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.bind.JAXB;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.DBHandleMap.ElementFactory;
import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.XmlProcessModel;


public class ProcessModelMap extends CachingDBHandleMap<ProcessModel> implements ElementFactory<ProcessModel> {

  private static final String TABLE = "processmodelss";
  private static final String COL_HANDLE = "pmhandle";
  private static final String COL_OWNER = "owner";
  private static final String COL_MODEL = "model";
  private int aColNoOwner;
  private int aColNoModel;
  private int aColNoHandle;

  public ProcessModelMap(String pResourceName) {
    super(resourceNameToDataSource(pResourceName), null, TABLE, COL_HANDLE);
  }

  private static DataSource resourceNameToDataSource(String pResourceName) {
    try {
      return (DataSource) new InitialContext().lookup(pResourceName);
    } catch (NamingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ProcessModel create(ResultSet pRow) throws SQLException {
    Principal owner = new SimplePrincipal(pRow.getString(aColNoOwner));
    Reader modelReader = pRow.getCharacterStream(aColNoModel);
    long handle = pRow.getLong(aColNoHandle);

    XmlProcessModel xmlModel = JAXB.unmarshal(modelReader, XmlProcessModel.class);
    ProcessModel result = xmlModel.toProcessModel();

    result.setOwner(owner);
    result.setHandle(handle);
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
        } else if (COL_MODEL.equals(colName)) {
          aColNoModel = i;
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
    return Arrays.asList(COL_OWNER, COL_MODEL);
  }

  @Override
  public void insertColumnValues(PreparedStatement pStatement, ProcessModel pElement) throws SQLException {
    pStatement.setString(1, pElement.getOwner().getName());

    // TODO see if this can be done in a streaming way.
    XmlProcessModel xmlModel = new XmlProcessModel(pElement);
    CharArrayWriter out = new CharArrayWriter();
    JAXB.marshal(xmlModel, out);
    pStatement.setCharacterStream(2, new CharArrayReader(out.toCharArray()));
  }

}
