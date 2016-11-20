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

import net.devrieze.util.Handle;
import nl.adaptivity.process.MemTransactionedHandleMap;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


/**
 * Created by pdvrieze on 07/05/16.
 */
public class MemProcessModelMap extends MemTransactionedHandleMap<ProcessModelImpl, StubProcessTransaction> implements IMutableProcessModelMap<StubProcessTransaction> {

  public MemProcessModelMap() {
    super();
  }

  @Override
  public Handle<ProcessModelImpl> getModelWithUuid(final StubProcessTransaction transaction, final UUID uuid) {
    for(ProcessModelImpl candidate:this) {
      if (uuid.equals(candidate.getUuid())) {
        return candidate.getHandle();
      }
    }
    return null;
  }


  @NotNull
  @Override
  public IMutableProcessModelMapAccess withTransaction(@NotNull final StubProcessTransaction transaction) {
    return IProcessModelMapKt.defaultWithTransaction(this, transaction);
  }

}
