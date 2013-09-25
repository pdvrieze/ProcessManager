package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.List;


public interface EndNode extends ProcessNode{
  void setExports(Collection<? extends XmlExportType> pExports);

  List<XmlExportType> getExports();

  ProcessNode getPredecessor();

  void setPredecessor(ProcessNode pPredecessor);

}