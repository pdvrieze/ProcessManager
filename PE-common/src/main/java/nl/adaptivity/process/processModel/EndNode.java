package nl.adaptivity.process.processModel;

import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public interface EndNode<T extends ProcessNode<T>> extends ProcessNode<T>{
  void setDefines(Collection<? extends IXmlDefineType> exports);

  @Nullable
  Identifiable getPredecessor();

  void setPredecessor(Identifier predecessor);

}