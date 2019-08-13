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

import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.svg.TextMeasurer.MeasureInfo


class SVGPen<M : MeasureInfo>(private val textMeasurer: TextMeasurer<M>) : Pen<SVGPen<M>> {

    var color = -0x1000000
        private set

    override var strokeWidth: Double = 0.toDouble()

    override var fontSize: Double = 0.toDouble()
        private set(value) {
            textMeasureInfo.setFontSize(value)
        }

    override var isTextItalics: Boolean = false

    override var isTextBold: Boolean = false

    private var _textMeasureInfo: M? = null
    private val textMeasureInfo: M
        get() = _textMeasureInfo ?: textMeasurer.getTextMeasureInfo(this).also { _textMeasureInfo = it }

    override fun setColor(red: Int, green: Int, blue: Int): SVGPen<M> {
        return setColor(red, green, blue, 0xff)
    }

    override fun setColor(red: Int, green: Int, blue: Int, alpha: Int): SVGPen<M> {
        color = alpha and 0xff shl 24 or (red and 0xff shl 16) or (green and 0xff shl 8) or (blue and 0xff)
        return this
    }

    @Deprecated("Use property access without return value")
    override fun setStrokeWidth(strokeWidth: Double): SVGPen<M> {
        this.strokeWidth = strokeWidth
        return this
    }

    @Deprecated("Use property access without return value")
    override fun setFontSize(fontSize: Double): SVGPen<M> {
        this.fontSize = fontSize
        return this
    }

    override fun measureTextWidth(text: String, foldWidth: Double): Double =
        textMeasurer.measureTextWidth(textMeasureInfo, text, foldWidth)

    override fun measureTextSize(dest: Rectangle, x: Double, y: Double, text: String, foldWidth: Double): Rectangle {
        textMeasurer.measureTextSize(dest, textMeasureInfo, text, foldWidth)
        dest.top += y
        dest.left += x
        return dest
    }

    override val textMaxAscent: Double
        get() = textMeasurer.getTextMaxAscent(textMeasureInfo)

    override val textAscent: Double
        get() = textMeasurer.getTextAscent(textMeasureInfo)

    override val textDescent: Double
        get() = textMeasurer.getTextDescent(textMeasureInfo)

    override val textMaxDescent: Double
        get() = textMeasurer.getTextMaxDescent(textMeasureInfo)

    override val textLeading: Double
        get() = textMeasurer.getTextLeading(textMeasureInfo)

    fun copy(): SVGPen<M> {
        val o = this
        return SVGPen(textMeasurer).apply {
            color = o.color
            strokeWidth = o.strokeWidth
            fontSize = o.fontSize
            isTextItalics = o.isTextItalics
            isTextBold = o.isTextBold
        }
    }

}
