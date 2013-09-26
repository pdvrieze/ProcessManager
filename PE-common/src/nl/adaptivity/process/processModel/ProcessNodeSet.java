package nl.adaptivity.process.processModel;

import net.devrieze.util.AbstractReadMap;


public class ProcessNodeSet<T extends ProcessNode<T>> extends AbstractReadMap<String, T, ProcessNode<T>> {


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
  protected String getKey(ProcessNode<T> pValue) {
    return pValue.getId();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected ProcessNode<T> asElement(Object pObject) {
    if (pObject instanceof ProcessNode) {
      return (ProcessNode<T>) pObject;
    }
    return null;
  }

}
