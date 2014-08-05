package nl.adaptivity.process.engine.processModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.security.SecureObject;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;

import org.w3c.dom.Node;


public class ProcessNodeInstance implements IProcessNodeInstance<ProcessNodeInstance>, SecureObject {

  private final ProcessNodeImpl aNode;

  private List<ProcessData> aResult = new ArrayList<>();

  private Collection<Handle<? extends ProcessNodeInstance>> aPredecessors;

  private TaskState aState = TaskState.Pending;

  private long aHandle = -1;

  private final ProcessInstance aProcessInstance;

  private Throwable aFailureCause;

  public ProcessNodeInstance(final ProcessNodeImpl pNode, final Handle<? extends ProcessNodeInstance> pPredecessor, final ProcessInstance pProcessInstance) {
    super();
    aNode = pNode;
    if (pPredecessor==null) {
      if (pNode instanceof StartNode) {
        aPredecessors = Collections.emptyList();
      } else {
        throw new NullPointerException("Nodes that are not startNodes need predecessors");
      }
    } else {
      aPredecessors = Collections.<Handle<? extends ProcessNodeInstance>>singletonList(pPredecessor);
    }
    aProcessInstance = pProcessInstance;
    if ((pPredecessor == null) && !(pNode instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  protected ProcessNodeInstance(final ProcessNodeImpl pNode, final Collection<? extends Handle<? extends ProcessNodeInstance>> pPredecessors, final ProcessInstance pProcessInstance) {
    super();
    aNode = pNode;
    aPredecessors = new ArrayList<>(pPredecessors);
    aProcessInstance = pProcessInstance;
    if (((aPredecessors == null) || (aPredecessors.size()==0)) && !(pNode instanceof StartNode)) {
      throw new NullPointerException("Non-start-node process node instances need predecessors");
    }
  }

  ProcessNodeInstance(ProcessNodeImpl pNode, ProcessInstance pProcessInstance, TaskState pState) {
    aNode = pNode;
    aProcessInstance = pProcessInstance;
    aState = pState;
  }

  public ProcessNodeImpl getNode() {
    return aNode;
  }

  public List<ProcessData> getResult() {
    return aResult;
  }

  public Collection<Handle<? extends ProcessNodeInstance>> getDirectPredecessors() {
    return aPredecessors;
  }

  public Throwable getFailureCause() {
    return aFailureCause;
  }

  @Override
  public TaskState getState() {
    return aState;
  }

  @Override
  public void setState(final TaskState pNewState) {
    if ((aState != null) && (aState.compareTo(pNewState) > 0)) {
      throw new IllegalArgumentException("State can only be increased (was:" + aState + " new:" + pNewState);
    }
    aState = pNewState;
    aProcessInstance.getEngine().updateStorage(this);
  }

  @Override
  public void setHandle(final long pHandle) {
    aHandle = pHandle;
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public <U> boolean provideTask(final IMessageService<U, ProcessNodeInstance> pMessageService) {
    setState(TaskState.Sent);
    try {
      return aNode.provideTask(pMessageService, this);
    } catch (RuntimeException e) {
      failTask(e);
      throw e;
    }
  }

  @Override
  public <U> boolean takeTask(final IMessageService<U, ProcessNodeInstance> pMessageService) {
    final boolean result = aNode.takeTask(pMessageService, this);
    setState(TaskState.Taken);
    return result;
  }

  @Override
  public <U> boolean startTask(final IMessageService<U, ProcessNodeInstance> pMessageService) {
    final boolean startTask = aNode.startTask(pMessageService, this);
    setState(TaskState.Started);
    return startTask;
  }

  @Override
  public void finishTask(final Node pResultPayload) {
    setState(TaskState.Complete);
    aResult.add(new ProcessData(null, pResultPayload==null ? null : pResultPayload.toString()));
  }

  @Override
  public void cancelTask() {
    setState(TaskState.Cancelled);
  }

  @Override
  public String toString() {
    return aNode.getClass().getSimpleName() + " (" + aState + ")";
  }

  public ProcessInstance getProcessInstance() {
    return aProcessInstance;
  }

  @Override
  public void failTask(final Throwable pCause) {
    setState(TaskState.Failed);
    aFailureCause = pCause;
  }

  /** package internal method for use when retrieving from the database.
   * Note that this method does not store the results into the database.
   * @param pResults the new results.
   */
  void setResult(List<ProcessData> pResults) {
    aResult.clear();
    aResult.addAll(pResults);
  }

}
