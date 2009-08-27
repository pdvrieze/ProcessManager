package nl.adaptivity.process.engine.processModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.processmodel.jaxb.XmlExportType;
import nl.adaptivity.process.processmodel.jaxb.XmlImportType;


public class Activity extends ProcessNode{

  private String aName;
  private String aCondition;
  private List<XmlImportType> aImports;
  private List<XmlExportType> aExports;

  
  public Activity(ProcessNode pPrevious) {
    super(pPrevious);
  }

  private static final long serialVersionUID = 282944120294737322L;

  @Override
  public boolean condition() {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  @Override
  public void start(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance, ProcessNodeInstance pPredecessor) {
    // TODO Auto-generated method stub
    // 
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  public String getName() {
    return aName;
  }

  public void setName(String pName) {
    aName = pName;
  }

  public void setCondition(String pCondition) {
    aCondition = pCondition;
  }

  public void setImports(List<XmlImportType> pImports) {
    aImports = new ArrayList<XmlImportType>(pImports.size());
    aImports.addAll(pImports);
  }

  public void setExports(List<XmlExportType> pExports) {
    aExports = new ArrayList<XmlExportType>(pExports.size());
    aExports.addAll(pExports);
  }

  
  public String getCondition() {
    return aCondition;
  }

  
  public List<XmlImportType> getImports() {
    return aImports;
  }

  
  public List<XmlExportType> getExports() {
    return aExports;
  }

}
