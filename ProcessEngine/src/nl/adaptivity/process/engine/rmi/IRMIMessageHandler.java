package nl.adaptivity.process.engine.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import nl.adaptivity.process.engine.IExtMessage;


public interface IRMIMessageHandler extends Remote {
  
  public void postMessage(IRMIProcessEngine pEngine, IExtMessage pMessage) throws RemoteException;

  public void postFinishedInstance(long pHandle) throws RemoteException;

  public void postCancelledInstance(long pHandle) throws RemoteException;

}
