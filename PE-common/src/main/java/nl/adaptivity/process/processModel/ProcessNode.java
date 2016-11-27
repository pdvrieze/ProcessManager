/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel;

import nl.adaptivity.diagram.Positioned;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;


public interface ProcessNode<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends Positioned, Identifiable, XmlSerializable {

  interface Visitor<R> {
    R visitStartNode(StartNode<?, ?> startNode);
    R visitActivity(Activity<?, ?> activity);
    R visitSplit(Split<?, ?> split);
    R visitJoin(Join<?, ?> join);
    R visitEndNode(EndNode<?, ?> endNode);
  }

  void setId(String id);

  @Nullable
  M getOwnerModel();

  void setOwnerModel(@NotNull M ownerModel);

  T asT();

  /**
   * Make all references (predecessors successors) directly reference nodes. Not names.
   */
  void resolveRefs();

  @NotNull
  Set<? extends Identifiable> getPredecessors();

  void setPredecessors(Collection<? extends Identifiable> predecessors);

  void removePredecessor(Identifiable node);

  void addPredecessor(Identifiable node);

  void addSuccessor(Identifiable node);

  void removeSuccessor(Identifiable node);

  @NotNull
  Set<? extends Identifiable> getSuccessors();

  int getMaxSuccessorCount();

  int getMaxPredecessorCount();

  void setSuccessors(Collection<? extends Identifiable> successors);

  String getLabel();

  boolean isPredecessorOf(T node);

  <R> R visit(Visitor<R> visitor);

  List<? extends IXmlResultType> getResults();

  XmlResultType getResult(final String name);

  List<? extends IXmlDefineType> getDefines();

  XmlDefineType getDefine(final String name);

  String getIdBase();
}