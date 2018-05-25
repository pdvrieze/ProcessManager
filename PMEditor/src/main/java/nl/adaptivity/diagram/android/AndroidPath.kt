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

package nl.adaptivity.diagram.android

import android.graphics.Path
import android.graphics.RectF
import nl.adaptivity.diagram.DiagramPath
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle


class AndroidPath : DiagramPath<AndroidPath> {

    val path = Path()

    override fun moveTo(x: Double, y: Double) = apply {
        path.moveTo(x.toFloat(), y.toFloat())
    }

    override fun lineTo(x: Double, y: Double) = apply {
        path.lineTo(x.toFloat(), y.toFloat())
    }

    override fun cubicTo(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) = apply {
        path.cubicTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), x3.toFloat(), y3.toFloat())
    }

    override fun close() = apply {
        path.close()
    }

    override fun getBounds(dest: Rectangle, stroke: Pen<*>?): Rectangle {
        val hsw = if (stroke == null) 0.0 else stroke.strokeWidth / 2
        val bounds = RectF()
        path.computeBounds(bounds, false)
        dest.set(bounds.left.toDouble() - hsw, bounds.top.toDouble() - hsw,
                 bounds.width().toDouble() + 2 * hsw, bounds.height().toDouble() + 2 * hsw)
        return dest
    }
}
