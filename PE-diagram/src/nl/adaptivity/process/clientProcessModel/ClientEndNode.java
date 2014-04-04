package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public class ClientEndNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements EndNode<T> {

  public ClientEndNode(ClientProcessModel<T> pOwner) {
    super(pOwner);
  }

  public ClientEndNode(String pId, ClientProcessModel<T> pOwner) {
    super(pId, pOwner);
  }

  protected ClientEndNode(ClientEndNode<T> pOrig) {
    super(pOrig);
  }

  @Override
  public int getMaxSuccessorCount() {
    return 0;
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
  public T getPredecessor() {
    ProcessNodeSet<T> list = getPredecessors();
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public void setPredecessor(T pPredecessor) {
    T previous = getPredecessor();
    if (previous==null) {
      removePredecessor(previous);
    }
    addPredecessor(pPredecessor);
  }

}
