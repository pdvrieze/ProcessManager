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

package nl.adaptivity.process.diagram

import nl.adaptivity.diagram.*
import kotlin.math.*


object Connectors {

  /** The distance of the control point from the head of the arrow  */
  private const val HEAD_CONTROL_DIST = 17.0
  private const val HEADLEN = 10.0
  private const val HEADANGLE = 35 * PI / 180
  private val HEADDX = cos(HEADANGLE) * HEADLEN
  private val HEADDY = sin(HEADANGLE) * HEADLEN
  private const val MINANGLE = 1 * PI / 180
  private const val TAIL_JOIN_DIST = 6.0
  private const val TAIL_CONTROL_DIST = TAIL_JOIN_DIST * 0.6

  @kotlin.jvm.JvmStatic
  fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> drawArrow(canvas: Canvas<S, PEN_T, PATH_T>,
                                            tailX: Double, tailY: Double, tailAngle: Double, headTargetX: Double,
                                            headTargetY: Double, headAngle: Double) {

    val pen = canvas.theme.getPen(ProcessThemeItems.LINE, 0)

    val arrowPath = getArrow(canvas.strategy, tailX, tailY, tailAngle, headTargetX, headTargetY, headAngle, pen)
    canvas.drawPath(arrowPath, pen, null)
  }

  @kotlin.jvm.JvmStatic
  fun <PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>,
    S : DrawingStrategy<S, PEN_T, PATH_T>> getArrow(strategy: S,
                                                    tailX: Double,
                                                    tailY: Double,
                                                    tailAngle: Double,
                                                    headTargetX: Double,
                                                    headTargetY: Double,
                                                    headAngle: Double,
                                                    pen: PEN_T): PATH_T {

    val dx = headTargetX - tailX
    val dy = headTargetY - tailY
    val angle = atan2(dy, dx)
    if (abs(angle) < MINANGLE || abs(headTargetX - tailX) < HEAD_CONTROL_DIST) {
      return getStraightArrow(strategy, tailX, tailY, headTargetX, headTargetY, pen)
    }

    val headDx: Double
    val headOffsetX: Double
    val headControlX: Double
    val headOffsetY = headTargetY // fully horizontal so no change
    val headControlY = headOffsetY
    val lineAngle: Double
    val capCorrect: Double
    val tailJoinX: Double
    val tailJoinY = tailY
    val tailControlX1: Double
    val tailControlY1 = tailY

    // The distance that the miter extends from the focal point of the arrow.
    val miterExtend = 0.5 * pen.strokeWidth / sin(HEADANGLE)
    if (tailX < headTargetX) { // left to right

      headDx = -HEADDX
      headOffsetX = (headTargetX - miterExtend).toFloat().toDouble()
      /* Point 3 represents the focal point of a spline.
       * Point 4 is the point where the spline stops and a straight line starts.
       */
      headControlX = headOffsetX - HEAD_CONTROL_DIST
      tailJoinX = tailX + TAIL_JOIN_DIST
      tailControlX1 = tailX + TAIL_CONTROL_DIST
      lineAngle = atan2(tailJoinY - headOffsetY, tailJoinX - headControlX)
      capCorrect = pen.strokeWidth / -2f
    } else { //right to left
      headDx = HEADDX
      headOffsetX = (headTargetX + miterExtend).toFloat().toDouble()
      /* headOffsetX represents the focal point of a spline such that with the miter, the x will be at headTargetX.
       * headControlX is x coordinate of the control point for the head
       */
      headControlX = headOffsetX + HEAD_CONTROL_DIST
      tailJoinX = tailX - TAIL_JOIN_DIST
      tailControlX1 = tailX - TAIL_CONTROL_DIST
      lineAngle = atan2(tailJoinY - headOffsetY, tailJoinX - headControlX)
      capCorrect = pen.strokeWidth / 2f
    }
    val headStartX = (headControlX + cos(lineAngle) * HEAD_CONTROL_DIST).toFloat()
    val headStartY = (headOffsetY + sin(lineAngle) * HEAD_CONTROL_DIST).toFloat()
    val tailEndX = (tailJoinX - cos(lineAngle) * TAIL_JOIN_DIST).toFloat()
    val tailEndY = (tailJoinY - sin(lineAngle) * TAIL_JOIN_DIST).toFloat()
    val tailControlX2 = (tailEndX + cos(lineAngle) * TAIL_CONTROL_DIST).toFloat()
    val tailControlY2 = (tailEndY + sin(lineAngle) * TAIL_CONTROL_DIST).toFloat()

    var tooShort = false
    if (dx > 0) {
      if (headStartX < tailX) {
        tooShort = true
      }
    } else {
      if (headStartX > tailX) {
        tooShort = true
      }
    }

    if (dy > 0) {
      if (headStartY < tailY) {
        tooShort = true
      }
    } else { // dy<0
      if (headStartY > tailY) {
        tooShort = true
      }
    }

    if (tooShort) {
      return getStraightArrow(strategy, tailX, tailY, headTargetX, headTargetY, pen)
    } else {

      val arrowPath = strategy.newPath()
      arrowPath.moveTo(tailX, tailY)
      arrowPath.cubicTo(tailControlX1, tailControlY1, tailControlX2.toDouble(), tailControlY2.toDouble(),
                        tailEndX.toDouble(), tailEndY.toDouble())
      arrowPath.lineTo(headStartX.toDouble(), headStartY.toDouble())
      arrowPath.cubicTo(headControlX.toFloat().toDouble(), headOffsetY.toFloat().toDouble(),
                        headControlX.toFloat().toDouble(), headOffsetY.toFloat().toDouble(), headOffsetX + capCorrect,
                        headTargetY)
      val headDy = HEADDY.toFloat()
      arrowPath.moveTo((headOffsetX + headDx).toFloat().toDouble(), headTargetY - headDy)
      arrowPath.lineTo(headOffsetX, headTargetY)
      arrowPath.lineTo((headOffsetX + headDx).toFloat().toDouble(), headTargetY + headDy)
      return arrowPath
    }
  }

