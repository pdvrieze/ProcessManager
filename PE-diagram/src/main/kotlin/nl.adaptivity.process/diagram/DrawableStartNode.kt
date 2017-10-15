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
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STARTNODERADIUS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.STROKEWIDTH
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper

interface IDrawableStartNode: IDrawableProcessNode {
  override val leftExtent get() = REFERENCE_OFFSET_X
  override val rightExtent get() = STARTNODERADIUS * 2 + STROKEWIDTH - REFERENCE_OFFSET_X
  override val topExtent get() = REFERENCE_OFFSET_Y
  override val bottomExtent get() = STARTNODERADIUS * 2 + STROKEWIDTH - REFERENCE_OFFSET_Y

  override val maxPredecessorCount: Int get() = 0

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>,
                                                                                                              clipBounds: Rectangle?) {
    if (hasPos()) {
      val realradius = STARTNODERADIUS + STROKEWIDTH / 2
      val fillPen = canvas.theme.getPen(ProcessThemeItems.LINEBG, state and Drawable.STATE_TOUCHED.inv())

      if (state and Drawable.STATE_TOUCHED != 0) {
        val touchedPen = canvas.theme.getPen(ProcessThemeItems.LINE, Drawable.STATE_TOUCHED)
        canvas.drawCircle(realradius, realradius, STARTNODERADIUS, touchedPen)
      }

      canvas.drawFilledCircle(realradius, realradius, realradius, fillPen)
    }
  }

  companion object {
    val REFERENCE_OFFSET_X = STARTNODERADIUS + STROKEWIDTH / 2
    val REFERENCE_OFFSET_Y = STARTNODERADIUS + STROKEWIDTH / 2
  }

}

class DrawableStartNode(builder: StartNode.Builder<*, *>,
                        buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) : /*ClientStartNode,*/ StartNodeBase<DrawableProcessNode, DrawableProcessModel?>(
  builder, buildHelper), DrawableProcessNode, IDrawableStartNode {

  class Builder : StartNodeBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode.Builder, IDrawableStartNode {

    override val _delegate: DrawableProcessNode.Builder.Delegate

    constructor(id: String? = null,
                successor: Identified? = null,
                label: String? = null,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                state: DrawableState = Drawable.STATE_DEFAULT,
                isCompat: Boolean = false,
                isMultiInstance: Boolean = false) : super(id, successor, label, defines, results, x, y, isMultiInstance) {
      _delegate = DrawableProcessNode.Builder.Delegate(state, isCompat)
    }

    constructor(node: StartNode<*, *>) : super(node) {
      _delegate = DrawableProcessNode.Builder.Delegate(node)
    }

    override fun copy(): IDrawableProcessNode {
      return Builder(id, successor?.identifier, label, defines, results, x, y, state, isCompat, isMultiInstance)
    }

    override fun build(buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) = DrawableStartNode(
      this, buildHelper)
  }

  override val _delegate = DrawableProcessNode.Delegate(builder)

  override val idBase: String
    get() = IDBASE

  override val maxSuccessorCount: Int
    get() = if (isCompat) Integer.MAX_VALUE else 1

  override val maxPredecessorCount get() = super<StartNodeBase>.maxPredecessorCount

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun copy(): DrawableStartNode {
    return builder().build(STUB_DRAWABLE_BUILD_HELPER)
  }

  override fun isWithinBounds(x: Double, y: Double): Boolean {
    val realradius = STARTNODERADIUS + STROKEWIDTH / 2
    val dx = Math.abs(this.x - x)
    val dy = Math.abs(this.y - y)
    return dx*dx+dy*dy <= realradius*realradius
  }

  @Deprecated("Use builders")
  override fun setId(id: String) = super.setId(id)
  @Deprecated("Use builders")
  override fun setLabel(label: String?) = super.setLabel(label)
  @Deprecated("Use builders")
  override fun setOwnerModel(newOwnerModel: DrawableProcessModel??) = super.setOwnerModel(newOwnerModel)
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

    val REFERENCE_OFFSET_X = STARTNODERADIUS + STROKEWIDTH / 2
    val REFERENCE_OFFSET_Y = STARTNODERADIUS + STROKEWIDTH / 2
    val IDBASE = "start"

    @Throws(XmlException::class)
    @JvmStatic
    fun deserialize(reader: XmlReader): Builder {
      return DrawableStartNode.Builder().deserializeHelper(reader)
    }

    @JvmStatic
    fun from(n: StartNode<*, *>, compat: Boolean = false)
      = Builder(n).apply { this.isCompat = compat }.build(STUB_DRAWABLE_BUILD_HELPER)
  }

}
