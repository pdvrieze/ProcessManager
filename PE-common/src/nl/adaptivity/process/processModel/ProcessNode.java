package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.Set;

import nl.adaptivity.diagram.Positioned;


public interface ProcessNode extends Positioned {

  public Set<? extends ProcessNode> getPredecessors();

  public void setPredecessors(Collection<? extends ProcessNode> predecessors);

  public void addSuccessor(ProcessNode pNode);

  public Set<? extends ProcessNode> getSuccessors();

  public void setSuccessors(Collection<? extends ProcessNode> pSuccessors);

  public String getId();

  public boolean isPredecessorOf(ProcessNode pNode);

}