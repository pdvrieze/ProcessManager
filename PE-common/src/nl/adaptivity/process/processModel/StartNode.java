package nl.adaptivity.process.processModel;

import java.util.List;


public interface StartNode extends ProcessNode {

  public List<XmlImportType> getImports();

}