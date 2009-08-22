package nl.adaptivity.process.engine.rmi;

import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;

import nl.adaptivity.process.engine.IProcessEngine;
import nl.adaptivity.process.engine.ProcessEngine;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.engine.ProcessModel;

public class RMIProcessEngine implements IRMIProcessEngine, Unreferenced {

  
  private IProcessEngine aEngine;

  public RMIProcessEngine() {
    aEngine = new ProcessEngine();
  }

  @Override
  public void quit() throws RemoteException {
    try {
      aEngine.quit();
    } catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  @Override
  public ProcessInstance startProcess(ProcessModel pModel) throws RemoteException {
    try {
      return aEngine.startProcess(pModel);
    } catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }

  @Override
  public void unreferenced() {
    System.out.println("quitting because unreferenced");
    System.exit(0);
  }

}
