package nl.adaptivity.process.engine.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import nl.adaptivity.process.engine.*;

public interface IRMIProcessEngine extends Remote{
  
  public HProcessInstance startProcess(ProcessModel pModel) throws RemoteException;
  
  public void quit() throws RemoteException;
  
  public void postMessage(MessageHandle pHOrigMessage, ExtMessage pMessage) throws RemoteException;
}
