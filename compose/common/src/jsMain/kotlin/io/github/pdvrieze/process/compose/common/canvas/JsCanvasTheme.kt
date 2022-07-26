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

package io.github.pdvrieze.process.compose.common.canvas

import androidx.compose.ui.graphics.Color
import nl.adaptivity.diagram.Theme
import nl.adaptivity.diagram.ThemeItem
import nl.adaptivity.process.diagram.ProcessThemeItems


class JsCanvasTheme(private val strategy: JsCanvasStrategy) : Theme<JsCanvasStrategy, JsCanvasPen, JsCanvasPath> {
    private val pens: MutableList<MutableList<JsCanvasPen?>?>

    init {
        pens = mutableListOf()
    }

    override fun getPen(item: ThemeItem, state: Int): JsCanvasPen {
        val itemState = item.getEffectiveState(state)
        val themeState = overrideState(item, state, itemState)

        val statePens = pens.getOrSet(item.itemNo) { mutableListOf() }

        return statePens.getOrSet(themeState) {
            overrideTheme(item.createPen(strategy, itemState), item, themeState)
        }
    }

    companion object {

        private const val SHADE_STATE_MASK = nl.adaptivity.diagram.Drawable.STATE_SELECTED or nl.adaptivity.diagram.Drawable.STATE_TOUCHED
        const val SHADER_RADIUS = 8f

        val TOUCHED_SHADE_COLOR = Color(0xff, 0xec, 0x1a, 0xb0)
        val SELECTED_SHADE_COLOR = Color(23, 166, 255, 0xb0)

        /**
         * Override the state provided by the themeItem.
         * @param item The item for which to override the state.
         * @param state The state present.
         * @param itemState The effective state of the item from the item's perspective
         * @return
         */
        private fun overrideState(item: ThemeItem, state: Int, itemState: Int): Int {
            if (item is ProcessThemeItems) {
                when (item) {
                    ProcessThemeItems.BACKGROUND, ProcessThemeItems.ENDNODEOUTERLINE, ProcessThemeItems.LINEBG -> return itemState or (state and SHADE_STATE_MASK)
                    ProcessThemeItems.LINE                                                                     -> return itemState
                    else                                                                                       -> return itemState
                }
            }
            return itemState
        }

        /**
         * Add a method that allows the theme from PE-diagram to be overridden for android. The current purpose
         * is to enable blur shadows.
         * @param pen The pen to override.
         * @param item The item for which the pen is.
         * @param state The state of the item.
         * @return The overridden pen. Optimally this is actually the same pen passed in.
         */
        private fun overrideTheme(pen: JsCanvasPen, item: ThemeItem, state: Int): JsCanvasPen {
            if (item is ProcessThemeItems) {
                when (item) {
                    ProcessThemeItems.BACKGROUND, ProcessThemeItems.ENDNODEOUTERLINE, ProcessThemeItems.LINEBG -> {
                    }
                    ProcessThemeItems.LINE                                                                     -> return pen
                    else                                                                                       -> return pen
                }
                if (state and nl.adaptivity.diagram.Drawable.STATE_TOUCHED > 0) {
                    pen.setShadowLayer(SHADER_RADIUS, TOUCHED_SHADE_COLOR)
                } else if (state and nl.adaptivity.diagram.Drawable.STATE_SELECTED > 0) {
                    pen.setShadowLayer(SHADER_RADIUS, SELECTED_SHADE_COLOR)
                }
            }
            return pen
        }
    }

    private fun <T> List<T>.maybeGet(index: Int): T? = when {
        index< size -> get(index)
        else -> null
    }

    private fun <T> MutableList<T?>.getOrSet(index: Int, valueGen: () ->T) : T {
        if (index >= size) {
            return valueGen().also {
                while(size+1<index) { add(null) }
            }
        }

        return when (val currentValue = get(index)) {
            null -> valueGen().also { set(index, it) }
            else -> currentValue
        }
    }

}
