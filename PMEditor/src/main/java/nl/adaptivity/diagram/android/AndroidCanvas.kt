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

import android.graphics.*
import android.graphics.Paint.Style
import nl.adaptivity.diagram.Canvas
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.Theme
import org.jetbrains.annotations.Contract


class AndroidCanvas(private var canvas: android.graphics.Canvas,
                    private var _theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>?)
    : IAndroidCanvas {

    override val strategy: AndroidStrategy
        get() = AndroidStrategy.INSTANCE

    override val theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>
        get() {
            return _theme ?: AndroidTheme(strategy).also { _theme = it }
        }

    fun setCanvas(canvas: android.graphics.Canvas) {
        this.canvas = canvas
    }

    override fun childCanvas(offsetX: Double, offsetY: Double, scale: Double): IAndroidCanvas {
        return OffsetCanvas(offsetX, offsetY, scale)
    }

    override fun drawFilledCircle(x: Double, y: Double, radius: Double, fill: AndroidPen) {
        val paint = fill.paint
        val oldStyle = paint.style
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), paint)
        paint.style = oldStyle
    }

    override fun drawCircle(x: Double, y: Double, radius: Double, stroke: AndroidPen) {
        canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), stroke.paint)
    }

    override fun drawCircle(x: Double,
                            y: Double,
                            radius: Double,
                            stroke: AndroidPen?,
                            fill: AndroidPen?) {
        if (fill != null) {
            drawFilledCircle(x, y, radius, fill)
        }
        if (stroke != null) {
            drawCircle(x, y, radius, stroke)
        }
    }

    override fun drawFilledRoundRect(rect: Rectangle,
                                     rx: Double,
                                     ry: Double,
                                     fill: AndroidPen) {
        val paint = fill.paint
        val oldStyle = paint.style
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(toRectF(rect), rx.toFloat(), ry.toFloat(), fill.paint)
        paint.style = oldStyle
    }

    override fun drawRoundRect(rect: Rectangle,
                               rx: Double,
                               ry: Double,
                               stroke: AndroidPen) {
        canvas.drawRoundRect(toRectF(rect), rx.toFloat(), ry.toFloat(), stroke.paint)
    }

    override fun drawRoundRect(rect: Rectangle,
                               rx: Double,
                               ry: Double,
                               stroke: AndroidPen?,
                               fill: AndroidPen?) {
        if (fill != null) {
            drawFilledRoundRect(rect, rx, ry, fill)
        }
        if (stroke != null) {
            drawRoundRect(rect, rx, ry, stroke)
        }
    }

    override fun drawFilledRect(rect: Rectangle, fill: AndroidPen) {
        val paint = fill.paint
        val oldStyle = paint.style
        paint.style = Paint.Style.FILL
        canvas.drawRect(toRectF(rect), fill.paint)
        paint.style = oldStyle
    }

    override fun drawRect(rect: Rectangle, stroke: AndroidPen) {
        canvas.drawRect(toRectF(rect), stroke.paint)
    }

    override fun drawRect(rect: Rectangle,
                          stroke: AndroidPen?,
                          fill: AndroidPen?) {
        if (fill != null) {
            drawFilledRect(rect, fill)
        }
        if (stroke != null) {
            drawRect(rect, stroke)
        }
    }

    override fun drawPoly(points: DoubleArray,
                          stroke: AndroidPen?,
                          fill: AndroidPen?) {

        val androidPath = toPath(points)
        if (fill != null) {
            val fillPaint = fill.paint
            val oldStyle = fillPaint.style
            fillPaint.style = Paint.Style.FILL
            canvas.drawPath(androidPath, fill.paint)
            fillPaint.style = oldStyle
        }
        if (stroke != null) {
            canvas.drawPath(androidPath, stroke.paint)
        }
    }

    override fun drawPath(path: AndroidPath,
                          stroke: AndroidPen?,
                          fill: AndroidPen?) {
        if (fill != null) drawFilledPath(path.path, fill.paint)
        if (stroke != null) drawPath(path.path, stroke.paint)
    }

    internal fun drawPath(path: Path, paint: Paint) {
        canvas.drawPath(path, paint)
    }

    private fun drawFilledPath(path: Path, paint: Paint) {
        val oldStyle = paint.style
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        paint.style = oldStyle
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

    private fun toRectF(rect: Rectangle): RectF {
        return RectF(rect.leftf, rect.topf, rect.rightf, rect.bottomf)
    }


    override fun scale(scale: Double): IAndroidCanvas {
        return OffsetCanvas(scale)
    }

    override fun translate(dx: Double, dy: Double): IAndroidCanvas {
        return if (dx == 0.0 && dy == 0.0) {
            this
        } else OffsetCanvas(dx, dy, 1.0)
    }

    override fun drawBitmap(left: Double, top: Double, bitmap: Bitmap, pen: AndroidPen) {
        canvas.drawBitmap(bitmap, left.toFloat(), top.toFloat(), pen.paint)
    }

    override fun drawText(textPos: Canvas.TextPos,
                          left: Double,
                          baselineY: Double,
                          text: String,
                          foldWidth: Double,
                          pen: AndroidPen) {
        drawText(textPos, left, baselineY, text, foldWidth, pen, 1.0)
    }

    private fun drawText(textPos: Canvas.TextPos,
                         x: Double,
                         y: Double,
                         text: String,
                         foldWidth: Double,
                         pen: AndroidPen,
                         scale: Double) {
        val paint = pen.paint
        paint.style = Style.FILL
        val left = getLeft(textPos, x, text, foldWidth, pen, scale)
        val baseline = getBaseLine(textPos, y, pen, scale)
        canvas.drawText(text, left, baseline, paint)
        //Only for debug purposes
        //    mCanvas.drawCircle(left, baseline, 3f, mRedPaint);
        //    mCanvas.drawCircle((float)pX, (float)pY, 3f, mGreenPaint);
    }

    private fun getBaseLine(textPos: Canvas.TextPos,
                            y: Double,
                            pen: Pen<*>,
                            scale: Double): Float {
        when (textPos) {
            Canvas.TextPos.MAXTOPLEFT,
            Canvas.TextPos.MAXTOP,
            Canvas.TextPos.MAXTOPRIGHT  -> return (y + pen.textMaxAscent * scale).toFloat()

            Canvas.TextPos.ASCENTLEFT,
            Canvas.TextPos.ASCENT,
            Canvas.TextPos.ASCENTRIGHT  -> return (y + pen.textAscent * scale).toFloat()

            Canvas.TextPos.LEFT,
            Canvas.TextPos.MIDDLE,
            Canvas.TextPos.RIGHT        -> return (y + (0.5 * pen.textAscent - 0.5 * pen.textDescent) * scale).toFloat()

            Canvas.TextPos.BASELINEMIDDLE,
            Canvas.TextPos.BASELINERIGHT,
            Canvas.TextPos.BASELINELEFT -> return y.toFloat()

            Canvas.TextPos.BOTTOMLEFT,
            Canvas.TextPos.BOTTOMRIGHT,
            Canvas.TextPos.BOTTOM       -> return (y - pen.textMaxDescent * scale).toFloat()

            Canvas.TextPos.DESCENTLEFT,
            Canvas.TextPos.DESCENTRIGHT,
            Canvas.TextPos.DESCENT      -> return (y - pen.textDescent * scale).toFloat()
        }
    }

    private fun getLeft(textPos: Canvas.TextPos,
                        x: Double,
                        text: String,
                        foldWidth: Double,
                        pen: AndroidPen,
                        scale: Double): Float {
        when (textPos) {
            Canvas.TextPos.BASELINELEFT,
            Canvas.TextPos.BOTTOMLEFT,
            Canvas.TextPos.LEFT,
            Canvas.TextPos.MAXTOPLEFT,
            Canvas.TextPos.ASCENTLEFT  -> return x.toFloat()

            Canvas.TextPos.ASCENT,
            Canvas.TextPos.DESCENT,
            Canvas.TextPos.BASELINEMIDDLE,
            Canvas.TextPos.MAXTOP,
            Canvas.TextPos.MIDDLE,
            Canvas.TextPos.BOTTOM      -> return (x - pen.measureTextWidth(text, foldWidth) * scale / 2).toFloat()

            Canvas.TextPos.MAXTOPRIGHT,
            Canvas.TextPos.ASCENTRIGHT,
            Canvas.TextPos.DESCENTLEFT,
            Canvas.TextPos.DESCENTRIGHT,
            Canvas.TextPos.RIGHT,
            Canvas.TextPos.BASELINERIGHT,
            Canvas.TextPos.BOTTOMRIGHT -> return (x - pen.measureTextWidth(text, foldWidth) * scale).toFloat()
        }
    }


    private inner class OffsetCanvas(xOffset: Double, yOffset: Double, private val scale: Double) : IAndroidCanvas {

        /** The offset of the canvas. This is in scaled coordinates.  */
        private val xOffset: Double = -xOffset
        private val yOffset: Double = -yOffset

        override val strategy: AndroidStrategy
            get() = AndroidStrategy.INSTANCE


        override val theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>
            get() = this@AndroidCanvas.theme


        constructor(base: OffsetCanvas, offsetX: Double, offsetY: Double, scale: Double) :
            this((base.xOffset - offsetX) * scale,
                 (base.yOffset - offsetY) * scale,
                 base.scale * scale)

        constructor(base: OffsetCanvas, scale: Double) :
            this(base.xOffset * scale, base.yOffset * scale, base.scale * scale)

        constructor(scale: Double) : this(0.0, 0.0, scale)

        override fun scale(scale: Double): IAndroidCanvas {
            return OffsetCanvas(this, scale)
        }

        override fun childCanvas(offsetX: Double, offsetY: Double, scale: Double): IAndroidCanvas {
            return OffsetCanvas(this, offsetX, offsetY, scale)
        }


        override fun translate(dx: Double, dy: Double): IAndroidCanvas {
            return OffsetCanvas(xOffset - dx, yOffset - dy, scale)
        }

        @Contract("null -> null; !null -> !null")
        private fun scalePen(pen: AndroidPen?): AndroidPen? {
            return pen?.scale(scale)
        }

        override fun drawCircle(x: Double, y: Double, radius: Double, stroke: AndroidPen) {
            this@AndroidCanvas.drawCircle(transformX(x), transformY(y), radius * scale, scalePen(stroke)!!)
        }

        override fun drawFilledCircle(x: Double,
                                      y: Double,
                                      radius: Double,
                                      fill: AndroidPen) {
            this@AndroidCanvas.drawFilledCircle(transformX(x), transformY(y), radius * scale, fill)
        }

        override fun drawCircle(x: Double, y: Double, radius: Double, stroke: AndroidPen?,
                                fill: AndroidPen?) {
            if (fill != null) {
                this@AndroidCanvas.drawFilledCircle(transformX(x), transformY(y), radius * scale, fill)
            }
            if (stroke != null) {
                this@AndroidCanvas.drawCircle(transformX(x), transformY(y), radius * scale, scalePen(stroke)!!)
            }
        }

        override fun drawBitmap(left: Double,
                                top: Double,
                                bitmap: Bitmap,
                                pen: AndroidPen) {
            this@AndroidCanvas.drawBitmap(transformX(left), transformY(top), bitmap, scalePen(pen)!!)
        }

        override fun drawRect(rect: Rectangle, stroke: AndroidPen) {
            this@AndroidCanvas.drawRect(rect.offsetScaled(-xOffset, -yOffset, scale), scalePen(stroke)!!)
        }

        override fun drawFilledRect(rect: Rectangle, fill: AndroidPen) {
            this@AndroidCanvas.drawFilledRect(rect.offsetScaled(-xOffset, -yOffset, scale), scalePen(fill)!!)
        }

        override fun drawRect(rect: Rectangle,
                              stroke: AndroidPen?,
                              fill: AndroidPen?) {
            if (fill != null) {
                this@AndroidCanvas.drawFilledRect(rect.offsetScaled(-xOffset, -yOffset, scale), fill)
            }

            if (stroke != null) {
                this@AndroidCanvas.drawRect(rect.offsetScaled(-xOffset, -yOffset, scale), scalePen(stroke)!!)
            }
        }

        override fun drawPoly(points: DoubleArray,
                              stroke: AndroidPen?,
                              fill: AndroidPen?) {
            this@AndroidCanvas.drawPoly(transform(points), scalePen(stroke), fill)
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
            return (x - xOffset) * scale
        }

        fun transformY(y: Double): Double {
            return (y - yOffset) * scale
        }

        override fun drawPath(path: AndroidPath, stroke: AndroidPen?, fill: AndroidPen?) {
            val transformedPath = transformPath(path)
            if (fill != null) {
                this@AndroidCanvas.drawFilledPath(transformedPath, fill.paint)
            }
            if (stroke != null) {
                this@AndroidCanvas.drawPath(transformedPath, scalePen(stroke)!!.paint)
            }
        }

        private fun transformPath(path: AndroidPath): Path {
            val transformedPath = Path(path.path)
            val matrix = Matrix()
            matrix.setScale(scale.toFloat(), scale.toFloat())
            matrix.preTranslate((-xOffset).toFloat(), (-yOffset).toFloat())
            transformedPath.transform(matrix)
            return transformedPath
        }

        override fun drawRoundRect(rect: Rectangle,
                                   rx: Double,
                                   ry: Double,
                                   stroke: AndroidPen) {
            this@AndroidCanvas.drawRoundRect(rect.offsetScaled(-xOffset, -yOffset, scale), rx * scale, ry * scale,
                                             scalePen(
                                                 stroke)!!)
        }

        override fun drawFilledRoundRect(rect: Rectangle,
                                         rx: Double,
                                         ry: Double,
                                         fill: AndroidPen) {
            this@AndroidCanvas.drawFilledRoundRect(rect.offsetScaled(-xOffset, -yOffset, scale), rx * scale,
                                                   ry * scale, scalePen(
                fill)!!)
        }

        override fun drawRoundRect(rect: Rectangle,
                                   rx: Double,
                                   ry: Double,
                                   stroke: AndroidPen?,
                                   fill: AndroidPen?) {
            if (fill != null) {
                this@AndroidCanvas.drawFilledRoundRect(rect.offsetScaled(-xOffset, -yOffset, scale), rx * scale,
                                                       ry * scale, scalePen(fill)!!)
            }
            if (stroke != null) {
                this@AndroidCanvas.drawRoundRect(rect.offsetScaled(-xOffset, -yOffset, scale), rx * scale,
                                                 ry * scale, scalePen(stroke)!!)
            }
        }

        override fun drawText(textPos: Canvas.TextPos,
                              left: Double,
                              baselineY: Double,
                              text: String,
                              foldWidth: Double,
                              pen: AndroidPen) {
            this@AndroidCanvas.drawText(textPos, transformX(left), transformY(baselineY), text, foldWidth * scale,
                                        scalePen(pen)!!, scale)
        }
    }

}
