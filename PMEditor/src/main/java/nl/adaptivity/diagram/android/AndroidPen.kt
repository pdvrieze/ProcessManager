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

import android.graphics.Paint
import android.graphics.Paint.*
import android.graphics.Typeface
import android.support.annotation.ColorInt
import android.support.annotation.FloatRange
import android.support.annotation.IntRange
import android.support.annotation.NonNull
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle
import kotlin.math.abs


class AndroidPen(paint: Paint) : Pen<AndroidPen> {


    val paint: Paint = paint.apply { style = Style.STROKE }

    override var strokeWidth: Double = 0.0
        set(@FloatRange(from = 0.0, to = java.lang.Float.MAX_VALUE.toDouble(), fromInclusive = false) value) {
            field = value
            paint.strokeWidth = value.toFloat()

        }

    private var shadowRadius = -1f
    private var shadowColor = 0
    private var shadowDx = 0f
    private var shadowDy = 0f

    override var fontSize: Double = Double.NaN
        set(@FloatRange(from = 0.0, fromInclusive = false) fontSize) {
            paint.textAlign = Align.LEFT
            paint.textSize = fontSize.toFloat()
            field = fontSize
        }


    private var _fontMetrics: FontMetrics? = null

    private val fontMetrics: FontMetrics
        get() {
            return _fontMetrics ?: kotlin.run {
                val ts = paint.textSize
                paint.textSize = fontSize.toFloat()
                paint.fontMetrics.also {
                    _fontMetrics = it
                    paint.textSize = ts
                }
            }
        }

    override val textMaxAscent: Double
        get() = abs(fontMetrics.top).toDouble()

    override val textAscent: Double
        get() = abs(fontMetrics.ascent).toDouble()


    override val textMaxDescent: Double
        get() = abs(fontMetrics.bottom).toDouble()

    override val textDescent: Double
        get() {
            return abs(fontMetrics.descent).toDouble()
        }

    override val textLeading: Double
        get() = fontMetrics.run { abs(top).toDouble() + abs(bottom) - abs(ascent) - abs(descent) }

    override var isTextItalics: Boolean
        get() = paint.typeface.isItalic
        set(italics) = updateStyle(italics, Typeface.ITALIC)

    override var isTextBold: Boolean
        get() = paint.typeface.isBold
        set(bold) = updateStyle(bold, Typeface.BOLD)


    override fun setColor(@IntRange(from = 0, to = 255) red: Int,
                          @IntRange(from = 0, to = 255) green: Int,
                          @IntRange(from = 0, to = 255) blue: Int): AndroidPen = apply {
        paint.setARGB(255, red, green, blue)
    }

    override fun setColor(@IntRange(from = 0, to = 255) red: Int,
                          @IntRange(from = 0, to = 255) green: Int,
                          @IntRange(from = 0, to = 255) blue: Int,
                          @IntRange(from = 0, to = 255) alpha: Int): AndroidPen = apply {
        paint.setARGB(alpha, red, green, blue)
    }

    override fun setStrokeWidth(strokeWidth: Double): AndroidPen = apply {
        this.strokeWidth = strokeWidth
    }

    override fun setFontSize(fontSize: Double): AndroidPen = apply {
        this.fontSize = fontSize
    }

    fun setShadowLayer(@FloatRange(from = 0.0, fromInclusive = false) radius: Float, @ColorInt color: Int) {
        shadowRadius = radius
        shadowColor = color
        shadowDx = 0f
        shadowDy = 0f
        paint.setShadowLayer(radius, shadowDx, shadowDy, color)
    }

    @NonNull
    fun scale(@FloatRange(from = 0.0, fromInclusive = false) scale: Double) = apply {
        paint.strokeWidth = (strokeWidth * scale).toFloat()
        if (shadowRadius > 0f) {
            paint.setShadowLayer((shadowRadius * scale).toFloat(), (shadowDx * scale).toFloat(),
                                 (shadowDy * scale).toFloat(), shadowColor)
        }
        if (!fontSize.isNaN()) {
            paint.textSize = (fontSize * scale).toFloat()
        }
    }

    override fun measureTextWidth(text: String,
                                  @FloatRange(from = 0.0, fromInclusive = false) foldWidth: Double): Double {
        paint.withTextSize(fontSize * FONT_MEASURE_FACTOR) {
            return paint.measureText(text).toDouble() / FONT_MEASURE_FACTOR
        }
    }

    @NonNull
    @Override
    override fun measureTextSize(dest: Rectangle, x: Double, y: Double, text: String, foldWidth: Double) = dest.apply {
        paint.withTextSize(fontSize * FONT_MEASURE_FACTOR) {
            val left = x
            val width = paint.measureText(text).toDouble() / FONT_MEASURE_FACTOR
            val fm = fontMetrics
            val top = y + fm.top - fm.leading / 2
            val height = fm.leading.toDouble() + fm.top + fm.bottom
            set(left, top, width, height)
        }
    }

    private fun updateStyle(enabled: Boolean, styleBits: Int) {
        val oldTypeface = paint.typeface
        val style: Int = when (oldTypeface) {
            null -> if (enabled) styleBits else Typeface.NORMAL
            else -> (oldTypeface.style and styleBits.inv()) or (if (enabled) styleBits else Typeface.NORMAL)
        }
        paint.typeface = Typeface.create(oldTypeface, style)
    }

    companion object {
        private const val FONT_MEASURE_FACTOR = 3f

    }
}

private inline fun <R> Paint.withTextSize(size: Double, body: () -> R): R {
    val origSize = textSize
    textSize = size.toFloat()
    try {
        return body()
    } finally {
        textSize = origSize
    }
}