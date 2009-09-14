package nl.adaptivity.process.userMessageHandler.server;

import java.util.Arrays;
import java.util.Collection;

import nl.adaptivity.jbi.components.genericSE.EndpointProvider;
import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.exec.Task.TaskState;



public class UserMessageService implements EndpointProvider {

  private InternalEndpoint internalEndpoint;
  private ExternalEndpoint externalEndpoint;

  private Collection<Task> tasks;
  
  @Override
  public Collection<GenericEndpoint> getEndpoints() {
    return Arrays.asList(internalEndpoint, externalEndpoint);
  }

  public boolean postTask(Task pTask) {
    return tasks.add(pTask);
  }

  public Collection<Task> getPendingTasks() {
    return tasks;
  }

  public TaskState finishTask(long pHandle) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  public TaskState takeTask(long pHandle) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

}
