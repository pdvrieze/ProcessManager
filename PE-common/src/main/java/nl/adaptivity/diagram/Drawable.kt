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

package nl.adaptivity.diagram


interface Drawable : Bounded, Cloneable {

  /**
   * The current state of the drawable. Individual implementations should specify what each state value means.
   * The `0` value however means the default.
   */
  var state: Int


  /**
   * Draw the drawable to the given canvas. The drawing will use a top left of (0,0).
   * The canvas will translate coordinates.
   * @param canvas The canvas to draw on.
   * @param clipBounds The part of the drawing to draw. Outside no drawing is needed.
   */
  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> draw(canvas: Canvas<S, PEN_T, PATH_T>,
                                                                                                     clipBounds: Rectangle?)

  /**
   * Override the definition of [Object.clone] to ensure the right
   * return type and make it public.
   *
   * @return A copy.
   */
  public override fun clone(): Drawable


  fun translate(dX: Double, dY: Double)

  fun setPos(left: Double, top: Double)

  override fun getItemAt(x: Double, y: Double): Drawable?

  companion object {

    const val STATE_DEFAULT = 0x0
    const val STATE_TOUCHED = 0x1
    const val STATE_SELECTED = 0x2
    const val STATE_FOCUSSED = 0x4
    const val STATE_DISABLED = 0x8
    const val STATE_CUSTOM1 = 0x10
    const val STATE_CUSTOM2 = 0x20
    const val STATE_CUSTOM3 = 0x40
    const val STATE_CUSTOM4 = 0x80
    const val STATE_DRAG = 0x100
    const val STATE_ACTIVE = 0x200
    const val STATE_MASK = 0xffff
  }
}
