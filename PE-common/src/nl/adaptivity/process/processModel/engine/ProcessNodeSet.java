package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.AbstractReadMap;

import nl.adaptivity.process.processModel.ProcessNode;


public class ProcessNodeSet<T extends ProcessNode> extends AbstractReadMap<String, T, ProcessNode> {


  public ProcessNodeSet() {
    super(false);
  }

  public ProcessNodeSet(int pSize) {
    super(pSize, false);
  }

  public ProcessNodeSet(Iterable<? extends T> pCollection) {
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
