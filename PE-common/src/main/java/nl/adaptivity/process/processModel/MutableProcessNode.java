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

import java.util.Collection;
import java.util.List;
import java.util.Set;


public interface MutableProcessNode<T extends MutableProcessNode<T, M>, M extends ProcessModel<T, M>> extends ProcessNode<T, M> {

  void setId(String id);

  void setOwnerModel(@NotNull M ownerModel);

  /**
   * Make all references (predecessors successors) directly reference nodes. Not names.
   */
  void resolveRefs();

  void setPredecessors(Collection<? extends Identifiable> predecessors);

  void removePredecessor(Identifiable node);

  void addPredecessor(Identifiable node);

  void addSuccessor(Identifiable node);

  void removeSuccessor(Identifiable node);

  void setSuccessors(Collection<? extends Identifiable> successors);

}