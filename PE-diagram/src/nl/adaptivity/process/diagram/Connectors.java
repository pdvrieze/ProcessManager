package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.process.diagram.ProcessThemeItems;


public final class Connectors {

  private Connectors() { /* No functions */ }
  
  private static final double LEADERLEN = 17d;
  private static final double HEADLEN= 10d;
  private static final double HEADANGLE=(35*Math.PI)/180;
  private static final double HEADDX = Math.cos(HEADANGLE)*HEADLEN;
  private static final double HEADDY = Math.sin(HEADANGLE)*HEADLEN;
  private static final double MINANGLE = (1*Math.PI)/180;

  public static <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> 
      void drawArrow(Canvas<S, PEN_T, PATH_T> pCanvas, Theme<S, PEN_T, PATH_T> pTheme, double canvasX1, double canvasY1, double a1, double canvasX2, double canvasY2, double a2) {

    PEN_T paint = pTheme.getPen(ProcessThemeItems.LINE, 0);
    
    PATH_T arrowPath = getArrow(pCanvas.getStrategy(), canvasX1, canvasY1, canvasX2, canvasY2, paint);
    pCanvas.drawPath(arrowPath, paint, null);
  }

  public static <PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>, S extends DrawingStrategy<S, PEN_T, PATH_T>> 
      PATH_T getArrow(S pStrategy, double pX1, double pY1, double pX2, double pY2, PEN_T pPen) {
    double dx = pX2-pX1;
    double dy = pY2-pY1;
    double angle = Math.atan2(dy,dx);
    if (Math.abs(angle)<MINANGLE || Math.abs(pX2-pX1)<LEADERLEN){
      return getStraightArrow(pStrategy, pX1, pY1, pX2, pY2, pPen);
    }

    final double headDx;
    final double x2;
    final double x3;
    final double y3 = pY2;
    final double angle2;
    final double capCorrect;

    // The distance that the miter extends from the focal point of the arrow.
    final double miterExtend = (0.5*pPen.getStrokeWidth())/Math.sin(HEADANGLE);
    if (pX1<pX2) { // left to right
      headDx = -HEADDX;
      x2 = (float) (pX2-miterExtend);
      /* Point 3 represents the focal point of a spline.
       * Point 4 is the point where the spline stops and a straight line starts.
       */
      x3 = x2-LEADERLEN;
      angle2 = Math.atan2((pY1-y3),(pX1-x3));
      capCorrect = pPen.getStrokeWidth()/-2f;
    } else { //right to left
      headDx = HEADDX;
      x2 = (float) (pX2+miterExtend);
      /* Point 3 represents the focal point of a spline.
       * Point 4 is the point where the spline stops and a straight line starts.
       */
      x3 = x2+LEADERLEN;
      angle2 = Math.atan2((pY1-y3),(pX1-x3));
      capCorrect = pPen.getStrokeWidth()/2f;
    }
    final float x4 = (float) (x3+Math.cos(angle2)*LEADERLEN);
    final float y4 = (float) (y3+Math.sin(angle2)*LEADERLEN);

    boolean tooShort = false;
    if (dx>0) {
      if (x4<pX1) { tooShort=true; }
    } else {
      if (x4>pX1) { tooShort=true; }
    }
    if (dy>0) {
      if (y4<pY1) { tooShort=true; }
    } else { // dy<0
      if (y4>pY1) { tooShort=true; }
    }
    if (tooShort) {
      return getStraightArrow(pStrategy, pX1, pY1, pX2, pY2, pPen);
    } else {
  
      PATH_T arrowPath = pStrategy.newPath();
      arrowPath.moveTo(pX1, pY1);
      arrowPath.lineTo(x4, y4);
      arrowPath.cubicTo((float)x3, (float)y3, (float)x3, (float)y3, x2+capCorrect, pY2);
      final float headDy = (float) (HEADDY);
      arrowPath.moveTo((float)(x2+headDx), pY2-headDy);
      arrowPath.lineTo(x2, pY2);
      arrowPath.lineTo((float)(x2+headDx), pY2+headDy);
      return arrowPath;
    }
  }

  public static <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> 
      void drawStraightArrow(Canvas<S, PEN_T, PATH_T> pCanvas, Theme<S, PEN_T, PATH_T> pTheme, double canvasX1, double canvasY1, double canvasX2, double canvasY2) {
    PEN_T paint = pTheme.getPen(ProcessThemeItems.LINE, 0);
    PATH_T arrowPath = getStraightArrow(pCanvas.getStrategy(), canvasX1, canvasY1, canvasX2, canvasY2, paint);
    pCanvas.drawPath(arrowPath, paint, null);
  }

  public static <PATH_T extends DiagramPath<PATH_T>, PEN_T extends Pen<PEN_T>, S extends DrawingStrategy<S, PEN_T, PATH_T>> 
     PATH_T getStraightArrow(S pStrategy, double pX1, double pY1, double pX2, double pY2, PEN_T pPen) {
    double dx = pX2-pX1;
    double dy = pY2-pY1;
    double angle = Math.atan2(dy,dx);

    final double miterExtend = (0.5*pPen.getStrokeWidth())/Math.sin(HEADANGLE);
    final float miterExtendX = (float) (Math.cos(angle)*miterExtend);
    final float miterExtendY = (float) (Math.sin(angle)*miterExtend);

    PATH_T arrowPath = pStrategy.newPath();

    double headLen = Math.sqrt((HEADDX*HEADDX)+(HEADDY*HEADDY));
    double x2 = pX2-miterExtendX;
    double y2 = pY2-miterExtendY;

    arrowPath.moveTo(pX1, pY1);
    arrowPath.lineTo(x2-miterExtendX, y2-miterExtendY);

    double headAngle = angle+Math.PI-HEADANGLE;
    final float headDX1 = (float) (Math.cos(headAngle)*headLen);
    final float headDY1 = (float) (Math.sin(headAngle)*headLen);
    headAngle = angle+Math.PI+HEADANGLE;
    final float headDX2 = (float) (Math.cos(headAngle)*headLen);
    final float headDY2 = (float) (Math.sin(headAngle)*headLen);

    arrowPath.moveTo(x2+headDX1, y2+headDY1);
    arrowPath.lineTo(x2, y2);
    arrowPath.lineTo(x2+headDX2, y2+headDY2);
    return arrowPath;
  }

}
