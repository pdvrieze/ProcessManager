package net.devrieze.util;

import java.sql.Connection;
import java.sql.SQLException;


/**
 * Created by pdvrieze on 18/08/15.
 */
public interface TransactionFactory<T extends Transaction> {

  T startTransaction();

  @Deprecated
  Connection getConnection() throws SQLException;

  boolean isValidTransaction(Transaction pTransaction);
}
