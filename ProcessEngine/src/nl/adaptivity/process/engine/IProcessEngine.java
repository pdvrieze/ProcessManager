package nl.adaptivity.process.engine;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IProcessEngine extends Remote{
  
  public ProcessInstance startProcess(ProcessModel pModel) throws RemoteException;
  
  public void quit() throws RemoteException;

}
