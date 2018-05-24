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

import nl.adaptivity.diagram.Canvas
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.Theme
import nl.adaptivity.diagram.svg.TextMeasurer.MeasureInfo
import nl.adaptivity.util.multiplatform.toHex
import nl.adaptivity.xml.XMLConstants.DEFAULT_NS_PREFIX
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.smartStartTag
import nl.adaptivity.xml.writeAttribute
import kotlin.math.abs

open class SVGCanvas<M : MeasureInfo>(override val strategy: SVGStrategy<M>) : Canvas<SVGStrategy<M>, SVGPen<M>, SVGPath> {

    internal var path: MutableList<IPaintedElem> = ArrayList()

    private val _bounds: Rectangle = Rectangle(Double.NaN, Double.NaN, 0.0, 0.0)

    var bounds: Rectangle
        get() = getBounds(Rectangle(0.0, 0.0, 0.0, 0.0))
        set(bounds) = _bounds.set(bounds)

    override val theme: Theme<SVGStrategy<M>, SVGPen<M>, SVGPath>
        get() = SVGTheme(strategy)

    internal interface IPaintedElem {

        fun serialize(out: XmlWriter)

        fun getBounds(dest: Rectangle): Rectangle
    }

    private abstract class PaintedElem<M : MeasureInfo> internal constructor(internal val stroke: SVGPen<M>?,
                                                                             internal val fill: SVGPen<M>?) : IPaintedElem {

        internal fun serializeFill(out: XmlWriter) {
            serializeStyle(out, null, fill, null)
        }

        internal fun serializeStroke(out: XmlWriter) {
            serializeStyle(out, stroke, null, null)
        }

        internal fun serializeStrokeFill(out: XmlWriter) {
            serializeStyle(out, stroke, fill, null)
        }
    }

    private abstract class BaseRect<M : MeasureInfo> internal constructor(bounds: Rectangle,
                                                                          stroke: SVGPen<M>?,
                                                                          fill: SVGPen<M>?) : PaintedElem<M>(stroke,
                                                                                                             fill) {
        internal val bounds: Rectangle = bounds.copy()

        internal inline fun serializeRect(out: XmlWriter, body: XmlWriter.() -> Unit) {
            out.smartStartTag(SVG_NAMESPACE, "rect", null) {
                writeAttribute("x", bounds.left)
                writeAttribute("y", bounds.top)
                writeAttribute("width", bounds.width)
                writeAttribute("height", bounds.height)
                body()
            }
        }

        override fun getBounds(dest: Rectangle): Rectangle {
            val strokeWidth = stroke?.strokeWidth ?: 0.0
            val delta = strokeWidth / 2
            dest[bounds.left - delta, bounds.top - delta, bounds.width + strokeWidth] = bounds.height + strokeWidth
            return dest
        }
    }

    private class Rect<M : MeasureInfo>(bounds: Rectangle, stroke: SVGPen<M>) : BaseRect<M>(bounds, stroke, null) {

        override fun serialize(out: XmlWriter) {
            serializeRect(out) {

                serializeStroke(this)
            }
        }

    }

    private class FilledRect<M : MeasureInfo> : BaseRect<M> {

        internal constructor(bounds: Rectangle, fill: SVGPen<M>) : super(bounds, null, fill)

        internal constructor(bounds: Rectangle, stroke: SVGPen<M>, fill: SVGPen<M>) : super(bounds, stroke, fill)

        override fun serialize(out: XmlWriter) {
            serializeRect(out) {
                serializeStrokeFill(out)
            }
        }

    }

    private abstract class BaseRoundRect<M : MeasureInfo> internal constructor(bounds: Rectangle,
                                                                               internal val rx: Double,
                                                                               internal val ry: Double,
                                                                               stroke: SVGPen<M>?,
                                                                               fill: SVGPen<M>?) : BaseRect<M>(bounds,
                                                                                                               stroke,
                                                                                                               fill) {

        internal inline fun serializeRoundRect(out: XmlWriter, body: XmlWriter.() -> Unit) {
            serializeRect(out) {
                writeAttribute("rx", rx)
                writeAttribute("ry", ry)
                body()
            }
        }
    }

    private class RoundRect<M : MeasureInfo> internal constructor(bounds: Rectangle,
                                                                  rx: Double,
                                                                  ry: Double,
                                                                  stroke: SVGPen<M>) : BaseRoundRect<M>(bounds, rx, ry,
                                                                                                        stroke, null) {

        override fun serialize(out: XmlWriter) {
            serializeRoundRect(out) {
                serializeStroke(out)
            }
        }

    }

