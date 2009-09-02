package nl.adaptivity.process.engine.processModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.*;

import nl.adaptivity.process.engine.ProcessInstance;

@XmlRootElement(name = "activity")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Activity", propOrder = { "imports", "exports", "message", "condition" })
public class Activity extends ProcessNode{

  private String aName;
  private String aCondition;
  private List<XmlImportType> aImports;
  private List<XmlExportType> aExports;
  
  private XmlMessage aMessage;

  
  public Activity(ProcessNode pPredecessor) {
    super(pPredecessor);
  }

  public Activity() {
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

  @XmlAttribute
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

  
  @XmlElement
  public String getCondition() {
    return aCondition;
  }

  
  @XmlElement(name="import")
  public List<XmlImportType> getImports() {
    return aImports;
  }

  
  @XmlElement(name="export")
  public List<XmlExportType> getExports() {
    return aExports;
  }
  
  @XmlAttribute(name="predecessor", required=true)
  @XmlIDREF
  public ProcessNode getPredecessor() {
    Collection<ProcessNode> ps = getPredecessors();
    if (ps==null || ps.size()!=1) {
      return null;
    }
    return ps.iterator().next();
  }
  
  public void setPredecessor(ProcessNode predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }

  public void setMessage(XmlMessage message) {
    aMessage = message;
  }

  @XmlElement(required=true)
  public XmlMessage getMessage() {
    return aMessage;
  }
  
  

}
