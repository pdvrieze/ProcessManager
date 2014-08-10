package nl.adaptivity.process.userMessageHandler.server;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import net.devrieze.util.db.DBTransaction;

import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;


@XmlSeeAlso(XmlTask.class)
public class ExternalEndpoint implements GenericEndpoint {

  public static final String ENDPOINT = "external";

  public static final QName SERVICENAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, "userMessageHandler");

  UserMessageService aService;

  private URI aURI;

  public ExternalEndpoint() {
    aService = UserMessageService.getInstance();
  }

  @Override
  public QName getServiceName() {
    return SERVICENAME;
  }

  @Override
  public String getEndpointName() {
    return ENDPOINT;
  }

  @Override
  public URI getEndpointLocation() {
    return aURI;
  }

  @Override
  public void initEndpoint(final ServletConfig pConfig) {
    final StringBuilder path = new StringBuilder(pConfig.getServletContext().getContextPath());
    path.append("/UserMessageService");
    try {
      aURI = new URI(null, null, path.toString(), null);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

  @XmlElementWrapper(name = "tasks", namespace = Constants.USER_MESSAGE_HANDLER_NS)
  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks")
  public Collection<XmlTask> getPendingTasks() throws SQLException {
    try (DBTransaction transaction = aService.newTransaction()) {
      return transaction.commit(aService.getPendingTasks(transaction));
    }
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}")
  public XmlTask updateTask(
      @RestParam(name="handle", type=ParamType.VAR) final String pHandle,
      @RestParam(type=ParamType.BODY) final XmlTask pNewTask,
      @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) throws SQLException, FileNotFoundException
  {
    try (DBTransaction transaction = aService.newTransaction()) {
      final XmlTask result = aService.updateTask(transaction, Long.parseLong(pHandle), pNewTask, pUser);
      if (result==null) { throw new FileNotFoundException(); }
      transaction.commit();
      return result;
    }
  }

  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks/${handle}")
  public XmlTask getPendingTask(@RestParam(name = "handle", type = ParamType.VAR) final String pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    return aService.getPendingTask(Long.parseLong(pHandle), pUser);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Started" })
  public IProcessNodeInstance.TaskState startTask(@RestParam(name = "handle", type = ParamType.VAR) final String pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    return aService.startTask(Long.parseLong(pHandle), pUser);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Taken" })
  public IProcessNodeInstance.TaskState takeTask(@RestParam(name = "handle", type = ParamType.VAR) final String pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    return aService.takeTask(Long.parseLong(pHandle), pUser);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Finished" })
  public IProcessNodeInstance.TaskState finishTask(@RestParam(name = "handle", type = ParamType.VAR) final String pHandle, @RestParam(type = ParamType.PRINCIPAL) final Principal pUser) {
    return aService.finishTask(Long.parseLong(pHandle), pUser);
  }

  @Override
  public void destroy() {
    aService.destroy();
  }

}
