package nl.adaptivity.process.exec;

import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Node;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.process.IMessageService;

public interface Task<V extends Task<V>> extends HandleAware<V>{

  @XmlRootElement(name="taskState", namespace="http://adaptivity.nl/userMessageHandler")
  public static enum TaskState {
    Available,
    Taken,
    Started,
    Complete,
    Failed, 
    Cancelled;
  }

  public TaskState getState();

  public void setState(TaskState aNewState);

  public <T> boolean provideTask(IMessageService<T, V> pMessageService);

  public <T> boolean takeTask(IMessageService<T, V> pMessageService);

  public <T> boolean startTask(IMessageService<T, V> pMessageService);

  public void finishTask(Node pPayload);

  public void failTask();

}
