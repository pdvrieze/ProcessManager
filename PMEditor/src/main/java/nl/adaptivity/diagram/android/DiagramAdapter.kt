/*
 * Copyright (c) 2016.
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

package nl.adaptivity.diagram.android

import android.graphics.RectF
import android.view.MotionEvent
import nl.adaptivity.diagram.Point
import nl.adaptivity.diagram.Theme

/**
 * A [DiagramAdapter] is responsible for providing the diagram to the diagramView.
 * @author Paul de Vrieze
 *
 * @param T The type of child view.
 * @param V The type of the actual child nodes.
 */
interface DiagramAdapter<T : LightView, V> {

    val count: Int

    val background: LightView

    val overlay: LightView?

    val theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>

    /**
     * Return a point that is to be the attractor for the element when considering the given coordinates as reference
     * @param element The element number
     * @param x The x coordinate of the reference point
     * @param y The y coordinate of the reference point
     * @return A point that is the reference
     */
    fun closestAttractor(element: Int, x: Double, y: Double): Point?

    fun getItem(position: Int): V

    fun getView(position: Int): T

    fun getRelativeDecorations(position: Int, scale: Double, selected: Boolean): List<RelativeLightView>

    fun getBounds(diagramBounds: RectF)

    fun onDecorationClick(view: DiagramView, position: Int, decoration: LightView)

    fun onDecorationMove(view: DiagramView, position: Int, decoration: RelativeLightView, x: Float, y: Float)

    fun onDecorationUp(view: DiagramView, position: Int, decoration: RelativeLightView, x: Float, y: Float)

    /** Called by a view to allow it to handle an event before any listeners.
     * @return `true` to stop propagation. `false` for unhandled events.
     */
    fun onNodeClickOverride(diagramView: DiagramView, touchedElement: Int, e: MotionEvent): Boolean

    fun getGravityX(pos: Int): Double

    fun getGravityY(pos: Int): Double

    /** Set the position of the given element. The coordinates are diagram coordinates.  */
    fun setPos(element: Int, diagx: Double, diagy: Double)
}
