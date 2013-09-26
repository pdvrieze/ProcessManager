package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.Set;

import nl.adaptivity.diagram.Positioned;


public interface ProcessNode<T extends ProcessNode<T>> extends Positioned {

  public Set<? extends T> getPredecessors();

  public void setPredecessors(Collection<? extends T> predecessors);

  public void addSuccessor(T pNode);

  public Set<? extends T> getSuccessors();

  public void setSuccessors(Collection<? extends T> pSuccessors);

  public String getId();

  public boolean isPredecessorOf(T pNode);

}