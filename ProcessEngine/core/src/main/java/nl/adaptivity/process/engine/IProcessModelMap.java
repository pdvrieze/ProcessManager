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

package nl.adaptivity.process.engine;

import net.devrieze.util.Transaction;
import net.devrieze.util.TransactionedHandleMap;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;

import java.sql.SQLException;
import java.util.UUID;


/**
 * Created by pdvrieze on 07/05/16.
 */
public interface IProcessModelMap<T extends Transaction> extends TransactionedHandleMap<ProcessModelImpl, T> {

  Handle<ProcessModelImpl> getModelWithUuid(final T transaction, final UUID uuid) throws SQLException;
}
