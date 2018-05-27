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

import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.Typeface
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.svg.SVGPen
import nl.adaptivity.diagram.svg.TextMeasurer


class AndroidTextMeasurer : TextMeasurer<AndroidTextMeasurer.AndroidMeasureInfo> {


    class AndroidMeasureInfo(internal val paint: Paint) : TextMeasurer.MeasureInfo {
        internal val fontMetrics = FontMetrics()

        init {
            paint.getFontMetrics(fontMetrics)
        }

        override fun setFontSize(fontSize: Double) {
            paint.textSize = fontSize.toFloat() * FONT_MEASURE_FACTOR
            paint.getFontMetrics(fontMetrics)
        }

    }

    override fun getTextMeasureInfo(svgPen: SVGPen<AndroidMeasureInfo>): AndroidMeasureInfo {
        val paint = Paint()
        paint.textSize = svgPen.fontSize.toFloat() * FONT_MEASURE_FACTOR
        paint.typeface = when {
            svgPen.isTextItalics -> Typeface.create(paint.typeface, Typeface.ITALIC)
            else                 -> Typeface.create(paint.typeface, Typeface.NORMAL)
        }
        return AndroidMeasureInfo(paint)
    }

    override fun measureTextWidth(textMeasureInfo: AndroidMeasureInfo, text: String, foldWidth: Double): Double {
        return (textMeasureInfo.paint.measureText(text) / FONT_MEASURE_FACTOR).toDouble()
    }

    override fun measureTextSize(dest: Rectangle,
                                 textMeasureInfo: AndroidMeasureInfo,
                                 text: String,
                                 foldWidth: Double): Rectangle {
        dest.left = 0.0
        dest.top = textMeasureInfo.fontMetrics.top.toDouble()
        dest.width = (textMeasureInfo.paint.measureText(text) / FONT_MEASURE_FACTOR).toDouble()
        dest.height = (textMeasureInfo.fontMetrics.bottom - textMeasureInfo.fontMetrics.top).toDouble()
        return dest
    }

    override fun getTextMaxAscent(textMeasureInfo: AndroidMeasureInfo): Double {
        return (Math.abs(textMeasureInfo.fontMetrics.top) / FONT_MEASURE_FACTOR).toDouble()
    }

    override fun getTextAscent(textMeasureInfo: AndroidMeasureInfo): Double {
        return (Math.abs(textMeasureInfo.fontMetrics.ascent) / FONT_MEASURE_FACTOR).toDouble()
    }

    override fun getTextMaxDescent(textMeasureInfo: AndroidMeasureInfo): Double {
        return (Math.abs(textMeasureInfo.fontMetrics.bottom) / FONT_MEASURE_FACTOR).toDouble()
    }

    override fun getTextDescent(textMeasureInfo: AndroidMeasureInfo): Double {
        return (Math.abs(textMeasureInfo.fontMetrics.descent) / FONT_MEASURE_FACTOR).toDouble()
    }

    override fun getTextLeading(textMeasureInfo: AndroidMeasureInfo): Double {
        return ((Math.abs(textMeasureInfo.fontMetrics.top) + Math.abs(textMeasureInfo.fontMetrics.bottom) - Math.abs(
            textMeasureInfo.fontMetrics.ascent) - Math.abs(
            textMeasureInfo.fontMetrics.descent)) / FONT_MEASURE_FACTOR).toDouble()
    }

    companion object {

        private val FONT_MEASURE_FACTOR = 1f
    }

}
