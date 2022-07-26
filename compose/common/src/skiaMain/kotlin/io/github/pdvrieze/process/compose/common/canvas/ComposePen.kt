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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle
import org.jetbrains.skia.FontMetrics
import kotlin.math.abs


class ComposePen(brush: Brush) : Pen<ComposePen> {


    var brush: Brush = brush
        private set

    override var strokeWidth: Double = 5.0
    var strokeCap: StrokeCap = StrokeCap.Square

    private var shadowRadius = -1f
    private var shadowColor = 0
    private var shadowDx = 0f
    private var shadowDy = 0f

    override var fontSize: Double = Double.NaN

//    private var fontMetrics: FontMetrics? = Fo

    override val textMaxAscent: Double
        get() = 4.0//abs(fontMetrics!!.top).toDouble()

    override val textAscent: Double
        get() = 4.0//abs(fontMetrics!!.ascent).toDouble()


    override val textMaxDescent: Double
        get() = -3.0//abs(fontMetrics!!.bottom).toDouble()

    override val textDescent: Double
        get() {
            return -3.0//abs(fontMetrics!!.descent).toDouble()
        }

    override val textLeading: Double
        get() = 5.0//fontMetrics!!.run { abs(top).toDouble() + abs(bottom) - abs(ascent) - abs(descent) }

    override var isTextItalics: Boolean
        get() = false//brush.typeface.isItalic
        set(italics) {}//updateStyle(italics, Typeface.ITALIC)

    override var isTextBold: Boolean
        get() = false// brush.typeface.isBold
        set(bold) {}//updateStyle(bold, Typeface.BOLD)


    fun getStroke(): Stroke {
        return Stroke(strokeWidth.toFloat(), cap = strokeCap)
    }



    override fun setColor(red: Int,
                           green: Int,
                           blue: Int): ComposePen = apply {
        brush = SolidColor(Color(red, green, blue, 255))
    }

    override fun setColor(red: Int,
                          green: Int,
                          blue: Int,
                          alpha: Int): ComposePen = apply {
        brush = SolidColor(Color(red, green, blue, alpha))
    }

    override fun setStrokeWidth(strokeWidth: Double): ComposePen = apply {
        this.strokeWidth = strokeWidth
    }

    override fun setFontSize(fontSize: Double): ComposePen = apply {
        this.fontSize = fontSize
    }

    fun setShadowLayer(radius: Float, color: Color) {
        // TODO()
//        shadowRadius = radius
//        shadowColor = color
//        shadowDx = 0f
//        shadowDy = 0f
//        brush.setShadowLayer(radius, shadowDx, shadowDy, color)
    }

    fun scale(scale: Double) = apply {
        this.strokeWidth *= scale
        if (shadowRadius > 0f) {
            TODO("Shadow not implemented yet")
//            brush.setShadowLayer((shadowRadius * scale).toFloat(), (shadowDx * scale).toFloat(),
//                                 (shadowDy * scale).toFloat(), shadowColor)
        }
        fontSize*=scale
/*
        if (!fontSize.isNaN()) {
            brush.textSize = (fontSize * scale).toFloat()
        }
*/
    }

    override fun measureTextWidth(text: String,
                                  foldWidth: Double): Double {
        return TODO()
/*
        brush.withTextSize(fontSize * FONT_MEASURE_FACTOR) {
            return brush.measureText(text).toDouble() / FONT_MEASURE_FACTOR
        }
*/
    }

    override fun measureTextSize(dest: Rectangle, x: Double, y: Double, text: String, foldWidth: Double) = dest.apply {
        TODO()
/*
        brush.withTextSize(fontSize * FONT_MEASURE_FACTOR) {
            val left = x
            val width = brush.measureText(text).toDouble() / FONT_MEASURE_FACTOR
            val fm = fontMetrics
            val top = y + fm.top - fm.leading / 2
            val height = fm.leading.toDouble() + fm.top + fm.bottom
            set(left, top, width, height)
        }
*/
    }

    private fun updateStyle(enabled: Boolean, styleBits: Int) {
/*
        val oldTypeface = brush.typeface
        val style: Int = when (oldTypeface) {
            null -> if (enabled) styleBits else Typeface.NORMAL
            else -> (oldTypeface.style and styleBits.inv()) or (if (enabled) styleBits else Typeface.NORMAL)
        }
        brush.typeface = Typeface.create(oldTypeface, style)
*/
    }

    companion object {
        private const val FONT_MEASURE_FACTOR = 3f

    }
}

/*
private inline fun <R> Paint.withTextSize(size: Double, body: () -> R): R {
    val origSize = textSize
    textSize = size.toFloat()
    try {
        return body()
    } finally {
        textSize = origSize
    }
}
*/
