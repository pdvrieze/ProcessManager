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

import net.devrieze.util.OldCachingHandleMap;
import net.devrieze.util.Handle;
import net.devrieze.util.Transaction;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;

import java.sql.SQLException;
import java.util.UUID;


/**
 * Extension to cachingHandleMap that handles the uuids needed for process models.
 * Created by pdvrieze on 20/05/16.
 */
public class CachingProcessModelMap<T extends Transaction> extends OldCachingHandleMap<ProcessModelImpl, T> implements IProcessModelMap<T> {

  public CachingProcessModelMap(final IProcessModelMap<T> base, final int cacheSize) {
    super(base, cacheSize);
  }

  @Override
  protected IProcessModelMap<T> getDelegate() {
    return (IProcessModelMap<T>) super.getDelegate();
  }

  @Override
  public Handle<? extends ProcessModelImpl> getModelWithUuid(final T transaction, final UUID uuid) throws SQLException {
    Handle<? extends ProcessModelImpl> modelWithUuid = getDelegate().getModelWithUuid(transaction, uuid);
    if (modelWithUuid instanceof ProcessModelImpl) {
      putCache(transaction, (ProcessModelImpl) modelWithUuid);
    }
    return modelWithUuid;
  }

}
