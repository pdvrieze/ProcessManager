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
import nl.adaptivity.process.diagram.DrawableJoinSplit.Companion.HORIZONTALDECORATIONLEN
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.JOINWIDTH
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper


class DrawableSplit : SplitBase<DrawableProcessNode, DrawableProcessModel?>, Split<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit {

  class Builder : SplitBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit.Builder {

    override val _delegate: DrawableProcessNode.Builder.Delegate

    constructor(id: String? = null,
                predecessor: Identified? = null,
                successors: Collection<Identified> = emptyList(),
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                min: Int = 1,
                max: Int = -1,
                state: DrawableState = Drawable.STATE_DEFAULT,
                multiInstance: Boolean = false) : super(id, predecessor, successors, label, defines, results, x, y, min, max, multiInstance) {
      _delegate = DrawableProcessNode.Builder.Delegate(state, false)
    }

    constructor(node: Split<*, *>) : super(node) {
      _delegate = DrawableProcessNode.Builder.Delegate(node)
    }

    override fun build(buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) = DrawableSplit(
      this, buildHelper)

  }

  override val _delegate : DrawableJoinSplit.Delegate

  constructor(builder: Split.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) : super(builder, buildHelper) {
    _delegate = DrawableJoinSplit.Delegate(builder)
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun clone(): DrawableSplit { return builder().build(STUB_DRAWABLE_BUILD_HELPER) }

  override val idBase: String
    get() = IDBASE

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle) {
    if (hasPos()) {
      super.draw(canvas, clipBounds)

      val path = _delegate.itemCache.getPath(canvas.strategy, 1) {
        if (DrawableJoinSplit.CURVED_ARROWS) {
          moveTo(CENTER_X - HORIZONTALDECORATIONLEN, CENTER_Y)
          lineTo(CENTER_X - INLEN, CENTER_Y)
          cubicTo(CENTER_X - INLEN * (1 - ARROWCONTROLRATIO), CENTER_Y, CENTER_X + ARROWHEADDX * (1 - ARROWCONTROLRATIO) - ARROWHEADADJUST, CENTER_Y - ARROWHEADDY * (1 - ARROWCONTROLRATIO) + ARROWHEADADJUST, CENTER_X + ARROWHEADDX - ARROWHEADADJUST, CENTER_Y - ARROWHEADDY + ARROWHEADADJUST)
          moveTo(CENTER_X - INLEN, CENTER_Y)
          cubicTo(CENTER_X - INLEN * (1 - ARROWCONTROLRATIO), CENTER_Y, CENTER_X + ARROWHEADDX * (1 - ARROWCONTROLRATIO) - ARROWHEADADJUST, CENTER_Y + ARROWHEADDY * (1 - ARROWCONTROLRATIO) - ARROWHEADADJUST, CENTER_X + ARROWHEADDX - ARROWHEADADJUST, CENTER_Y + ARROWHEADDY - ARROWHEADADJUST)
        } else {
          moveTo(CENTER_X - HORIZONTALDECORATIONLEN, CENTER_Y)
          lineTo(CENTER_X, CENTER_Y)
          moveTo(CENTER_X + ARROWHEADDX - ARROWHEADADJUST, CENTER_Y - ARROWHEADDY + ARROWHEADADJUST)
          lineTo(CENTER_X, CENTER_Y)
          lineTo(CENTER_X + ARROWHEADDX - ARROWHEADADJUST, CENTER_Y + ARROWHEADDY - ARROWHEADADJUST)
        }

        moveTo(CENTER_X + ARROWHEADDX - ARROWDNEAR, CENTER_Y - ARROWHEADDY + ARROWDFAR)
        lineTo(CENTER_X + ARROWHEADDX, CENTER_Y - ARROWHEADDY)
        lineTo(CENTER_X + ARROWHEADDX - ARROWDFAR, CENTER_Y - ARROWHEADDY + ARROWDNEAR)
        moveTo(CENTER_X + ARROWHEADDX - ARROWDFAR, CENTER_Y + ARROWHEADDY - ARROWDNEAR)
        lineTo(CENTER_X + ARROWHEADDX, CENTER_Y + ARROWHEADDY)
        lineTo(CENTER_X + ARROWHEADDX - ARROWDNEAR, CENTER_Y + ARROWHEADDY - ARROWDFAR)

      }

      val linePen = canvas.theme.getPen(ProcessThemeItems.INNERLINE, _delegate.state and Drawable.STATE_TOUCHED.inv())
      canvas.drawPath(path, linePen, null)
    }
  }

  @Deprecated("Use builders")
  override fun setId(id: String) = super.setId(id)
  @Deprecated("Use builders")
  override fun setLabel(label: String?) = super.setLabel(label)
  @Deprecated("Use builders")
  override fun setOwnerModel(newOwnerModel: DrawableProcessModel?) = super.setOwnerModel(newOwnerModel)
  @Deprecated("Use builders")
  override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)
  @Deprecated("Use builders")
  override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)
  @Deprecated("Use builders")
  override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)
  @Deprecated("Use builders")
  override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)
  @Deprecated("Use builders")
  override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)
  @Deprecated("Use builders")
  override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

  companion object {

    private const val ARROWHEADDX = JOINWIDTH * 0.2
    private const val ARROWHEADDY = JOINWIDTH * 0.2

    private val ARROWHEADADJUST = 0.5 * STROKEWIDTH * Math.sqrt(0.5 / (Math.sin(DrawableJoinSplit.ARROWHEADANGLE) * Math.sin(DrawableJoinSplit.ARROWHEADANGLE)))
    /** The y coordinate if the line were horizontal.  */
    private val ARROWDFAR = DrawableJoinSplit.ARROWLEN * Math.sin(0.25 * Math.PI - DrawableJoinSplit.ARROWHEADANGLE)
    /** The x coordinate if the line were horizontal.  */
    private val ARROWDNEAR = DrawableJoinSplit.ARROWLEN * Math.cos(0.25 * Math.PI - DrawableJoinSplit.ARROWHEADANGLE)
    private val INLEN = Math.sqrt(ARROWHEADDX * ARROWHEADDX + ARROWHEADDY * ARROWHEADDY)
    const val IDBASE = "split"

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): Builder {
      return Builder().deserializeHelper(reader)
    }

    @JvmStatic
    fun from(elem: Split<*, *>): DrawableSplit {
      return DrawableSplit.Builder(elem).build(STUB_DRAWABLE_BUILD_HELPER)
    }
  }

}
