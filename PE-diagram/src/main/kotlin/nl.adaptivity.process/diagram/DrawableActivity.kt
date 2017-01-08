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
import nl.adaptivity.process.diagram.ProcessThemeItems.*
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYHEIGHT
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYROUNDX
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYROUNDY
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ACTIVITYWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.copyProcessNodeAttrs
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.*


open class DrawableActivity : DrawableActivity, DrawableProcessNode {

  class Builder : ActivityBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode.Builder {

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
                stateP: DrawableState = Drawable.STATE_DEFAULT,
                compat: Boolean = false) : super(id, predecessor, successor, label, defines, results, message,
                                                        condition, name, x, y) {
      this.state = stateP
      this.isCompat = compat
    }

    override var state: DrawableState
    override var isCompat = false

    constructor(node: Activity<*, *>) : super(node) {
      state = (node as? Drawable)?.state ?: Drawable.STATE_DEFAULT
      isCompat = (node as? DrawableProcessNode)?.isCompat ?: false
    }

    override fun build(newOwner: DrawableProcessModel?): DrawableActivity {
      return DrawableActivity(this, newOwner)
    }

  }

  private var state = Drawable.STATE_DEFAULT

  val isBodySpecified: Boolean
    get() = message != null

  val isUserTask: Boolean
    get() {
      val message = XmlMessage.get(message)
      return message != null && Endpoints.USER_TASK_SERVICE_DESCRIPTOR.isSameService(message.endpointDescriptor)
    }

  val isService: Boolean
    get() = isBodySpecified && !isUserTask
  override val isCompat: Boolean
  override var condition: String? = null
  override val maxSuccessorCount: Int
    get() = if (isCompat) Integer.MAX_VALUE else 1

  override fun getState(): Int {
    return state
  }

  override fun setState(state: Int) {
    if (state == state) {
      return
    }
    this.state = state
    ownerModel?.notifyNodeChanged(this)
  }

  @Deprecated("Use the builder")
  @JvmOverloads constructor(owner: DrawableProcessModel?, compat: Boolean = false) : this(Builder(compat = compat),owner)

  @Deprecated("Use the builder")
  constructor(owner: DrawableProcessModel?, id: String, compat: Boolean) : this(Builder(id=id, compat = compat),owner)

  @Deprecated("Use the builder")
  constructor(orig: Activity<*, *>, compat: Boolean) : this(Builder(orig).apply { isCompat=compat }, null)  {
    if (orig is DrawableActivity) {
      state = orig.state
    }
  }

  constructor(builder: Activity.Builder<*, *>, newOwnerModel: DrawableProcessModel?) : super(builder, newOwnerModel) {
    isCompat = (builder as? Builder)?.isCompat ?: false
    state = (builder as? Builder)?.state ?: Drawable.STATE_DEFAULT
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun clone(): DrawableActivity {
    if (javaClass == DrawableActivity::class.java) {
      return DrawableActivity(this, isCompat)
    }
    throw RuntimeException(CloneNotSupportedException())
  }

  override fun isWithinBounds(x: Double, y: Double): Boolean {
    val hwidth = (ACTIVITYWIDTH + STROKEWIDTH) / 2
    val hheight = (ACTIVITYHEIGHT + STROKEWIDTH) / 2
    return Math.abs(x - x) <= hwidth && Math.abs(y - y) <= hheight
  }

  override val idBase: String
    get() = IDBASE

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle) {

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

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> drawLabel(canvas: Canvas<S, PEN_T, PATH_T>,
                                            clipBounds: Rectangle?,
                                            left: Double,
                                            top: Double) {

    if (hasPos()) with(canvas){
      val textPen = theme.getPen(DIAGRAMLABEL, state)
      val label = getDrawnLabel(textPen)
      if (!label.isNullOrBlank()) {
        val topCenter = ACTIVITYHEIGHT + STROKEWIDTH + textPen.textLeading / 2
        drawText(TextPos.ASCENT, REFERENCE_OFFSET_X, topCenter, label, java.lang.Double.MAX_VALUE, textPen)
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

  override fun setId(id: String) = super.setId(id)
  override fun setLabel(label: String?) = super.setLabel(label)
  @Throws(XmlException::class)
  override fun serializeCondition(out: XmlWriter) {
    if (!condition.isNullOrEmpty()) {
      out.writeSimpleElement(Condition.ELEMENTNAME, condition)
    }
  }

  override fun setOwnerModel(newOwnerModel: DrawableProcessModel?) = super.setOwnerModel(newOwnerModel)

  override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)

  override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)

  override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)

  override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)

  override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)

  override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

  companion object {

    private const val REFERENCE_OFFSET_X = (ACTIVITYWIDTH + STROKEWIDTH) / 2
    private const val REFERENCE_OFFSET_Y = (ACTIVITYHEIGHT + STROKEWIDTH) / 2
    const val IDBASE = "ac"
    private val _bounds by lazy { Rectangle(STROKEWIDTH / 2, STROKEWIDTH / 2, ACTIVITYWIDTH, ACTIVITYHEIGHT) }

    @JvmStatic
    @Deprecated("")
    @Throws(XmlException::class)
    fun deserialize(ownerModel: DrawableProcessModel, reader: XmlReader): DrawableActivity {
      return DrawableActivity.Builder(compat = true).deserializeHelper(reader).build(ownerModel)
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): Builder {
      return DrawableActivity.Builder(compat = true).deserializeHelper(reader)
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
