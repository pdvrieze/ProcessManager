package nl.adaptivity.process;

import net.devrieze.util.Transaction;

import java.sql.Connection;
import java.sql.SQLException;


/**
 * Created by pdvrieze on 09/12/15.
 */
public class StubTransactionFactory implements net.devrieze.util.TransactionFactory {

  private StubTransaction mTransaction = new StubTransaction();

  @Override
  public StubTransaction startTransaction() {
    return mTransaction;
  }


  @Override
  public Connection getConnection() throws SQLException {
    throw new UnsupportedOperationException("No connections in the stub");
  }

  @Override
  public boolean isValidTransaction(final Transaction transaction) {
    return mTransaction == transaction;
  }
}
