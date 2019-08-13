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

package nl.adaptivity.diagram.svg

import nl.adaptivity.diagram.DiagramPath
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.util.multiplatform.append
import kotlin.math.*


class SVGPath : DiagramPath<SVGPath> {

    private val path = mutableListOf<IPathElem>()

    private interface IPathElem {

        val x: Double
        val y: Double

        fun appendPathSpecTo(builder: Appendable)

        fun getBounds(storage: Rectangle, previous: IPathElem, stroke: Pen<*>?)
    }

    private abstract class OperTo(override val x: Double, override val y: Double) : IPathElem

    private class MoveTo(x: Double, y: Double) : OperTo(x, y) {

        override fun appendPathSpecTo(builder: Appendable) {
            builder.append("M").append(x).append(' ').append(y).append(' ')
        }

        override fun getBounds(storage: Rectangle, previous: IPathElem, stroke: Pen<*>?) {
            storage[x, y, 0.0] = 0.0
        }
    }

    private class LineTo(x: Double, y: Double) : OperTo(x, y) {

        override fun appendPathSpecTo(builder: Appendable) {
            builder.append("L").append(x).append(' ').append(y).append(' ')
        }

        override fun getBounds(storage: Rectangle, previous: IPathElem, stroke: Pen<*>?) {
            // TODO this is not valid as it does not consider miters, nor does it support link closing
            val sw = stroke?.strokeWidth ?: 0.0
            if (sw.isFinite() && sw > 0.0) {
                val hsw = sw / 2
                val height = y - previous.y
                val width = x - previous.x
                val angle = atan(height / width)
                val extendX = abs(sin(angle) * hsw)
                val extendY = abs(cos(angle) * hsw)
                storage[min(previous.x, x) - extendX, min(previous.y, y) - extendY,
                    abs(previous.x - x) + 2 * extendX] = abs(previous.y - y) + 2 * extendY
            } else {
                storage[min(previous.x, x), min(previous.y, y), abs(previous.x - x)] =
                    abs(previous.y - y)
            }
        }
    }

    private class CubicTo(
        private val mCX1: Double,
        private val mCY1: Double,
        private val mCX2: Double,
        private val mCY2: Double,
        x: Double,
        y: Double
                         ) : OperTo(x, y) {

        override fun appendPathSpecTo(builder: Appendable) {
            builder.append("C").append(mCX1).append(' ').append(mCY1).append(' ')
                .append(mCX2).append(' ').append(mCY2).append(' ')
                .append(x).append(' ').append(y).append(' ')
        }


        override fun getBounds(storage: Rectangle, previous: IPathElem, stroke: Pen<*>?) {
            val hsw = stroke?.run { strokeWidth / 2 } ?: 0.0
            val previousX = previous.x
            val previousY = previous.y

            val cx = 3 * mCX1 - 3 * previousX
            val bx = 3 * mCX2 - 6 * mCX1 + 3 * previousX
            val ax = x - 3 * mCX2 + 3 * mCX1 - previousX

            val cy = 3 * mCY1 - 3 * previousY
            val by = 3 * mCY2 - 6 * mCY1 + 3 * previousY
            val ay = y - 3 * mCY2 + 3 * mCY1 - previousY

            val dax = 3 * (x - 3 * mCX2 + 3 * mCX1 - previousX)
            val day = 3 * (y - 3 * mCY2 + 3 * mCY1 - previousY)
            val dbx = 2 * (3 * mCX2 - 6 * mCX1 + 3 * previousX)
            val dby = 2 * (3 * mCY2 - 6 * mCY1 + 3 * previousY)
            val dcx = 3 * mCX1 - 3 * previousX
            val dcy = 3 * mCY1 - 3 * previousY


            var left = min(previousX, x)
            var right = max(previousX, x)
            var top = min(previousY, y)
            var bottom = max(previousY, y)

            var t1 = 0.0
            var t2 = 0.0
            var t3 = 0.0
            var t4 = 0.0

            if (dax != 0.0) {
                t1 = (-dbx + sqrt(dbx * dbx - 4.0 * dax * dcx)) / (2 * dax)
                t2 = (-dbx - sqrt(dbx * dbx - 4.0 * dax * dcx)) / (2 * dax)
            }

            if (day != 0.0) {
                t3 = (-dby + sqrt(dby * dby - 4.0 * day * dcy)) / (2 * day)
                t4 = (-dby - sqrt(dby * dby - 4.0 * day * dcy)) / (2 * day)
            }

            for (t in doubleArrayOf(t1, t2, t3, t4)) {
                if (t > 0.0 && t < 1.0) {
                    val x = ((ax * t + bx) * t + cx) * t + previousX
                    left = min(x, left)
                    right = max(x, right)

                    val y = ((ay * t + by) * t + cy) * t + previousY
                    top = min(y, top)
                    bottom = max(y, bottom)
                }
            }

            storage[left - hsw, top - hsw, right - left + hsw * 2] = bottom - top + hsw * 2

        }


    }

    private class Close : IPathElem {

        override val x: Double
            get() = Double.NaN

        override val y: Double
            get() = Double.NaN

        override fun appendPathSpecTo(builder: Appendable) {
            builder.append("Z ")
        }

        override fun getBounds(storage: Rectangle, previous: IPathElem, stroke: Pen<*>?) {
            storage[previous.x, previous.y, 0.0] = 0.0
        }
    }

    override fun moveTo(x: Double, y: Double): SVGPath {
        path.add(MoveTo(x, y))
        return this
    }

    override fun lineTo(x: Double, y: Double): SVGPath {
        path.add(LineTo(x, y))
        return this
    }

    override fun cubicTo(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): SVGPath {
        path.add(CubicTo(x1, y1, x2, y2, x3, y3))
        return this
    }

    override fun close(): SVGPath {
        path.add(Close())
        return this
    }

    fun toPathData(): String {
        return buildString {
            path.forEach { it.appendPathSpecTo(this) }
        }
    }

    override fun getBounds(dest: Rectangle, stroke: Pen<*>?): Rectangle {
        if (path.size == 1) {
            val elem = path.get(0)
            val strokeWidth = stroke?.strokeWidth ?: 0.0
            dest[elem.x - strokeWidth / 2, elem.y - strokeWidth / 2, strokeWidth] = strokeWidth
        } else if (path.size > 1) {
            val tmpRect = Rectangle(0.0, 0.0, 0.0, 0.0)
            path.get(1).getBounds(dest, path.get(0), stroke)
            for (i in 2 until path.size) {
                path.get(i).getBounds(tmpRect, path.get(i - 1), stroke)
                dest.extendBounds(tmpRect)
            }
        }
        return dest
    }
}
