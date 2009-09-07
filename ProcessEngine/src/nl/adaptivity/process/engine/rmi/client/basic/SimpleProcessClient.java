package nl.adaptivity.process.engine.rmi.client.basic;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import nl.adaptivity.process.engine.ExtMessage;
import nl.adaptivity.process.engine.HProcessInstance;
import nl.adaptivity.process.engine.MessageHandle;
import nl.adaptivity.process.engine.rmi.IRMIMessageHandler;
import nl.adaptivity.process.engine.rmi.IRMIProcessEngine;
import nl.adaptivity.process.processModel.*;
import static nl.adaptivity.process.engine.rmi.RMIProcessEngineConstants.*;


public class SimpleProcessClient implements IRMIMessageHandler{

  private static SimpleProcessClient aClient;

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      Registry registry = LocateRegistry.getRegistry();
      IRMIProcessEngine engine = (IRMIProcessEngine) registry.lookup(_SERVICENAME);

      aClient = new SimpleProcessClient();
      IRMIMessageHandler stub = (IRMIMessageHandler) UnicastRemoteObject.exportObject(aClient);
      registry.bind(_USERMSGSERVICENAME, stub);

      
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          aClient.quit();
        }
      });
      
      
      startProcessInstance(engine);

      
            
      engine=null;
      System.gc();
      
    } catch (Exception e) {
        System.err.println("Client exception: " + e.toString());
        e.printStackTrace();
    }

  }

  private static void startProcessInstance(IRMIProcessEngine pEngine) throws RemoteException {
    
    ProcessModel processModel = getProcessModel2();
    pEngine.startProcess(processModel);
    
  }

  private static ProcessModel getProcessModel() {
    ProcessNode startNode = new StartNode();
    Activity sayHello1 = new UserMessage(startNode, null, "Hello1");
    
    EndNode endNode = new EndNode(sayHello1);
    
    ProcessModel result = new ProcessModel(endNode);
    return result;
  }

  private static ProcessModel getProcessModel2() {
    ProcessNode startNode = new StartNode();
    Activity sayHello1 = new UserMessage(startNode, null, "Hello1");
    Activity sayHello2 = new UserMessage(startNode, null, "Hello2");
    
    EndNode endNode = new EndNode(Join.andJoin(sayHello1, sayHello2));
    
    ProcessModel result = new ProcessModel(endNode);
    return result;
  }

  private boolean aHasQuit;

  @Override
  public void postMessage(final IRMIProcessEngine pEngine, final ExtMessage pMessage) throws RemoteException {
    System.out.println("Message: "+pMessage);
    new Thread() {
      @Override
      public void run() {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        try {
          pEngine.postMessage(new MessageHandle(pMessage.getHandle()), ExtMessage.complete(new HProcessInstance(pMessage.getProcessInstanceHandle()), pMessage.getHandle()));
        } catch (RemoteException e) {
          e.printStackTrace();
        }
      }
    }.start();
  }

  @Override
  public void postFinishedInstance(long pHandle) {
    System.out.println("Instance "+pHandle+" finished");
    quit();
    
  }

  private void quit() {
    try {
      synchronized(this) {
        if (aHasQuit) {
          return;
        }
        aHasQuit = true;
      }
      Registry registry = LocateRegistry.getRegistry();
      try {
        registry.unbind(_USERMSGSERVICENAME);
        UnicastRemoteObject.unexportObject(aClient, true);
      } catch (NotBoundException e) {
        throw new RemoteException("Could not unregister service, quiting anyway", e);
      }
    } catch (RemoteException e1) {
      e1.printStackTrace();
    }
    System.gc();
  }

  @Override
  public void postCancelledInstance(long pHandle) {
    System.out.println("Instance "+pHandle+" cancelled");
    quit();
  }

}