    private class FilledRoundRect<M : MeasureInfo> : BaseRoundRect<M> {

        internal constructor(bounds: Rectangle, rx: Double, ry: Double, stroke: SVGPen<M>, fill: SVGPen<M>) :
            super(bounds, rx, ry, stroke, fill)

        internal constructor(bounds: Rectangle, rx: Double, ry: Double, fill: SVGPen<M>) :
            super(bounds, rx, ry, null, fill)

        override fun serialize(out: XmlWriter) {
            serializeRoundRect(out) {
                serializeFill(out)
            }
        }

    }

    private abstract class BaseCircle<M : MeasureInfo>(internal val mX: Double,
                                                       internal val mY: Double,
                                                       internal val mRadius: Double,
                                                       stroke: SVGPen<M>?,
                                                       fill: SVGPen<M>?) : PaintedElem<M>(stroke, fill) {

        fun serializeCircle(out: XmlWriter) {
            out.startTag(SVG_NAMESPACE, "circle", null)
            out.writeAttribute("cx", mX)
            out.writeAttribute("cy", mY)
            out.writeAttribute("r", mRadius)
        }

        override fun getBounds(dest: Rectangle): Rectangle {
            val strokeWidth = stroke?.strokeWidth ?: 0.0
            val delta = strokeWidth / 2
            dest[mX - mRadius - delta, mY - mRadius - delta, mRadius * 2 + strokeWidth] = mRadius * 2 + strokeWidth
            return dest
        }

    }

    private class Circle<M : MeasureInfo>(x: Double, y: Double, radius: Double, color: SVGPen<M>) :
        BaseCircle<M>(x, y, radius, color, null) {

        override fun serialize(out: XmlWriter) {
            serializeCircle(out)
            serializeStroke(out)
            out.endTag(SVG_NAMESPACE, "circle", null)
        }

    }

    private class FilledCircle<M : MeasureInfo> : BaseCircle<M> {

        constructor(x: Double, y: Double, radius: Double, fill: SVGPen<M>) : super(x, y, radius, null, fill)

        constructor(x: Double, y: Double, radius: Double, stroke: SVGPen<M>, fill: SVGPen<M>) : super(x, y, radius,
                                                                                                      stroke, fill)

        override fun serialize(out: XmlWriter) {
            serializeCircle(out)
            serializeStrokeFill(out)
            out.endTag(SVG_NAMESPACE, "circle", null)
        }

    }

    private class PaintedPath<M : MeasureInfo>(internal val path: SVGPath,
                                               internal val stroke: SVGPen<M>,
                                               internal val fill: SVGPen<M>?) : IPaintedElem {

        override fun serialize(out: XmlWriter) {
            out.startTag(SVG_NAMESPACE, "path", null)
            serializeStyle(out, stroke, fill, null)

            out.attribute(null, "d", null, path.toPathData())

            out.endTag(SVG_NAMESPACE, "path", null)
        }

        override fun getBounds(dest: Rectangle): Rectangle {
            return path.getBounds(dest, stroke)
        }
    }


    private class DrawText<M : MeasureInfo>(internal val textPos: Canvas.TextPos,
                                            internal val x: Double,
                                            internal val y: Double,
                                            internal val text: String,
                                            internal val foldWidth: Double,
                                            color: SVGPen<M>) : PaintedElem<M>(null, color) {

        override fun serialize(out: XmlWriter) {
            out.smartStartTag(SVG_NAMESPACE, "text", null) {
                writeAttribute("x", x)
                writeAttribute("y", y)
                serializeStyle(this, null, fill, textPos)

                smartStartTag(SVG_NAMESPACE, "tspan", null) {
                    text(text)
                }
            }
        }

        override fun getBounds(dest: Rectangle): Rectangle {

            fill!!.measureTextSize(dest, x, y, text, foldWidth)
            textPos.offset(dest, fill)
            return dest
        }
    }

