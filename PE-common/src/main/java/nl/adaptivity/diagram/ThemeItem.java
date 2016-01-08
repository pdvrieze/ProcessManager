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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.diagram;


public interface ThemeItem {
  /**
   * Get the number for this theme item. Multiple items should never return the same
   * number. Conciseness will help though.
   * @return The item ordinal.
   */
  int getItemNo();

  /**
   * Get the state that needs to be used for drawing the item at the given state. This allows
   * for optimization in caching.
   * @param The state needed.
   * @return The effective state.
   */
  int getEffectiveState(int state);

  <PEN_T extends Pen<PEN_T>> PEN_T createPen(DrawingStrategy<?, PEN_T, ?> strategy, int state);
}
