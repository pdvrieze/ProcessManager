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


interface Pen<out PEN_T : Pen<PEN_T>> {
    val strokeWidth: Double
    val fontSize: Double

    val textMaxAscent: Double

    val textAscent: Double

    val textMaxDescent: Double

    val textDescent: Double

    /**
     * The space recommended to separate two lines (beyond ascent and descent.
     * @return The leading
     */
    val textLeading: Double

    var isTextItalics: Boolean

    var isTextBold: Boolean
    fun setColor(red: Int, green: Int, blue: Int): PEN_T
    fun setColor(red: Int, green: Int, blue: Int, alpha: Int): PEN_T

    fun setStrokeWidth(strokeWidth: Double): PEN_T

    fun setFontSize(fontSize: Double): PEN_T

    /**
     * Measure the full bounding rectangle for the text.
     */
    fun measureTextSize(dest: Rectangle, x: Double, y: Double, text: String, foldWidth: Double): Rectangle

    /**
     * Measure the size of the given text. This is the width and height that will
     * be used when actually drawing the text.
     *
     * @param text The text to measure
     * @param foldWidth The width at which to fold the text
     * @return The width of the text with the given pen
     */
    fun measureTextWidth(text: String, foldWidth: Double): Double

}
