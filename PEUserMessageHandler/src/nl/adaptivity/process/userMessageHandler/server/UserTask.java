package nl.adaptivity.process.userMessageHandler.server;

import javax.jbi.component.ComponentContext;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.jbi.util.EndPointDescriptor;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.exec.Task.TaskState;


public interface UserTask  extends HandleAware<Task>{

  public TaskState getState();

  public void setState(TaskState aNewState);

  public void setEndpoint(EndPointDescriptor pEndPoint);

  void setContext(ComponentContext pContext);

  ComponentContext getContext();

}
