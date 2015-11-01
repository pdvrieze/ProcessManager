package nl.adaptivity.process.processModel;

import nl.adaptivity.process.util.Identifiable;

import java.util.Collection;


public interface EndNode<T extends ProcessNode<T>> extends ProcessNode<T>{
  void setDefines(Collection<? extends IXmlDefineType> pExports);

  Identifiable getPredecessor();

  void setPredecessor(T pPredecessor);

}