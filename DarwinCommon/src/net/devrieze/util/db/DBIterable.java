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

package net.devrieze.util.db;

import net.devrieze.util.AutoCloseableIterator;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;


public interface DBIterable<T> extends AutoCloseable {

  @NotNull
  Iterable<T> all();

  @NotNull
  AutoCloseableIterator<T> iterator();
  
  @Override
  void close() throws SQLException;
}
