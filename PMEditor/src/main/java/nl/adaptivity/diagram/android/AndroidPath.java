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

import android.graphics.Path;
import nl.adaptivity.diagram.DiagramPath;


public final class AndroidPath implements DiagramPath<AndroidPath> {

  private Path mPath = new Path();

  @Override
  public AndroidPath moveTo(double x, double y) {
    mPath.moveTo((float)x, (float) y);
    return this;
  }

  @Override
  public AndroidPath lineTo(double x, double y) {
    mPath.lineTo((float)x, (float) y);
    return this;
  }

  @Override
  public AndroidPath cubicTo(double x1, double y1, double x2, double y2, double x3, double y3) {
    mPath.cubicTo((float)x1, (float)y1, (float)x2, (float)y2, (float)x3, (float)y3);
    return this;
  }

  @Override
  public AndroidPath close() {
    mPath.close();
    return this;
  }

  public Path getPath() {
    return mPath;
  }

}
