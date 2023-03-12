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

package nl.adaptivity.process.diagram

import nl.adaptivity.diagram.*
import nl.adaptivity.process.ProcessConsts.Endpoints
import nl.adaptivity.process.diagram.ProcessThemeItems.BACKGROUND
import nl.adaptivity.process.diagram.ProcessThemeItems.LINE
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYHEIGHT
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYROUNDX
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYROUNDY
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

interface IDrawableActivity : IDrawableProcessNode {
    override val leftExtent: Double
        get() = (ACTIVITYWIDTH + STROKEWIDTH) / 2
    override val rightExtent: Double
        get() = (ACTIVITYWIDTH + STROKEWIDTH) / 2
    override val topExtent: Double
        get() = (ACTIVITYHEIGHT + STROKEWIDTH) / 2
    override val bottomExtent: Double
        get() = (ACTIVITYHEIGHT + STROKEWIDTH) / 2


    override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>>
        draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?) {

        if (hasPos()) with(canvas) {
            val linePen = theme.getPen(LINE, state and Drawable.STATE_TOUCHED.inv())
            val bgPen = theme.getPen(BACKGROUND, state)

            if (state and Drawable.STATE_TOUCHED != 0) {
                val touchedPen = theme.getPen(LINE, Drawable.STATE_TOUCHED)
                drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, touchedPen)
            }

            drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, bgPen)
            drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, linePen)
        }
    }

    val name: String?

    /**
     * Get the label that would be drawn to the screen. This will set the pen to italics or not unless no label could be determined.
     * @param textPen The textPen to set to italics (or not).
     *
     * @param PEN_T The pen type for the canvas
     *
     * @return The actual label.
     */
    override fun <PEN_T : Pen<PEN_T>> getDrawnLabel(textPen: PEN_T): String? {
        return label ?: name?.apply { textPen.isTextItalics = false }
        ?: "<$id>".apply { textPen.isTextItalics = true }
    }

    companion object {
        val _bounds by lazy { Rectangle(STROKEWIDTH / 2, STROKEWIDTH / 2, ACTIVITYWIDTH, ACTIVITYHEIGHT) }
    }

    val message: IXmlMessage?

    val childId: String?

    val isBodySpecified get() = message != null

    val isUserTask: Boolean
        get() {
            val message = XmlMessage.from(message)
            return message != null && Endpoints.USER_TASK_SERVICE_DESCRIPTOR.isSameService(message.endpointDescriptor)
        }

    val isService get() = isBodySpecified && !isUserTask

    val isComposite get() = childId != null

}

open class DrawableActivity @JvmOverloads constructor(
    builder: MessageActivity.Builder,
    buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, *, *, *> = STUB_DRAWABLE_BUILD_HELPER,
    otherNodes: Iterable<ProcessNode.Builder> = emptyList()
) : MessageActivityBase(
    builder,
    buildHelper.newOwner,
    otherNodes
), DrawableProcessNode {

    class Builder constructor(
        id: String? = null,
        predecessor: Identifiable? = null,
        successor: Identifiable? = null,
        label: String? = null,
        x: Double = Double.NaN,
        y: Double = Double.NaN,
        defines: Collection<IXmlDefineType> = emptyList(),
        results: Collection<IXmlResultType> = emptyList(),
        override var childId: String? = null,
        override var message: IXmlMessage? = null,
        condition: Condition? = null,
        name: String? = null,
        override var state: DrawableState = Drawable.STATE_DEFAULT,
        isMultiInstance: Boolean = false,
        override var isCompat: Boolean = false,
        override var authRestrictions: AuthRestriction? = null
    ) : BaseBuilder(
        id, predecessor, successor, label, defines, results,
        condition, name, x, y, isMultiInstance
    ), MessageActivity.Builder, CompositeActivity.ReferenceBuilder,
        DrawableProcessNode.Builder<DrawableActivity>,
        IDrawableActivity {

        override val predecessors: Set<Identified>
            get() = predecessor?.identifier?.let { setOf(it) } ?: emptySet()

        override val successors: Set<Identified>
            get() = successor?.identifier?.let { setOf(it) } ?: emptySet()

        constructor() : this(id = null)

        @Suppress("PropertyName")
        override val _delegate: DrawableProcessNode.Builder.Delegate = DrawableProcessNode.Builder.Delegate(state, isCompat)

        constructor(node: Activity) : this(
            node.id,
            node.predecessor,
            node.successor,
            node.label,
            node.x,
            node.y,
            node.defines,
            node.results,
            (node as? CompositeActivity)?.childModel?.id,
            (node as? MessageActivity)?.message,
            node.condition,
            node.name,
            Drawable.STATE_DEFAULT,
            node.isMultiInstance,
            (node as? DrawableActivity)?.isCompat ?: false
        )

        override fun copy(): Builder {
            return Builder(
                id,
                predecessor?.identifier,
                successor,
                label,
                x,
                y,
                defines,
                results,
                message = XmlMessage.from(message),
                condition = condition,
                name = name,
                state = state,
                isMultiInstance = isMultiInstance,
                isCompat = isCompat
            )
        }

    }

    @Suppress("PropertyName")
    override val _delegate: DrawableProcessNode.Delegate = DrawableProcessNode.Delegate(builder)

    val isBodySpecified get() = message != null

    val isUserTask: Boolean
        get() {
            val message = XmlMessage.from(message)
            return message != null && Endpoints.USER_TASK_SERVICE_DESCRIPTOR.isSameService(message.endpointDescriptor)
        }

    val isService get() = isBodySpecified && !isUserTask

    val isComposite get() = false//this.childModel != null

    override val condition: Condition? = builder.condition

    override val maxSuccessorCount: Int
        get() = if (isCompat) Int.MAX_VALUE else 1

    override val maxPredecessorCount: Int get() = 1

    override fun builder(): Builder {
        return Builder(this)
    }

    override val idBase: String
        get() = IDBASE

    companion object {
        const val IDBASE = "ac"

        @Deprecated(
            "Use the builder", ReplaceWith(
                "Builder(elem).apply { isCompat = compat }.build()",
                "nl.adaptivity.process.diagram.DrawableActivity.Builder"
            )
        )
        @JvmStatic
        fun from(elem: Activity, compat: Boolean): DrawableActivity {
            val builder: Builder = Builder(elem).apply { isCompat = compat }
            return builder.build()
        }
    }

}
