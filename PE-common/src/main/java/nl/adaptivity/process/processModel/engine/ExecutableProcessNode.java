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

package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.processModel.XmlResultType;

import java.sql.SQLException;
import java.util.List;


/**
 * Created by pdvrieze on 23/11/15.
 */
public interface ExecutableProcessNode extends ProcessNode<ExecutableProcessNode, ProcessModelImpl> {

  /**
   * Should this node be able to be provided?
   *
   *
   * @param transaction
   * @param instance The instance against which the condition should be evaluated.
   * @return <code>true</code> if the node can be started, <code>false</code> if
   *         not.
   */
  <T extends Transaction> boolean condition(T transaction, IProcessNodeInstance<T, ?> instance);

  /**
   * Take action to make task available
   *
   *
   * @param transaction
   * @param messageService The message service to use for the communication.
   * @param instance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically taken
   */
  <V, T extends Transaction, U extends IProcessNodeInstance<T, U>> boolean provideTask(T transaction, IMessageService<V, T, U> messageService, U instance) throws SQLException;

  /**
   * Take action to accept the task (but not start it yet)
   *
   * @param messageService The message service to use for the communication.
   * @param instance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically started
   */
  <V, T extends Transaction, U extends IProcessNodeInstance<T, U>> boolean takeTask(IMessageService<V, T, U> messageService, U instance);

  <V, T extends Transaction, U extends IProcessNodeInstance<T, U>> boolean startTask(IMessageService<V, T, U> messageService, U instance);


  List<? extends XmlResultType> getResults();

  List<? extends XmlDefineType> getDefines();

  void setId(String s);
}
