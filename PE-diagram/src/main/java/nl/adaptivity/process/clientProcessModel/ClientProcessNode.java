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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.MutableProcessNode;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.process.util.IdentifyableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface ClientProcessNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends MutableProcessNode<T, M> {

  interface Builder<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ProcessNode.Builder<T, M> {

    boolean isCompat();
    void setCompat(boolean value);

    @NotNull
    @Override
    ClientProcessNode<T, M> build(M newOwner);
  }

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
  IdentifyableSet<? extends Identified> getPredecessors();

  @NotNull
  @Override
  IdentifyableSet<? extends Identified> getSuccessors();

  void setOwnerModel(@Nullable M owner);

  void setId(@Nullable String id);

  void setLabel(@Nullable String label);

  @Nullable
  M getOwnerModel();

}
