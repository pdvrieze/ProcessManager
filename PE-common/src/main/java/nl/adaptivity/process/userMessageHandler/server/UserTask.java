package nl.adaptivity.process.userMessageHandler.server;

import java.security.Principal;
import java.util.List;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;


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

  void setState(TaskState aNewState, Principal pUser);

  void setEndpoint(EndpointDescriptorImpl pEndPoint);

  Principal getOwner();

  List<? extends TaskItem> getItems();

  void setItems(List<? extends TaskItem> pItems);

  long getRemoteHandle();

  long getInstanceHandle();

  String getSummary();

}
