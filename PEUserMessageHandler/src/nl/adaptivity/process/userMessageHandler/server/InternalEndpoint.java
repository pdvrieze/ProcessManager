package nl.adaptivity.process.userMessageHandler.server;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import nl.adaptivity.jbi.components.genericSE.GenericEndpoint;
import nl.adaptivity.jbi.util.EndPointDescriptor;
import nl.adaptivity.process.exec.Task.TaskState;

@XmlSeeAlso(InternalEndpoint.XmlTask.class)
public class InternalEndpoint implements GenericEndpoint {

  @XmlRootElement(name="task")
  public static class XmlTask implements UserTask{
    private long aHandle;
    private TaskState aState;
    private String aSummary;

    public XmlTask() {
      aHandle = -1;
    }

    public XmlTask(long pHandle) {
      aHandle = pHandle;
    }

    @XmlAttribute
    @Override
    public TaskState getState() {
      return aState;
    }

    @Override
    public void setState(TaskState pNewState) {
      aState = pNewState;
      updateRemoteTaskState(aState);
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
  public boolean postTask(@WebParam(name="replies", mode=Mode.IN) EndPointDescriptor pEndPoint, @WebParam(name="task", mode=Mode.IN) UserTask pTask) {
    return aService.postTask(pTask);
  }
}
