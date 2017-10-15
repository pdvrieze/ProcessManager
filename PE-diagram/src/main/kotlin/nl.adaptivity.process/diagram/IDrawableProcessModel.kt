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
import nl.adaptivity.process.processModel.modelNodes
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.IdentifyableSet

/**
 * Shared code between various process models and their builders.
 */
interface IDrawableProcessModel: Diagram {

  val itemCache: ItemCache

  override val childElements: List<IDrawableProcessNode>


  var topPadding:Double

  var leftPadding: Double

  var bottomPadding: Double

  var rightPadding: Double


  fun getNode(nodeId: String): IDrawableProcessNode? = childElements.firstOrNull { it.id == nodeId }

  override fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>, clipBounds: Rectangle?) {
    //    updateBounds(); // don't use getBounds as that may force a layout. Don't do layout in draw code
    val childCanvas = canvas.childCanvas(0.0, 0.0, 1.0)
    val strategy = canvas.strategy

    val arcPen = canvas.theme.getPen(ProcessThemeItems.LINE, state)

    val con = itemCache.getPathList<S, PEN_T, PATH_T>(strategy, 0) {
      childElements.asSequence()
          .filter { it.x.isFinite() && it.y.isFinite() }
          .flatMap { start ->
            start.successors.asSequence().map { getNode(it.id) }.filterNotNull().filter {
              it.x.isFinite() && it.y.isFinite()
            }.map { end ->
              val x1 = start.bounds.right/*-STROKEWIDTH*/
              val y1 = start.y
              val x2 = end.bounds.left/*+STROKEWIDTH*/
              val y2 = end.y
              Connectors.getArrow(strategy, x1, y1, 0.0, x2, y2, Math.PI, arcPen)
            }
          }.toList()
    }

    for (path in con) {
      childCanvas.drawPath(path, arcPen)
    }

    for (node in childElements) {
      val b = node.bounds
      node.draw(childCanvas.childCanvas(b.left, b.top, 1.0), null)
    }

    for (node in childElements) {
      // TODO do something better with the left and top coordinates
      val b = node.bounds
      node.drawLabel(childCanvas.childCanvas(b.left, b.top, 1.0), null, node.x, node.y)
    }
  }

  override fun translate(dX: Double, dY: Double) {
    childElements.forEach {
      translate(dX, dY)
    }
  }

  override fun getItemAt(x: Double, y: Double): Drawable? {
    childElements.asSequence().mapNotNull { it.getItemAt(x,y) }.firstOrNull()?.let { return it }

    return if (isWithinBounds(x,y)) this else null
  }
}