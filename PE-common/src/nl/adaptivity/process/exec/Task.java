package nl.adaptivity.process.exec;

import net.devrieze.util.HandleMap.HandleAware;

public interface Task extends HandleAware{
  
  
  public static enum TaskState {
    Available,
    Taken,
    Started,
    Complete;
  }

  public TaskState getState();
  
  public void setState(TaskState aNewState);

}
