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
import nl.adaptivity.process.diagram.ProcessThemeItems.ENDNODEOUTERLINE
import nl.adaptivity.process.diagram.ProcessThemeItems.LINE
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ENDNODEOUTERRADIUS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STARTNODERADIUS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified

interface IDrawableEventNode : IDrawableProcessNode {
    override val leftExtent get() = REFERENCE_OFFSET_X
    override val rightExtent get() = ENDNODEOUTERRADIUS * 2 + STROKEWIDTH - REFERENCE_OFFSET_X
    override val topExtent get() = REFERENCE_OFFSET_Y
    override val bottomExtent get() = ENDNODEOUTERRADIUS * 2 + STROKEWIDTH - REFERENCE_OFFSET_Y

    override val maxPredecessorCount: Int get() = 0

    override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>,
                                                                                                                clipBounds: Rectangle?) {
        if (hasPos()) {
            with (canvas) {
                val outerLinePen = theme.getPen(ENDNODEOUTERLINE, state and Drawable.STATE_TOUCHED.inv())
                val innerLinePen = outerLinePen


                val hsw = RootDrawableProcessModel.ENDNODEOUTERSTROKEWIDTH / 2

                if (state and Drawable.STATE_TOUCHED != 0) {
                    val touchedPen = theme.getPen(LINE, Drawable.STATE_TOUCHED)
                    drawCircle(ENDNODEOUTERRADIUS + hsw,
                        ENDNODEOUTERRADIUS + hsw,
                        ENDNODEOUTERRADIUS, touchedPen)
                    drawCircle(ENDNODEOUTERRADIUS + hsw,
                        ENDNODEOUTERRADIUS + hsw,
                        STARTNODERADIUS, touchedPen)
                }
                drawCircle(ENDNODEOUTERRADIUS + hsw,
                    ENDNODEOUTERRADIUS + hsw, ENDNODEOUTERRADIUS,
                    outerLinePen)
                drawCircle(ENDNODEOUTERRADIUS + hsw,
                    ENDNODEOUTERRADIUS + hsw,
                    STARTNODERADIUS, innerLinePen)
            }
        }
    }

    companion object {
        const val REFERENCE_OFFSET_X = ENDNODEOUTERRADIUS + STROKEWIDTH / 2
        const val REFERENCE_OFFSET_Y = ENDNODEOUTERRADIUS + STROKEWIDTH / 2
    }

}

class DrawableEventNode(builder: EventNode.Builder,
                        buildHelper: ProcessModel.BuildHelper<*,*,*,*>) :
    EventNodeBase<DrawableProcessNode, DrawableProcessModel?>(builder, buildHelper),
    DrawableProcessNode {

    class Builder : EventNodeBase.Builder, DrawableProcessNode.Builder<DrawableEventNode>, IDrawableEventNode {

        override val _delegate: DrawableProcessNode.Builder.Delegate


        override val predecessors: Set<Identified>
            get() = setOfNotNull(predecessor?.identifier)

        override val successors: Set<Identified>
            get() = setOfNotNull(successor?.identifier)

        constructor() : this(id = null)

        constructor(id: String? = null,
                    predecessor: Identifiable? = null,
                    successor: Identified? = null,
                    label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    state: DrawableState = Drawable.STATE_DEFAULT,
                    isCompat: Boolean = false,
                    isMultiInstance: Boolean = false) : super(id, predecessor, successor, label, defines, results, x, y,
                                                              isMultiInstance) {
            _delegate = DrawableProcessNode.Builder.Delegate(state, isCompat)
        }

        constructor(node: EventNode) : super(node) {
            _delegate = DrawableProcessNode.Builder.Delegate(node)
        }

        override fun copy(): Builder {
            return Builder(id, predecessor, successor?.identifier, label, defines, results, x, y, state, isCompat, isMultiInstance)
        }

    }

    override val _delegate = DrawableProcessNode.Delegate(builder)

    override val idBase: String
        get() = IDBASE

    override val maxSuccessorCount: Int
        get() = if (isCompat) Int.MAX_VALUE else 1

    override val maxPredecessorCount get() = super.maxPredecessorCount

    override fun builder(): Builder {
        return Builder(this)
    }

    companion object {

        const val REFERENCE_OFFSET_X = STARTNODERADIUS + STROKEWIDTH / 2
        const val REFERENCE_OFFSET_Y = STARTNODERADIUS + STROKEWIDTH / 2
        const val IDBASE = "event"
    }

}