  @kotlin.jvm.JvmStatic
  fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> drawStraightArrow(canvas: Canvas<S, PEN_T, PATH_T>, theme: Theme<S, PEN_T, PATH_T>,
                                                    canvasX1: Double, canvasY1: Double, canvasX2: Double, canvasY2: Double) {

    val paint = theme.getPen(ProcessThemeItems.LINE, 0)
    val arrowPath = getStraightArrow(canvas.strategy, canvasX1, canvasY1, canvasX2, canvasY2, paint)
    canvas.drawPath(arrowPath, paint, null)
  }

  @kotlin.jvm.JvmStatic
  fun <PATH_T : DiagramPath<PATH_T>,
    PEN_T : Pen<PEN_T>,
    S : DrawingStrategy<S, PEN_T, PATH_T>> getStraightArrow(strategy: S, x1: Double, y1: Double,
                                                            x2: Double, y2: Double, pen: PEN_T): PATH_T {
    val dx = x2 - x1
    val dy = y2 - y1
    val angle = atan2(dy, dx)

    val miterExtend = 0.5 * pen.strokeWidth / sin(HEADANGLE)
    val miterExtendX = (cos(angle) * miterExtend).toFloat()
    val miterExtendY = (sin(angle) * miterExtend).toFloat()

    val arrowPath = strategy.newPath()

    val headLen = sqrt(HEADDX * HEADDX + HEADDY * HEADDY)
    val newX2 = x2 - miterExtendX
    val newY2 = y2 - miterExtendY

    arrowPath.moveTo(x1, y1)
    arrowPath.lineTo(newX2 - miterExtendX, newY2 - miterExtendY)

    var headAngle = angle + PI - HEADANGLE
    val headDX1 = (cos(headAngle) * headLen).toFloat()
    val headDY1 = (sin(headAngle) * headLen).toFloat()
    headAngle = angle + PI + HEADANGLE
    val headDX2 = (cos(headAngle) * headLen).toFloat()
    val headDY2 = (sin(headAngle) * headLen).toFloat()

    arrowPath.moveTo(newX2 + headDX1, newY2 + headDY1)
    arrowPath.lineTo(newX2, newY2)
    arrowPath.lineTo(newX2 + headDX2, newY2 + headDY2)
    return arrowPath
  }

}/* No functions */
