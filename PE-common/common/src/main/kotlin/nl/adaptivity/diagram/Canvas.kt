/*
 * Copyright (c) 2018.
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

package nl.adaptivity.diagram

import nl.adaptivity.util.multiplatform.JvmDefault


interface Canvas<S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> {

    enum class TextPos {
        MAXTOPLEFT, MAXTOP, MAXTOPRIGHT,
        ASCENTLEFT, ASCENT, ASCENTRIGHT,
        LEFT, MIDDLE, RIGHT,
        BASELINELEFT, BASELINEMIDDLE, BASELINERIGHT,
        DESCENTLEFT, DESCENT, DESCENTRIGHT,
        BOTTOMLEFT, BOTTOM, BOTTOMRIGHT;

        fun offset(rect: Rectangle, pen: Pen<*>) {
            when (this) {
                MAXTOPLEFT, ASCENTLEFT, LEFT, BASELINELEFT, DESCENTLEFT, BOTTOMLEFT       -> {
                }
                MAXTOP, ASCENT, MIDDLE, BASELINEMIDDLE, DESCENT, BOTTOM                   -> rect.left -= rect.width / 2
                MAXTOPRIGHT, ASCENTRIGHT, RIGHT, BASELINERIGHT, DESCENTRIGHT, BOTTOMRIGHT -> rect.left -= rect.width
            }// Keep the left where it is
            when (this) {
                MAXTOPLEFT, MAXTOP, MAXTOPRIGHT             -> {
                }
                ASCENTLEFT, ASCENT, ASCENTRIGHT             -> rect.top += pen.textAscent - pen.textMaxDescent
                LEFT, MIDDLE, RIGHT                         -> rect.top += (pen.textMaxAscent + pen.textMaxDescent) / 2 - pen.textMaxAscent
                BASELINELEFT, BASELINEMIDDLE, BASELINERIGHT -> rect.top -= pen.textMaxAscent
                DESCENTLEFT, DESCENT, DESCENTRIGHT          -> rect.top += pen.textDescent - pen.textMaxAscent
                BOTTOMLEFT, BOTTOM, BOTTOMRIGHT             -> rect.top += pen.textMaxDescent - pen.textMaxAscent
            }//

        }
    }

    val strategy: S

    /**
     * Create a new canvas that offsets the current one. Offset is applied before scaling (so it's effect will be modified by the scale.

     * @param offsetX The x offset to apply
     *
     * @param offsetY The y offset to apply
     *
     * @param scale The new scale.
     *
     * @return The new canvas representing the change
     */
    fun childCanvas(offsetX: Double, offsetY: Double, scale: Double): Canvas<S, PEN_T, PATH_T>

    /**
     * Draw a circle filled with the given color.

     * @param x Center point x
     *
     * @param y Center point y
     *
     * @param radius Circle radius
     *
     * @param fill fill color
     */
    fun drawFilledCircle(x: Double, y: Double, radius: Double, fill: PEN_T)

    fun drawCircle(x: Double, y: Double, radius: Double, stroke: PEN_T)

    /**
     * Draw a circle filled with the given color.

     * @param x      Center point x
     *
     * @param y      Center point y
     *
     * @param radius Circle radius
     *
     * @param stroke stroke color
     *
     * @param fill   Fill color
     */
    fun drawCircle(x: Double, y: Double, radius: Double, stroke: PEN_T?, fill: PEN_T?) {
        if (fill != null) drawFilledCircle(x, y, radius, fill)
        if (stroke != null) drawCircle(x, y, radius, stroke)
    }

    fun drawRect(rect: Rectangle, stroke: PEN_T)

    fun drawFilledRect(rect: Rectangle, fill: PEN_T)

    fun drawRect(rect: Rectangle, stroke: PEN_T?, fill: PEN_T?) {
        if (fill != null) drawFilledRect(rect, fill)
        if (stroke != null) drawRect(rect, stroke)
    }

    fun drawRoundRect(rect: Rectangle, rx: Double, ry: Double, stroke: PEN_T)

    fun drawFilledRoundRect(rect: Rectangle, rx: Double, ry: Double, fill: PEN_T)

    fun drawRoundRect(rect: Rectangle, rx: Double, ry: Double, stroke: PEN_T?, fill: PEN_T?) {
        if (fill != null) drawFilledRoundRect(rect, rx, ry, fill)
        if (stroke != null) drawRoundRect(rect, rx, ry, stroke)
    }


    fun drawPoly(points: DoubleArray, stroke: PEN_T?, fill: PEN_T?)

    /**
     * These are implemented in terms of drawPath, but don't allow for path caching.
     * @param points The points of the poly
     *
     * @param stroke The color
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use fillable version")
    @JvmDefault
    fun drawPoly(points: DoubleArray, stroke: PEN_T) = drawPoly(points, stroke, null)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use strokable version")
    @JvmDefault
    fun drawFilledPoly(points: DoubleArray, fill: PEN_T) = drawPoly(points, null, fill)

    fun drawPath(path: PATH_T, stroke: PEN_T, fill: PEN_T? = null)

    val theme: Theme<S, PEN_T, PATH_T>

    /**
     * Draw the given text onto the canvas.
     * @param textPos The position of the text anchor.
     *
     * @param left The left point for drawing the text.
     *
     * @param baselineY The coordinate of the text baseline
     *
     * @param text The text to draw.
     *
     * @param foldWidth The width at which to fold the text.
     *
     * @param pen The pen to use for it all.
     */
    fun drawText(textPos: TextPos, left: Double, baselineY: Double, text: String, foldWidth: Double, pen: PEN_T)

}
