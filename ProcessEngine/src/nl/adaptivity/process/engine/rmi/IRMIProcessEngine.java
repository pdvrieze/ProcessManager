package nl.adaptivity.process.engine.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import nl.adaptivity.process.engine.ExtMessage;
import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.MessageHandle;
import nl.adaptivity.process.processModel.ProcessModel;

public interface IRMIProcessEngine extends Remote{
  
  public HProcessInstance startProcess(ProcessModel pModel) throws RemoteException;
  
  public void quit() throws RemoteException;
  
  public void postMessage(MessageHandle pHOrigMessage, ExtMessage pMessage) throws RemoteException;
}
