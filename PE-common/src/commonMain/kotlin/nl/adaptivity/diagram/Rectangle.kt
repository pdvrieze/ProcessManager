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

import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

data class Rectangle(
    @JvmField var left: Double = Double.NaN,
    @JvmField var top: Double = Double.NaN,
    @JvmField var width: Double = Double.NaN,
    @JvmField var height: Double = Double.NaN
                    ) {

    @Deprecated("Use copy instead", ReplaceWith("copy()"))
    fun clone(): Rectangle = copy()

    val leftf: Float
        @JvmName("leftf") get() = left.toFloat()

    val topf: Float
        @JvmName("topf") get() = top.toFloat()

    val right: Double
        @JvmName("right") get() = left + width

    val rightf: Float
        @JvmName("rightf") get() = right.toFloat()

    val bottom: Double
        @JvmName("bottom") get() = top + height

    val bottomf: Float
        @JvmName("bottomf") get() = bottom.toFloat()

    val widthf: Float
        @JvmName("widthf") get() = width.toFloat()

    val heightf: Float
        @JvmName("heightf") get() = height.toFloat()

    val hasUndefined: Boolean get() = !(left.isFinite() && top.isFinite() && width.isFinite() && height.isFinite())

    /**
     * Set the rectangle coordinates to be undefined.
     */
    fun clear() {
        left = Double.NaN
        width = Double.NaN
        top = Double.NaN
        height = Double.NaN

    }

    /**
     * Create an offsetted rectangle. The offsets should not be prescaled. They will be scaled in the method.
     * The scaling is from the top left of the rectangle.
     * @param xOffset The x offset.
     *
     * @param yOffset The y offset.
     *
     * @param scale The scaling needed.
     *
     * @return A new rectangle that is moved from the original one.
     */
    fun offsetScaled(xOffset: Double, yOffset: Double, scale: Double): Rectangle {
        return Rectangle((left + xOffset) * scale, (top + yOffset) * scale, width * scale, height * scale)
    }

    override fun toString(): String {
        return "Rectangle [l=$left, t=$top, w=$width, h=$height]"
    }

    operator fun set(left: Double, top: Double, width: Double, height: Double) {
        this.left = left
        this.top = top
        this.width = width
        this.height = height
    }

    fun set(bounds: Rectangle) {
        left = bounds.left
        top = bounds.top
        width = bounds.width
        height = bounds.height
    }

    /**
     * Adjust the bounds such that the corners are moved by amount in the outward direction
     */
    fun outset(adjust: Double) {
        val dadjust = adjust * 2
        set(left - adjust, top - adjust, width + dadjust, height + dadjust)
    }

    fun inset(adjust: Double) = outset(-adjust)

    fun extendBounds(bounds: Rectangle) {
        val newleft = minOf(left, bounds.left)
        val newtop = minOf(top, bounds.top)
        width = maxOf(right, bounds.right) - newleft
        height = maxOf(bottom, bounds.bottom) - newtop
        left = newleft
        top = newtop
    }

    fun contains(x: Double, y: Double): Boolean {
        return x >= left &&
            y >= top &&
            x <= left + width &&
            y <= top + height
    }

    fun contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())

}

@Deprecated("Use property", ReplaceWith("right"))
fun Rectangle.right() = right

@Deprecated("Use property", ReplaceWith("bottom"))
fun Rectangle.bottom() = bottom