    private class SubCanvas<M : MeasureInfo> constructor(strategy: SVGStrategy<M>,
                                                         internal val x: Double,
                                                         internal val y: Double,
                                                         internal val scale: Double) :
        SVGCanvas<M>(strategy), IPaintedElem {

        override fun serialize(out: XmlWriter) {
            out.smartStartTag(SVG_NAMESPACE, "g", null) {
                if (x == 0.0 && y == 0.0) {
                    if (scale != 1.0) {
                        attribute(null, "transform", null, "scale($scale)")
                    }
                } else if (scale == 1.0) {
                    attribute(null, "transform", null, "translate($x,$y)")
                } else {
                    attribute(null, "transform", null,
                              "matrix(" + scale + ",0,0," + scale + "," + x * scale + "," + y * scale + ")")
                }
                for (element in path) {
                    element.serialize(this)
                }
            }
        }

    }

    // Only for debug purposes
    //  private SVGPen<M> mRedPen;
    //  private SVGPen<M> mGreenPen;

    constructor(textMeasurer: TextMeasurer<M>) : this(SVGStrategy<M>(textMeasurer))

    init {
        // Only for debug purposes
        //    mRedPen = mStrategy.newPen();
        //    mRedPen.setColor(0xff, 0, 0);
        //    mGreenPen = mStrategy.newPen();
        //    mGreenPen.setColor(0, 0xff, 0);
    }

    private fun ensureBounds() {
        if (_bounds.top.isFinite() &&
            _bounds.left.isFinite() &&
            _bounds.height.isFinite() &&
            _bounds.width.isFinite()) {
            return
        }
        if (path.size > 0) {
            val tmpBounds = Rectangle(0.0, 0.0, 0.0, 0.0)
            _bounds.set(path[0].getBounds(tmpBounds))
            for (i in 1 until path.size) {
                _bounds.extendBounds(path[i].getBounds(tmpBounds))
            }
        } else {
            _bounds[0.0, 0.0, 0.0] = 0.0
        }
    }

    fun getBounds(dest: Rectangle): Rectangle {
        ensureBounds()
        dest.set(_bounds)
        return dest
    }

    override fun childCanvas(offsetX: Double,
                             offsetY: Double,
                             scale: Double): Canvas<SVGStrategy<M>, SVGPen<M>, SVGPath> {
        val result = SubCanvas(strategy, offsetX, offsetY, scale)
        _bounds.top = Double.NaN
        path.add(result)
        return result
    }

    override fun drawFilledCircle(x: Double, y: Double, radius: Double, fill: SVGPen<M>) {
        _bounds.top = Double.NaN
        path.add(FilledCircle(x, y, radius, fill.copy()))
    }

    override fun drawRect(rect: Rectangle, stroke: SVGPen<M>) {
        _bounds.top = Double.NaN
        path.add(Rect(rect, stroke))
    }

    override fun drawFilledRect(rect: Rectangle, fill: SVGPen<M>) {
        _bounds.top = Double.NaN
        path.add(FilledRect(rect, fill))
    }

    override fun drawRect(rect: Rectangle, stroke: SVGPen<M>?, fill: SVGPen<M>?) {
        _bounds.top = Double.NaN
        val r = when {
            stroke == null -> when (fill) {
                null -> throw IllegalArgumentException("Either fill or stroke must be non-null")
                else -> FilledRect(rect, fill)
            }
            fill == null   -> Rect(rect, stroke)
            else           -> FilledRect(rect, stroke, fill)
        }
        path.add(r)
    }

    override fun drawCircle(x: Double, y: Double, radius: Double, stroke: SVGPen<M>) {
        _bounds.top = Double.NaN
        path.add(Circle(x, y, radius, stroke))
    }

    override fun drawCircle(x: Double,
                            y: Double,
                            radius: Double,
                            stroke: SVGPen<M>?,
                            fill: SVGPen<M>?) {
        _bounds.top = Double.NaN
        val r = when {
            stroke == null -> when (fill) {
                null -> throw IllegalArgumentException("Either fill or stroke must be non-null")
                else -> FilledCircle(x, y, radius, fill)
            }
            fill == null   -> Circle(x, y, radius, stroke)
            else           -> FilledCircle(x, y, radius, stroke, fill)
        }
        path.add(r)
    }

    override fun drawRoundRect(rect: Rectangle, rx: Double, ry: Double, stroke: SVGPen<M>) {
        _bounds.top = Double.NaN
        path.add(RoundRect(rect, rx, ry, stroke))

    }

    override fun drawFilledRoundRect(rect: Rectangle, rx: Double, ry: Double, fill: SVGPen<M>) {
        _bounds.top = Double.NaN
        path.add(FilledRoundRect(rect, rx, ry, fill))
    }

