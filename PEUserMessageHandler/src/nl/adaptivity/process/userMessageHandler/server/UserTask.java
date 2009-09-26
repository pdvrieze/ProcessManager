package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.exec.Task.TaskState;


public interface UserTask  extends HandleAware<Task>{

  public TaskState getState();

  public void setState(TaskState aNewState);

}
