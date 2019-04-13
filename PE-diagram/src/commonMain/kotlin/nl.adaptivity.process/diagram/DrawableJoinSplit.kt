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

package nl.adaptivity.process.diagram


import nl.adaptivity.diagram.*
import nl.adaptivity.diagram.Canvas.TextPos
import nl.adaptivity.diagram.Drawable.Companion.STATE_TOUCHED
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.JOINHEIGHT
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.JOINWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.processModel.JoinSplit
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.util.multiplatform.JvmDefault
import kotlin.math.PI
import kotlin.math.abs

interface IDrawableJoinSplit : IDrawableProcessNode {
    override val leftExtent: Double
        get() = (JOINWIDTH + DrawableJoinSplit.STROKEEXTEND) / 2
    override val rightExtent: Double
        get() = (JOINWIDTH + DrawableJoinSplit.STROKEEXTEND) / 2
    override val topExtent: Double
        get() = (JOINWIDTH + DrawableJoinSplit.STROKEEXTEND) / 2
    override val bottomExtent: Double
        get() = (JOINWIDTH + DrawableJoinSplit.STROKEEXTEND) / 2

    val itemCache: ItemCache
    val min: Int
    val max: Int

    val maxSiblings: Int get() = if (this is IDrawableSplit) successors.size else predecessors.size

    val minMaxText: String
        get() {
            if (DrawableJoinSplit.TEXT_DESC) when {
                isXor() -> return "xor"
                isOr()  -> return "or"
                isAnd() -> return "and"
            }

            if (min >= 0 && min == max) {
                return min.toString()
            }

            return buildString {
                append(if (min < 0) "?" else min.toString())
                append("...")
                append(if (max < 0) "?" else max.toString())
            }
        }

    override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>,
                                                                                                                clipBounds: Rectangle?) {
        if (hasPos()) {
            val dx = JOINWIDTH / 2
            val hse = DrawableJoinSplit.STROKEEXTEND / 2
            val path = itemCache.getPath(canvas.strategy, 0) {
                val dy = JOINHEIGHT / 2

                moveTo(hse, dy + hse)
                lineTo(dx + hse, hse)
                lineTo(JOINWIDTH + hse, dy + hse)
                lineTo(dx + hse, JOINHEIGHT + hse)
                close()
            }

            val linePen = canvas.theme.getPen(ProcessThemeItems.LINE, state and STATE_TOUCHED.inv())
            val bgPen = canvas.theme.getPen(ProcessThemeItems.BACKGROUND, state)

            if (state and STATE_TOUCHED != 0) {
                val touchedPen = canvas.theme.getPen(ProcessThemeItems.LINE, STATE_TOUCHED)
                canvas.drawPath(path, touchedPen, null)
            }
            canvas.drawPath(path, linePen, bgPen)

            drawDecoration(canvas, clipBounds)

            if (this.min >= 0 || this.max > 0) {
                val textPen = canvas.theme.getPen(ProcessThemeItems.DIAGRAMTEXT, state)
                val s = this.minMaxText

                canvas.drawText(TextPos.DESCENT, hse + dx, -hse, s, Double.MAX_VALUE, textPen)
            }
        }
    }

    fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> drawDecoration(canvas: Canvas<S, PEN_T, PATH_T>,
                                                                                                                 clipBounds: Rectangle?)

    @JvmDefault
    override fun isWithinBounds(x: Double, y: Double): Boolean {
        val realradiusX = (JOINWIDTH + DrawableJoinSplit.STROKEEXTEND) / 2
        val realradiusY = (JOINHEIGHT + DrawableJoinSplit.STROKEEXTEND) / 2

        // Scale the horizontal disposition to the expected radius. The values will be absolute so always positive
        val dx = abs(x - this.x) / realradiusX
        val dy = abs(y - this.y) / realradiusY

        // Diamond means that the combined deviation has to be 1 if we correct for radius
        return dx + dy <= 1.0
    }

    /** Determine whether the node represents an or split.  */
    @JvmDefault
    fun isOr(): Boolean = this.min == 1 && this.max >= maxSiblings

    /** Determine whether the node represents an xor split.  */
    @JvmDefault
    fun isXor(): Boolean = this.min == 1 && this.max == 1

    /** Determine whether the node represents an and split.  */
    @JvmDefault
    fun isAnd(): Boolean = this.min == this.max && this.min >= maxSiblings

}

interface DrawableJoinSplit : JoinSplit, DrawableProcessNode {

    class Delegate(builder: ProcessNode.IBuilder) : DrawableProcessNode.Delegate(builder) {

        val itemCache = ItemCache()

    }

    interface Builder<R : DrawableJoinSplit> : DrawableProcessNode.Builder<R>, JoinSplit.Builder, IDrawableJoinSplit

    override fun builder(): Builder<out DrawableJoinSplit>

    override val _delegate: Delegate

    companion object {

        const val CURVED_ARROWS = true
        const val TEXT_DESC = true

        const val SQRT2 = 1.4142135623730951
        const val STROKEEXTEND = SQRT2 * STROKEWIDTH
        const val REFERENCE_OFFSET_X = (JOINWIDTH + STROKEEXTEND) / 2
        const val REFERENCE_OFFSET_Y = (JOINHEIGHT + STROKEEXTEND) / 2
        const val HORIZONTALDECORATIONLEN = JOINWIDTH * 0.4
        const val CENTER_X = (JOINWIDTH + STROKEEXTEND) / 2
        const val CENTER_Y = (JOINHEIGHT + STROKEEXTEND) / 2
        const val ARROWHEADANGLE = 35 * PI / 180
        const val ARROWLEN = JOINWIDTH * 0.15
        const val ARROWCONTROLRATIO = 0.85
    }

}
