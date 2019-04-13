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
import nl.adaptivity.process.diagram.DrawableJoinSplit.Companion.ARROWCONTROLRATIO
import nl.adaptivity.process.diagram.DrawableJoinSplit.Companion.CENTER_X
import nl.adaptivity.process.diagram.DrawableJoinSplit.Companion.CENTER_Y
import nl.adaptivity.process.diagram.DrawableJoinSplit.Companion.CURVED_ARROWS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.JOINHEIGHT
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.JOINWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified
import kotlin.jvm.JvmStatic
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

interface IDrawableJoin : IDrawableJoinSplit {

    override val maxPredecessorCount: Int get() = Int.MAX_VALUE

    override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> drawDecoration(
        canvas: Canvas<S, PEN_T, PATH_T>,
        clipBounds: Rectangle?) {
        if (hasPos()) {
            val path = itemCache.getPath(canvas.strategy, 1) {
                if (CURVED_ARROWS) {
                    moveTo(CENTER_X + INLEN, CENTER_Y)
                    lineTo(CENTER_X + ARROWHEADD_X - ARROWHEAD_ADJUST, CENTER_Y)
                    moveTo(CENTER_X + ARROWHEADD_X - ARROWDNEAR, CENTER_Y - ARROWDFAR)
                    lineTo(CENTER_X + ARROWHEADD_X, CENTER_Y)
                    lineTo(CENTER_X + ARROWHEADD_X - ARROWDNEAR, CENTER_Y + ARROWDFAR)
                    moveTo(CENTER_X - IND_X, CENTER_Y - IND_Y)
                    cubicTo(CENTER_X - IND_X * (1 - ARROWCONTROLRATIO), CENTER_Y - IND_Y * (1 - ARROWCONTROLRATIO),
                            CENTER_X + INLEN * (1 - ARROWCONTROLRATIO), CENTER_Y, CENTER_X + INLEN, CENTER_Y)
                    cubicTo(CENTER_X + INLEN * (1 - ARROWCONTROLRATIO), CENTER_Y,
                            CENTER_X - IND_X * (1 - ARROWCONTROLRATIO), CENTER_Y + IND_Y * (1 - ARROWCONTROLRATIO),
                            CENTER_X - IND_X, CENTER_Y + IND_Y)
                    moveTo(CENTER_X + INLEN, CENTER_Y)
                    lineTo(CENTER_X + ARROWHEADD_X, CENTER_Y)
                } else {
                    moveTo(CENTER_X, CENTER_Y)
                    lineTo(CENTER_X + ARROWHEADD_X - ARROWHEAD_ADJUST, CENTER_X)
                    moveTo(CENTER_X + ARROWHEADD_X - ARROWDNEAR, CENTER_Y - ARROWDFAR)
                    lineTo(CENTER_X + ARROWHEADD_X, CENTER_Y)
                    lineTo(CENTER_X + ARROWHEADD_X - ARROWDNEAR, CENTER_Y + ARROWDFAR)
                    moveTo(CENTER_X - IND_X, CENTER_Y - IND_Y)
                    lineTo(CENTER_X, CENTER_Y)
                    lineTo(CENTER_X - IND_X, CENTER_Y + IND_Y)
                }
            }

            val linePen = canvas.theme.getPen(ProcessThemeItems.INNERLINE, state)
            canvas.drawPath(path, linePen, null)
        }
    }

    companion object {
        private const val ARROWHEADD_X = JOINWIDTH * 0.375
        private val ARROWHEAD_ADJUST = 0.5 * STROKEWIDTH / sin(DrawableJoinSplit.ARROWHEADANGLE)

        /** The y coordinate if the line were horizontal.  */
        private val ARROWDFAR = DrawableJoinSplit.ARROWLEN * sin(DrawableJoinSplit.ARROWHEADANGLE)
        /** The x coordinate if the line were horizontal.  */
        private val ARROWDNEAR = DrawableJoinSplit.ARROWLEN * cos(DrawableJoinSplit.ARROWHEADANGLE)
        private const val IND_X = JOINWIDTH * 0.2
        private const val IND_Y = JOINHEIGHT * 0.2
        private val INLEN = sqrt(IND_X * IND_X + IND_Y * IND_Y)
    }

}

class DrawableJoin(builder: Join.Builder,
                   buildHelper: ProcessModel.BuildHelper<*, *, *, *>) : JoinBase<DrawableProcessNode, DrawableProcessModel?>(
    builder, buildHelper), Join, DrawableJoinSplit {

    class Builder : JoinBase.Builder, DrawableJoinSplit.Builder<DrawableJoin>, IDrawableJoin {

        override val _delegate: DrawableProcessNode.Builder.Delegate

        constructor() : this(id = null)

        constructor(id: String? = null,
                    predecessors: Collection<Identified> = emptyList(),
                    successor: Identified? = null,
                    label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    min: Int = 1,
                    max: Int = -1,
                    state: DrawableState = Drawable.STATE_DEFAULT,
                    isMultiMerge: Boolean = false,
                    isMultiInstance: Boolean = false,
                    isCompat: Boolean = false) : super(id, predecessors, successor, label, defines, results,
                                                       x, y, min, max, isMultiMerge, isMultiInstance) {
            _delegate = DrawableProcessNode.Builder.Delegate(state, isCompat)
        }

        constructor(node: Join) : super(node) {
            _delegate = DrawableProcessNode.Builder.Delegate(node)
        }

        override val itemCache = ItemCache()

        override fun copy(): Builder =
            Builder(id, predecessors, successor?.identifier, label, defines, results, x, y, min, max, state,
                    isMultiMerge, isMultiInstance, isCompat)
    }

    override val _delegate: DrawableJoinSplit.Delegate

    override val maxSuccessorCount: Int
        get() = if (isCompat) Int.MAX_VALUE else 1

    override val maxPredecessorCount get() = super<JoinBase>.maxPredecessorCount

    override fun builder(): Builder {
        return Builder(this)
    }

    companion object {

        private const val ARROWHEADD_X = JOINWIDTH * 0.375
        private val ARROWHEAD_ADJUST = 0.5 * STROKEWIDTH / sin(DrawableJoinSplit.ARROWHEADANGLE)

        /** The y coordinate if the line were horizontal.  */
        private val ARROWDFAR = DrawableJoinSplit.ARROWLEN * sin(DrawableJoinSplit.ARROWHEADANGLE)
        /** The x coordinate if the line were horizontal.  */
        private val ARROWDNEAR = DrawableJoinSplit.ARROWLEN * cos(DrawableJoinSplit.ARROWHEADANGLE)
        private const val IND_X = JOINWIDTH * 0.2
        private const val IND_Y = JOINHEIGHT * 0.2
        private val INLEN = sqrt(IND_X * IND_X + IND_Y * IND_Y)

        @Deprecated("Use the builder",
                    ReplaceWith("Builder(elem).build()", "nl.adaptivity.process.diagram.DrawableJoin.Builder"))
        @JvmStatic
        fun from(elem: Join, compat: Boolean): DrawableJoin {
            return Builder(elem).build()
        }

    }

    init {
        _delegate = DrawableJoinSplit.Delegate(builder)
    }
}
