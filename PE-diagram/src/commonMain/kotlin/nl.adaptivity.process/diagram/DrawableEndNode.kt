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
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ENDNODEINNERRRADIUS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ENDNODEOUTERRADIUS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ENDNODEOUTERSTROKEWIDTH
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.JvmDefault
import nl.adaptivity.util.multiplatform.isTypeOf
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.deserializeHelper
import kotlin.math.abs

interface IDrawableEndNode : IDrawableProcessNode {
    override val leftExtent: Double
        get() = ENDNODEOUTERRADIUS + ENDNODEOUTERSTROKEWIDTH / 2
    override val rightExtent: Double
        get() = ENDNODEOUTERRADIUS + ENDNODEOUTERSTROKEWIDTH / 2
    override val topExtent: Double
        get() = ENDNODEOUTERRADIUS + ENDNODEOUTERSTROKEWIDTH / 2
    override val bottomExtent: Double
        get() = ENDNODEOUTERRADIUS + ENDNODEOUTERSTROKEWIDTH / 2
    override val maxSuccessorCount: Int get() = 0

    override fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
        PEN_T : Pen<PEN_T>,
        PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?) {

        if (hasPos()) with(canvas) {
            val outerLinePen = theme.getPen(ENDNODEOUTERLINE,
                                            state and Drawable.STATE_TOUCHED.inv())
            val innerPen = theme.getPen(ProcessThemeItems.LINEBG, state and Drawable.STATE_TOUCHED.inv())

            val hsw = ENDNODEOUTERSTROKEWIDTH / 2

            if (state and Drawable.STATE_TOUCHED != 0) {
                val touchedPen = theme.getPen(ProcessThemeItems.LINE, Drawable.STATE_TOUCHED)
                drawCircle(ENDNODEOUTERRADIUS + hsw,
                           ENDNODEOUTERRADIUS + hsw,
                           ENDNODEOUTERRADIUS, touchedPen)
                drawCircle(ENDNODEOUTERRADIUS + hsw,
                           ENDNODEOUTERRADIUS + hsw,
                           ENDNODEINNERRRADIUS, touchedPen)
            }
            drawCircle(ENDNODEOUTERRADIUS + hsw,
                       ENDNODEOUTERRADIUS + hsw, ENDNODEOUTERRADIUS,
                       outerLinePen)
            drawFilledCircle(ENDNODEOUTERRADIUS + hsw,
                             ENDNODEOUTERRADIUS + hsw,
                             ENDNODEINNERRRADIUS, innerPen)
        }
    }

    override fun isWithinBounds(x: Double, y: Double): Boolean {
        val realradius = ENDNODEOUTERRADIUS + ENDNODEOUTERSTROKEWIDTH / 2
        val dx = abs(this.x - x)
        val dy = abs(this.y - y)
        return dx * dx + dy * dy <= realradius * realradius
    }

}

class DrawableEndNode : EndNodeBase, DrawableProcessNode {

    class Builder : EndNodeBase.Builder, DrawableProcessNode.Builder<DrawableEndNode>, IDrawableEndNode {

        override val _delegate: DrawableProcessNode.Builder.Delegate

        override var isCompat: kotlin.Boolean
            get() = false
            set(compat) {
                if (compat) throw IllegalArgumentException("Compatibility not supported on end nodes.")
            }

        constructor() : this(id = null)

        constructor(id: String? = null,
                    predecessor: Identified? = null,
                    label: String? = null,
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    isMultiInstance: Boolean = false,
                    state: Int = Drawable.STATE_DEFAULT) : super(id, predecessor, label, defines, results, x, y,
                                                                 isMultiInstance) {
            _delegate = DrawableProcessNode.Builder.Delegate(state, false)
        }

        constructor(node: EndNode) : super(node) {
            _delegate = DrawableProcessNode.Builder.Delegate(node)
        }

        override fun copy() =
            Builder(id, predecessor?.identifier, label, x, y, defines, results, isMultiInstance, state)

    }

    override val _delegate: DrawableProcessNode.Delegate

    override val idBase: String get() = IDBASE
    override val isCompat: Boolean
        get() = false

    override val maxPredecessorCount get() = super<EndNodeBase>.maxPredecessorCount
    override val maxSuccessorCount get() = super<EndNodeBase>.maxSuccessorCount

    @Deprecated("Use the builder", ReplaceWith("this(Builder(orig))"))
    constructor(orig: EndNode) : this(Builder(orig), STUB_DRAWABLE_BUILD_HELPER.newOwner, emptyList())

    constructor(builder: EndNode.Builder, newOwner: ProcessModel<*>, otherNodes: Iterable<ProcessNode.Builder> )
        : super(builder, newOwner, otherNodes) {
        _delegate = DrawableProcessNode.Delegate(builder)
    }

    override fun builder(): Builder {
        return Builder(this)
    }

    companion object {

        private const val REFERENCE_OFFSET_X = ENDNODEOUTERRADIUS
        private const val REFERENCE_OFFSET_Y = ENDNODEOUTERRADIUS
        const val IDBASE = "end"

        @kotlin.jvm.JvmStatic
        fun deserialize(reader: XmlReader): DrawableEndNode.Builder {
            return DrawableEndNode.Builder(state = Drawable.STATE_DEFAULT).deserializeHelper(reader)
        }

        @Deprecated("Use the builder",
                    ReplaceWith("Builder(elem).build()", "nl.adaptivity.process.diagram.DrawableEndNode.Builder"))
        fun from(elem: EndNode): DrawableEndNode = Builder(elem).build()
    }

}
