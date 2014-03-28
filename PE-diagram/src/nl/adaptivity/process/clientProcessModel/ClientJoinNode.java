package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;

import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public class ClientJoinNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Join<T> {

  private ProcessNodeSet<T> aPredecessors;

  private ProcessNodeSet<T> aSuccessors;

  private int aMin;

  private int aMax;

  public ClientJoinNode(ClientProcessModel<T> pOwner) {
    super(pOwner);
  }

  public ClientJoinNode(String pId, ClientProcessModel<T> pOwner) {
    super(pId, pOwner);
  }

  protected ClientJoinNode(ClientJoinNode<T> pOrig) {
    super(pOrig);
    aPredecessors = pOrig.aPredecessors == null ? null : pOrig.aPredecessors.clone();
    aSuccessors = pOrig.aSuccessors == null ? null : pOrig.aSuccessors.clone();
  }

  @Override
  public ProcessNodeSet<T> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = ProcessNodeSet.processNodeSet();
    }
    return aSuccessors;
  }

  @Override
  public ProcessNodeSet<T> getPredecessors() {
    if (aPredecessors==null) {
      aPredecessors = ProcessNodeSet.processNodeSet();
    }
    return aPredecessors;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setPredecessors(Collection<? extends T> pPredecessors) {
    aPredecessors = ProcessNodeSet.processNodeSet(pPredecessors);
    for(T pred:getPredecessors()) {
      if (!pred.getSuccessors().contains(this)) {
        pred.addSuccessor((T) this);
      }
    }
  }

  @Override
  public void addSuccessor(T pNode) {
    if (aSuccessors==null) {
      aSuccessors = ProcessNodeSet.processNodeSet(1);
    }
    aSuccessors.add(pNode);
  }

  @Override
  public void removeSuccessor(T pNode) {
    if (aSuccessors!=null) {
      aSuccessors.remove(pNode);
    }
  }

  @Override
  public boolean isPredecessorOf(T pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void removePredecessor(T pNode) {
    if (aPredecessors!=null) {
      aPredecessors.remove(pNode);
    }
  }

  @Override
  public void setMax(int pMax) {
    aMax = pMax;
  }

  @Override
  public int getMax() {
    return aMax;
  }

  @Override
  public void setMin(int pMin) {
    aMin = pMin;
  }

  @Override
  public int getMin() {
    return aMin;
  }

}
