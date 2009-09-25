package nl.adaptivity.process.userMessageHandler.server;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;


public class InternalEndpoint implements GenericEndpoint {

  @XmlRootElement(name="task")
  public static class XmlTask implements Task{
    private long aHandle;
    private TaskState aState;
    private String aSummary;

    public XmlTask() {
      aHandle = -1;
    }
    
    public XmlTask(long pHandle) {
      aHandle = pHandle;
    }

    @Override
    public void failTask() {
      setState(TaskState.Failed);
      updateRemoteTaskState(getState());
    }

    @Override
    public void finishTask(Object pPayload) {
      setState(TaskState.Complete);
      updateRemoteTaskState(getState());
    }

    @XmlAttribute
    @Override
    public TaskState getState() {
      return aState;
    }

    @Override
    public boolean provideTask() {
      setState(TaskState.Available);
      updateRemoteTaskState(getState());
      return false;
    }

    @Override
    public void setState(TaskState pNewState) {
      aState = pNewState;
    }

    @Override
    public <T> boolean startTask(IMessageService<T> pMessageService) {
      setState(TaskState.Taken);
      updateRemoteTaskState(getState());
      return false;
    }

    @Override
    public boolean takeTask() {
      setState(TaskState.Taken);
      updateRemoteTaskState(getState());
      return false;
    }

    private void updateRemoteTaskState(TaskState pState) {
      // TODO Auto-generated method stub
      // 
      throw new UnsupportedOperationException("Not yet implemented");
      
    }

    @XmlAttribute(name="handle")
    @Override
    public void setHandle(long pHandle) {
      aHandle = pHandle;
    }

    @Override
    public long getHandle() {
      return aHandle;
    }
    
    @XmlAttribute(name="summary")
    public String getSummary() {
      return aSummary;
    }
    
    public void setSummary(String pSummary) {
      aSummary = pSummary;
    }
  }
  
  private static final String ENDPOINT = "internal";
  public static final QName SERVICENAME = new QName("http:://adaptivity.nl/userMessageHandler", "userMessageHandler");
  private UserMessageService aService;

  public InternalEndpoint(UserMessageService pService) {
    aService = pService;
  }

  @Override
  public QName getService() {
    return SERVICENAME;
  }

  @Override
  public String getEndpoint() {
    return ENDPOINT;
  }

  @WebMethod
  boolean postTask(@WebParam(name="task", mode=Mode.IN) Task pTask) {
    return aService.postTask(pTask);
  }
}
