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
import nl.adaptivity.process.diagram.ProcessThemeItems.*
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYHEIGHT
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYROUNDX
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYROUNDY
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.JvmOverloads
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.util.multiplatform.isTypeOf
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.writeSimpleElement

interface IDrawableActivity: IDrawableProcessNode {
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
        drawRoundRect(DrawableActivity._bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, touchedPen)
      }

      drawFilledRoundRect(DrawableActivity._bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, bgPen)
      drawRoundRect(DrawableActivity._bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, linePen)
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

    private const val REFERENCE_OFFSET_X = (ACTIVITYWIDTH + STROKEWIDTH) / 2
    private const val REFERENCE_OFFSET_Y = (ACTIVITYHEIGHT + STROKEWIDTH) / 2

  }

}

open class DrawableActivity @JvmOverloads constructor(builder: Activity.Builder<*, *>,
                                                          buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?> = STUB_DRAWABLE_BUILD_HELPER) :
  ActivityBase<DrawableProcessNode, DrawableProcessModel?>(builder, buildHelper), DrawableProcessNode, IDrawableActivity {

  final class Builder : ActivityBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode.Builder, IDrawableActivity {

    constructor() : this(id=null)

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
                state: DrawableState = Drawable.STATE_DEFAULT,
                multiInstance: Boolean = false,
                isCompat: Boolean = false) : super(id, predecessor, successor, label, defines, results, message,
                                                   condition, name, x, y, multiInstance) {
      _delegate = DrawableProcessNode.Builder.Delegate(state = state, isCompat = isCompat)
    }

    override val _delegate: DrawableProcessNode.Builder.Delegate

    constructor(node: Activity<*, *>) : super(node) {
      _delegate = DrawableProcessNode.Builder.Delegate(node)
    }

    override fun copy(): Builder {
      return Builder(id, predecessor?.identifier, successor, label, x, y, defines, results, XmlMessage.get(message), condition, name, state, isMultiInstance, isCompat)
    }

    override fun build(buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>): DrawableActivity {
      return DrawableActivity(this, buildHelper)
    }

  }

  override fun copy(): IDrawableProcessNode {
    if (isTypeOf<DrawableActivity>(this)) throw UnsupportedOperationException("Copy must be overridden at the leaf")
    return builder().build(STUB_DRAWABLE_BUILD_HELPER)
  }

  override val _delegate: DrawableProcessNode.Delegate = DrawableProcessNode.Delegate(builder)

  val isBodySpecified get() = message != null

  val isUserTask: Boolean
    get() {
      val message = XmlMessage.get(message)
      return message != null && Endpoints.USER_TASK_SERVICE_DESCRIPTOR.isSameService(message.endpointDescriptor)
    }

  val isService get() = isBodySpecified && !isUserTask

  val isComposite get() = this.childModel!=null

  override var condition: String? = null

  override val maxSuccessorCount: Int
    get() = if (isCompat) Int.MAX_VALUE else 1

  override val maxPredecessorCount: Int get() = 1

  override fun builder(): Builder {
    return Builder(this)
  }

  override val idBase: String
    get() = IDBASE

  override fun serializeCondition(out: XmlWriter) {
    if (!condition.isNullOrEmpty()) {
      out.writeSimpleElement(Condition.ELEMENTNAME, condition)
    }
  }

  @Deprecated("Use the builder")
  override fun setId(id: String) = super.setId(id)

  @Deprecated("Use the builder")
  override fun setLabel(label: String?) = super.setLabel(label)

  @Deprecated("Use the builder")
  override fun setOwnerModel(newOwnerModel: DrawableProcessModel?) = super.setOwnerModel(newOwnerModel)

  @Deprecated("Use the builder")
  override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)

  @Deprecated("Use the builder")
  override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)

  @Deprecated("Use the builder")
  override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)

  @Deprecated("Use the builder")
  override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)

  @Deprecated("Use the builder")
  override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)

  @Deprecated("Use the builder")
  override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

  companion object {
    const val IDBASE = "ac"
    val _bounds by lazy { Rectangle(STROKEWIDTH / 2, STROKEWIDTH / 2, ACTIVITYWIDTH, ACTIVITYHEIGHT) }

    @Deprecated("Use the builder")
    @JvmStatic
    fun from(elem: Activity<*, *>, compat: Boolean): DrawableActivity {
      return Builder(elem).apply { isCompat = compat }.build(STUB_DRAWABLE_BUILD_HELPER)
    }
  }

}
