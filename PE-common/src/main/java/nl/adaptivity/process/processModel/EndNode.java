package nl.adaptivity.process.processModel;

import java.util.Collection;


public interface EndNode<T extends ProcessNode<T>> extends ProcessNode<T>{
  void setDefines(Collection<? extends IXmlDefineType> pExports);

  T getPredecessor();

  void setPredecessor(T pPredecessor);

}