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
import nl.adaptivity.process.clientProcessModel.ClientJoinNode
import nl.adaptivity.process.diagram.DrawableJoinSplit.Companion
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.*

import nl.adaptivity.process.diagram.RootDrawableProcessModel.*


class DrawableJoin : ClientJoinNode<DrawableProcessNode, DrawableProcessModel?>, Join<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit {

  class Builder : ClientJoinNode.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit.Builder {

    constructor() {}

    constructor(compat: Boolean) : super(compat) {}

    constructor(predecessors: Collection<Identified>, successor: Identified, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>, min: Int, max: Int) : super(predecessors, successor, id, label, x, y, defines, results, min, max) {}

    constructor(node: Join<*, *>) : super(node) {}

    override fun build(newOwner: DrawableProcessModel?): DrawableJoin {
      return DrawableJoin(this, newOwner)
    }
  }

  override val _delegate: DrawableJoinSplitDelegate

  @JvmOverloads constructor(ownerModel: DrawableProcessModel?, compat: Boolean = false) : super(ownerModel, compat) {
    _delegate = DrawableJoinSplitDelegate()
  }

  constructor(ownerModel: DrawableProcessModel?, id: String, compat: Boolean) : super(ownerModel, id, compat) {
    _delegate = DrawableJoinSplitDelegate()
  }

  constructor(orig: DrawableJoin, compat: Boolean) : super(orig, compat) {
    _delegate = DrawableJoinSplitDelegate(orig._delegate)
  }

  constructor(builder: ClientJoinNode.Builder<*, *>, newOwnerModel: DrawableProcessModel?) : super(builder, newOwnerModel) {
    _delegate = DrawableJoinSplitDelegate()
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun clone(): DrawableJoin {
    if (javaClass == DrawableJoin::class.java) {
      return DrawableJoin(this, isCompat)
    }
    throw RuntimeException(CloneNotSupportedException())
  }

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle) {
    super.draw(canvas, clipBounds)

    val strategy = canvas.strategy
    var path = _delegate.itemCache.getPath(strategy, 1)
    if (path == null) {
      path = strategy.newPath()
      if (DrawableJoinSplit.CURVED_ARROWS) {
        path!!.moveTo(DrawableJoinSplit.CENTERX + INLEN, DrawableJoinSplit.CENTERY)
            .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWHEADADJUST, DrawableJoinSplit.CENTERX)
            .moveTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWDNEAR, DrawableJoinSplit.CENTERY - ARROWDFAR)
            .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX, DrawableJoinSplit.CENTERY)
            .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWDNEAR, DrawableJoinSplit.CENTERY + ARROWDFAR)
            .moveTo(DrawableJoinSplit.CENTERX - INDX, DrawableJoinSplit.CENTERY - INDY)
            .cubicTo(DrawableJoinSplit.CENTERX - INDX * (1 - DrawableJoinSplit.ARROWCONTROLRATIO), DrawableJoinSplit.CENTERY - INDY * (1 - DrawableJoinSplit.ARROWCONTROLRATIO), DrawableJoinSplit.CENTERX + INLEN * (1 - DrawableJoinSplit.ARROWCONTROLRATIO), DrawableJoinSplit.CENTERY, DrawableJoinSplit.CENTERX + INLEN, DrawableJoinSplit.CENTERY)
            .cubicTo(DrawableJoinSplit.CENTERX + INLEN * (1 - DrawableJoinSplit.ARROWCONTROLRATIO), DrawableJoinSplit.CENTERY, DrawableJoinSplit.CENTERX - INDX * (1 - DrawableJoinSplit.ARROWCONTROLRATIO), DrawableJoinSplit.CENTERY + INDY * (1 - DrawableJoinSplit.ARROWCONTROLRATIO), DrawableJoinSplit.CENTERX - INDX, DrawableJoinSplit.CENTERY + INDY)
      } else {
        path!!.moveTo(DrawableJoinSplit.CENTERX, DrawableJoinSplit.CENTERY)
            .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWHEADADJUST, DrawableJoinSplit.CENTERX)
            .moveTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWDNEAR, DrawableJoinSplit.CENTERY - ARROWDFAR)
            .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX, DrawableJoinSplit.CENTERY)
            .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWDNEAR, DrawableJoinSplit.CENTERY + ARROWDFAR)
            .moveTo(DrawableJoinSplit.CENTERX - INDX, DrawableJoinSplit.CENTERY - INDY)
            .lineTo(DrawableJoinSplit.CENTERX, DrawableJoinSplit.CENTERY)
            .lineTo(DrawableJoinSplit.CENTERX - INDX, DrawableJoinSplit.CENTERY + INDY)
      }
      _delegate.itemCache.setPath(strategy, 1, path)
    }
    if (hasPos()) {
      val linePen = canvas.theme.getPen(ProcessThemeItems.INNERLINE, state)
      canvas.drawPath(path, linePen, null)
    }
  }

  companion object {

    private const val ARROWHEADDX = JOINWIDTH * 0.375
    private val ARROWHEADADJUST = 0.5 * STROKEWIDTH / Math.sin(DrawableJoinSplit.ARROWHEADANGLE)

    /** The y coordinate if the line were horizontal.  */
    private val ARROWDFAR = DrawableJoinSplit.ARROWLEN * Math.sin(DrawableJoinSplit.ARROWHEADANGLE)
    /** The x coordinate if the line were horizontal.  */
    private val ARROWDNEAR = DrawableJoinSplit.ARROWLEN * Math.cos(DrawableJoinSplit.ARROWHEADANGLE)
    private const val INDX = JOINWIDTH * 0.2
    private const val INDY = JOINHEIGHT * 0.2
    private val INLEN = Math.sqrt(INDX * INDX + INDY * INDY)

    @JvmStatic
    fun from(elem: Join<*, *>, compat: Boolean): DrawableJoin {
      val owner: DrawableProcessModel? = (elem as? DrawableProcessNode)?.ownerModel
      return DrawableJoin(owner, compat)
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(ownerModel: DrawableProcessModel?, reader: XmlReader): DrawableJoin {
      return Builder(true).deserializeHelper(reader).build(ownerModel)
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): Builder {
      return Builder(true).deserializeHelper(reader)
    }
  }
}