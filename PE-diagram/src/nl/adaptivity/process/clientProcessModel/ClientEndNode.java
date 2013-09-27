package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public class ClientEndNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements EndNode<T> {

  private ProcessNodeSet<T> aPredecessor = ProcessNodeSet.singleton();

  @Override
  public ProcessNodeSet<T> getSuccessors() {
    return ProcessNodeSet.empty();
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
  public List<IXmlExportType> getExports() {
    return super.getExports();
  }

  @Override
  public void setExports(Collection<? extends IXmlExportType> pExports) {
    super.setExports(pExports);
  }

  @Override
  public ProcessNodeSet<T> getPredecessors() {
    return aPredecessor;
  }

  @Override
  public T getPredecessor() {
    return aPredecessor.size()==0 ? null : aPredecessor.get(0);
  }

}
