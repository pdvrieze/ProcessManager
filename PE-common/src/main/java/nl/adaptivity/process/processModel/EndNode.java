package nl.adaptivity.process.processModel;

import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;

import java.util.Collection;


public interface EndNode<T extends ProcessNode<T>> extends ProcessNode<T>{
  void setDefines(Collection<? extends IXmlDefineType> pExports);

  Identifiable getPredecessor();

  void setPredecessor(Identifier predecessor);

}