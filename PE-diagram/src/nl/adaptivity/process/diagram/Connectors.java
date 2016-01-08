package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;


public final class Connectors {

  private Connectors() { /* No functions */ }
  
  private static final double LEADERLEN = 17d;
  private static final double HEADLEN= 10d;
  private static final double HEADANGLE=(35*Math.PI)/180;
  private static final double HEADDX = Math.cos(HEADANGLE)*HEADLEN;
  private static final double HEADDY = Math.sin(HEADANGLE)*HEADLEN;
  private static final double MINANGLE = (1*Math.PI)/180;

  public static <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> 
      void drawArrow(Canvas<S, PEN_T, PATH_T> canvas, Theme<S, PEN_T, PATH_T> theme, double canvasX1, double canvasY1, double a1, double canvasX2, double canvasY2, double a2) {

    PEN_T paint = theme.getPen(ProcessThemeItems.LINE, 0);
    
    PATH_T arrowPath = getArrow(canvas.getStrategy(), canvasX1, canvasY1, canvasX2, canvasY2, paint);
    canvas.drawPath(arrowPath, paint, null);
  }

  public static <PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>, S extends DrawingStrategy<S, PEN_T, PATH_T>> 
      PATH_T getArrow(S strategy, double x1, double y1, double x2, double y2, PEN_T pen) {
    double dx = x2-x1;
    double dy = y2-y1;
    double angle = Math.atan2(dy,dx);
    if (Math.abs(angle)<MINANGLE || Math.abs(x2-x1)<LEADERLEN){
      return getStraightArrow(strategy, x1, y1, x2, y2, pen);
    }

    final double headDx;
    final double x3;
    final double x4;
    final double y3 = y2;
    final double angle2;
    final double capCorrect;

    // The distance that the miter extends from the focal point of the arrow.
    final double miterExtend = (0.5*pen.getStrokeWidth())/Math.sin(HEADANGLE);
    if (x1<x2) { // left to right
      headDx = -HEADDX;
      x3 = (float) (x2-miterExtend);
      /* Point 3 represents the focal point of a spline.
       * Point 4 is the point where the spline stops and a straight line starts.
       */
      x4 = x3-LEADERLEN;
      angle2 = Math.atan2((y1-y3),(x1-x4));
      capCorrect = pen.getStrokeWidth()/-2f;
    } else { //right to left
      headDx = HEADDX;
      x3 = (float) (x2+miterExtend);
      /* Point 3 represents the focal point of a spline.
       * Point 4 is the point where the spline stops and a straight line starts.
       */
      x4 = x3+LEADERLEN;
      angle2 = Math.atan2((y1-y3),(x1-x4));
      capCorrect = pen.getStrokeWidth()/2f;
    }
    final float x5 = (float) (x4+Math.cos(angle2)*LEADERLEN);
    final float y6 = (float) (y3+Math.sin(angle2)*LEADERLEN);

    boolean tooShort = false;
    if (dx>0) {
      if (x5<x1) { tooShort=true; }
    } else {
      if (x5>x1) { tooShort=true; }
    }
    if (dy>0) {
      if (y6<y1) { tooShort=true; }
    } else { // dy<0
      if (y6>y1) { tooShort=true; }
    }
    if (tooShort) {
      return getStraightArrow(strategy, x1, y1, x2, y2, pen);
    } else {
  
      PATH_T arrowPath = strategy.newPath();
      arrowPath.moveTo(x1, y1);
      arrowPath.lineTo(x5, y6);
      arrowPath.cubicTo((float)x4, (float)y3, (float)x4, (float)y3, x3+capCorrect, y2);
      final float headDy = (float) (HEADDY);
      arrowPath.moveTo((float)(x3+headDx), y2-headDy);
      arrowPath.lineTo(x3, y2);
      arrowPath.lineTo((float)(x3+headDx), y2+headDy);
      return arrowPath;
    }
  }

  public static <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> 
      void drawStraightArrow(Canvas<S, PEN_T, PATH_T> canvas, Theme<S, PEN_T, PATH_T> theme, double canvasX1, double canvasY1, double canvasX2, double canvasY2) {
    PEN_T paint = theme.getPen(ProcessThemeItems.LINE, 0);
    PATH_T arrowPath = getStraightArrow(canvas.getStrategy(), canvasX1, canvasY1, canvasX2, canvasY2, paint);
    canvas.drawPath(arrowPath, paint, null);
  }

  public static <PATH_T extends DiagramPath<PATH_T>, PEN_T extends Pen<PEN_T>, S extends DrawingStrategy<S, PEN_T, PATH_T>> 
     PATH_T getStraightArrow(S strategy, double x1, double y1, double x2, double y2, PEN_T pen) {
    double dx = x2-x1;
    double dy = y2-y1;
    double angle = Math.atan2(dy,dx);

    final double miterExtend = (0.5*pen.getStrokeWidth())/Math.sin(HEADANGLE);
    final float miterExtendX = (float) (Math.cos(angle)*miterExtend);
    final float miterExtendY = (float) (Math.sin(angle)*miterExtend);

    PATH_T arrowPath = strategy.newPath();

    double headLen = Math.sqrt((HEADDX*HEADDX)+(HEADDY*HEADDY));
    double newX2 = x2-miterExtendX;
    double newY2 = y2-miterExtendY;

    arrowPath.moveTo(x1, y1);
    arrowPath.lineTo(newX2-miterExtendX, newY2-miterExtendY);

    double headAngle = angle+Math.PI-HEADANGLE;
    final float headDX1 = (float) (Math.cos(headAngle)*headLen);
    final float headDY1 = (float) (Math.sin(headAngle)*headLen);
    headAngle = angle+Math.PI+HEADANGLE;
    final float headDX2 = (float) (Math.cos(headAngle)*headLen);
    final float headDY2 = (float) (Math.sin(headAngle)*headLen);

    arrowPath.moveTo(newX2+headDX1, newY2+headDY1);
    arrowPath.lineTo(newX2, newY2);
    arrowPath.lineTo(newX2+headDX2, newY2+headDY2);
    return arrowPath;
  }

}
