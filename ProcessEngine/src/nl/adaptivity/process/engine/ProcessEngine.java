package nl.adaptivity.process.engine;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Arrays;

import static nl.adaptivity.process.engine.ProcessEngineConstants.*;

public class ProcessEngine implements IProcessEngine {

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    System.out.println("Codebase: "+System.getProperty("java.rmi.server.codebase"));
    System.out.println("Args: "+Arrays.asList(args).toString());
    
    try {
      ProcessEngine engine = new ProcessEngine();
      IProcessEngine stub = (IProcessEngine) UnicastRemoteObject.exportObject(engine, _PORT);
      
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
  public void quit() throws RemoteException {
    System.out.println("quit");
    Registry registry = LocateRegistry.getRegistry();
    try {
      registry.unbind(_SERVICENAME);
      UnicastRemoteObject.unexportObject(this, false);
    } catch (NotBoundException e) {
      throw new RemoteException("Could not unregister service, quiting anyway", e);
    }
    new Thread() {
      @Override
      public void run() {
        System.out.print("Shutting down...");
        try {
          sleep(2000);
        } catch (InterruptedException e) {
          // I don't care
        }
        System.out.println("done");
        System.exit(0);
      }
      
    }.start();
  }

  @Override
  public ProcessInstance startProcess(ProcessModel pModel) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

}
