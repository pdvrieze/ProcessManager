package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.HandleMap.HandleAware;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;

import java.security.Principal;
import java.util.List;


public interface UserTask<T extends UserTask<T>> extends HandleAware<T> {


  interface TaskItem {

    List<String> getOptions();

    String getValue();

    String getType();

    String getName();

    String getParams();

    String getLabel();

  }

  TaskState getState();

  void setState(TaskState newState, Principal user);

  void setEndpoint(EndpointDescriptorImpl endPoint);

  Principal getOwner();

  List<? extends TaskItem> getItems();

  void setItems(List<? extends TaskItem> items);

  long getRemoteHandle();

  long getInstanceHandle();

  String getSummary();

}
