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

import nl.adaptivity.diagram.Rectangle;
import android.graphics.Bitmap;


public interface IAndroidCanvas extends nl.adaptivity.diagram.Canvas<AndroidStrategy, AndroidPen, AndroidPath>{
  IAndroidCanvas scale(double scale);
  IAndroidCanvas translate(double left, double right);
  @Override
  IAndroidCanvas childCanvas(Rectangle area, double scale);

  void drawBitmap(double left, double top, Bitmap bitmap, AndroidPen pen);
}
