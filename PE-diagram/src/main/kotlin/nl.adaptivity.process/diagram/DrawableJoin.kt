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
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified


class DrawableJoin : JoinBase<DrawableProcessNode, DrawableProcessModel?>, Join<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit {

  class Builder : JoinBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit.Builder {

    override val _delegate: DrawableProcessNode.Builder.Delegate

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
                multiInstance: Boolean = false,
                isCompat: Boolean = false) : super(id, predecessors, successor, label, defines, results,
                                                   x, y, min, max, multiInstance) {
      _delegate = DrawableProcessNode.Builder.Delegate(state, isCompat)
    }

    constructor(node: Join<*, *>) : super(node) {
      _delegate = DrawableProcessNode.Builder.Delegate(node)
    }

    override fun build(buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) = DrawableJoin(
      this, buildHelper)
  }

  override val _delegate: DrawableJoinSplit.Delegate

  override val maxSuccessorCount: Int
    get() = if (isCompat) Integer.MAX_VALUE else 1

  constructor(builder: Join.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) : super(builder, buildHelper) {
    _delegate = DrawableJoinSplit.Delegate(builder)
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun clone(): DrawableJoin {
    return builder().build(STUB_DRAWABLE_BUILD_HELPER)
  }

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle) {
    if (hasPos()) {
      super.draw(canvas, clipBounds)

      val path = _delegate.itemCache.getPath(canvas.strategy, 1) {
        if (CURVED_ARROWS) {
          moveTo(CENTER_X + INLEN, CENTER_Y)
          lineTo(CENTER_X + ARROWHEADD_X - ARROWHEAD_ADJUST, CENTER_X)
          moveTo(CENTER_X + ARROWHEADD_X - ARROWDNEAR, CENTER_Y - ARROWDFAR)
          lineTo(CENTER_X + ARROWHEADD_X, CENTER_Y)
          lineTo(CENTER_X + ARROWHEADD_X - ARROWDNEAR, CENTER_Y + ARROWDFAR)
          moveTo(CENTER_X - IND_X, CENTER_Y - IND_Y)
          cubicTo(CENTER_X - IND_X * (1 - ARROWCONTROLRATIO), CENTER_Y - IND_Y * (1 - ARROWCONTROLRATIO), CENTER_X + INLEN * (1 - ARROWCONTROLRATIO), CENTER_Y, CENTER_X + INLEN, CENTER_Y)
          cubicTo(CENTER_X + INLEN * (1 - ARROWCONTROLRATIO), CENTER_Y, CENTER_X - IND_X * (1 - ARROWCONTROLRATIO), CENTER_Y + IND_Y * (1 - ARROWCONTROLRATIO), CENTER_X - IND_X, CENTER_Y + IND_Y)
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

  override fun setId(id: String) = super.setId(id)
  override fun setLabel(label: String?) = super.setLabel(label)
  override fun setOwnerModel(newOwnerModel: DrawableProcessModel?) = super.setOwnerModel(newOwnerModel)
  override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)
  override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)
  override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)
  override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)
  override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)
  override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

  companion object {

    private const val ARROWHEADD_X = JOINWIDTH * 0.375
    private val ARROWHEAD_ADJUST = 0.5 * STROKEWIDTH / Math.sin(DrawableJoinSplit.ARROWHEADANGLE)

    /** The y coordinate if the line were horizontal.  */
    private val ARROWDFAR = DrawableJoinSplit.ARROWLEN * Math.sin(DrawableJoinSplit.ARROWHEADANGLE)
    /** The x coordinate if the line were horizontal.  */
    private val ARROWDNEAR = DrawableJoinSplit.ARROWLEN * Math.cos(DrawableJoinSplit.ARROWHEADANGLE)
    private const val IND_X = JOINWIDTH * 0.2
    private const val IND_Y = JOINHEIGHT * 0.2
    private val INLEN = Math.sqrt(IND_X * IND_X + IND_Y * IND_Y)

    @Deprecated("Use the builder")
    @JvmStatic
    fun from(elem: Join<*, *>, compat: Boolean): DrawableJoin {
      return Builder(elem).build(STUB_DRAWABLE_BUILD_HELPER)
    }

  }
}
