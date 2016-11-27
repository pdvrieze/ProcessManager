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

package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.MutableProcessNode;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;


public interface ClientProcessNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends MutableProcessNode<T, M> {

  boolean isCompat();

  /**
   * Set the X coordinate of the reference point of the element. This is
   * normally the center.
   *
   * @param x The x coordinate
   */
  void setX(double x);

  /**
   * Set the Y coordinate of the reference point of the element. This is
   * normally the center of the symbol (excluding text).
   *
   * @param y
   */
  void setY(double y);

  @NotNull
  @Override
  Set<? extends Identifiable> getPredecessors();

  @NotNull
  @Override
  Set<? extends Identifiable> getSuccessors();

  void setOwnerModel(M owner);

  void setId(String id);

  @Nullable
  M getOwnerModel();

}