    override fun drawRoundRect(rect: Rectangle,
                               rx: Double,
                               ry: Double,
                               stroke: SVGPen<M>?,
                               fill: SVGPen<M>?) {
        _bounds.top = Double.NaN
        val r = when {
            stroke == null -> when (fill) {
                null -> throw IllegalArgumentException("Either fill or stroke must be non-null")
                else -> FilledRoundRect(rect, rx, ry, fill)
            }
            fill == null   -> RoundRect(rect, rx, ry, stroke)
            else           -> FilledRoundRect(rect, rx, ry, stroke, fill)
        }
        path.add(r)
    }

    override fun drawPoly(points: DoubleArray, stroke: SVGPen<M>?, fill: SVGPen<M>?) {
        if (points.size > 1) {
            val path = pointsToPath(points)
            drawPath(path, stroke!!, fill)
        }
    }

    override fun drawPath(path: SVGPath, stroke: SVGPen<M>, fill: SVGPen<M>?) {
        _bounds.top = Double.NaN
        this.path.add(PaintedPath(path, stroke, fill))
    }

    override fun drawText(textPos: Canvas.TextPos, left: Double, baselineY: Double, text: String, foldWidth: Double,
                          pen: SVGPen<M>) {
        val adjustedY = when {
            sUseBaselineAlign -> baselineY
            else              -> adjustToBaseline(textPos, baselineY, pen)
        }

        _bounds.top = Double.NaN
        path.add(DrawText(textPos, left, adjustedY, text, foldWidth, pen.copy()))
    }

    private fun adjustToBaseline(textPos: nl.adaptivity.diagram.Canvas.TextPos, y: Double, pen: SVGPen<M>): Double {
        return when (textPos) {
            Canvas.TextPos.BASELINELEFT,
            Canvas.TextPos.BASELINEMIDDLE,
            Canvas.TextPos.BASELINERIGHT -> y

            Canvas.TextPos.BOTTOM,
            Canvas.TextPos.BOTTOMLEFT,
            Canvas.TextPos.BOTTOMRIGHT   -> y - pen.textMaxDescent

            Canvas.TextPos.DESCENT,
            Canvas.TextPos.DESCENTLEFT,
            Canvas.TextPos.DESCENTRIGHT  -> y - pen.textDescent
            Canvas.TextPos.LEFT,
            Canvas.TextPos.MIDDLE,
            Canvas.TextPos.RIGHT         -> y + 0.5 * (pen.textAscent - pen.textDescent)

            Canvas.TextPos.ASCENT,
            Canvas.TextPos.ASCENTLEFT,
            Canvas.TextPos.ASCENTRIGHT   -> y + pen.textAscent

            Canvas.TextPos.MAXTOP,
            Canvas.TextPos.MAXTOPLEFT,
            Canvas.TextPos.MAXTOPRIGHT   -> y + pen.textMaxAscent
        }
    }

    open fun serialize(out: XmlWriter) {
        ensureBounds()
        out.smartStartTag(SVG_NAMESPACE, "svg", DEFAULT_NS_PREFIX) {
            writeAttribute("version", "1.1")
            writeAttribute("width", _bounds.width)
            writeAttribute("height", _bounds.height)
            val closeGroup: Boolean
            // As svg outer element only supports width and height, when the topleft corner is not at the
            // origin then wrap the content into a group that translates appropriately.
            if (abs(_bounds.left) > SERIALIZATION_MIN_OFFSET || abs(_bounds.top) > SERIALIZATION_MIN_OFFSET) {
                closeGroup = true
                startTag(SVG_NAMESPACE, "g", null)
                attribute(null, "transform", null, "translate(" + -_bounds.left + "," + -_bounds.top + ")")
            } else {
                closeGroup = false
            }


            for (element in path) {
                try {
                    element.serialize(this)
                } catch (e: XmlException) {
                    throw RuntimeException(e)
                }

            }
            if (closeGroup) endTag(SVG_NAMESPACE, "g", null)
        }
    }

