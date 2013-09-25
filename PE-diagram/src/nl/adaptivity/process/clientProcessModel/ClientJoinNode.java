package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;


public class ClientJoinNode extends ClientProcessNode implements Join {

  private Set<ProcessNode> aPredecessors;

  private Set<ProcessNode> aSuccessors;

  private int aMin;

  private int aMax;

  @Override
  public Set<ProcessNode> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ProcessNode>();
    }
    return aSuccessors;
  }

  @Override
  public Set<ProcessNode> getPredecessors() {
    return aPredecessors;
  }

  @Override
  public void setPredecessors(Collection<? extends ProcessNode> pPredecessors) {
    aPredecessors = new HashSet<ProcessNode>(pPredecessors);
  }

  @Override
  protected void setPredecessor(ProcessNode pPredecessor) {
    if (aPredecessors==null) {
      aPredecessors = new HashSet<ProcessNode>(1);
    } else {
      aPredecessors.clear();
    }

    aPredecessors.add(pPredecessor);
  }

  @Override
  public void setSuccessor(ProcessNode pNode) {
    if (aSuccessors==null) {
      aSuccessors = new HashSet<ProcessNode>(1);
    } else {
      aSuccessors.clear();
    }

    aSuccessors.add(pNode);
  }

  @Override
  public boolean isPredecessorOf(ProcessNode pNode) {
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
