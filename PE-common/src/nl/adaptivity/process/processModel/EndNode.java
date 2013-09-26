package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.List;


public interface EndNode<T extends ProcessNode<T>> extends ProcessNode<T>{
  void setExports(Collection<? extends XmlExportType> pExports);

  List<XmlExportType> getExports();

  T getPredecessor();

  void setPredecessor(T pPredecessor);

}