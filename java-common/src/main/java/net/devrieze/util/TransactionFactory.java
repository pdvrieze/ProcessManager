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
