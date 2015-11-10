package nl.adaptivity.process.processModel;

import java.util.Collection;
import java.util.Set;

import nl.adaptivity.diagram.Positioned;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.Nullable;


public interface ProcessNode<T extends ProcessNode<T>> extends Positioned, Identifiable {

  interface Visitor<R> {
    R visitStartNode(StartNode<?> startNode);
    R visitActivity(Activity<?> activity);
    R visitSplit(Split<?> split);
    R visitJoin(Join<?> join);
    R visitEndNode(EndNode<?> endNode);
  }

  @Nullable
  Set<? extends Identifiable> getPredecessors();

  void setPredecessors(Collection<? extends Identifiable> predecessors);

  void removePredecessor(Identifiable node);

  void addPredecessor(Identifiable node);

  void addSuccessor(T node);

  void removeSuccessor(T node);

  @Nullable
  Set<? extends T> getSuccessors();

  int getMaxSuccessorCount();

  int getMaxPredecessorCount();

  void setSuccessors(Collection<? extends T/** TODO Identifyable*/> successors);

  String getLabel();

  boolean isPredecessorOf(T node);

  <R> R visit(Visitor<R> visitor);

  Collection<? extends IXmlResultType> getResults();

  Collection<? extends IXmlDefineType> getDefines();

}