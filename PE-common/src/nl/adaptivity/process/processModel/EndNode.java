package nl.adaptivity.process.processModel;

import java.util.List;


public interface EndNode extends ProcessNode{
  public abstract List<XmlExportType> getExports();

  ProcessNode getPredecessor();

  void setPredecessor(ProcessNode pPredecessor);

}