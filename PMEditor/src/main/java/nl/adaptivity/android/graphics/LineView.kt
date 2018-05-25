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

package nl.adaptivity.android.graphics

import android.graphics.Canvas
import android.graphics.RectF
import nl.adaptivity.diagram.Theme
import nl.adaptivity.diagram.android.*
import nl.adaptivity.process.diagram.Connectors


class LineView(private var x1: Float,
               private var y1: Float,
               private var x2: Float,
               private var y2: Float) : AbstractLightView(), LightView {

    override fun getBounds(target: RectF) {
        if (x1 <= x2) {
            target.left = x1
            target.right = x2
        } else {
            target.left = x2
            target.right = x1
        }
        if (y1 <= y2) {
            target.top = y1
            target.bottom = y2
        } else {
            target.top = y2
            target.bottom = y1
        }
    }

    override fun draw(canvas: Canvas, theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>, scale: Double) {
        val x1: Float
        val x2: Float
        val y1: Float
        val y2: Float
        if (this.x1 <= this.x2) {
            x1 = 0f
            x2 = ((this.x2 - this.x1) * scale).toFloat()
        } else {
            x2 = 0f
            x1 = ((this.x1 - this.x2) * scale).toFloat()
        }
        if (this.y1 <= this.y2) {
            y1 = 0f
            y2 = ((this.y2 - this.y1) * scale).toFloat()
        } else {
            y2 = 0f
            y1 = ((this.y1 - this.y2) * scale).toFloat()
        }
        drawArrow(canvas, theme, x1, y1, x2, y2, scale)
    }

    fun setPos(x1: Float, y1: Float, x2: Float, y2: Float) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
    }

    companion object {

        fun drawArrow(canvas: Canvas,
                      theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>,
                      canvasX1: Float,
                      canvasY1: Float,
                      canvasX2: Float,
                      canvasY2: Float,
                      scale: Double) {
            val androidCanvas = AndroidCanvas(canvas, theme).childCanvas(0.0, 0.0, scale)
            Connectors.drawArrow(androidCanvas, canvasX1 / scale, canvasY1 / scale, 0.0, canvasX2 / scale,
                                 canvasY2 / scale, Math.PI)
            return
        }

        fun drawStraightArrow(canvas: Canvas,
                              theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>,
                              canvasX1: Float,
                              canvasY1: Float,
                              canvasX2: Float,
                              canvasY2: Float,
                              scale: Double) {
            val androidCanvas = AndroidCanvas(canvas, theme).childCanvas(0.0, 0.0, scale)
            Connectors.drawStraightArrow(androidCanvas, theme, canvasX1 / scale, canvasY1 / scale, canvasX2 / scale,
                                         canvasY2 / scale)
            return
        }
    }

}
