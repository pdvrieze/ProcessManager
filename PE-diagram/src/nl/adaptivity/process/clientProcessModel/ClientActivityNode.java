package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;

import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.IXmlImportType;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public class ClientActivityNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Activity<T> {

  private String aName;

  private String aCondition;

  private ProcessNodeSet<T> aPredecessor = ProcessNodeSet.singleton();

  private ProcessNodeSet<T> aSuccessors;

  private IXmlMessage aMessage;

  @Override
  public ProcessNodeSet<T> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = ProcessNodeSet.processNodeSet();
    }
    return aSuccessors;
  }


  public ClientActivityNode(ClientProcessModel<T> pOwner) {
    super(pOwner);
  }


  public ClientActivityNode(String pId, ClientProcessModel<T> pOwner) {
    super(pId, pOwner);
  }

  protected ClientActivityNode(ClientActivityNode<T> pOrig) {
    super(pOrig);
    aName = pOrig.aName;
    aCondition = pOrig.aCondition;
    aPredecessor = pOrig.aPredecessor;
    aSuccessors = pOrig.aSuccessors;
    aMessage = pOrig.aMessage;
  }

  @Override
  public ProcessNodeSet<T> getPredecessors() {
    return aPredecessor;
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
    return aPredecessor.get(0);
  }

  @Override
  public IXmlMessage getMessage() {
    return aMessage;
  }

  @Override
  public void setMessage(IXmlMessage pMessage) {
    aMessage = pMessage;
  }

  @Override
  public void addSuccessor(T pNode) {
    if (aSuccessors==null) {
      aSuccessors = ProcessNodeSet.processNodeSet(1);
    }
    aSuccessors.add(pNode);
  }


  @Override
  public List<IXmlImportType> getImports() {
    return super.getImports();
  }

  @Override
  public void setImports(Collection<? extends IXmlImportType> pImports) {
    super.setImports(pImports);
  }

  @Override
  public List<IXmlExportType> getExports() {
    return super.getExports();
  }

  @Override
  public void setExports(Collection<? extends IXmlExportType> pExports) {
    super.setExports(pExports);
  }

}
