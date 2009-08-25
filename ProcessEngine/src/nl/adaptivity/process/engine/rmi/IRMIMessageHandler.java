package nl.adaptivity.process.engine.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import nl.adaptivity.process.engine.IMessage;


public interface IRMIMessageHandler extends Remote {
  
  public void postMessage(IRMIProcessEngine pEngine, IMessage pMessage) throws RemoteException;

  public void postFinishedInstance(long pHandle) throws RemoteException;

  public void postCancelledInstance(long pHandle) throws RemoteException;

}
