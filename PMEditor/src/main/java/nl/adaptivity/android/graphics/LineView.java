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

package nl.adaptivity.android.graphics;

import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.AndroidCanvas;
import nl.adaptivity.diagram.android.AndroidPath;
import nl.adaptivity.diagram.android.AndroidPen;
import nl.adaptivity.diagram.android.AndroidStrategy;
import nl.adaptivity.diagram.android.IAndroidCanvas;
import nl.adaptivity.diagram.android.LightView;
import nl.adaptivity.process.diagram.Connectors;
import android.graphics.Canvas;
import android.graphics.RectF;


public class LineView extends
AbstractLightView implements LightView {

  private float mX1;
  private float mY1;
  private float mX2;
  private float mY2;

  public LineView(float x1, float y1, float x2, float y2) {
    mX1 = x1;
    mY1 = y1;
    mX2 = x2;
    mY2 = y2;
  }

  @Override
  public void getBounds(RectF target) {
    if (mX1<=mX2) {
      target.left =mX1;
      target.right=mX2;
    } else {
      target.left =mX2;
      target.right=mX1;
    }
    if (mY1<=mY2) {
      target.top =mY1;
      target.bottom=mY2;
    } else {
      target.top =mY2;
      target.bottom=mY1;
    }
  }

  @Override
  public void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale) {
    float x1, x2, y1, y2;
    if (mX1<=mX2) { x1=0; x2=(float) ((mX2-mX1)*scale); } else { x2=0; x1=(float) ((mX1-mX2)*scale); }
    if (mY1<=mY2) { y1=0; y2=(float) ((mY2-mY1)*scale); } else { y2=0; y1=(float) ((mY1-mY2)*scale); }
    drawArrow(canvas, theme, x1, y1, x2, y2, scale);
  }

  public static void drawArrow(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, float canvasX1, float canvasY1, float canvasX2, float canvasY2, double scale) {
    IAndroidCanvas androidCanvas = new AndroidCanvas(canvas, theme).childCanvas(0d, 0d, scale);
    Connectors.drawArrow(androidCanvas, theme, canvasX1/scale, canvasY1/scale, 0, canvasX2/scale, canvasY2/scale, 0);
    return;
  }

  public static void drawStraightArrow(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, float canvasX1, float canvasY1, float canvasX2, float canvasY2, double scale) {
    IAndroidCanvas androidCanvas = new AndroidCanvas(canvas, theme).childCanvas(0d, 0d, scale);
    Connectors.drawStraightArrow(androidCanvas, theme, canvasX1/scale, canvasY1/scale, canvasX2/scale, canvasY2/scale);
    return;
  }

  @Override
  public void move(float x, float y) {
    mX1+=x;
    mX2+=x;
    mY1+=y;
    mY2+=y;
  }

  @Override
  public void setPos(float left, float top) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setPos(float x1, float y1, float x2, float y2) {
    mX1 = x1;
    mY1 = y1;
    mX2 = x2;
    mY2 = y2;
  }

}
