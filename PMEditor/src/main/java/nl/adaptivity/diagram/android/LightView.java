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

package nl.adaptivity.diagram.android;

import android.graphics.Canvas;
import android.graphics.RectF;
import nl.adaptivity.diagram.Theme;

/** An interface for a lightweight view. */
public interface LightView {

  void setFocussed(boolean focussed);

  boolean isFocussed();

  void setSelected(boolean selected);

  boolean isSelected();

  void setTouched(boolean touched);

  boolean isTouched();

  void getBounds(RectF target);

  void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale);

  void move(float x, float y);

  void setPos(float left, float top);

  void setActive(boolean active);

  boolean isActive();

}
