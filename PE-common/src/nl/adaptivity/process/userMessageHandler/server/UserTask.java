package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.messaging.EndPointDescriptor;
import nl.adaptivity.process.exec.Task.TaskState;


public interface UserTask<T extends UserTask<T>> extends HandleAware<T> {

  public TaskState getState();

  public void setState(TaskState aNewState, Principal pUser);

  public void setEndpoint(EndPointDescriptor pEndPoint);

  public Principal getOwner();

}
