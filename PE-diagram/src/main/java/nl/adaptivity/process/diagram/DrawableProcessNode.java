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

package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientProcessNode;
import nl.adaptivity.process.processModel.ModelCommon;
import org.jetbrains.annotations.NotNull;


public interface DrawableProcessNode extends ClientProcessNode<DrawableProcessNode, DrawableProcessModel>, Drawable {

  interface Builder extends ClientProcessNode.Builder<DrawableProcessNode, DrawableProcessModel> {

    @NotNull
    @Override
    DrawableProcessNode build(ModelCommon<DrawableProcessNode, DrawableProcessModel> newOwner);
  }

  void setLabel(String label);

  <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top);

  /** Get the base to use for generating ID's. */
  String getIdBase();

  DrawableProcessNode clone();

  @NotNull
  ModelCommon<DrawableProcessNode, DrawableProcessModel> getOwnerModel();

  @NotNull
  @Override
  Builder builder();
}
