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

import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.svg.JVMTextMeasurer.JvmMeasureInfo

import java.awt.*
import java.awt.font.FontRenderContext


class JVMTextMeasurer : TextMeasurer<JvmMeasureInfo> {

    class JvmMeasureInfo(internal var font: Font) : TextMeasurer.MeasureInfo {
        internal val fontRenderContext: FontRenderContext
        private var leading = Double.NaN
        private var maxAscent = Double.NaN
        private var maxDescent = Double.NaN
        private var ascent = Double.NaN
        private var descent = Double.NaN

        val textLeading: Double
            get() {
                calcMetrics()
                return leading
            }

        val textDescent: Double
            get() {
                calcMetrics()
                return descent
            }

        val textMaxDescent: Double
            get() {
                calcMetrics()
                return maxDescent
            }

        val textAscent: Double
            get() {
                calcMetrics()
                return ascent
            }

        val textMaxAscent: Double
            get() {
                calcMetrics()
                return maxAscent
            }

        init {
            fontRenderContext = FontRenderContext(null, true, true)
        }

        private fun calcMetrics() {
            if (maxAscent.isNaN()) {
                val maxBounds = font.getMaxCharBounds(fontRenderContext)
                maxAscent = maxBounds.minY
                maxDescent = maxBounds.maxY
            }
            if (leading.isNaN()) {
                val linemetrics = font.getLineMetrics(
                    SAMPLE_LETTERS, fontRenderContext
                                                     )
                leading = linemetrics.leading.toDouble()
                ascent = linemetrics.ascent.toDouble()
                descent = linemetrics.descent.toDouble()
            }
        }

        @Override
        override fun setFontSize(fontSize: Double) {
            font = font.deriveFont(fontSize.toFloat())
            leading = Double.NaN
            maxAscent = Double.NaN
            maxDescent = Double.NaN
            ascent = Double.NaN
            descent = Double.NaN
        }

    }

    @Override
    override fun getTextMeasureInfo(svgPen: SVGPen<JvmMeasureInfo>): JvmMeasureInfo {
        val style = (if (svgPen.isTextItalics) Font.ITALIC else 0) or if (svgPen.isTextBold) Font.BOLD else 0

        val font = Font(FONTNAME, style, 10).deriveFont(Math.ceil(svgPen.fontSize * FONT_MEASURE_FACTOR).toFloat())

        return JvmMeasureInfo(font)
    }

    @Override
    override fun measureTextWidth(textMeasureInfo: JvmMeasureInfo, text: String, foldWidth: Double): Double {
        val bounds = textMeasureInfo.font.getStringBounds(text, textMeasureInfo.fontRenderContext)

        return bounds.width
    }

    @Override
    override fun measureTextSize(
        dest: Rectangle,
        textMeasureInfo: JvmMeasureInfo,
        text: String,
        foldWidth: Double
                                ): Rectangle {
        val bounds = textMeasureInfo.font.getStringBounds(text, textMeasureInfo.fontRenderContext)

        dest.left = 0.0
        dest.top = bounds.minY
        dest.width = bounds.width
        dest.height = bounds.height
        return dest
    }

    @Override
    override fun getTextMaxAscent(textMeasureInfo: JvmMeasureInfo): Double {
        return textMeasureInfo.textMaxAscent
    }

    @Override
    override fun getTextAscent(textMeasureInfo: JvmMeasureInfo): Double {
        return textMeasureInfo.textAscent
    }

    @Override
    override fun getTextMaxDescent(textMeasureInfo: JvmMeasureInfo): Double {
        return textMeasureInfo.textMaxDescent
    }

    @Override
    override fun getTextDescent(textMeasureInfo: JvmMeasureInfo): Double {
        return textMeasureInfo.textDescent
    }

    @Override
    override fun getTextLeading(textMeasureInfo: JvmMeasureInfo): Double {
        return textMeasureInfo.textLeading
    }

    companion object {

        private const val SAMPLE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        private const val FONT_MEASURE_FACTOR = 1f
        private const val FONTNAME = "SansSerif"
    }

}
