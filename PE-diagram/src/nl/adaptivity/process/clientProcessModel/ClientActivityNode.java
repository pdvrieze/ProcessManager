package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.XmlExportType;
import nl.adaptivity.process.processModel.XmlImportType;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.engine.ProcessNodeSet;


public class ClientActivityNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Activity<T> {

  private String aName;

  private String aCondition;

  private T aPredecessor;

  private Set<T> aSuccessors;

  private XmlMessage aMessage;

  @Override
  public Set<? extends T> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<T>();
    }
    return aSuccessors;
  }


  @Override
  public Set<? extends T> getPredecessors() {
    return Collections.singleton(aPredecessor);
  }

  @Override
  public boolean isPredecessorOf(T pNode) {
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
  public T getPredecessor() {
    return aPredecessor;
  }

  @Override
  public void setPredecessor(T pPredecessor) {
    aPredecessor = pPredecessor;
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
  protected void setSuccessor(T pSuccessor) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet<T>(1);
    } else {
      aSuccessors.clear();
    }
    aSuccessors.add(pSuccessor);
  }

  @Override
  public void addSuccessor(T pNode) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet<T>(1);
    }
    aSuccessors.add(pNode);
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
