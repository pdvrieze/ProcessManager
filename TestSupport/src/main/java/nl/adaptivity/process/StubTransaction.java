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

  @Override
  public void addRollbackHandler(final Runnable runnable) {
    // Do nothing
  }
}
