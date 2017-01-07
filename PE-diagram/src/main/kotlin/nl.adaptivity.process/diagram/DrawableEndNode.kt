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
import nl.adaptivity.process.clientProcessModel.ClientEndNode
import nl.adaptivity.process.diagram.ProcessThemeItems.ENDNODEOUTERLINE
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ENDNODEINNERRRADIUS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ENDNODEOUTERRADIUS
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Companion.ENDNODEOUTERSTROKEWIDTH
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper


class DrawableEndNode : ClientEndNode<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode {

  class Builder : ClientEndNode.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode.Builder {

    constructor(id: String? = null,
                predecessor: Identified? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                state: Int = Drawable.STATE_DEFAULT) : super(id, predecessor, label, defines, results, x, y) {
      this.state = state
    }

    override var state: Int

    constructor(node: EndNode<*, *>) : super(node) {
      this.state = (node as? Drawable)?.state ?: Drawable.STATE_DEFAULT
    }

    override fun build(newOwner: DrawableProcessModel?) = DrawableEndNode(this, newOwner)
  }

  private var state = Drawable.STATE_DEFAULT


  constructor(ownerModel: DrawableProcessModel) : super(ownerModel) {}

  constructor(ownerModel: DrawableProcessModel, id: String) : super(ownerModel, id) {}

  constructor(orig: EndNode<*, *>) : super(orig, null) {
    if (orig is DrawableEndNode) {
      state = orig.state
    }
  }

  constructor(builder: EndNode.Builder<*, *>, newOwnerModel: DrawableProcessModel?) : super(builder, newOwnerModel!!) {}

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun clone(): DrawableEndNode {
    if (javaClass == DrawableEndNode::class.java) {
      return DrawableEndNode(this)
    }
    throw CloneNotSupportedException()
  }

  override fun getBounds(): Rectangle {
    return Rectangle(x - ENDNODEOUTERRADIUS, y - ENDNODEOUTERRADIUS,
                     ENDNODEOUTERRADIUS * 2 + ENDNODEOUTERSTROKEWIDTH,
                     ENDNODEOUTERRADIUS * 2 + ENDNODEOUTERSTROKEWIDTH)
  }

  override fun setPos(left: Double, top: Double) {
    x = left + REFERENCE_OFFSET_X
    y = left + REFERENCE_OFFSET_Y
  }

  override fun translate(dX: Double, dY: Double) {
    x += dX
    y += dY
  }

  override fun isWithinBounds(x: Double, y: Double): Boolean {
    val realradius = ENDNODEOUTERRADIUS + ENDNODEOUTERSTROKEWIDTH / 2

    return Math.abs(x - getX()) <= realradius && Math.abs(y - getY()) <= realradius
  }

  override fun getItemAt(x: Double, y: Double): Drawable? {
    val realradius = ENDNODEOUTERRADIUS + ENDNODEOUTERSTROKEWIDTH / 2

    return if (Math.abs(x - getX()) <= realradius && Math.abs(y - getY()) <= realradius) this else null
  }

  override fun getState(): Int {
    return state
  }

  override fun setState(state: Int) {
    this.state = state
  }

  override val idBase: String get() = IDBASE

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle) {

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

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> drawLabel(
      canvas: Canvas<S, PEN_T, PATH_T>,
      clipBounds: Rectangle?,
      left: Double,
      top: Double) {
    defaultDrawLabel(this, canvas, clipBounds, left, top)
  }

  companion object {

    private const val REFERENCE_OFFSET_X = ENDNODEOUTERRADIUS
    private const val REFERENCE_OFFSET_Y = ENDNODEOUTERRADIUS
    const val IDBASE = "end"

    @Deprecated("")
    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(ownerModel: DrawableProcessModel, reader: XmlReader): DrawableEndNode {
      return DrawableEndNode.Builder(state = Drawable.STATE_DEFAULT).deserializeHelper(reader).build(ownerModel)
    }

    @Throws(XmlException::class)
    @JvmStatic
    fun deserialize(reader: XmlReader): DrawableEndNode.Builder {
      return DrawableEndNode.Builder(state = Drawable.STATE_DEFAULT).deserializeHelper(reader)
    }

    fun from(elem: EndNode<*, *>): DrawableEndNode {
      val result = DrawableEndNode(elem)
      return result
    }
  }

}
