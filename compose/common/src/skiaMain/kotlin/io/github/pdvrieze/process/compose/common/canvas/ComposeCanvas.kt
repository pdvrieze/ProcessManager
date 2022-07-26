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
import androidx.compose.ui.graphics.drawscope.DrawScope
import nl.adaptivity.diagram.Canvas
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.Theme
import kotlin.contracts.ExperimentalContracts


val Rectangle.topLeft: Offset get()  = Offset(leftf, topf)

val Rectangle.size: Size get() = Size(widthf, heightf)

class ComposeCanvas(
    private var drawScope: DrawScope,
    private var _theme: Theme<ComposeStrategy, ComposePen, ComposePath>?,
) : IComposeCanvas {

    override val strategy: ComposeStrategy
        get() = ComposeStrategy.INSTANCE

    override val theme: Theme<ComposeStrategy, ComposePen, ComposePath>
        get() {
            return _theme ?: ComposeTheme(strategy).also { _theme = it }
        }

    fun setCanvas(canvas: DrawScope) {
        this.drawScope = canvas
    }

    override fun childCanvas(offsetX: Double, offsetY: Double, scale: Double): IComposeCanvas {
        return OffsetCanvas(offsetX, offsetY, scale)
    }

    override fun drawFilledCircle(x: Double, y: Double, radius: Double, fill: ComposePen) {
        drawScope.drawCircle(fill.brush, radius.toFloat(), Offset(x.toFloat(), y.toFloat()))
    }

    override fun drawCircle(x: Double, y: Double, radius: Double, stroke: ComposePen) {
        drawScope.drawCircle(stroke.brush, radius.toFloat(), Offset(x.toFloat(), y.toFloat()), style = stroke.getStroke())
    }

    override fun drawCircle(
        x: Double,
        y: Double,
        radius: Double,
        stroke: ComposePen?,
        fill: ComposePen?,
    ) {
        if (fill != null) {
            drawFilledCircle(x, y, radius, fill)
        }
        if (stroke != null) {
            drawCircle(x, y, radius, stroke)
        }
    }

    override fun drawFilledRoundRect(
        rect: Rectangle,
        rx: Double,
        ry: Double,
        fill: ComposePen,
    ) {
        drawScope.drawRoundRect(fill.brush,rect.topLeft, rect.size, CornerRadius(rx.toFloat(), ry.toFloat()))
    }

    override fun drawRoundRect(
        rect: Rectangle,
        rx: Double,
        ry: Double,
        stroke: ComposePen,
    ) {
        drawScope.drawRoundRect(stroke.brush,rect.topLeft, rect.size, CornerRadius(rx.toFloat(), ry.toFloat()), style = stroke.getStroke())
    }

    override fun drawRoundRect(
        rect: Rectangle,
        rx: Double,
        ry: Double,
        stroke: ComposePen?,
        fill: ComposePen?,
    ) {
        if (fill != null) {
            drawFilledRoundRect(rect, rx, ry, fill)
        }
        if (stroke != null) {
            drawRoundRect(rect, rx, ry, stroke)
        }
    }

    override fun drawFilledRect(rect: Rectangle, fill: ComposePen) {
        drawScope.drawRect(fill.brush,rect.topLeft, rect.size)
    }

    override fun drawRect(rect: Rectangle, stroke: ComposePen) {
        drawScope.drawRect(stroke.brush,rect.topLeft, rect.size, style = stroke.getStroke())
    }

    override fun drawRect(
        rect: Rectangle,
        stroke: ComposePen?,
        fill: ComposePen?,
    ) {
        if (fill != null) {
            drawFilledRect(rect, fill)
        }
        if (stroke != null) {
            drawRect(rect, stroke)
        }
    }

    override fun drawPoly(
        points: DoubleArray,
        stroke: ComposePen?,
        fill: ComposePen?,
    ) {

        val composePath = toPath(points)
        if (fill != null) {
            drawScope.drawPath(composePath, fill.brush)
        }
        if (stroke != null) {
            drawScope.drawPath(composePath, stroke.brush, style = stroke.getStroke())
        }
    }

    override fun drawPath(
        path: ComposePath,
        stroke: ComposePen?,
        fill: ComposePen?,
    ) {
//        drawScope.drawPath(path.path, SolidColor(Color.Blue), style = Stroke(width=4f))
        if (fill != null) drawFilledPath(path.path, fill)
        if (stroke != null) {
            drawScope.drawPath(path.path, stroke.brush, style = stroke.getStroke())
        }
    }

    private fun drawFilledPath(path: Path, fill: ComposePen) {
        drawScope.drawPath(path, fill.brush)
    }

    private fun toPath(points: DoubleArray): Path {
        val result = Path()

        val len = points.size - 1
        if (len > 0) {
            result.moveTo(points[0].toFloat(), points[1].toFloat())
            var i = 2
            while (i < len) {
                result.lineTo(points[i].toFloat(), points[++i].toFloat())
                ++i
            }
            result.close()
        }
        return result
    }


    override fun scale(scale: Double): IComposeCanvas {
        return OffsetCanvas(scale)
    }

    override fun translate(dx: Double, dy: Double): IComposeCanvas {
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
        pen: ComposePen,
    ) {
        drawText(textPos, left, baselineY, text, foldWidth, pen, 1.0)
    }

    private fun drawText(
        textPos: Canvas.TextPos,
        x: Double,
        y: Double,
        text: String,
        foldWidth: Double,
        pen: ComposePen,
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
        pen: ComposePen,
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


    private inner class OffsetCanvas(xOffset: Double, yOffset: Double, private val scale: Double) : IComposeCanvas {

        /** The offset of the canvas. This is in scaled coordinates.  */
        private val xOffset: Double = -xOffset
        private val yOffset: Double = -yOffset

        override val strategy: ComposeStrategy
            get() = ComposeStrategy.INSTANCE


        override val theme: Theme<ComposeStrategy, ComposePen, ComposePath>
            get() = this@ComposeCanvas.theme


        constructor(base: OffsetCanvas, offsetX: Double, offsetY: Double, scale: Double) :
            this(
                (base.xOffset - offsetX) * scale,
                (base.yOffset - offsetY) * scale,
                base.scale * scale
            )

        constructor(base: OffsetCanvas, scale: Double) :
            this(base.xOffset * scale, base.yOffset * scale, base.scale * scale)

        constructor(scale: Double) : this(0.0, 0.0, scale)

        override fun scale(scale: Double): IComposeCanvas {
            return OffsetCanvas(this, scale)
        }

        override fun childCanvas(offsetX: Double, offsetY: Double, scale: Double): IComposeCanvas {
            return OffsetCanvas(this, offsetX, offsetY, scale)
        }


        override fun translate(dx: Double, dy: Double): IComposeCanvas {
            return OffsetCanvas(xOffset - dx, yOffset - dy, scale)
        }

        private fun ComposePen.scale(): ComposePen {
            return this.scale(scale)
        }

        override fun drawCircle(x: Double, y: Double, radius: Double, stroke: ComposePen) {
            this@ComposeCanvas.drawCircle(transformX(x), transformY(y), radius * scale, stroke.scale()!!)
        }

        override fun drawFilledCircle(
            x: Double,
            y: Double,
            radius: Double,
            fill: ComposePen,
        ) {
            this@ComposeCanvas.drawFilledCircle(transformX(x), transformY(y), radius * scale, fill)
        }

        override fun drawCircle(
            x: Double, y: Double, radius: Double, stroke: ComposePen?,
            fill: ComposePen?,
        ) {
            if (fill != null) {
                this@ComposeCanvas.drawFilledCircle(transformX(x), transformY(y), radius * scale, fill)
            }
            if (stroke != null) {
                this@ComposeCanvas.drawCircle(transformX(x), transformY(y), radius * scale, stroke.scale()!!)
            }
        }

/*
        override fun drawBitmap(
            left: Double,
            top: Double,
            bitmap: Bitmap,
            pen: ComposePen,
        ) {
            this@ComposeCanvas.drawBitmap(transformX(left), transformY(top), bitmap, scalePen(pen)!!)
        }
*/

        override fun drawRect(rect: Rectangle, stroke: ComposePen) {
            this@ComposeCanvas.drawRect(rect.offsetScaled(-xOffset, -yOffset, scale), stroke.scale()!!)
        }

        override fun drawFilledRect(rect: Rectangle, fill: ComposePen) {
            this@ComposeCanvas.drawFilledRect(rect.offsetScaled(-xOffset, -yOffset, scale), fill.scale()!!)
        }

        override fun drawRect(
            rect: Rectangle,
            stroke: ComposePen?,
            fill: ComposePen?,
        ) {
            if (fill != null) {
                this@ComposeCanvas.drawFilledRect(rect.offsetScaled(-xOffset, -yOffset, scale), fill)
            }

            if (stroke != null) {
                this@ComposeCanvas.drawRect(rect.offsetScaled(-xOffset, -yOffset, scale), stroke.scale()!!)
            }
        }

        override fun drawPoly(
            points: DoubleArray,
            stroke: ComposePen?,
            fill: ComposePen?,
        ) {
            this@ComposeCanvas.drawPoly(transform(points), stroke?.scale(), fill)
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

        override fun drawPath(path: ComposePath, stroke: ComposePen?, fill: ComposePen?) {
            this@ComposeCanvas.drawPath(path, stroke, fill)
/*
            this@ComposeCanvas.drawScope.scale(scale.toFloat()) {
                translate((-xOffset).toFloat(), (-yOffset).toFloat()) {
                    ComposeCanvas(this, _theme).drawPath(path, stroke, fill)
                    // TODO check whether the pen needs to be scale: scalePen(stroke)
                    // TODO skip the temporary canvas.
                }
            }
*/
        }

        private fun transformPath(path: ComposePath): ComposePath {
            val newPath = ComposePath()
            newPath.path.addPath(path.path)
            newPath.path.translate(Offset((-xOffset).toFloat(), (-yOffset).toFloat()))
            return newPath
/*

            val transformedPath =
            val transformedPath = Path(path.path)
            val matrix = Matrix()
            matrix.setScale(scale.toFloat(), scale.toFloat())
            matrix.preTranslate((-xOffset).toFloat(), (-yOffset).toFloat())
            transformedPath.transform(matrix)
            return transformedPath
*/
        }

        override fun drawRoundRect(
            rect: Rectangle,
            rx: Double,
            ry: Double,
            stroke: ComposePen,
        ) {
            this@ComposeCanvas.drawRoundRect(
                rect.offsetScaled(xOffset, yOffset, scale), rx * scale, ry * scale,
                stroke.scale()!!
            )
        }

        override fun drawFilledRoundRect(
            rect: Rectangle,
            rx: Double,
            ry: Double,
            fill: ComposePen,
        ) {
            this@ComposeCanvas.drawFilledRoundRect(
                rect.offsetScaled(xOffset, yOffset, scale), rx * scale,
                ry * scale, fill.scale()
            )
        }

        override fun drawRoundRect(
            rect: Rectangle,
            rx: Double,
            ry: Double,
            stroke: ComposePen?,
            fill: ComposePen?,
        ) {
            if (fill != null) {
                this@ComposeCanvas.drawFilledRoundRect(
                    rect.offsetScaled(-xOffset, -yOffset, scale), rx * scale,
                    ry * scale, fill.scale()!!
                )
            }
            if (stroke != null) {
                this@ComposeCanvas.drawRoundRect(
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
            pen: ComposePen,
        ) {
            this@ComposeCanvas.drawText(
                textPos, transformX(left), transformY(baselineY), text, foldWidth * scale,
                pen.scale()!!, scale
            )
        }
    }

}
