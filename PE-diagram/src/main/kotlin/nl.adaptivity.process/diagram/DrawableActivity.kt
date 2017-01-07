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
import nl.adaptivity.process.ProcessConsts.Endpoints
import nl.adaptivity.process.clientProcessModel.ClientActivityNode
import nl.adaptivity.process.diagram.ProcessThemeItems.*
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYHEIGHT
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYROUNDX
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYROUNDY
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.copyProcessNodeAttrs
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper


class DrawableActivity : ClientActivityNode<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode {

    class Builder : ClientActivityNode.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode.Builder {

        @Deprecated("Compat should no longer be used.")
        constructor(compat: Boolean) : super(compat) {}

        constructor(id: String? = null,
                    predecessor: Identified? = null,
                    successor: Identified? = null,
                    label: String? = null,
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    message: XmlMessage? = null,
                    condition: String? = null,
                    name: String? = null,
                    compat: Boolean = false) : super(id, predecessor, successor, label, x, y, defines, results, message, condition, name, compat)

        constructor(node: Activity<*, *>) : super(node) {}

        override fun build(newOwner: DrawableProcessModel?): DrawableActivity {
            return DrawableActivity(this, newOwner)
        }

    }

    private var mState = Drawable.STATE_DEFAULT

    @JvmOverloads constructor(owner: DrawableProcessModel, compat: Boolean = false) : super(owner, compat) {}

    constructor(owner: DrawableProcessModel, id: String, compat: Boolean) : super(owner, id, compat) {}

    constructor(orig: Activity<*, *>, compat: Boolean) : super(orig, null, compat) {
        if (orig is DrawableActivity) {
            mState = orig.mState
        }
    }

    constructor(builder: Activity.Builder<*, *>, newOwnerModel: DrawableProcessModel?) : super(builder, newOwnerModel) {}

    override fun builder(): Builder {
        return Builder(this)
    }

    override fun clone(): DrawableActivity {
        if (javaClass == DrawableActivity::class.java) {
            return DrawableActivity(this, isCompat)
        }
        throw RuntimeException(CloneNotSupportedException())
    }

    override fun getBounds(): Rectangle {

        return Rectangle(x - REFERENCE_OFFSET_X, y - REFERENCE_OFFSET_Y, ACTIVITYWIDTH + STROKEWIDTH, ACTIVITYHEIGHT + STROKEWIDTH)
    }

    override fun translate(dX: Double, dY: Double) {
        x = x + dX
        y = y + dY
    }

    override fun setPos(left: Double, top: Double) {
        x = left + REFERENCE_OFFSET_X
        y = left + REFERENCE_OFFSET_Y
    }

    override fun isWithinBounds(x: Double, y: Double): Boolean {
        val hwidth = (ACTIVITYWIDTH + STROKEWIDTH) / 2
        val hheight = (ACTIVITYHEIGHT + STROKEWIDTH) / 2
        return Math.abs(x - getX()) <= hwidth && Math.abs(y - getY()) <= hheight
    }

    override fun getItemAt(x: Double, y: Double): Drawable? {
        return if (isWithinBounds(x, y)) this else null
    }

    override fun getState(): Int {
        return mState
    }

    override fun setState(state: Int) {
        if (state == mState) {
            return
        }
        mState = state
        ownerModel?.notifyNodeChanged(this)
    }

    override val idBase: String
        get() = IDBASE

    override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle) {
        if (hasPos()) {
            val linePen = canvas.theme.getPen(LINE, mState and Drawable.STATE_TOUCHED.inv())
            val bgPen = canvas.theme.getPen(BACKGROUND, mState)

            if (_bounds == null) {
                _bounds = Rectangle(STROKEWIDTH / 2, STROKEWIDTH / 2, ACTIVITYWIDTH, ACTIVITYHEIGHT)
            }

            if (mState and Drawable.STATE_TOUCHED != 0) {
                val touchedPen = canvas.theme.getPen(LINE, Drawable.STATE_TOUCHED)
                canvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, touchedPen)
            }
            canvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, bgPen)
            canvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, linePen)
        }
    }

    override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> drawLabel(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?, left: Double, top: Double) {
        if (hasPos()) {
            val textPen = canvas.theme.getPen(DIAGRAMLABEL, mState)
            val label = getDrawnLabel(textPen)
            if (!label.isNullOrBlank()) {
                val topCenter = ACTIVITYHEIGHT + STROKEWIDTH + textPen.textLeading / 2
                canvas.drawText(TextPos.ASCENT, REFERENCE_OFFSET_X, topCenter, label, java.lang.Double.MAX_VALUE, textPen)
            }
        }
    }

    /**
     * Get the label that would be drawn to the screen. This will set the pen to italics or not unless no label could be determined.
     * @param textPen The textPen to set to italics (or not).
     * *
     * @param <PEN_T>
     * *
     * @return The actual label.
    </PEN_T> */
    private fun <PEN_T : Pen<PEN_T>> getDrawnLabel(textPen: PEN_T): String? {
        return label ?: name?.apply { textPen.isTextItalics = false } 
                ?: "<$id>".apply { textPen.isTextItalics = true }
    }

    val isBodySpecified: Boolean
        get() = message != null

    val isUserTask: Boolean
        get() {
            val message = XmlMessage.get(message)
            return message != null && Endpoints.USER_TASK_SERVICE_DESCRIPTOR.isSameService(message.endpointDescriptor)
        }

    val isService: Boolean
        get() = isBodySpecified && !isUserTask

    companion object {

        private const val REFERENCE_OFFSET_X = (ACTIVITYWIDTH + STROKEWIDTH) / 2
        private const val REFERENCE_OFFSET_Y = (ACTIVITYHEIGHT + STROKEWIDTH) / 2
        const val IDBASE = "ac"
        private var _bounds: Rectangle? = null

        @JvmStatic
        @Deprecated("")
        @Throws(XmlException::class)
        fun deserialize(ownerModel: DrawableProcessModel, reader: XmlReader): DrawableActivity {
            return DrawableActivity.Builder(true).deserializeHelper(reader).build(ownerModel)
        }

        @JvmStatic
        @Throws(XmlException::class)
        fun deserialize(reader: XmlReader): Builder {
            return DrawableActivity.Builder(true).deserializeHelper(reader)
        }

        @JvmStatic
        fun from(elem: Activity<*, *>, compat: Boolean): DrawableActivity {
            val result = DrawableActivity(elem, compat)
            copyProcessNodeAttrs(elem, result)
            result.name = elem.name
            result.setLabel(elem.label)
            result.condition = elem.condition
            result.setDefines(elem.defines)
            result.setResults(elem.results)
            result.message = elem.message
            return result
        }
    }

}
