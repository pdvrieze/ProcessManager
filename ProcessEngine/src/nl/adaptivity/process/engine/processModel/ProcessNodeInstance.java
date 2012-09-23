package nl.adaptivity.process.engine.processModel;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Node;

import net.devrieze.util.security.SecureObject;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;


public class ProcessNodeInstance implements Task<ProcessNodeInstance>, SecureObject{

  private final ProcessNode aNode;
  private Node aPayload;
  private final Collection<ProcessNodeInstance> aPredecessors;

  private TaskState aState=null;
  private long aHandle = -1;
  private final ProcessInstance aProcessInstance;
  private Throwable aFailureCause;

  public ProcessNodeInstance(ProcessNode pNode, ProcessNodeInstance pPredecessor, ProcessInstance pProcessInstance) {
    super();
    aNode = pNode;
    aPredecessors = Collections.singletonList(pPredecessor);
    aProcessInstance = pProcessInstance;
    if (aPredecessors==null && ! (pNode instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  protected ProcessNodeInstance(ProcessNode pNode, Collection<ProcessNodeInstance> pPredecessors, ProcessInstance pProcessInstance) {
    super();
    aNode = pNode;
    aPredecessors = pPredecessors;
    aProcessInstance = pProcessInstance;
    if (aPredecessors==null && ! (pNode instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  public ProcessNode getNode() {
    return aNode;
  }

  public Node getPayload() {
    return aPayload;
  }

  public Collection<ProcessNodeInstance> getDirectPredecessors() {
    return aPredecessors;
  }

  @Override
  public TaskState getState() {
    return aState;
  }

  @Override
  public void setState(TaskState pNewState) {
    if (aState != null && aState.compareTo(pNewState)>0) {
      throw new IllegalArgumentException("State can only be increased (was:"+aState+" new:"+pNewState);
    }
    aState = pNewState;
  }

  @Override
  public void setHandle(long pHandle) {
    aHandle = pHandle;
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  public <T> boolean provideTask(IMessageService<T, ProcessNodeInstance> pMessageService) {
    setState(TaskState.Sent);
    return aNode.provideTask(pMessageService, this);
  }

  public <T> boolean takeTask(IMessageService<T, ProcessNodeInstance> pMessageService) {
    setState(TaskState.Taken);
    return aNode.takeTask(pMessageService, this);
  }

  public <T> boolean startTask(IMessageService<T, ProcessNodeInstance> pMessageService) {
    setState(TaskState.Started);
    return aNode.startTask(pMessageService, this);
  }

  public void finishTask(Node pPayload) {
    setState(TaskState.Complete);
    aPayload = pPayload;
  }

  @Override
  public void cancelTask() {
    setState(TaskState.Cancelled);
  }

  @Override
  public String toString() {
    return aNode.getClass().getSimpleName()+" ("+aState+")";
  }

  public ProcessInstance getProcessInstance() {
    return aProcessInstance;
  }

  @Override
  public void failTask(Throwable pCause) {
    setState(TaskState.Failed);
    aFailureCause = pCause;
  }

}
