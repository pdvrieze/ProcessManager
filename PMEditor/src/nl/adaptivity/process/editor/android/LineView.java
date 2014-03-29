package nl.adaptivity.process.editor.android;

import nl.adaptivity.android.graphics.AbstractLightView;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.AndroidPath;
import nl.adaptivity.diagram.android.AndroidPen;
import nl.adaptivity.diagram.android.AndroidStrategy;
import nl.adaptivity.diagram.android.LightView;
import nl.adaptivity.process.diagram.ProcessThemeItems;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;


public class LineView extends
AbstractLightView implements LightView {

  private static final double LEADERLEN = 17d;
  private static final double HEADLEN= 10d;
  private static final double HEADANGLE=(35*Math.PI)/180;
  private static final double HEADDX = Math.cos(HEADANGLE)*HEADLEN;
  private static final double HEADDY = Math.sin(HEADANGLE)*HEADLEN;
  private static final double MINANGLE = (1*Math.PI)/180;

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

//    pCanvas.drawLine(x1, y1, x2, y2, paint);
  }

  public static void drawArrow(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, float canvasX1, float canvasY1, float canvasX2, float canvasY2, double pScale) {
    double dx = canvasX2-canvasX1;
    double dy = canvasY2-canvasY1;
    double angle = Math.atan2(dy,dx);
    if (Math.abs(angle)<MINANGLE || Math.abs(canvasX2-canvasX1)<LEADERLEN*pScale){
      drawStraightArrow(pCanvas, pTheme, canvasX1, canvasY1, canvasX2, canvasY2, pScale);

      return;
    }
    Paint paint = pTheme.getPen(ProcessThemeItems.LINE, 0).scale(pScale).getPaint();

    final double headDx;
    final float x2;
    final double x3;
    final double y3 = canvasY2;
    final double angle2;
    final float capCorrect;

    if (canvasX1<canvasX2) { // left to right
      headDx = -HEADDX*pScale;
      x2 = (float) (canvasX2-(0.5*paint.getStrokeWidth())/Math.sin(HEADANGLE));
      /* Point 3 represents the focal point of a spline.
       * Point 4 is the point where the spline stops and a straight line starts.
       */
      x3 = x2-LEADERLEN*pScale;
      angle2 = Math.atan2((canvasY1-y3),(canvasX1-x3));
      capCorrect = paint.getStrokeCap()==Paint.Cap.BUTT? 0f: paint.getStrokeWidth()/-2f;
    } else { //right to left
      headDx = HEADDX*pScale;
      x2 = (float) (canvasX2+(0.5*paint.getStrokeWidth())/Math.sin(HEADANGLE));
      /* Point 3 represents the focal point of a spline.
       * Point 4 is the point where the spline stops and a straight line starts.
       */
      x3 = x2+LEADERLEN*pScale;
      angle2 = Math.atan2((canvasY1-y3),(canvasX1-x3));
      capCorrect = paint.getStrokeCap()==Paint.Cap.BUTT? 0f: paint.getStrokeWidth()/2f;
    }
    final float x4 = (float) (x3+Math.cos(angle2)*LEADERLEN*pScale);
    final float y4 = (float) (y3+Math.sin(angle2)*LEADERLEN*pScale);

    boolean tooShort = false;
    if (dx>0) {
      if (x4<canvasX1) { tooShort=true; }
    } else {
      if (x4>canvasX1) { tooShort=true; }
    }
    if (dy>0) {
      if (y4<canvasY1) { tooShort=true; }
    } else { // dy<0
      if (y4>canvasY1) { tooShort=true; }
    }
    if (tooShort) {
      drawStraightArrow(pCanvas, pTheme, canvasX1, canvasY1, canvasX2, canvasY2, pScale);
      return;
    }

    Path arrowPath = new Path();
    arrowPath.moveTo(canvasX1, canvasY1);
    arrowPath.lineTo(x4, y4);
    arrowPath.cubicTo((float)x3, (float)y3, (float)x3, (float)y3, x2+capCorrect, canvasY2);
    final float headDy = (float) (HEADDY*pScale);
    arrowPath.moveTo((float)(x2+headDx), canvasY2-headDy);
    arrowPath.lineTo(x2, canvasY2);
    arrowPath.lineTo((float)(x2+headDx), canvasY2+headDy);
    pCanvas.drawPath(arrowPath, paint);
  }

  public static void drawStraightArrow(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, float canvasX1, float canvasY1, float canvasX2, float canvasY2, double pScale) {
    Paint paint = pTheme.getPen(ProcessThemeItems.LINE, 0).scale(pScale).getPaint();
    double dx = canvasX2-canvasX1;
    double dy = canvasY2-canvasY1;
    double angle = Math.atan2(dy,dx);

    pCanvas.drawLine(canvasX1, canvasY1, canvasX2, canvasY2, paint);
    Path arrowPath = new Path();

    double headLen = Math.sqrt((pScale*HEADDX*pScale*HEADDX)+(pScale*HEADDY*pScale*HEADDY));
    float x2 = canvasX2;
    float y2 = canvasY2;

    arrowPath.moveTo(canvasX1, canvasY1);
    arrowPath.lineTo(x2, y2);

    double headAngle = angle+Math.PI-HEADANGLE;
    final float headDX1 = (float) (Math.cos(headAngle)*headLen);
    final float headDY1 = (float) (Math.sin(headAngle)*headLen);
    headAngle = angle+Math.PI+HEADANGLE;
    final float headDX2 = (float) (Math.cos(headAngle)*headLen);
    final float headDY2 = (float) (Math.sin(headAngle)*headLen);

    arrowPath.moveTo(x2+headDX1, y2+headDY1);
    arrowPath.lineTo(x2, y2);
    arrowPath.lineTo(x2+headDX2, y2+headDY2);
    pCanvas.drawPath(arrowPath, paint);
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
