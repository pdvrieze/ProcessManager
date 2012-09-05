package nl.adaptivity.process.engine.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.w3c.dom.Node;

import net.devrieze.util.HandleMap;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.engine.ProcessInstance.ProcessInstanceRef;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRefs;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.XmlProcessModel;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;


public class ServletProcessEngine extends HttpServlet implements IMessageService<ServletProcessEngine.ServletMessage, ProcessNodeInstance> {

  public static final String PROCESS_ENGINE_NS = "http://adaptivity.nl/ProcessEngine/";

  static class ServletMessage{
    
  }
  
  private ProcessEngine aProcessEngine;
  private boolean aKeepRunning = true;
  private Thread aThread;
  
  /*
   * Servlet methods 
   */

  @Override
  public void destroy() {
    aKeepRunning = false;
    aThread.interrupt();
  }

  @Override
  public ServletConfig getServletConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getServletInfo() {
    return "ServletProcessEngine";
  }
  
  @Override
  public void init(ServletConfig pArg0) throws ServletException {
    aProcessEngine = new ProcessEngine(this);
    // TODO Auto-generated method stub

  }

  /*
   * IMessageService methods 
   */

  @Override
  public ServletMessage createMessage(XmlMessage pMessage) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public boolean sendMessage(ServletMessage pMessage, ProcessNodeInstance pInstance) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }
  
  /*
   * Web interface for this servlet 
   */


  @RestMethod(method=HttpMethod.GET, path="/processModels")
  public ProcessModelRefs getProcesModelRefs() {
    Iterable<ProcessModel> processModels = aProcessEngine.getProcessModels();
    ProcessModelRefs list = new ProcessModelRefs();
    for (ProcessModel pm: processModels) {
      list.add(pm.getRef());
    }
    return list;
  }

  @RestMethod(method=HttpMethod.GET, path="/processInstances")
  @XmlElementWrapper(name="processInstances", namespace=PROCESS_ENGINE_NS)
  public Collection<? extends ProcessInstanceRef> getProcesInstanceRefs() {
    Iterable<ProcessInstance> processInstances = aProcessEngine.getInstances();
    Collection<ProcessInstanceRef> list = new ArrayList<ProcessInstanceRef>();
    for (ProcessInstance pi: processInstances) {
      list.add(pi.getRef());
    }
    return list;
  }

  @RestMethod(method=HttpMethod.POST, path="/processModels")
  public ProcessModelRefs postProcessModel(@RestParam(name="processUpload", type=ParamType.ATTACHMENT) DataHandler attachment) throws IOException {

    XmlProcessModel pm;
    try {
      pm = JAXB.unmarshal(attachment.getInputStream(), XmlProcessModel.class);
    } catch (IOException e) {
      throw e;
    }
    if (pm!=null) {
      aProcessEngine.addProcessModel(pm.toProcessModel());
    }

    return getProcesModelRefs();
  }

  @RestMethod(method=HttpMethod.POST, path="/processModels/${handle}", query={"op=newInstance"})
  public HProcessInstance startProcess(@RestParam(name="handle", type=ParamType.VAR) long pHandle, @RestParam(name="name", type=ParamType.QUERY) String pName) {
    return aProcessEngine.startProcess(HandleMap.<ProcessModel>handle(pHandle), pName, null);
  }

  @RestMethod(method=HttpMethod.POST, path="/processModels/${handle}", post={"name"})
  public void renameProcess(@RestParam(name="handle", type=ParamType.VAR) long pHandle, @RestParam(name="name", type=ParamType.QUERY) String pName) {
    aProcessEngine.renameProcess(HandleMap.<ProcessModel>handle(pHandle), pName);
  }

  @WebMethod(operationName="updateTaskState")
  @RestMethod(method=HttpMethod.POST, path="/tasks/${handle}", query={"state"})
  public TaskState updateTaskState(@WebParam(name="handle",mode=Mode.IN) @RestParam(name="handle",type=ParamType.VAR) long pHandle,
                              @WebParam(name="state", mode=Mode.IN) @RestParam(name="state", type=ParamType.QUERY) TaskState pNewState) {
    return aProcessEngine.updateTaskState(pHandle, pNewState);
  }

  @WebMethod(operationName="finishTask")
  @RestMethod(method=HttpMethod.POST, path="/tasks/${handle}", query={"state=Complete"})
  public TaskState finishTask(@WebParam(name="handle",mode=Mode.IN) @RestParam(name="handle",type=ParamType.VAR) long pHandle,
                              @WebParam(name="payload", mode=Mode.IN) @RestParam(name="payload", type=ParamType.QUERY) Node pPayload) {
    return aProcessEngine.finishTask(pHandle, pPayload);
  }


  @RestMethod(method=HttpMethod.GET, path="/processModels/${handle}")
  public XmlProcessModel getProcessModel(@RestParam(name="handle",type=ParamType.VAR) long pHandle) throws FileNotFoundException {
    try {
      return new XmlProcessModel(aProcessEngine.getProcessModel(pHandle));
    } catch (NullPointerException e) {
      throw (FileNotFoundException) new FileNotFoundException("Process handle invalid").initCause(e);
    }
  }
  
  

}
