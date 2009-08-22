package nl.adaptivity.process.engine.client.basic;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import nl.adaptivity.process.engine.IProcessEngine;

import static nl.adaptivity.process.engine.ProcessEngineConstants.*;


public class QuitClient {

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      Registry registry = LocateRegistry.getRegistry();
      IProcessEngine stub = (IProcessEngine) registry.lookup(_SERVICENAME);
      stub.quit();
      
    } catch (Exception e) {
        System.err.println("Client exception: " + e.toString());
        e.printStackTrace();
    }
  }

}
