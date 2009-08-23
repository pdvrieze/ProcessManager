package nl.adaptivity.process.engine;

import java.io.Serializable;

public class ProcessEngine implements IProcessEngine {

  @Override
  public ProcessInstance startProcess(ProcessModel pModel) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  @Override
  public void postMessage(MessageHandle pHOrigMessage, Serializable pMessage) {
    IMessage repliedMessage = retrieveMessage(pHOrigMessage);
    
    // TODO Auto-generated method stub
    // 
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  private IMessage retrieveMessage(MessageHandle pOrigMessage) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

}
