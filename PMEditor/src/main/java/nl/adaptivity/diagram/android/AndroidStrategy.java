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

package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.DrawingStrategy;
import android.graphics.Paint;

public enum AndroidStrategy implements DrawingStrategy<AndroidStrategy, AndroidPen, AndroidPath>{
  INSTANCE
  ;

  @Override
  public AndroidPen newPen() {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setStrokeCap(Paint.Cap.SQUARE);
    paint.setAntiAlias(true);
    return new AndroidPen(paint);
  }

  @Override
  public AndroidPath newPath() {
    return new AndroidPath();
  }

}