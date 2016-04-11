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

import net.devrieze.util.TransactionFactory;
import nl.adaptivity.process.MemTransactionedHandleMap;
import nl.adaptivity.process.StubTransaction;
import nl.adaptivity.process.StubTransactionFactory;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.engine.servlet.ServletProcessEngine;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;

import java.net.URI;


/**
 * Created by pdvrieze on 09/12/15.
 */
public class TestServletProcessEngine extends ServletProcessEngine {

  private final MemTransactionedHandleMap<ProcessModelImpl> mProcessModels;
  private final MemTransactionedHandleMap<ProcessInstance> mProcessInstances;
  private final MemTransactionedHandleMap<ProcessNodeInstance> mProcessNodeInstances;
  private TransactionFactory mTransactionFactory;

  public TestServletProcessEngine() {
    mTransactionFactory = new StubTransactionFactory();
    mProcessModels = new MemTransactionedHandleMap<>();
    mProcessInstances = new MemTransactionedHandleMap<>();
    mProcessNodeInstances = new MemTransactionedHandleMap<>();
    ProcessEngine<StubTransaction> engine = ProcessEngine.newTestInstance(this, mTransactionFactory, mProcessModels, mProcessInstances, mProcessNodeInstances);
    init(engine);
  }

  public void reset() {
    mProcessInstances.reset();;
    mProcessModels.reset();
    mProcessNodeInstances.reset();
  }

  public TransactionFactory getTransactionFactory() {
    return mTransactionFactory;
  }

  @Override
  public void setLocalEndpoint(final URI localURL) {
    super.setLocalEndpoint(localURL);
  }
}
