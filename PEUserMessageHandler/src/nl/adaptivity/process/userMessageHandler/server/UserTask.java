package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.jbi.util.EndPointDescriptor;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.messaging.AsyncMessenger;


public interface UserTask <T extends UserTask<T>> extends HandleAware<T>{

  public TaskState getState();

  public void setState(TaskState aNewState);

  public void setEndpoint(EndPointDescriptor pEndPoint);

  void setContext(AsyncMessenger pContext);

  AsyncMessenger getContext();

}
