/*
 * Copyright (c) 2017.
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

import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;


public final class AndroidPath implements DiagramPath<AndroidPath> {

  private final Path mPath = new Path();

  @NonNull
  @Override
  public AndroidPath moveTo(final double x, final double y) {
    mPath.moveTo((float)x, (float) y);
    return this;
  }

  @NonNull
  @Override
  public AndroidPath lineTo(final double x, final double y) {
    mPath.lineTo((float)x, (float) y);
    return this;
  }

  @NonNull
  @Override
  public AndroidPath cubicTo(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3) {
    mPath.cubicTo((float)x1, (float)y1, (float)x2, (float)y2, (float)x3, (float)y3);
    return this;
  }

  @NonNull
  @Override
  public AndroidPath close() {
    mPath.close();
    return this;
  }

  @NonNull
  public Path getPath() {
    return mPath;
  }

  @NonNull
  @Override
  public Rectangle getBounds(final Rectangle dest, final Pen<?> stroke) {
    final RectF bounds = new RectF();
    mPath.computeBounds(bounds, false);
    dest.set(bounds.left, bounds.top, bounds.width(), bounds.height());
    return dest;
  }
}
