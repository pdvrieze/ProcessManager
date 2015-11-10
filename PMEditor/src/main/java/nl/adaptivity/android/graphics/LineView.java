package nl.adaptivity.android.graphics;

import nl.adaptivity.diagram.Rectangle;
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

  private float aX1;
  private float aY1;
  private float aX2;
  private float aY2;

  public LineView(float x1, float y1, float x2, float y2) {
    aX1 = x1;
    aY1 = y1;
    aX2 = x2;
    aY2 = y2;
  }

  @Override
  public void getBounds(RectF target) {
    if (aX1<=aX2) {
      target.left =aX1;
      target.right=aX2;
    } else {
      target.left =aX2;
      target.right=aX1;
    }
    if (aY1<=aY2) {
      target.top =aY1;
      target.bottom=aY2;
    } else {
      target.top =aY2;
      target.bottom=aY1;
    }
  }

  @Override
  public void draw(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, double scale) {
    float x1, x2, y1, y2;
    if (aX1<=aX2) { x1=0; x2=(float) ((aX2-aX1)*scale); } else { x2=0; x1=(float) ((aX1-aX2)*scale); }
    if (aY1<=aY2) { y1=0; y2=(float) ((aY2-aY1)*scale); } else { y2=0; y1=(float) ((aY1-aY2)*scale); }
    drawArrow(canvas, theme, x1, y1, x2, y2, scale);
  }

  public static void drawArrow(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, float canvasX1, float canvasY1, float canvasX2, float canvasY2, double scale) {
    IAndroidCanvas androidCanvas = new AndroidCanvas(canvas, theme).childCanvas(new Rectangle(0d/*Math.min(aX1, aX2)*/, 0d,0d/*Math.min(aY1, aY2)*/, 0d), scale);
    Connectors.drawArrow(androidCanvas, theme, canvasX1/scale, canvasY1/scale, 0, canvasX2/scale, canvasY2/scale, 0);
    return;
  }

  public static void drawStraightArrow(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, float canvasX1, float canvasY1, float canvasX2, float canvasY2, double scale) {
    IAndroidCanvas androidCanvas = new AndroidCanvas(canvas, theme).childCanvas(new Rectangle(0d/*Math.min(aX1, aX2)*/, 0d,0d/*Math.min(aY1, aY2)*/, 0d), scale);
    Connectors.drawStraightArrow(androidCanvas, theme, canvasX1/scale, canvasY1/scale, canvasX2/scale, canvasY2/scale);
    return;
  }

  @Override
  public void move(float x, float y) {
    aX1+=x;
    aX2+=x;
    aY1+=y;
    aY2+=y;
  }

  @Override
  public void setPos(float left, float top) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setPos(float x1, float y1, float x2, float y2) {
    aX1 = x1;
    aY1 = y1;
    aX2 = x2;
    aY2 = y2;
  }

}
