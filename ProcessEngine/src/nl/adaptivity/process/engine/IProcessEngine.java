package nl.adaptivity.process.engine;

import org.w3c.dom.Node;

import nl.adaptivity.process.processModel.ProcessModel;


public interface IProcessEngine {

  public HProcessInstance startProcess(ProcessModel<?> pModel, Node pPayload);

  public void finishInstance(ProcessInstance pProcessInstance);

  public void cancelAll();
}
