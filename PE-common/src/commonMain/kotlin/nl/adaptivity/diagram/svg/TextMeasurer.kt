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
import nl.adaptivity.diagram.svg.TextMeasurer.MeasureInfo


interface TextMeasurer<M : MeasureInfo> {


    interface MeasureInfo {

        fun setFontSize(fontSize: Double)

    }

    fun getTextMeasureInfo(svgPen: SVGPen<M>): M

    fun measureTextWidth(textMeasureInfo: M, text: String, foldWidth: Double): Double

    fun getTextMaxAscent(textMeasureInfo: M): Double

    fun getTextAscent(textMeasureInfo: M): Double

    fun getTextMaxDescent(textMeasureInfo: M): Double

    fun getTextDescent(textMeasureInfo: M): Double

    fun getTextLeading(textMeasureInfo: M): Double

    fun measureTextSize(dest: Rectangle, textMeasureInfo: M, text: String, foldWidth: Double): Rectangle
}
