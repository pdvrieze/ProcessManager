package nl.adaptivity.process.engine;

public interface IProcessEngine{
  
  public ProcessInstance startProcess(ProcessModel pModel);
  
  public void quit();

}
