/*
 * Copyright (c) 2018.
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


interface ThemeItem {
    /**
     * Get the number for this theme item. Multiple items should never return the same
     * number. Conciseness will help though.
     * @return The item ordinal.
     */
    val itemNo: Int

    /**
     * Get the state that needs to be used for drawing the item at the given state. This allows
     * for optimization in caching.
     * @param The state needed.
     * @return The effective state.
     */
    fun getEffectiveState(state: Int): Int

    fun <PEN_T : Pen<PEN_T>> createPen(strategy: DrawingStrategy<*, PEN_T, *>, state: Int): PEN_T
}
