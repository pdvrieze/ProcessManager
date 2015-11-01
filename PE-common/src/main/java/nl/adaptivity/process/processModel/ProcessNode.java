package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.Set;

import nl.adaptivity.diagram.Positioned;
import nl.adaptivity.process.util.Identifiable;


public interface ProcessNode<T extends ProcessNode<T>> extends Positioned, Identifiable {

  public static interface Visitor<R> {
    R visitStartNode(StartNode<?> pStartNode);
    R visitActivity(Activity<?> pActivity);
    R visitSplit(Split<?> pSplit);
    R visitJoin(Join<?> pJoin);
    R visitEndNode(EndNode<?> pEndNode);
  }

  public Set<? extends Identifiable> getPredecessors();

  public void setPredecessors(Collection<? extends Identifiable> predecessors);

  public void removePredecessor(Identifiable pNode);

  public void addPredecessor(Identifiable pNode);

  public void addSuccessor(T pNode);

  public void removeSuccessor(T pNode);

  public Set<? extends T> getSuccessors();

  public int getMaxSuccessorCount();

  public int getMaxPredecessorCount();

  public void setSuccessors(Collection<? extends T/** TODO Identifyable*/> pSuccessors);

  public String getLabel();

  public boolean isPredecessorOf(T pNode);

  public <R> R visit(Visitor<R> pVisitor);

  public Collection<? extends IXmlResultType> getResults();

  public Collection<? extends IXmlDefineType> getDefines();

}