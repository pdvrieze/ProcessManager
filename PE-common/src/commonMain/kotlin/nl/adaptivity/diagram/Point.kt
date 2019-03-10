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

import kotlin.math.sqrt


class Point(val x: Double, val y: Double) {

    override fun toString(): String {
        return "($x, $y)"
    }

    fun distanceTo(other: Point): Double {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    companion object {
        @Deprecated("Use constructor", ReplaceWith("Point(x,y)"))
        fun of(x: Double, y: Double): Point {
            return Point(x, y)
        }
    }
}
