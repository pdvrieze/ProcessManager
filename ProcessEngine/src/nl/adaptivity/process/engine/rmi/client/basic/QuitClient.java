package nl.adaptivity.process.engine.rmi.client.basic;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import nl.adaptivity.process.engine.rmi.IRMIProcessEngine;
import static nl.adaptivity.process.engine.rmi.RMIProcessEngineConstants.*;


public class QuitClient {

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      Registry registry = LocateRegistry.getRegistry();
      IRMIProcessEngine stub = (IRMIProcessEngine) registry.lookup(_SERVICENAME);
      stub.quit();
      stub=null;
      System.gc();
      
    } catch (Exception e) {
        System.err.println("Client exception: " + e.toString());
        e.printStackTrace();
    }
  }

}