    companion object {

        const val SERIALIZATION_MIN_OFFSET = 0.000001

        private const val SVG_NAMESPACE = "http://www.w3.org/2000/svg"

        private var sUseBaselineAlign = false

        fun serializeStyle(out: XmlWriter, stroke: SVGPen<*>?, fill: SVGPen<*>?, textPos: Canvas.TextPos?) {
            val style = StringBuilder()
            if (stroke != null) {
                val color = stroke.color
                style.append("stroke: ").append(colorToSVGpaint(color)).append("; ")
                if (hasAlpha(color)) {
                    style.append("stroke-opacity: ").append(colorToSVGOpacity(color)).append("; ")
                }
                style.append("stroke-width: ").append(stroke.strokeWidth.toString()).append("; ")
            } else {
                style.append("stroke:none; ")
            }
            if (fill != null) {
                val color = fill.color
                style.append("fill: ").append(colorToSVGpaint(color)).append("; ")
                if (hasAlpha(color)) {
                    style.append("fill-opacity: ").append(colorToSVGOpacity(color)).append("; ")
                }
                if (textPos != null) {
                    style.append("font-family: Arial, Helvetica, sans; ")
                    style.append("font-size: ").append(fill.fontSize.toString()).append("; ")
                    if (fill.isTextItalics) {
                        style.append("font-style: italic; ")
                    } else {
                        style.append("font-style: normal; ")
                    }
                    style.append("text-anchor: ").append(toAnchor(textPos)).append("; ")
                    if (sUseBaselineAlign) {
                        style.append("alignment-baseline: ").append(toBaseline(textPos)).append("; ")
                    }
                }
            } else {
                style.append("fill:none; ")
            }

            out.attribute(null, "style", null, style.toString())
        }

        private fun toBaseline(textPos: Canvas.TextPos) = when (textPos) {
            Canvas.TextPos.BASELINELEFT,
            Canvas.TextPos.BASELINEMIDDLE,
            Canvas.TextPos.BASELINERIGHT -> "auto"

            Canvas.TextPos.DESCENTLEFT,
            Canvas.TextPos.DESCENT,
            Canvas.TextPos.DESCENTRIGHT,
            Canvas.TextPos.BOTTOM,
            Canvas.TextPos.BOTTOMLEFT,
            Canvas.TextPos.BOTTOMRIGHT   -> "after-edge"

            Canvas.TextPos.LEFT,
            Canvas.TextPos.MIDDLE,
            Canvas.TextPos.RIGHT         -> "central"

            Canvas.TextPos.ASCENT,
            Canvas.TextPos.ASCENTLEFT,
            Canvas.TextPos.ASCENTRIGHT,
            Canvas.TextPos.MAXTOP,
            Canvas.TextPos.MAXTOPLEFT,
            Canvas.TextPos.MAXTOPRIGHT   -> "before-edge"
        }

        private fun toAnchor(textPos: Canvas.TextPos): String = when (textPos) {
            Canvas.TextPos.MAXTOPLEFT,
            Canvas.TextPos.ASCENTLEFT,
            Canvas.TextPos.DESCENTLEFT,
            Canvas.TextPos.LEFT,
            Canvas.TextPos.BASELINELEFT,
            Canvas.TextPos.BOTTOMLEFT  -> "start"

            Canvas.TextPos.MAXTOP,
            Canvas.TextPos.ASCENT,
            Canvas.TextPos.DESCENT,
            Canvas.TextPos.MIDDLE,
            Canvas.TextPos.BASELINEMIDDLE,
            Canvas.TextPos.BOTTOM      -> "middle"

            Canvas.TextPos.MAXTOPRIGHT,
            Canvas.TextPos.ASCENTRIGHT,
            Canvas.TextPos.DESCENTRIGHT,
            Canvas.TextPos.RIGHT,
            Canvas.TextPos.BASELINERIGHT,
            Canvas.TextPos.BOTTOMRIGHT -> "end"
        }

        private fun hasAlpha(color: Int): Boolean {
            return color.ushr(24) != 0xff
        }

        private fun colorToSVGOpacity(color: Int): String {
            val alpha = color.ushr(24).toDouble()
            return (alpha / 255.0).toString().substring(0, 8)
        }

        private fun colorToSVGpaint(color: Int): String {
            return "#${(color and 0xffffff).toHex().padStart(6, '0')}" // Ignore alpha here
        }

        private fun pointsToPath(points: DoubleArray): SVGPath {
            val path = SVGPath()
            path.moveTo(points[0], points[1])
            var i = 2
            while (i < points.size) {
                path.lineTo(points[i], points[i + 1])
                i += 2
            }
            return path
        }
    }
}
