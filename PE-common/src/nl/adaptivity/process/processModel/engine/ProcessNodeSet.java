package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.AbstractReadMap;

import nl.adaptivity.process.processModel.ProcessNode;


public class ProcessNodeSet extends AbstractReadMap<String, ProcessNode, ProcessNode> {


  public ProcessNodeSet() {
    super(false);
  }

  public ProcessNodeSet(int pSize) {
    super(pSize, false);
  }

  public ProcessNodeSet(Iterable<? extends ProcessNode> pCollection) {
    super(pCollection, false);
  }

  @Override
  protected String getKey(ProcessNode pValue) {
    return pValue.getId();
  }

  @Override
  protected ProcessNode asElement(Object pObject) {
    if (pObject instanceof ProcessNode) {
      return (ProcessNode) pObject;
    }
    return null;
  }

}
