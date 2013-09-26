package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.engine.ProcessNodeSet;


public class ClientJoinNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements Join<T> {

  private Set<T> aPredecessors;

  private Set<T> aSuccessors;

  private int aMin;

  private int aMax;

  @Override
  public Set<T> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<T>();
    }
    return aSuccessors;
  }

  @Override
  public Set<T> getPredecessors() {
    return aPredecessors;
  }

  @Override
  public void setPredecessors(Collection<? extends T> pPredecessors) {
    aPredecessors = new HashSet<T>(pPredecessors);
  }

  @Override
  protected void setPredecessor(T pPredecessor) {
    if (aPredecessors==null) {
      aPredecessors = new HashSet<T>(1);
    } else {
      aPredecessors.clear();
    }

    aPredecessors.add(pPredecessor);
  }

  @Override
  public void setSuccessor(T pNode) {
    if (aSuccessors==null) {
      aSuccessors = new HashSet<T>(1);
    } else {
      aSuccessors.clear();
    }

    aSuccessors.add(pNode);
  }

  @Override
  public void addSuccessor(T pNode) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet<T>(1);
    }
    aSuccessors.add(pNode);
  }

  @Override
  public boolean isPredecessorOf(T pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
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
