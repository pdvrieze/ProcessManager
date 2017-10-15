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

package nl.adaptivity.process.editor.android;

import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.android.IAndroidCanvas;
import nl.adaptivity.diagram.android.LWDrawableView;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.IDrawableProcessNode;


/**
 * A lightweight drawable view that adds label drawing for process nodes.
 * Created by pdvrieze on 10/01/16.
 */
public class LWProcessDrawableView extends LWDrawableView {

  public LWProcessDrawableView(final DrawableProcessNode.Builder item) {
    super(item);
  }

  @Override
  public DrawableProcessNode.Builder getItem() {
    return (DrawableProcessNode.Builder) super.getItem();
  }

  @Override
  protected void onDraw(final IAndroidCanvas androidCanvas, final Rectangle clipBounds) {
    super.onDraw(androidCanvas, clipBounds);
    getItem().drawLabel(androidCanvas, clipBounds, 0, 0);
  }
}
