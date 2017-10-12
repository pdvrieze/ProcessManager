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
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper


class DrawableEndNode : EndNodeBase<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode {

  class Builder : EndNodeBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode.Builder {

    override val _delegate: DrawableProcessNode.Builder.Delegate

    override var isCompat: kotlin.Boolean
      get() = false
      set(compat) {
        if (compat) throw IllegalArgumentException("Compatibility not supported on end nodes.")
      }

    constructor(id: String? = null,
                predecessor: Identified? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                multiInstance: Boolean = false,
                state: Int = Drawable.STATE_DEFAULT) : super(id, predecessor, label, defines, results, x, y, multiInstance) {
      _delegate = DrawableProcessNode.Builder.Delegate(state, false)
    }

    constructor(node: EndNode<*, *>) : super(node) {
      _delegate = DrawableProcessNode.Builder.Delegate(node)
    }

    override fun build(buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) = DrawableEndNode(this, buildHelper)
  }

  override val _delegate: DrawableProcessNode.Delegate

  override val idBase: String get() = IDBASE
  override val isCompat: Boolean
    get() = false

  @Deprecated("Use the builder", ReplaceWith("this(Builder(orig))"))
  constructor(orig: EndNode<*, *>) : this(Builder(orig), STUB_DRAWABLE_BUILD_HELPER)

  constructor(builder: EndNode.Builder<*, *>,
              buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) : super(builder, buildHelper) {
    _delegate = DrawableProcessNode.Delegate(builder)
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun clone(): DrawableEndNode {
    if (javaClass == DrawableEndNode::class.java) {
      return DrawableEndNode(this)
    }
    throw CloneNotSupportedException()
  }

  override fun isWithinBounds(x: Double, y: Double): Boolean {
    val realradius = ENDNODEOUTERRADIUS + ENDNODEOUTERSTROKEWIDTH / 2

    return Math.abs(x - x) <= realradius && Math.abs(y - y) <= realradius
  }

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

    private const val REFERENCE_OFFSET_X = ENDNODEOUTERRADIUS
    private const val REFERENCE_OFFSET_Y = ENDNODEOUTERRADIUS
    const val IDBASE = "end"

    @Throws(XmlException::class)
    @JvmStatic
    fun deserialize(reader: XmlReader): DrawableEndNode.Builder {
      return DrawableEndNode.Builder(state = Drawable.STATE_DEFAULT).deserializeHelper(reader)
    }

    @Deprecated("Use the builder")
    fun from(elem: EndNode<*, *>): DrawableEndNode {
      val result = DrawableEndNode(elem)
      return result
    }
  }

}
