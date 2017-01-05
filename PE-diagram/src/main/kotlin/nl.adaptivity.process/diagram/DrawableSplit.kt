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
import nl.adaptivity.process.clientProcessModel.ClientSplitNode
import nl.adaptivity.process.diagram.DrawableJoinSplit.Companion
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.*

import nl.adaptivity.process.diagram.RootDrawableProcessModel.*


class DrawableSplit : ClientSplitNode<DrawableProcessNode, DrawableProcessModel?>, Split<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit {

  class Builder : ClientSplitNode.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit.Builder {

    constructor() {}

    constructor(predecessors: Collection<Identified>, successors: Collection<Identified>, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>, min: Int, max: Int) : super(predecessors, successors, id, label, x, y, defines, results, min, max) {}

    constructor(node: Split<*, *>) : super(node) {}

    override fun build(newOwner: DrawableProcessModel?): DrawableSplit {
      return DrawableSplit(this, newOwner)
    }

  }

  override val _delegate : DrawableJoinSplitDelegate

  constructor(ownerModel: DrawableProcessModel?) : super(Builder(), ownerModel) {
    _delegate = DrawableJoinSplitDelegate()
  }

  constructor(orig: Split<*, *>) : super(orig.builder(), null) {
    if (orig is DrawableSplit) {
      _delegate = DrawableJoinSplitDelegate(orig._delegate)
    } else {
      _delegate = DrawableJoinSplitDelegate()
    }
  }

  constructor(builder: Split.Builder<*, *>, newOwnerModel: DrawableProcessModel?) : super(builder, newOwnerModel) {
    _delegate = DrawableJoinSplitDelegate()
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun clone(): DrawableSplit {
    if (javaClass == DrawableSplit::class.java) {
      return DrawableSplit(this)
    }
    throw CloneNotSupportedException()
  }

  override fun isCompat() = false

  override val maxSuccessorCount: Int
    get() = Integer.MAX_VALUE

  override val idBase: String
    get() = IDBASE

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle) {
    super.draw(canvas, clipBounds)

    val strategy = canvas.strategy
    var path = _delegate.itemCache.getPath(strategy, 1)
    if (path == null) {
      path = strategy.newPath()
      if (DrawableJoinSplit.CURVED_ARROWS) {
        path!!.moveTo(DrawableJoinSplit.CENTERX - DrawableJoinSplit.HORIZONTALDECORATIONLEN, DrawableJoinSplit.CENTERY)
            .lineTo(DrawableJoinSplit.CENTERX - INLEN, DrawableJoinSplit.CENTERY)
            .cubicTo(DrawableJoinSplit.CENTERX - INLEN * (1 - DrawableJoinSplit.ARROWCONTROLRATIO), DrawableJoinSplit.CENTERY, DrawableJoinSplit.CENTERX + ARROWHEADDX * (1 - DrawableJoinSplit.ARROWCONTROLRATIO) - ARROWHEADADJUST, DrawableJoinSplit.CENTERY - ARROWHEADDY * (1 - DrawableJoinSplit.ARROWCONTROLRATIO) + ARROWHEADADJUST, DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWHEADADJUST, DrawableJoinSplit.CENTERY - ARROWHEADDY + ARROWHEADADJUST)
            .moveTo(DrawableJoinSplit.CENTERX - INLEN, DrawableJoinSplit.CENTERY)
            .cubicTo(DrawableJoinSplit.CENTERX - INLEN * (1 - DrawableJoinSplit.ARROWCONTROLRATIO), DrawableJoinSplit.CENTERY, DrawableJoinSplit.CENTERX + ARROWHEADDX * (1 - DrawableJoinSplit.ARROWCONTROLRATIO) - ARROWHEADADJUST, DrawableJoinSplit.CENTERY + ARROWHEADDY * (1 - DrawableJoinSplit.ARROWCONTROLRATIO) - ARROWHEADADJUST, DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWHEADADJUST, DrawableJoinSplit.CENTERY + ARROWHEADDY - ARROWHEADADJUST)
      } else {
        path!!.moveTo(DrawableJoinSplit.CENTERX - DrawableJoinSplit.HORIZONTALDECORATIONLEN, DrawableJoinSplit.CENTERY)
            .lineTo(DrawableJoinSplit.CENTERX, DrawableJoinSplit.CENTERY)
            .moveTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWHEADADJUST, DrawableJoinSplit.CENTERY - ARROWHEADDY + ARROWHEADADJUST)
            .lineTo(DrawableJoinSplit.CENTERX, DrawableJoinSplit.CENTERY)
            .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWHEADADJUST, DrawableJoinSplit.CENTERY + ARROWHEADDY - ARROWHEADADJUST)
      }

      path.moveTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWDNEAR, DrawableJoinSplit.CENTERY - ARROWHEADDY + ARROWDFAR)
          .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX, DrawableJoinSplit.CENTERY - ARROWHEADDY)
          .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWDFAR, DrawableJoinSplit.CENTERY - ARROWHEADDY + ARROWDNEAR)
          .moveTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWDFAR, DrawableJoinSplit.CENTERY + ARROWHEADDY - ARROWDNEAR)
          .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX, DrawableJoinSplit.CENTERY + ARROWHEADDY)
          .lineTo(DrawableJoinSplit.CENTERX + ARROWHEADDX - ARROWDNEAR, DrawableJoinSplit.CENTERY + ARROWHEADDY - ARROWDFAR)

      _delegate.itemCache.setPath(strategy, 1, path)
    }
    if (hasPos()) {
      val linePen = canvas.theme.getPen(ProcessThemeItems.INNERLINE, _delegate.state and Drawable.STATE_TOUCHED.inv())
      canvas.drawPath(path, linePen, null)
    }
  }

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
    @Deprecated("")
    @Throws(XmlException::class)
    fun deserialize(ownerModel: DrawableProcessModel?, reader: XmlReader): DrawableSplit {
      return DrawableSplit.Builder().deserializeHelper(reader).build(ownerModel)
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): Builder {
      return Builder().deserializeHelper(reader)
    }

    @JvmStatic
    fun from(elem: Split<*, *>): DrawableSplit {
      return DrawableSplit(elem)
    }
  }

}
