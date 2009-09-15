package nl.adaptivity.process.exec;

import net.devrieze.util.HandleMap.HandleAware;

public interface Task extends HandleAware{
  
  
  public enum TaskState {
    Available,
    Taken,
    Complete;
  }

  public TaskState getState();
  
  public void setState(TaskState aNewState);

}
