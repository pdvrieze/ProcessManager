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

package nl.adaptivity.process.engine.processModel;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.Transaction;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessException;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.process.util.Identifiable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;


public class JoinInstance<T extends Transaction> extends ProcessNodeInstance<T> {

  public JoinInstance(final JoinImpl node, final Collection<? extends Handle<? extends ProcessNodeInstance<?>>> predecessors, final ProcessInstance processInstance, final NodeInstanceState state) {
    super(node, predecessors, processInstance, state);
  }

  public JoinInstance(T transaction, final JoinImpl node, final Collection<? extends Handle<? extends ProcessNodeInstance<?>>> predecessors, final ProcessInstance processInstance) throws SQLException {
    super(node, predecessors, processInstance);
  }

  /**
   * Constructor for ProcessNodeInstanceMap.
   * @param node
   * @param processInstance
   */
  JoinInstance(Transaction transaction, JoinImpl node, ProcessInstance processInstance, NodeInstanceState state) throws SQLException {
    super(transaction, node, processInstance, state);
  }

  @Override
  public JoinImpl getNode() {
    return (JoinImpl) super.getNode();
  }

  public boolean addPredecessor(T transaction, final ProcessNodeInstance predecessor) throws SQLException {
    if (canAddNode(transaction) && getDirectPredecessors().add(predecessor)) {
      getProcessInstance().getEngine().updateStorage(transaction, this);
      return true;
    }
    return false;
  }

  public boolean isFinished() {
    return (getState() == NodeInstanceState.Complete) || (getState() == NodeInstanceState.Failed);
  }

  @Override
  public <V> boolean startTask(T transaction, final IMessageService<V, T, ProcessNodeInstance<T>> messageService) throws SQLException {
    if (getNode().startTask(messageService, this)) {
      return updateTaskState(transaction);
    }
    return false;
  }

  /**
   * Update the state of the task, based on the predecessors
   * @param transaction The transaction to use for the operations.
   * @return <code>true</code> if the task is complete, <code>false</code> if not.
   * @throws SQLException
   */
  private boolean updateTaskState(final T transaction) throws SQLException {
    if (getState()==NodeInstanceState.Complete) return false; // Don't update if we're already complete

    final JoinImpl join                      = getNode();
    int            totalPossiblePredecessors = join.getPredecessors().size();
    int            realizedPredecessors      = getDirectPredecessors().size();

    if (realizedPredecessors == totalPossiblePredecessors) { // Did we receive all possible predecessors
      return true;
    }

    int complete =0;
    int skipped = 0;
    for (final ProcessNodeInstance predecessor : getDirectPredecessors(transaction)) {
      switch(predecessor.getState()) {
        case Complete:
          complete +=1;
          break;
        case Cancelled:
        case Failed:
          skipped +=1;
          break;
        default:
          // do nothing
      }
    }
    if (totalPossiblePredecessors-skipped<join.getMin()) {
      failTask(transaction, new ProcessException("Too many predecessors have failed"));
      cancelNoncompletedPredecessors(transaction);
      return false;
    }

    if (complete >= join.getMin()) {
      if (complete >= join.getMax()) {
        return true;
      }
        // XXX todo if we skipped/failed too many predecessors to ever be able to finish,
      return getProcessInstance().getActivePredecessorsFor(transaction, join).size() == 0;
    }
    return false;
  }

  private void cancelNoncompletedPredecessors(final T transaction) throws SQLException {
    Collection<ProcessNodeInstance<T>> preds = getProcessInstance().getActivePredecessorsFor(transaction, getNode());
    for (ProcessNodeInstance<T> pred: preds) {
      pred.tryCancelTask(transaction);
    }
  }

  @Override
  public <V> boolean provideTask(T transaction, final IMessageService<V, T, ProcessNodeInstance<T>> messageService) throws SQLException {
    if (!isFinished()) {
      return getNode().provideTask(transaction, messageService, this);
    }
    final Collection<? extends Handle<? extends ProcessNodeInstance>> directSuccessors = getProcessInstance().getDirectSuccessors(transaction, this);
    boolean canAdd = false;
    for (final Handle<? extends ProcessNodeInstance> hDirectSuccessor : directSuccessors) {
      ProcessNodeInstance directSuccessor = getProcessInstance().getEngine().getNodeInstance(transaction, hDirectSuccessor, SecurityProvider.SYSTEMPRINCIPAL);
      if ((directSuccessor.getState() == NodeInstanceState.Started) || (directSuccessor.getState() == NodeInstanceState.Complete)) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

  @Override
  public void tickle(final T transaction, final IMessageService<?, T, ProcessNodeInstance<T>> messageService) throws SQLException {
    super.tickle(transaction, messageService);
    Set<Identifiable> missingIdentifiers = new TreeSet<>();
    missingIdentifiers.addAll(getNode().getPredecessors());
    for (Handle<? extends ProcessNodeInstance<?>> predDef : getDirectPredecessors()) {
      ProcessNodeInstance<T> pred = getProcessInstance().getEngine()
                                                     .getNodeInstance(transaction, predDef, SecurityProvider.SYSTEMPRINCIPAL);
      missingIdentifiers.remove(pred.getNode());
    }
    for (Identifiable missingIdentifier: missingIdentifiers) {
      ProcessNodeInstance<T> candidate = getProcessInstance().getNodeInstance(transaction, missingIdentifier);
      if (candidate!=null) {
        addPredecessor(transaction, candidate);
      }
    }
    if(updateTaskState(transaction) && getState()!=NodeInstanceState.Complete) {
      getProcessInstance().finishTask(transaction, messageService, this, null);
    }
  }

  private boolean canAddNode(T transaction) throws SQLException {
    if (!isFinished()) {
      return true;
    }
    final Collection<? extends Handle<? extends ProcessNodeInstance>> directSuccessors = getProcessInstance().getDirectSuccessors(transaction, this);
    boolean canAdd = false;
    for (final Handle<? extends ProcessNodeInstance> hDirectSuccessor : directSuccessors) {
      ProcessNodeInstance directSuccessor = getProcessInstance().getEngine().getNodeInstance(transaction, hDirectSuccessor, SecurityProvider.SYSTEMPRINCIPAL);
      if ((directSuccessor.getState() == NodeInstanceState.Started) || (directSuccessor.getState() == NodeInstanceState.Complete)) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

}
