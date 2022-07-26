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

package io.github.pdvrieze.process.compose.common.canvas

import nl.adaptivity.diagram.DiagramPath
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle
import org.w3c.dom.Path2D


class JsCanvasPath(val path: Path2D = Path2D()) : DiagramPath<JsCanvasPath> {
    private var minX: Double = Double.POSITIVE_INFINITY
    private var minY: Double = Double.POSITIVE_INFINITY
    private var maxX: Double = Double.NEGATIVE_INFINITY
    private var maxY: Double = Double.NEGATIVE_INFINITY

    private fun updateBounds(x: Double, y: Double) {
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
    }

    override fun moveTo(x: Double, y: Double) = apply {
        updateBounds(x, y)
        path.moveTo(x, y)
    }

    override fun lineTo(x: Double, y: Double) = apply {
        updateBounds(x, y)
        path.lineTo(x, y)
    }

    override fun cubicTo(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) = apply {
        updateBounds(x1, y1)
        updateBounds(x2, y2)
        updateBounds(x3, y3)
        path.bezierCurveTo(x1, y1, x2, y2, x3, y3)
    }

    override fun close() = apply {
        path.closePath()
    }

    override fun getBounds(dest: Rectangle, stroke: Pen<*>?): Rectangle {
        val hsw = stroke?.let { it.strokeWidth/2 } ?: 0.0

        dest.set(minX- hsw,  minY - hsw,
                 maxX-minX+2*hsw, maxY-minY+2*hsw)
        return dest
    }
}
