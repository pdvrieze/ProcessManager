package nl.adaptivity.process.engine.processModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.processmodel.jaxb.XmlImportType;


@XmlRootElement(name="start")
@XmlAccessorType(XmlAccessType.NONE)
public class StartNode extends ProcessNode {

  public StartNode() {
    super((ProcessNode) null);
  }

  private static final long serialVersionUID = 7779338146413772452L;
  
  private List<XmlImportType> aImports;

  @Override
  public void start(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance, ProcessNodeInstance pPredecessor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean condition() {
    return true;
  }

  @XmlElement(name="import")
  public List<XmlImportType> getImport() {
    if (aImports == null) {
      aImports = new ArrayList<XmlImportType>();
    }
    return this.aImports;
  }
}
