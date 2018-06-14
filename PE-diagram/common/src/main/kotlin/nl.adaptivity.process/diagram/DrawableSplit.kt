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
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.deserializeHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


interface IDrawableSplit: IDrawableJoinSplit {
  override val maxSuccessorCount: Int get() = Int.MAX_VALUE

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> drawDecoration(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?) {
    if (hasPos()) {
      val path = itemCache.getPath(canvas.strategy, 1) {
        if (DrawableJoinSplit.CURVED_ARROWS) {
          moveTo(CENTER_X - HORIZONTALDECORATIONLEN, CENTER_Y)
          lineTo(CENTER_X - INLEN, CENTER_Y)
          cubicTo(CENTER_X - INLEN * (1 - ARROWCONTROLRATIO), CENTER_Y, CENTER_X + ARROWHEADDX * (1 - ARROWCONTROLRATIO) - ARROWHEADADJUST, CENTER_Y - ARROWHEADDY * (1 - ARROWCONTROLRATIO) + ARROWHEADADJUST, CENTER_X + ARROWHEADDX - ARROWHEADADJUST, CENTER_Y - ARROWHEADDY + ARROWHEADADJUST)
          lineTo(CENTER_X + ARROWHEADDX, CENTER_Y - ARROWHEADDY)
          moveTo(CENTER_X - INLEN, CENTER_Y)
          cubicTo(CENTER_X - INLEN * (1 - ARROWCONTROLRATIO), CENTER_Y, CENTER_X + ARROWHEADDX * (1 - ARROWCONTROLRATIO) - ARROWHEADADJUST, CENTER_Y + ARROWHEADDY * (1 - ARROWCONTROLRATIO) - ARROWHEADADJUST, CENTER_X + ARROWHEADDX - ARROWHEADADJUST, CENTER_Y + ARROWHEADDY - ARROWHEADADJUST)
          lineTo(CENTER_X + ARROWHEADDX, CENTER_Y + ARROWHEADDY)
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

      val linePen = canvas.theme.getPen(ProcessThemeItems.INNERLINE, state and Drawable.STATE_TOUCHED.inv())
      canvas.drawPath(path, linePen, null)
    }
  }

  companion object {
    private const val ARROWHEADDX = JOINWIDTH * 0.2
    private const val ARROWHEADDY = JOINWIDTH * 0.2

    private val ARROWHEADADJUST = 0.5 * STROKEWIDTH * sqrt(0.5 / (sin(DrawableJoinSplit.ARROWHEADANGLE) * sin(DrawableJoinSplit.ARROWHEADANGLE)))
    /** The y coordinate if the line were horizontal.  */
    private val ARROWDFAR = DrawableJoinSplit.ARROWLEN * sin(0.25 * PI - DrawableJoinSplit.ARROWHEADANGLE)
    /** The x coordinate if the line were horizontal.  */
    private val ARROWDNEAR = DrawableJoinSplit.ARROWLEN * cos(0.25 * PI - DrawableJoinSplit.ARROWHEADANGLE)

    private val INLEN = sqrt(ARROWHEADDX * ARROWHEADDX + ARROWHEADDY * ARROWHEADDY)

  }

}

class DrawableSplit : SplitBase<DrawableProcessNode, DrawableProcessModel?>, Split<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit, IDrawableSplit {

  class Builder : SplitBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableJoinSplit.Builder<DrawableSplit>, IDrawableSplit {

    override val _delegate: DrawableProcessNode.Builder.Delegate

    constructor(): this(id=null)

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

    override val itemCache = ItemCache()

    constructor(node: Split<*, *>) : super(node) {
      _delegate = DrawableProcessNode.Builder.Delegate(node)
    }

    override fun copy()
      = Builder(id, predecessor?.identifier, successors, label, defines, results, x, y, min, max, state, isMultiInstance)

    override fun build(buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>)
      = DrawableSplit(this, buildHelper)

  }

  override val _delegate : DrawableJoinSplit.Delegate
  override val itemCache = ItemCache()

  override val maxSuccessorCount get() = super<SplitBase>.maxSuccessorCount
  override val maxPredecessorCount get() = super<SplitBase>.maxPredecessorCount

  constructor(builder: Split.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<DrawableProcessNode, DrawableProcessModel?>) : super(builder, buildHelper) {
    _delegate = DrawableJoinSplit.Delegate(builder)
  }

  override fun builder(): Builder {
    return Builder(this)
  }

  override fun copy(): DrawableSplit { return builder().build() }

  override val idBase: String
    get() = IDBASE

  companion object {

    private const val ARROWHEADDX = JOINWIDTH * 0.2
    private const val ARROWHEADDY = JOINWIDTH * 0.2

    private val ARROWHEADADJUST = 0.5 * STROKEWIDTH * sqrt(0.5 / (sin(DrawableJoinSplit.ARROWHEADANGLE) * sin(DrawableJoinSplit.ARROWHEADANGLE)))
    /** The y coordinate if the line were horizontal.  */
    private val ARROWDFAR = DrawableJoinSplit.ARROWLEN * sin(0.25 * PI - DrawableJoinSplit.ARROWHEADANGLE)
    /** The x coordinate if the line were horizontal.  */
    private val ARROWDNEAR = DrawableJoinSplit.ARROWLEN * cos(0.25 * PI - DrawableJoinSplit.ARROWHEADANGLE)
    private val INLEN = sqrt(ARROWHEADDX * ARROWHEADDX + ARROWHEADDY * ARROWHEADDY)
    const val IDBASE = "split"

    @JvmStatic
    fun deserialize(reader: XmlReader): Builder {
      return Builder().deserializeHelper(reader)
    }

    fun from(elem: Split<*, *>): DrawableSplit {
      return DrawableSplit.Builder(elem).build()
    }
  }

}
