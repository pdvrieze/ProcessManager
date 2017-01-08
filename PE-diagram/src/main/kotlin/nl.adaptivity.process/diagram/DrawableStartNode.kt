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
import nl.adaptivity.process.clientProcessModel.ClientProcessNode
import nl.adaptivity.process.clientProcessModel.ClientStartNode
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STARTNODERADIUS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.StartNodeBase
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper


class DrawableStartNode : /*ClientStartNode,*/ StartNodeBase<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode {

  class Builder : StartNodeBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode.Builder {

    override var state: DrawableState
    override var isCompat = false

    constructor(id: String? = null,
                successor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                state: DrawableState = Drawable.STATE_DEFAULT) : super(id, successor, label, defines, results, x, y) {
      this.state = state
    }

    constructor(node: StartNode<*, *>) : super(node) {
      state = (node as? Drawable)?.state ?: Drawable.STATE_DEFAULT
    }

    override fun build(newOwner: DrawableProcessModel?) = DrawableStartNode(this, newOwner)
  }

  private var mState = Drawable.STATE_DEFAULT

  @JvmOverloads constructor(ownerModel: DrawableProcessModel?, compat: Boolean = false) : super(ownerModel) {
    isCompat = compat
  }

  constructor(ownerModel: DrawableProcessModel?, id: String, compat: Boolean = false) : super(ownerModel, id=id) {
    isCompat = compat
  }

  @Deprecated("Use the constructor that takes a builder")
  constructor(orig: DrawableStartNode, newOwner: DrawableProcessModel? = null) : this(orig.builder(), newOwner)

  constructor(builder: StartNode.Builder<*, *>, newOwnerModel: DrawableProcessModel?) : super(builder, newOwnerModel) {
    val dsnBuilder = builder as? Builder
    isCompat = dsnBuilder?.isCompat ?: false
    mState = dsnBuilder?.state ?: Drawable.STATE_DEFAULT
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun clone(): DrawableStartNode {
    if (javaClass == DrawableStartNode::class.java) {
      return DrawableStartNode(this)
    }
    throw RuntimeException(CloneNotSupportedException())
  }

  override val idBase: String
    get() = IDBASE

  override val isCompat: Boolean

  override val maxSuccessorCount: Int
    get() = if (isCompat) Integer.MAX_VALUE else 1

  override val leftExtent get() = REFERENCE_OFFSET_X
  override val rightExtent get() = STARTNODERADIUS * 2 + STROKEWIDTH - REFERENCE_OFFSET_X
  override val topExtent get() = REFERENCE_OFFSET_Y
  override val bottomExtent get() = STARTNODERADIUS * 2 + STROKEWIDTH - REFERENCE_OFFSET_Y

  override fun isWithinBounds(x: Double, y: Double): Boolean {
    val realradius = STARTNODERADIUS + STROKEWIDTH / 2
    return Math.abs(x - x) <= realradius && Math.abs(y - y) <= realradius
  }

  override fun getState(): Int {
    return mState
  }

  override fun setState(state: Int) {
    mState = state
  }

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>,
                                                                                                              clipBounds: Rectangle) {
    if (hasPos()) {
      val realradius = STARTNODERADIUS + STROKEWIDTH / 2
      val fillPen = canvas.theme.getPen(ProcessThemeItems.LINEBG, mState and Drawable.STATE_TOUCHED.inv())

      if (mState and Drawable.STATE_TOUCHED != 0) {
        val touchedPen = canvas.theme.getPen(ProcessThemeItems.LINE, Drawable.STATE_TOUCHED)
        canvas.drawCircle(realradius, realradius, STARTNODERADIUS, touchedPen)
      }

      canvas.drawFilledCircle(realradius, realradius, realradius, fillPen)
    }
  }

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> drawLabel(
      canvas: Canvas<S, PEN_T, PATH_T>,
      clipBounds: Rectangle?,
      left: Double,
      top: Double) {
    defaultDrawLabel(this, canvas, clipBounds, left, top)
  }

  override fun setId(id: String) = super.setId(id!!)
  override fun setLabel(label: String?) = super.setLabel(label)
  override fun setOwnerModel(newOwnerModel: DrawableProcessModel??) = super.setOwnerModel(newOwnerModel)
  override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)
  override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)
  override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)
  override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)
  override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)
  override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

  companion object {

    private val REFERENCE_OFFSET_X = STARTNODERADIUS + STROKEWIDTH / 2
    private val REFERENCE_OFFSET_Y = STARTNODERADIUS + STROKEWIDTH / 2
    val IDBASE = "start"

    @Throws(XmlException::class)
    fun deserialize(`in`: XmlReader): Builder {
      return Builder().deserializeHelper(`in`)
    }

    @Deprecated("")
    @Throws(XmlException::class)
    fun deserialize(ownerModel: DrawableProcessModel, `in`: XmlReader): DrawableStartNode {
      return DrawableStartNode.Builder().deserializeHelper(`in`).build(ownerModel)
    }

    @JvmStatic
    fun from(n: StartNode<*, *>, compat: Boolean = false)
      = Builder(n).apply { this.isCompat = compat }.build(null)
  }

}
