package nl.adaptivity.process.engine.processModel;

import java.util.Collection;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.InternalMessage;
import nl.adaptivity.process.engine.Payload;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;


public class ProcessNodeInstance implements Task{

  private final ProcessNode aNode;
  private final InternalMessage aMessage;
  private Payload aPayload;
  private final Collection<ProcessNodeInstance> aPredecessors;

  private TaskState aState=null;

  public ProcessNodeInstance(ProcessNode pNode, InternalMessage pMessage, Collection<ProcessNodeInstance> pPredecessor) {
    super();
    aNode = pNode;
    aMessage = pMessage;
    aPredecessors = pPredecessor;
    if (aPredecessors==null && ! (pNode instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  public ProcessNode getNode() {
    return aNode;
  }

  public InternalMessage getMessage() {
    return aMessage;
  }

  public Payload getPayload() {
    return aPayload;
  }

  protected Collection<ProcessNodeInstance> getPredecessors() {
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
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");

  }

  @Override
  public long getHandle() {
    // TODO Auto-generated method stub
    // return 0;
    throw new UnsupportedOperationException("Not yet implemented");

  }

  public <T> boolean provideTask(IMessageService<T> pMessageService) {
    setState(TaskState.Available);
    return aNode.provideTask(pMessageService, this);
  }

  public <T> boolean takeTask(IMessageService<T> pMessageService) {
    setState(TaskState.Taken);
    return aNode.takeTask(pMessageService, this);
  }

  public <T> boolean startTask(IMessageService<T> pMessageService) {
    setState(TaskState.Started);
    return aNode.startTask(pMessageService, this);
  }

  public void finishTask(Object pPayload) {
    aPayload = new Payload(pPayload);
  }

  @Override
  public void failTask() {
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");

  }

  @Override
  public String toString() {
    return aNode.getClass().getSimpleName()+" ("+aState+")";
  }

}
