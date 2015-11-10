package nl.adaptivity.process.engine;

import org.w3c.dom.Node;

import nl.adaptivity.process.processModel.ProcessModel;


public interface IProcessEngine {

  public HProcessInstance startProcess(ProcessModel<?> model, Node payload);

  public void finishInstance(ProcessInstance processInstance);

  public void cancelAll();
}
