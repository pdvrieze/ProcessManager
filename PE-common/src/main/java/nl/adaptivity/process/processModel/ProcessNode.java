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
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;


/**
 * Created by pdvrieze on 27/11/16.
 */
public interface ProcessNode<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends Positioned, Identifiable, XmlSerializable {

  interface Visitor<R> {
    R visitStartNode(StartNode<?, ?> startNode);
    R visitActivity(Activity<?, ?> activity);
    R visitSplit(Split<?, ?> split);
    R visitJoin(Join<?, ?> join);
    R visitEndNode(EndNode<?, ?> endNode);
  }

  T asT();

  boolean isPredecessorOf(T node);

  <R> R visit(Visitor<R> visitor);

  XmlResultType getResult(String name);

  XmlDefineType getDefine(String name);

  @Nullable
  M getOwnerModel();

  @NotNull
  Set<? extends Identifiable> getPredecessors();

  @NotNull
  Set<? extends Identifiable> getSuccessors();

  int getMaxSuccessorCount();

  int getMaxPredecessorCount();

  String getLabel();

  List<? extends IXmlResultType> getResults();

  List<? extends IXmlDefineType> getDefines();

  String getIdBase();
}
