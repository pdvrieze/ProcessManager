package nl.adaptivity.process;

import net.devrieze.util.Transaction;

import java.sql.SQLException;


/**
 * Created by pdvrieze on 09/12/15.
 */
public class StubTransaction implements Transaction {

  @Override
  public void close() { }

  @Override
  public void commit() throws SQLException { }

  @Override
  public void rollback() throws SQLException {
    System.err.println("Rollback needed (but not supported on the stub");
  }

  @Override
  public <T> T commit(final T value) throws SQLException {
    return value;
  }
}
