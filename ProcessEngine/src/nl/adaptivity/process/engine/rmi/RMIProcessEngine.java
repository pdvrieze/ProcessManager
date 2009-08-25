package nl.adaptivity.process.engine.rmi;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Arrays;

import nl.adaptivity.process.engine.*;
import static nl.adaptivity.process.engine.rmi.RMIProcessEngineConstants.*;

public class RMIProcessEngine implements IRMIProcessEngine, Unreferenced, ProcessMessageListener {

  
  private static RMIProcessEngine aRmiEngine;
  private IProcessEngine aEngine;

  public RMIProcessEngine() {
    aEngine = new ProcessEngine();
    aEngine.setMessageListener(this);
  }

  @Override
  public void quit() throws RemoteException {
    try{
      System.out.println("quit() called");
      try {
        aEngine.cancelAll();
      } finally {
        Registry registry = LocateRegistry.getRegistry();
        try {
          registry.unbind(_SERVICENAME);
          UnicastRemoteObject.unexportObject(aRmiEngine, true);
        } catch (NotBoundException e) {
          throw new RemoteException("Could not unregister service, quiting anyway", e);
        }
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  @Override
  public HProcessInstance startProcess(ProcessModel pModel) throws RemoteException {
    try {
      return aEngine.startProcess(pModel);
    } catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  @Override
  public void postMessage(MessageHandle pHOrigMessage, IMessage pMessage) throws RemoteException {
    try {
      aEngine.postMessage(pHOrigMessage, pMessage);
    } catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  @Override
  public void unreferenced() {
    System.out.println("quitting because unreferenced");
    System.exit(0);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    System.out.println("Codebase: "+System.getProperty("java.rmi.server.codebase"));
    System.out.println("Args: "+Arrays.asList(args).toString());
    
    try {
      aRmiEngine = new RMIProcessEngine();
      IRMIProcessEngine stub = (IRMIProcessEngine) UnicastRemoteObject.exportObject(aRmiEngine, _PORT);
      
      Registry registry = LocateRegistry.getRegistry();
      registry.bind(_SERVICENAME, stub);
      
      System.out.println("Server started");
      
    } catch (RemoteException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (AlreadyBoundException e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  @Override
  public void fireMessage(final Message pMessage) {
    Registry registry;
    try {
      registry = LocateRegistry.getRegistry();
      IRMIMessageHandler stub = (IRMIMessageHandler) registry.lookup(_USERMSGSERVICENAME);
      if (stub!=null) {
        stub.postMessage(this, pMessage);
      } else {
        System.out.println("fireMessage("+pMessage+")");
        debugReply(pMessage);
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (NotBoundException e) {
      System.out.println("fireMessage("+pMessage+")");
      debugReply(pMessage);
    }
  }

  private void debugReply(final Message pMessage) {
    new Thread() {
      @Override
      public void run() {
        try {
          try {
            sleep(1000);
          } catch (InterruptedException e1) {
          }
          postMessage(new MessageHandle(pMessage.getHandle()), Message.complete(new HProcessInstance(pMessage.getProcessInstanceHandle()), pMessage.getHandle()));
        } catch (RemoteException e1) {
          e1.printStackTrace();
        }
      }
    }.start();
  }

  @Override
  public void fireFinishedInstance(long pHandle) {
    Registry registry;
    try {
      registry = LocateRegistry.getRegistry();
      IRMIMessageHandler stub = (IRMIMessageHandler) registry.lookup(_USERMSGSERVICENAME);
      if (stub!=null) {
        stub.postFinishedInstance(pHandle);
        stub = null;
      } else {
        System.out.println("Process instance ("+pHandle+") finished");
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (NotBoundException e) {
      System.out.println("Process instance ("+pHandle+") finished");
    }
  }

  @Override
  public void cancelInstance(long pHandle) {
    Registry registry;
    try {
      registry = LocateRegistry.getRegistry();
      IRMIMessageHandler stub = (IRMIMessageHandler) registry.lookup(_USERMSGSERVICENAME);
      if (stub!=null) {
        stub.postCancelledInstance(pHandle);
        stub = null;
      } else {
        System.out.println("Process instance ("+pHandle+") finished");
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (NotBoundException e) {
      System.out.println("Process instance ("+pHandle+") finished");
    }
  }

}
