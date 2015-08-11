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
  public void getBounds(RectF pTarget) {
    if (aX1<=aX2) {
      pTarget.left =aX1;
      pTarget.right=aX2;
    } else {
      pTarget.left =aX2;
      pTarget.right=aX1;
    }
    if (aY1<=aY2) {
      pTarget.top =aY1;
      pTarget.bottom=aY2;
    } else {
      pTarget.top =aY2;
      pTarget.bottom=aY1;
    }
  }

  @Override
  public void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale) {
    float x1, x2, y1, y2;
    if (aX1<=aX2) { x1=0; x2=(float) ((aX2-aX1)*pScale); } else { x2=0; x1=(float) ((aX1-aX2)*pScale); }
    if (aY1<=aY2) { y1=0; y2=(float) ((aY2-aY1)*pScale); } else { y2=0; y1=(float) ((aY1-aY2)*pScale); }
    drawArrow(pCanvas, pTheme, x1, y1, x2, y2, pScale);
  }

  public static void drawArrow(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, float canvasX1, float canvasY1, float canvasX2, float canvasY2, double pScale) {
    IAndroidCanvas canvas = new AndroidCanvas(pCanvas, pTheme).childCanvas(new Rectangle(0d/*Math.min(aX1, aX2)*/, 0d,0d/*Math.min(aY1, aY2)*/, 0d), pScale);
    Connectors.drawArrow(canvas, pTheme, canvasX1/pScale, canvasY1/pScale, 0, canvasX2/pScale, canvasY2/pScale, 0);
    return;
  }

  public static void drawStraightArrow(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, float canvasX1, float canvasY1, float canvasX2, float canvasY2, double pScale) {
    IAndroidCanvas canvas = new AndroidCanvas(pCanvas, pTheme).childCanvas(new Rectangle(0d/*Math.min(aX1, aX2)*/, 0d,0d/*Math.min(aY1, aY2)*/, 0d), pScale);
    Connectors.drawStraightArrow(canvas, pTheme, canvasX1/pScale, canvasY1/pScale, canvasX2/pScale, canvasY2/pScale);
    return;
  }

  @Override
  public void move(float pX, float pY) {
    aX1+=pX;
    aX2+=pX;
    aY1+=pY;
    aY2+=pY;
  }

  @Override
  public void setPos(float pLeft, float pTop) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setPos(float pX1, float pY1, float pX2, float pY2) {
    aX1 = pX1;
    aY1 = pY1;
    aX2 = pX2;
    aY2 = pY2;
  }

}
