package nl.adaptivity.process.processModel;

import java.util.Collection;

import nl.adaptivity.diagram.Positioned;


public interface ProcessNode extends Positioned {

  public abstract Collection<ProcessNodeImpl> getPredecessors();

  public abstract void setPredecessors(Collection<? extends ProcessNode> predecessors);

  public abstract void addSuccessor(ProcessNode pNode);

  public abstract Collection<ProcessNode> getSuccessors();

  public abstract String getId();

  public abstract boolean isPredecessorOf(ProcessNode pNode);

}