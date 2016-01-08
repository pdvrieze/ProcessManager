package net.devrieze.util.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import net.devrieze.util.db.DBHandleMap.HMElementFactory;


/**
 * Abstract baseclass for {@link ElementFactory} and {@link HMElementFactory}.
 * This class provides implementations that allow optional extensions for
 * complex datatypes. Simple types do not need factories that override these.
 *
 * @author Paul de Vrieze
 * @param <T> Type of the element created / handled
 */
public abstract class AbstractElementFactory<T> implements HMElementFactory<T> {

  @Override
  public CharSequence getFilterExpression() {
    return null;
  }

  @Override
  public int setFilterParams(PreparedStatement pStatement, int pOffset) throws SQLException {
    return 0;
  }

  @Override
  public void initResultSet(ResultSetMetaData pMetaData) throws SQLException {
    // Do nothing.
  }

  @Override
  public void postCreate(DBTransaction pConnection, T pElement) throws SQLException {
    // Do nothing.
  }

  @Override
  public void postStore(DBTransaction pConnection, long pHandle, T pOldValue, T pElement) throws SQLException {
    // Simple case, do nothing
  }

  @Override
  public void preRemove(DBTransaction pConnection, long pHandle) throws SQLException {
    // Don't do anything
  }

  @Override
  public void preRemove(DBTransaction pConnection, T pElement) throws SQLException {
    // Don't do anything
  }

  /**
   * Default implementation that just creates the element.
   */
  @Override
  public void preRemove(DBTransaction pConnection, ResultSet pElementSource) throws SQLException {
    preRemove(pConnection, create(pConnection, pElementSource));
  }

  @Override
  public void preClear(DBTransaction pConnection) throws SQLException {
    // Don't do anything
  }

}
