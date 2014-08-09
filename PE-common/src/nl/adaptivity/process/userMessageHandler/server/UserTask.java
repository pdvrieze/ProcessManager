package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;
import java.util.List;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;


public interface UserTask<T extends UserTask<T>> extends HandleAware<T> {


  public interface TaskItem {

    public abstract List<String> getOptions();

    public abstract String getValue();

    public abstract String getType();

    public abstract String getName();

    public abstract String getParams();

    public abstract String getLabel();

  }

  public TaskState getState();

  public void setState(TaskState aNewState, Principal pUser);

  public void setEndpoint(EndpointDescriptorImpl pEndPoint);

  public Principal getOwner();

  public List<? extends TaskItem> getItems();

  public void setItems(List<? extends TaskItem> pItems);

  public long getRemoteHandle();

  public long getInstanceHandle();

  public String getSummary();

}
