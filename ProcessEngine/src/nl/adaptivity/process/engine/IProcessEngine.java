package nl.adaptivity.process.engine;

import nl.adaptivity.process.processModel.ProcessModel;

public interface IProcessEngine{

  public HProcessInstance startProcess(ProcessModel pModel, Payload pPayload);

  public void setMessageListener(ProcessMessageListener pProcessEngine);

  public void finishInstance(ProcessInstance pProcessInstance);

  public void cancelAll();
}
