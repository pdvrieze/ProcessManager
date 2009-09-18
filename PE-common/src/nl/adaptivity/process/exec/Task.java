package nl.adaptivity.process.exec;

import javax.xml.bind.annotation.XmlRootElement;

import net.devrieze.util.HandleMap.HandleAware;

public interface Task extends HandleAware{
  
  @XmlRootElement(name="taskState", namespace="http:://adaptivity.nl/userMessageHandler")
  public static enum TaskState {
    Available,
    Taken,
    Started,
    Complete;
  }

  public TaskState getState();
  
  public void setState(TaskState aNewState);

}
