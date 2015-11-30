package nl.adaptivity.process.userMessageHandler.server;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import net.devrieze.util.db.DBTransaction;

import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance;
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

  UserMessageService mService;

  private URI mURI;

  public ExternalEndpoint() {
    mService = UserMessageService.getInstance();
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
    return mURI;
  }

  @Override
  public void initEndpoint(final ServletConfig config) {
    final StringBuilder path = new StringBuilder(config.getServletContext().getContextPath());
    path.append("/UserMessageService");
    try {
      mURI = new URI(null, null, path.toString(), null);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

  @XmlElementWrapper(name = "tasks", namespace = Constants.USER_MESSAGE_HANDLER_NS)
  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks")
  public Collection<XmlTask> getPendingTasks() throws SQLException {
    try (DBTransaction transaction = mService.newTransaction()) {
      return transaction.commit(mService.getPendingTasks(transaction));
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error retrieving tasks", e);
      throw e;
    }
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}")
  public XmlTask updateTask(
      @RestParam(name="handle", type=ParamType.VAR) final String handle,
      @RestParam(type=ParamType.BODY) final XmlTask partialNewTask,
      @RestParam(type = ParamType.PRINCIPAL) final Principal user) throws SQLException, FileNotFoundException
  {
    try (DBTransaction transaction = mService.newTransaction()) {
      final XmlTask result = mService.updateTask(transaction, Long.parseLong(handle), partialNewTask, user);
      if (result==null) { throw new FileNotFoundException(); }
      transaction.commit();
      return result;
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
      throw e;
    }
  }

  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks/${handle}")
  public XmlTask getPendingTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.getPendingTask(Long.parseLong(handle), user);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Started" })
  public IProcessNodeInstance.TaskState startTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.startTask(Long.parseLong(handle), user);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Taken" })
  public IProcessNodeInstance.TaskState takeTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.takeTask(Long.parseLong(handle), user);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Finished" })
  public IProcessNodeInstance.TaskState finishTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.finishTask(Long.parseLong(handle), user);
  }

  @Override
  public void destroy() {
    mService.destroy();
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

}
