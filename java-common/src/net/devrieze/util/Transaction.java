package net.devrieze.util;

import java.sql.SQLException;


/**
 * Created by pdvrieze on 18/08/15.
 */
public interface Transaction extends AutoCloseable {

  // Don't let transaction close throw exception, only runtime exceptions allowed
  @Override
  void close();

  void commit() throws SQLException;

  void rollback() throws SQLException;

  <T> T commit(T pValue) throws SQLException;
}
