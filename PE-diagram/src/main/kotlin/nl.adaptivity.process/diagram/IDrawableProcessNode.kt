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
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.isBetween

interface IDrawableProcessNode: Drawable, Identifiable {

  val predecessors: Set<@JvmWildcard Identified>

  val successors: Set<@JvmWildcard Identified>

  val maxSuccessorCount: Int get() = 1

  val maxPredecessorCount: Int get() = 1

  override val id: String?

  val label: String?

  override fun copy(): IDrawableProcessNode

  override fun getItemAt(x: Double, y: Double): Drawable? {
    return if ((x-this.x).isBetween(-leftExtent, rightExtent) &&
        (y-this.y).isBetween(-topExtent, bottomExtent)) this
    else null
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>,
    PEN_T : Pen<PEN_T>,
    PATH_T : DiagramPath<PATH_T>> drawLabel(canvas: Canvas<S, PEN_T, PATH_T>,
                                                                  clipBounds: Rectangle?,
                                                                  left: Double,
                                                                  top: Double) {
    // TODO better use extends, especially determining where X is.
    if (hasPos()) with(canvas){
      val textPen = theme.getPen(ProcessThemeItems.DIAGRAMLABEL, state)
      val label = getDrawnLabel(textPen)
      if (label!=null && label.isNotBlank()) {
        val topCenter = topExtent + bottomExtent + textPen.textLeading / 2
        drawText(Canvas.TextPos.ASCENT, leftExtent, topCenter, label, java.lang.Double.MAX_VALUE, textPen)
      }
    }
  }

  fun <PEN_T : Pen<PEN_T>> getDrawnLabel(textPen: PEN_T): String? {
    return label?.apply { textPen.isTextItalics = false }
           ?: "<$id>".apply { textPen.isTextItalics = true }
  }

}