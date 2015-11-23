package nl.adaptivity.process.processModel;

import nl.adaptivity.diagram.Positioned;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;


public interface ProcessNode<T extends ProcessNode<T>> extends Positioned, Identifiable, XmlSerializable {

  interface Visitor<R> {
    R visitStartNode(StartNode<?> startNode);
    R visitActivity(Activity<?> activity);
    R visitSplit(Split<?> split);
    R visitJoin(Join<?> join);
    R visitEndNode(EndNode<?> endNode);
  }

  @Nullable
  ProcessModelBase<T> getOwnerModel();

  void setOwnerModel(@NotNull ProcessModelBase<T> ownerModel);

  T asT();

  /**
   * Make all references (predecessors successors) directly reference nodes. Not names.
   */
  void resolveRefs();

  @Nullable
  Set<? extends Identifiable> getPredecessors();

  void setPredecessors(Collection<? extends Identifiable> predecessors);

  void removePredecessor(Identifiable node);

  void addPredecessor(Identifiable node);

  void addSuccessor(Identifiable node);

  void removeSuccessor(Identifiable node);

  @Nullable
  Set<? extends Identifiable> getSuccessors();

  int getMaxSuccessorCount();

  int getMaxPredecessorCount();

  void setSuccessors(Collection<? extends Identifiable> successors);

  String getLabel();

  boolean isPredecessorOf(T node);

  <R> R visit(Visitor<R> visitor);

  List<? extends IXmlResultType> getResults();

  List<? extends IXmlDefineType> getDefines();

}