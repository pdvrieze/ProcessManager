package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.XmlExportType;
import nl.adaptivity.process.processModel.XmlImportType;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.engine.ProcessNodeSet;


public class ClientActivityNode extends ClientProcessNode implements Activity {

  private String aName;

  private String aCondition;

  private ClientProcessNode aPredecessor;

  private Set<ClientProcessNode> aSuccessors;

  private XmlMessage aMessage;

  @Override
  public Set<? extends ClientProcessNode> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ClientProcessNode>();
    }
    return aSuccessors;
  }


  @Override
  public Set<? extends ClientProcessNode> getPredecessors() {
    return Collections.singleton(aPredecessor);
  }

  @Override
  public boolean isPredecessorOf(ProcessNode pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public String getName() {
    return aName;
  }

  @Override
  public void setName(String pName) {
    aName = pName;
  }

  @Override
  public String getCondition() {
    return aCondition;
  }

  @Override
  public void setCondition(String pCondition) {
    aCondition = pCondition;
  }

  @Override
  public ProcessNode getPredecessor() {
    return aPredecessor;
  }

  @Override
  public void setPredecessor(ProcessNode pPredecessor) {
    aPredecessor = (ClientProcessNode) pPredecessor;
  }

  @Override
  public XmlMessage getMessage() {
    return aMessage;
  }

  @Override
  public void setMessage(XmlMessage pMessage) {
    aMessage = pMessage;
  }

  @Override
  protected void setSuccessor(ProcessNode pSuccessor) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet<ClientProcessNode>(1);
    } else {
      aSuccessors.clear();
    }
    aSuccessors.add((ClientProcessNode) pSuccessor);
  }

  @Override
  public void addSuccessor(ProcessNode pNode) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet<ClientProcessNode>(1);
    }
    aSuccessors.add((ClientProcessNode) pNode);
  }


  @Override
  public List<XmlImportType> getImports() {
    return super.getImports();
  }

  @Override
  public void setImports(Collection<? extends XmlImportType> pImports) {
    super.setImports(pImports);
  }

  @Override
  public List<XmlExportType> getExports() {
    return super.getExports();
  }

  @Override
  public void setExports(Collection<? extends XmlExportType> pExports) {
    super.setExports(pExports);
  }


}
