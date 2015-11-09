package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.Set;

import nl.adaptivity.diagram.Positioned;
import nl.adaptivity.process.util.Identifiable;


public interface ProcessNode<T extends ProcessNode<T>> extends Positioned, Identifiable {

  interface Visitor<R> {
    R visitStartNode(StartNode<?> pStartNode);
    R visitActivity(Activity<?> pActivity);
    R visitSplit(Split<?> pSplit);
    R visitJoin(Join<?> pJoin);
    R visitEndNode(EndNode<?> pEndNode);
  }

  Set<? extends Identifiable> getPredecessors();

  void setPredecessors(Collection<? extends Identifiable> predecessors);

  void removePredecessor(Identifiable pNode);

  void addPredecessor(Identifiable pNode);

  void addSuccessor(T pNode);

  void removeSuccessor(T pNode);

  Set<? extends T> getSuccessors();

  int getMaxSuccessorCount();

  int getMaxPredecessorCount();

  void setSuccessors(Collection<? extends T/** TODO Identifyable*/> pSuccessors);

  String getLabel();

  boolean isPredecessorOf(T pNode);

  <R> R visit(Visitor<R> pVisitor);

  Collection<? extends IXmlResultType> getResults();

  Collection<? extends IXmlDefineType> getDefines();

}