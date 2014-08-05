package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.Set;

import nl.adaptivity.diagram.Positioned;


public interface ProcessNode<T extends ProcessNode<T>> extends Positioned {

  public static interface Visitor<R> {
    R visitStartNode(StartNode<?> pStartNode);
    R visitActivity(Activity<?> pActivity);
    R visitSplit(Split<?> pSplit);
    R visitJoin(Join<?> pJoin);
    R visitEndNode(EndNode<?> pEndNode);
  }

  public Set<? extends T> getPredecessors();

  public void setPredecessors(Collection<? extends T> predecessors);

  public void removePredecessor(T pNode);

  public void addPredecessor(T pNode);

  public void addSuccessor(T pNode);

  public void removeSuccessor(T pNode);

  public Set<? extends T> getSuccessors();

  public int getMaxSuccessorCount();

  public int getMaxPredecessorCount();

  public void setSuccessors(Collection<? extends T> pSuccessors);

  public String getId();

  public String getLabel();

  public boolean isPredecessorOf(T pNode);

  public <R> R visit(Visitor<R> pVisitor);

  public Collection<? extends IXmlImportType> getImports();

  public Collection<? extends IXmlExportType> getExports();

}