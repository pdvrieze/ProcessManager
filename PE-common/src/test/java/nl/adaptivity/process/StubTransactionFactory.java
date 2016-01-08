/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

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
