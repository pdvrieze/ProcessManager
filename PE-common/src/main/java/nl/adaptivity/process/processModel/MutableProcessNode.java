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

import nl.adaptivity.process.util.Identified;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


public interface MutableProcessNode<T extends MutableProcessNode<T, M>, M extends ProcessModel<T, M>> extends ProcessNode<T, M> {

  void setId(@NotNull String id);

  void setOwnerModel(@NotNull M ownerModel);

  void setPredecessors(Collection<? extends Identified> predecessors);

  void removePredecessor(Identified node);

  void addPredecessor(Identified nodeId);

  void addSuccessor(Identified node);

  void removeSuccessor(Identified node);

  void setSuccessors(Collection<? extends Identified> successors);

}