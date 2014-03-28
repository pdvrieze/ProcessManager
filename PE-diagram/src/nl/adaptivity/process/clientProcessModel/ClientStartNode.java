package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;

import nl.adaptivity.process.processModel.IXmlImportType;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.processModel.StartNode;

public class ClientStartNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements StartNode<T> {

  private ProcessNodeSet<T> aSuccessors;
  public ClientStartNode(ClientProcessModel<T> pOwner) {
    super(pOwner);
  }

  public ClientStartNode(final String pId, ClientProcessModel<T> pOwner) {
    super(pId, pOwner);
  }

  protected ClientStartNode(final ClientStartNode<T> pOrig) {
    super(pOrig);
    aSuccessors = pOrig.aSuccessors==null ? null : pOrig.aSuccessors.clone();
  }

  @Override
  public List<IXmlImportType> getImports() {
    return super.getImports();
  }

  @Override
  public ProcessNodeSet<T> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = ProcessNodeSet.processNodeSet();
    }
    return aSuccessors;
  }

  @Override
  public void removeSuccessor(T pNode) {
    if (aSuccessors!=null) {
      aSuccessors.remove(pNode);
    }
  }

  @Override
  public ProcessNodeSet<T> getPredecessors() {
    return ProcessNodeSet.empty();
  }

  @Override
  public void setPredecessors(Collection<? extends T> pPredecessors) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void setPredecessor(T pPredecessor) {
    throw new UnsupportedOperationException("Start nodes have no predecessors");
  }

  @Override
  public void removePredecessor(T pNode) {
    throw new UnsupportedOperationException("Start nodes have no predecessors");
  }

  @Override
  public boolean isPredecessorOf(T pNode) {
    return aSuccessors.contains(pNode);
  }

  @Override
  public void addSuccessor(T pNode) {
    if (aSuccessors==null) {
      aSuccessors = ProcessNodeSet.processNodeSet(1);
    }
    pNode.setOwner(getOwner());
    aSuccessors.add(pNode);
  }

}
