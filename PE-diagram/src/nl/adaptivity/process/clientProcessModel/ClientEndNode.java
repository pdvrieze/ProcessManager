package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.XmlExportType;


public class ClientEndNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements EndNode<T> {

  private T aPredecessor;

  @Override
  public Set<? extends T> getSuccessors() {
    return Collections.emptySet();
  }

  @Override
  public void setSuccessor(T pNode) {
    throw new UnsupportedOperationException("end nodes never have successors");
  }

  @Override
  public void addSuccessor(T pNode) {
    throw new UnsupportedOperationException("end nodes never have successors");
  }

  @Override
  public boolean isPredecessorOf(T pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public List<XmlExportType> getExports() {
    return super.getExports();
  }

  @Override
  public void setExports(Collection<? extends XmlExportType> pExports) {
    super.setExports(pExports);
  }

  @Override
  public Set<? extends T> getPredecessors() {
    return Collections.singleton(aPredecessor);
  }

  @Override
  public T getPredecessor() {
    return aPredecessor;
  }

  @Override
  public void setPredecessor(T pPredecessor) {
    aPredecessor = pPredecessor;
  }

}
