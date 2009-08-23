package nl.adaptivity.process.engine.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.engine.ProcessModel;

public interface IRMIProcessEngine extends Remote{
  
  public ProcessInstance startProcess(ProcessModel pModel) throws RemoteException;
  
  public void quit() throws RemoteException;
  
  public void postMessage(ProcessInstance pProcesInstance, Serializable pMessage) throws RemoteException;
}
