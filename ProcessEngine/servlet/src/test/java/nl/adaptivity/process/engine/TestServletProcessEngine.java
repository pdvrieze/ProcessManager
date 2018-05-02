/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine;

import net.devrieze.util.security.SecureObject;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.MemTransactionedHandleMap;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.engine.servlet.ServletProcessEngine;
import org.jetbrains.annotations.NotNull;

import java.net.URI;


/**
 * Created by pdvrieze on 09/12/15.
 */
public class TestServletProcessEngine extends ServletProcessEngine {

    private final MemProcessModelMap                                                               mProcessModels;
    private final MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction> mProcessInstances;
    private final MemTransactionedHandleMap<SecureObject<ProcessNodeInstance<?>>, StubProcessTransaction>
                                                                                                   mProcessNodeInstances;
    private       ProcessTransactionFactory<StubProcessTransaction>                                mTransactionFactory;

    // Object Initialization
    public TestServletProcessEngine(final EndpointDescriptorImpl localURL) {
        mTransactionFactory = new ProcessTransactionFactory<StubProcessTransaction>() {
            @NotNull
            @Override
            public StubProcessTransaction startTransaction(
                @NotNull final IProcessEngineData<StubProcessTransaction> engineData) {
                return new StubProcessTransaction(engineData);
            }
        };
        mProcessModels = new MemProcessModelMap();
        mProcessInstances = new MemTransactionedHandleMap<>();
        mProcessNodeInstances = new MemTransactionedHandleMap<>();
        MessageService messageService = new MessageService(localURL);
        ProcessEngine<StubProcessTransaction> engine =
            ProcessEngine.newTestInstance(messageService, mTransactionFactory, mProcessModels, mProcessInstances,
                                          mProcessNodeInstances, false);
        init(engine);
    }
// Object Initialization end

    public void reset() {
        mProcessInstances.reset();
        ;
        mProcessModels.reset();
        mProcessNodeInstances.reset();
    }

    // Property accessors start
    public ProcessTransactionFactory<StubProcessTransaction> getTransactionFactory() {
        return mTransactionFactory;
    }

    @Override
    public void setLocalEndpoint(final URI localURL) {
        super.setLocalEndpoint(localURL);
    }
// Property acccessors end
}
