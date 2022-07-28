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

@file:OptIn(ExperimentalContracts::class)

package io.github.pdvrieze.process.compose.common.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import nl.adaptivity.diagram.Canvas
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.Theme
import org.w3c.dom.*
import kotlin.contracts.ExperimentalContracts
import kotlin.math.PI


val Rectangle.topLeft: Offset get()  = Offset(leftf, topf)

val Rectangle.size: Size get() = Size(widthf, heightf)

class JsCanvas(
    private var jsCanvas: HTMLCanvasElement,
    private var _theme: Theme<JsCanvasStrategy, JsCanvasPen, JsCanvasPath>?,
) : IJsCanvas {

    private val context = jsCanvas.getContext("2d") as CanvasRenderingContext2D

    private inline fun strokePath(body: CanvasPath.()->Unit) {
        context.beginPath()
        context.body()
        context.stroke()
    }

    private inline fun fillPath(fillRule: CanvasFillRule, body: CanvasPath.()->Unit) {
        context.beginPath()
        context.body()
        context.fill(fillRule)
    }

    private inline fun fillPath(body: CanvasPath.()->Unit) {
        context.beginPath()
        context.body()
        context.fill()
    }

    override val strategy: JsCanvasStrategy =
        JsCanvasStrategy(context)

    private fun setFill(fill: JsCanvasPen) {
        context.apply {
            fillStyle = fill.fillStyle
        }
    }

    private fun setStroke(stroke: JsCanvasPen) {
        context.apply {
            strokeStyle = stroke.strokeStyle
            lineCap = stroke.strokeCap
            lineWidth = stroke.strokeWidth
        }
    }

    override val theme: Theme<JsCanvasStrategy, JsCanvasPen, JsCanvasPath>
        get() {
            return _theme ?: JsCanvasTheme(strategy).also { _theme = it }
        }

    override fun childCanvas(offsetX: Double, offsetY: Double, scale: Double): IJsCanvas {
        return OffsetCanvas(offsetX, offsetY, scale)
    }

    override fun drawFilledCircle(x: Double, y: Double, radius: Double, fill: JsCanvasPen) {
        setFill(fill)
        fillPath {
            arc(x, y, radius, 0.0, PI*2)
        }
    }

    override fun drawCircle(x: Double, y: Double, radius: Double, stroke: JsCanvasPen) {
        setStroke(stroke)
        strokePath {
            arc(x, y, radius, 0.0, PI*2)
            closePath()
        }
    }

    override fun drawCircle(
        x: Double,
        y: Double,
        radius: Double,
        stroke: JsCanvasPen?,
        fill: JsCanvasPen?,
    ) {
        if (fill != null) {
            drawFilledCircle(x, y, radius, fill)
        }
        if (stroke != null) {
            drawCircle(x, y, radius, stroke)
        }
    }

    fun roundRect(rect: Rectangle, rx: Double, ry: Double): JsCanvasPath {
        val path = Path2D()
        path.moveTo(rect.left+rx, rect.top)
        path.lineTo(rect.right-rx, rect.top)
        path.arcTo(rect.right, rect.top, rect.right, rect.top+ry, rx, ry, PI*0.5)
        path.lineTo(rect.right, rect.bottom-ry)
        path.arcTo(rect.right, rect.bottom, rect.right-rx, rect.bottom, rx, ry, PI*0.5)
        path.lineTo(rect.left+rx, rect.bottom)
        path.arcTo(rect.left, rect.bottom, rect.left, rect.bottom-ry, rx, ry, PI*0.5)
        path.lineTo(rect.left, rect.bottom-ry)
        path.arcTo(rect.left, rect.top, rect.left+rx, rect.top, rx, ry, PI*0.5)
        path.closePath()

        return JsCanvasPath(path)
    }

    override fun drawFilledRoundRect(
        rect: Rectangle,
        rx: Double,
        ry: Double,
        fill: JsCanvasPen,
    ) {
        setFill(fill)
        context.fill(roundRect(rect, rx, ry).path)
    }

    override fun drawRoundRect(
        rect: Rectangle,
        rx: Double,
        ry: Double,
        stroke: JsCanvasPen,
    ) {
        setStroke(stroke)
        context.stroke(roundRect(rect, rx, ry).path)
    }

    override fun drawRoundRect(
        rect: Rectangle,
        rx: Double,
        ry: Double,
        stroke: JsCanvasPen?,
        fill: JsCanvasPen?,
    ) {
        val p = roundRect(rect, rx, ry).path
        if (fill != null) {
            setFill(fill)
            context.fill(p)
        }
        if (stroke != null) {
            setStroke(stroke)
            context.stroke(p)
        }
    }

    override fun drawFilledRect(rect: Rectangle, fill: JsCanvasPen) {
        setFill(fill)
        context.fillRect(rect.left, rect.top, rect.width, rect.height)
    }

    override fun drawRect(rect: Rectangle, stroke: JsCanvasPen) {
        setStroke(stroke)
        context.rect(rect.left, rect.top, rect.width, rect.height)
    }

    override fun drawRect(
        rect: Rectangle,
        stroke: JsCanvasPen?,
        fill: JsCanvasPen?,
    ) {
        if (fill != null) {
            setFill(fill)
            context.fillRect(rect.left, rect.top, rect.width, rect.height)
        }
        if (stroke != null) {
            setStroke(stroke)
            context.rect(rect.left, rect.top, rect.width, rect.height)
        }
    }

    override fun drawPoly(
        points: DoubleArray,
        stroke: JsCanvasPen?,
        fill: JsCanvasPen?,
    ) {

        val composePath = toPath(points)
        if (fill != null) {
            setFill(fill)
            context.fill(composePath)
        }
        if (stroke != null) {
            setStroke(stroke)
            context.fill(composePath)
        }
    }

    override fun drawPath(
        path: JsCanvasPath,
        stroke: JsCanvasPen?,
        fill: JsCanvasPen?,
    ) {
//        drawScope.drawPath(path.path, SolidColor(Color.Blue), style = Stroke(width=4f))
        if (fill != null) drawFilledPath(path.path, fill)
        if (stroke != null) {
            setStroke(stroke)
            context.stroke(path.path)
        }
    }

    private fun drawFilledPath(path: Path2D, fill: JsCanvasPen) {
        setFill(fill)
        context.fill(path)
    }

    private fun toPath(points: DoubleArray): Path2D {
        val result = Path2D()

        val len = points.size - 1
        if (len > 0) {
            result.moveTo(points[0], points[1])
            var i = 2
            while (i < len) {
                result.lineTo(points[i], points[++i])
                ++i
            }
            result.closePath()
        }
        return result
    }


    override fun scale(scale: Double): IJsCanvas {
        return OffsetCanvas(scale)
    }

    override fun translate(dx: Double, dy: Double): IJsCanvas {
        return if (dx == 0.0 && dy == 0.0) {
            this
        } else OffsetCanvas(dx, dy, 1.0)
    }

    override fun drawText(
        textPos: Canvas.TextPos,
        left: Double,
        baselineY: Double,
        text: String,
        foldWidth: Double,
        pen: JsCanvasPen,
    ) {
        drawText(textPos, left, baselineY, text, foldWidth, pen, 1.0)
    }

    private fun drawText(
        textPos: Canvas.TextPos,
        x: Double,
        y: Double,
        text: String,
        foldWidth: Double,
        pen: JsCanvasPen,
        scale: Double,
    ) {
//        val paint = pen.brush
//        paint.style = Style.FILL
//        val left = getLeft(textPos, x, text, foldWidth, pen, scale)
//        val baseline = getBaseLine(textPos, y, pen, scale)
//        drawScope.drawText(text, left, baseline, paint)
        //Only for debug purposes
        //    mCanvas.drawCircle(left, baseline, 3f, mRedPaint);
        //    mCanvas.drawCircle((float)pX, (float)pY, 3f, mGreenPaint);
    }

    private fun getBaseLine(
        textPos: Canvas.TextPos,
        y: Double,
        pen: Pen<*>,
        scale: Double,
    ): Float {
        when (textPos) {
            Canvas.TextPos.MAXTOPLEFT,
            Canvas.TextPos.MAXTOP,
            Canvas.TextPos.MAXTOPRIGHT,
            -> return (y + pen.textMaxAscent * scale).toFloat()

            Canvas.TextPos.ASCENTLEFT,
            Canvas.TextPos.ASCENT,
            Canvas.TextPos.ASCENTRIGHT,
            -> return (y + pen.textAscent * scale).toFloat()

            Canvas.TextPos.LEFT,
            Canvas.TextPos.MIDDLE,
            Canvas.TextPos.RIGHT,
            -> return (y + (0.5 * pen.textAscent - 0.5 * pen.textDescent) * scale).toFloat()

            Canvas.TextPos.BASELINEMIDDLE,
            Canvas.TextPos.BASELINERIGHT,
            Canvas.TextPos.BASELINELEFT,
            -> return y.toFloat()

            Canvas.TextPos.BOTTOMLEFT,
            Canvas.TextPos.BOTTOMRIGHT,
            Canvas.TextPos.BOTTOM,
            -> return (y - pen.textMaxDescent * scale).toFloat()

            Canvas.TextPos.DESCENTLEFT,
            Canvas.TextPos.DESCENTRIGHT,
            Canvas.TextPos.DESCENT,
            -> return (y - pen.textDescent * scale).toFloat()
        }
    }

    private fun getLeft(
        textPos: Canvas.TextPos,
        x: Double,
        text: String,
        foldWidth: Double,
        pen: JsCanvasPen,
        scale: Double,
    ): Float {
        when (textPos) {
            Canvas.TextPos.BASELINELEFT,
            Canvas.TextPos.BOTTOMLEFT,
            Canvas.TextPos.LEFT,
            Canvas.TextPos.MAXTOPLEFT,
            Canvas.TextPos.ASCENTLEFT,
            -> return x.toFloat()

            Canvas.TextPos.ASCENT,
            Canvas.TextPos.DESCENT,
            Canvas.TextPos.BASELINEMIDDLE,
            Canvas.TextPos.MAXTOP,
            Canvas.TextPos.MIDDLE,
            Canvas.TextPos.BOTTOM,
            -> return (x - pen.measureTextWidth(text, foldWidth) * scale / 2).toFloat()

            Canvas.TextPos.MAXTOPRIGHT,
            Canvas.TextPos.ASCENTRIGHT,
            Canvas.TextPos.DESCENTLEFT,
            Canvas.TextPos.DESCENTRIGHT,
            Canvas.TextPos.RIGHT,
            Canvas.TextPos.BASELINERIGHT,
            Canvas.TextPos.BOTTOMRIGHT,
            -> return (x - pen.measureTextWidth(text, foldWidth) * scale).toFloat()
        }
    }


    private inner class OffsetCanvas(xOffset: Double, yOffset: Double, private val scale: Double) : IJsCanvas {

        /** The offset of the canvas. This is in scaled coordinates.  */
        private val xOffset: Double = -xOffset
        private val yOffset: Double = -yOffset

        override val strategy: JsCanvasStrategy
            get() = this@JsCanvas.strategy


        override val theme: Theme<JsCanvasStrategy, JsCanvasPen, JsCanvasPath>
            get() = this@JsCanvas.theme


        constructor(base: OffsetCanvas, offsetX: Double, offsetY: Double, scale: Double) :
            this(
                (base.xOffset - offsetX) * scale,
                (base.yOffset - offsetY) * scale,
                base.scale * scale
            )

        constructor(base: OffsetCanvas, scale: Double) :
            this(base.xOffset * scale, base.yOffset * scale, base.scale * scale)

        constructor(scale: Double) : this(0.0, 0.0, scale)

        override fun scale(scale: Double): IJsCanvas {
            return OffsetCanvas(this, scale)
        }

        override fun childCanvas(offsetX: Double, offsetY: Double, scale: Double): IJsCanvas {
            return OffsetCanvas(this, offsetX, offsetY, scale)
        }


        override fun translate(dx: Double, dy: Double): IJsCanvas {
            return OffsetCanvas(xOffset - dx, yOffset - dy, scale)
        }

        private fun JsCanvasPen.scale(): JsCanvasPen {
            return this.scale(scale)
        }

        override fun drawCircle(x: Double, y: Double, radius: Double, stroke: JsCanvasPen) {
            this@JsCanvas.drawCircle(transformX(x), transformY(y), radius * scale, stroke.scale()!!)
        }

        override fun drawFilledCircle(
            x: Double,
            y: Double,
            radius: Double,
            fill: JsCanvasPen,
        ) {
            this@JsCanvas.drawFilledCircle(transformX(x), transformY(y), radius * scale, fill)
        }

        override fun drawCircle(
            x: Double, y: Double, radius: Double, stroke: JsCanvasPen?,
            fill: JsCanvasPen?,
        ) {
            if (fill != null) {
                this@JsCanvas.drawFilledCircle(transformX(x), transformY(y), radius * scale, fill)
            }
            if (stroke != null) {
                this@JsCanvas.drawCircle(transformX(x), transformY(y), radius * scale, stroke.scale()!!)
            }
        }

/*
        override fun drawBitmap(
            left: Double,
            top: Double,
            bitmap: Bitmap,
            pen: ComposePen,
        ) {
            this@JsCanvas.drawBitmap(transformX(left), transformY(top), bitmap, scalePen(pen)!!)
        }
*/

        override fun drawRect(rect: Rectangle, stroke: JsCanvasPen) {
            this@JsCanvas.drawRect(rect.offsetScaled(-xOffset, -yOffset, scale), stroke.scale()!!)
        }

        override fun drawFilledRect(rect: Rectangle, fill: JsCanvasPen) {
            this@JsCanvas.drawFilledRect(rect.offsetScaled(-xOffset, -yOffset, scale), fill.scale()!!)
        }

        override fun drawRect(
            rect: Rectangle,
            stroke: JsCanvasPen?,
            fill: JsCanvasPen?,
        ) {
            if (fill != null) {
                this@JsCanvas.drawFilledRect(rect.offsetScaled(-xOffset, -yOffset, scale), fill)
            }

            if (stroke != null) {
                this@JsCanvas.drawRect(rect.offsetScaled(-xOffset, -yOffset, scale), stroke.scale()!!)
            }
        }

        override fun drawPoly(
            points: DoubleArray,
            stroke: JsCanvasPen?,
            fill: JsCanvasPen?,
        ) {
            this@JsCanvas.drawPoly(transform(points), stroke?.scale(), fill)
        }

        private fun transform(points: DoubleArray): DoubleArray {
            val result = DoubleArray(points.size)
            val len = points.size - 1
            var i = 0
            while (i < len) {
                result[i] = transformX(points[i])
                ++i
                result[i] = transformY(points[i])
                ++i
            }
            return result
        }

        fun transformX(x: Double): Double {
            return (x + xOffset) * scale
        }

        fun transformY(y: Double): Double {
            return (y + yOffset) * scale
        }

        override fun drawPath(path: JsCanvasPath, stroke: JsCanvasPen?, fill: JsCanvasPen?) {
            context.save()
            context.scale(scale, scale)
            context.translate(xOffset, yOffset)
            this@JsCanvas.drawPath(path, stroke, fill)
            context.restore()
        }

        override fun drawRoundRect(
            rect: Rectangle,
            rx: Double,
            ry: Double,
            stroke: JsCanvasPen,
        ) {
            this@JsCanvas.drawRoundRect(
                rect.offsetScaled(xOffset, yOffset, scale), rx * scale, ry * scale,
                stroke.scale()!!
            )
        }

        override fun drawFilledRoundRect(
            rect: Rectangle,
            rx: Double,
            ry: Double,
            fill: JsCanvasPen,
        ) {
            this@JsCanvas.drawFilledRoundRect(
                rect.offsetScaled(xOffset, yOffset, scale), rx * scale,
                ry * scale, fill.scale()
            )
        }

        override fun drawRoundRect(
            rect: Rectangle,
            rx: Double,
            ry: Double,
            stroke: JsCanvasPen?,
            fill: JsCanvasPen?,
        ) {
            if (fill != null) {
                this@JsCanvas.drawFilledRoundRect(
                    rect.offsetScaled(-xOffset, -yOffset, scale), rx * scale,
                    ry * scale, fill.scale()!!
                )
            }
            if (stroke != null) {
                this@JsCanvas.drawRoundRect(
                    rect.offsetScaled(-xOffset, -yOffset, scale), rx * scale,
                    ry * scale, stroke.scale()!!
                )
            }
        }

        override fun drawText(
            textPos: Canvas.TextPos,
            left: Double,
            baselineY: Double,
            text: String,
            foldWidth: Double,
            pen: JsCanvasPen,
        ) {
            this@JsCanvas.drawText(
                textPos, transformX(left), transformY(baselineY), text, foldWidth * scale,
                pen.scale()!!, scale
            )
        }
    }

}
