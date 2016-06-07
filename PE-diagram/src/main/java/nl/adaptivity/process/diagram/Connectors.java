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

package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;


public final class Connectors {

  private Connectors() { /* No functions */ }

  /** The distance of the control point from the head of the arrow */
  private static final double HEAD_CONTROL_DIST = 17d;
  private static final double HEADLEN= 10d;
  private static final double HEADANGLE=(35*Math.PI)/180;
  private static final double HEADDX = Math.cos(HEADANGLE)*HEADLEN;
  private static final double HEADDY = Math.sin(HEADANGLE)*HEADLEN;
  private static final double MINANGLE = (1*Math.PI)/180;
  private static final double TAIL_JOIN_DIST = 6d;
  private static final double TAIL_CONTROL_DIST=TAIL_JOIN_DIST*0.6d;

  public static <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> 
      void drawArrow(Canvas<S, PEN_T, PATH_T> canvas, Theme<S, PEN_T, PATH_T> theme, double canvasX1, double canvasY1, double a1, double canvasX2, double canvasY2, double a2) {

    PEN_T paint = theme.getPen(ProcessThemeItems.LINE, 0);
    
    PATH_T arrowPath = getArrow(canvas.getStrategy(), canvasX1, canvasY1, canvasX2, canvasY2, paint);
    canvas.drawPath(arrowPath, paint, null);
  }

  public static <PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>, S extends DrawingStrategy<S, PEN_T, PATH_T>> 
      PATH_T getArrow(S strategy, double tailX, double tailY, double headTargetX, double headTargetY, PEN_T pen) {
    double dx = headTargetX-tailX;
    double dy = headTargetY-tailY;
    double angle = Math.atan2(dy,dx);
    if (Math.abs(angle)<MINANGLE || Math.abs(headTargetX-tailX) < HEAD_CONTROL_DIST){
      return getStraightArrow(strategy, tailX, tailY, headTargetX, headTargetY, pen);
    }

    final double headDx;
    final double headOffsetX;
    final double headControlX;
    final double headOffsetY = headTargetY; // fully horizontal so no change
    final double headControlY = headOffsetY;
    final double lineAngle;
    final double capCorrect;
    final double tailJoinX;
    final double tailJoinY = tailY;
    final double tailControlX1;
    final double tailControlY1 = tailY;

    // The distance that the miter extends from the focal point of the arrow.
    final double miterExtend = (0.5*pen.getStrokeWidth())/Math.sin(HEADANGLE);
    if (tailX<headTargetX) { // left to right

      headDx = -HEADDX;
      headOffsetX = (float) (headTargetX-miterExtend);
      /* Point 3 represents the focal point of a spline.
       * Point 4 is the point where the spline stops and a straight line starts.
       */
      headControlX = headOffsetX - HEAD_CONTROL_DIST;
      tailJoinX = tailX + TAIL_JOIN_DIST;
      tailControlX1 = tailX + TAIL_CONTROL_DIST;
      lineAngle = Math.atan2((tailJoinY-headOffsetY),(tailJoinX-headControlX));
      capCorrect = pen.getStrokeWidth()/-2f;
    } else { //right to left
      headDx = HEADDX;
      headOffsetX = (float) (headTargetX+miterExtend);
      /* headOffsetX represents the focal point of a spline such that with the miter, the x will be at headTargetX.
       * headControlX is x coordinate of the control point for the head
       */
      headControlX = headOffsetX + HEAD_CONTROL_DIST;
      tailJoinX = tailX - TAIL_JOIN_DIST;
      tailControlX1 = tailX - TAIL_CONTROL_DIST;
      lineAngle = Math.atan2((tailJoinY-headOffsetY),(tailJoinX-headControlX));
      capCorrect = pen.getStrokeWidth()/2f;
    }
    final float headStartX = (float) (headControlX+ Math.cos(lineAngle) * HEAD_CONTROL_DIST);
    final float headStartY = (float) (headOffsetY+ Math.sin(lineAngle) * HEAD_CONTROL_DIST);
    final float tailEndX = (float) (tailJoinX- Math.cos(lineAngle) * TAIL_JOIN_DIST);
    final float tailEndY = (float) (tailJoinY- Math.sin(lineAngle) * TAIL_JOIN_DIST);
    final float tailControlX2 = (float) (tailEndX + Math.cos(lineAngle) * TAIL_CONTROL_DIST);
    final float tailControlY2 = (float) (tailEndY + Math.sin(lineAngle) * TAIL_CONTROL_DIST);

    boolean tooShort = false;
    if (dx>0) {
      if (headStartX<tailX) { tooShort=true; }
    } else {
      if (headStartX>tailX) { tooShort=true; }
    }
    if (dy>0) {
      if (headStartY<tailY) { tooShort=true; }
    } else { // dy<0
      if (headStartY>tailY) { tooShort=true; }
    }
    if (tooShort) {
      return getStraightArrow(strategy, tailX, tailY, headTargetX, headTargetY, pen);
    } else {
  
      PATH_T arrowPath = strategy.newPath();
      arrowPath.moveTo(tailX, tailY);
      arrowPath.cubicTo(tailControlX1, tailControlY1, tailControlX2, tailControlY2, tailEndX, tailEndY);
      arrowPath.lineTo(headStartX, headStartY);
      arrowPath.cubicTo((float)headControlX, (float)headOffsetY, (float)headControlX, (float)headOffsetY, headOffsetX+capCorrect, headTargetY);
      final float headDy = (float) (HEADDY);
      arrowPath.moveTo((float)(headOffsetX+headDx), headTargetY-headDy);
      arrowPath.lineTo(headOffsetX, headTargetY);
      arrowPath.lineTo((float)(headOffsetX+headDx), headTargetY+headDy);
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
