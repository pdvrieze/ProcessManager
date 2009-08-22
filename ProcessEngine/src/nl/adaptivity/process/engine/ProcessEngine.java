package nl.adaptivity.process.engine;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

import nl.adaptivity.process.engine.rmi.IRMIProcessEngine;
import nl.adaptivity.process.engine.rmi.RMIProcessEngine;
import static nl.adaptivity.process.engine.rmi.RMIProcessEngineConstants.*;

public class ProcessEngine implements IProcessEngine {

  private static RMIProcessEngine engine;

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    System.out.println("Codebase: "+System.getProperty("java.rmi.server.codebase"));
    System.out.println("Args: "+Arrays.asList(args).toString());
    
    try {
      engine = new RMIProcessEngine();
      IRMIProcessEngine stub = (IRMIProcessEngine) UnicastRemoteObject.exportObject(engine, _PORT);
      
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
  public void quit() {
    try{
      System.out.println("quit() called");
      Registry registry = LocateRegistry.getRegistry();
      try {
        registry.unbind(_SERVICENAME);
        UnicastRemoteObject.unexportObject(engine, true);
      } catch (NotBoundException e) {
        throw new RemoteException("Could not unregister service, quiting anyway", e);
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  @Override
  public ProcessInstance startProcess(ProcessModel pModel) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

}
