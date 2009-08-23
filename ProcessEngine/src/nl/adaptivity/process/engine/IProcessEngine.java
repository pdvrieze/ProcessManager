package nl.adaptivity.process.engine;

import java.io.Serializable;

public interface IProcessEngine{
  
  public ProcessInstance startProcess(ProcessModel pModel);

  public void postMessage(ProcessInstance pProcesInstance, Serializable pMessage);
}
