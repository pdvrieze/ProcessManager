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

import net.devrieze.util.hasFlag
import nl.adaptivity.diagram.Drawable.Companion.STATE_CUSTOM1
import nl.adaptivity.diagram.Drawable.Companion.STATE_CUSTOM2
import nl.adaptivity.diagram.Drawable.Companion.STATE_CUSTOM3
import nl.adaptivity.diagram.Drawable.Companion.STATE_CUSTOM4
import nl.adaptivity.diagram.Drawable.Companion.STATE_DEFAULT
import nl.adaptivity.diagram.Drawable.Companion.STATE_SELECTED
import nl.adaptivity.diagram.Drawable.Companion.STATE_TOUCHED
import nl.adaptivity.diagram.DrawingStrategy
import nl.adaptivity.diagram.Pen
import nl.adaptivity.diagram.ThemeItem

enum class ProcessThemeItems(
    private var fill: Boolean = false,
    private var parent: ProcessThemeItems? = null,
    private var stroke: Double = 0.0,
    private var fontSize: Double = Double.NaN,
    vararg private var specifiers: StateSpecifier) : ThemeItem {

  LINE(RootDrawableProcessModel.STROKEWIDTH, state(STATE_DEFAULT, 0, 0, 0),
       stateStroke(STATE_SELECTED, 0, 0, 255, 255, 2.0),
       stateStroke(STATE_TOUCHED, 255, 255, 0, 127, 7.0),
       state(STATE_CUSTOM1, 0, 0, 255),
       state(STATE_CUSTOM2, 255, 255, 0),
       state(STATE_CUSTOM3, 255, 0, 0),
       state(STATE_CUSTOM4, 0, 255, 0)) {

    override fun getEffectiveState(state: Int): Int {
      if (state hasFlag STATE_TOUCHED) return STATE_TOUCHED

      return effectiveStateHelper(state).ifInvalid(super.getEffectiveState(state))
    }

  },

  INNERLINE(RootDrawableProcessModel.STROKEWIDTH * 0.85, state(STATE_DEFAULT, 0, 0, 0x20, 0xb0)),

  BACKGROUND(state(STATE_DEFAULT, 255, 255, 255)) {

    override fun getEffectiveState(state: Int) = STATE_DEFAULT

  },
  ENDNODEOUTERLINE(RootDrawableProcessModel.ENDNODEOUTERSTROKEWIDTH, LINE),

  LINEBG(LINE),

  DIAGRAMTEXT(RootDrawableProcessModel.STROKEWIDTH, RootDrawableProcessModel.DIAGRAMTEXT_SIZE,
              state(STATE_DEFAULT, 0, 0, 0)),

  DIAGRAMLABEL(RootDrawableProcessModel.STROKEWIDTH, RootDrawableProcessModel.DIAGRAMLABEL_SIZE,
               state(STATE_DEFAULT, 0, 0, 0));

  constructor(stroke: Double, parent: ProcessThemeItems) : this(false, parent = parent, stroke = stroke)

  constructor(parent: ProcessThemeItems) : this(fill = true, parent = parent)

  constructor(stroke: Double, vararg specifiers: StateSpecifier) : this(fill=false, stroke = stroke, specifiers = *specifiers)

  constructor(stroke: Double, fontSize: Double, vararg specifiers: StateSpecifier) :
    this(fill = true, stroke = stroke, fontSize = fontSize, specifiers = *specifiers)

  constructor(vararg specifiers: StateSpecifier) : this(fill = true, specifiers = *specifiers)

  override val itemNo: Int get() = ordinal

  override fun getEffectiveState(state: Int): Int {
    parent?.let { return it.getEffectiveState(state) }

    return effectiveStateHelper(state).ifInvalid(state)
  }

  internal fun effectiveStateHelper(state: Int): Int {
    return when {
      state hasFlag STATE_CUSTOM1 -> STATE_CUSTOM1
      state hasFlag STATE_CUSTOM2 -> STATE_CUSTOM2
      state hasFlag STATE_CUSTOM3 -> STATE_CUSTOM3
      state hasFlag STATE_CUSTOM4 -> STATE_CUSTOM4
      else                        -> -1
    }
  }

  override fun <PEN_T : Pen<PEN_T>> createPen(strategy: DrawingStrategy<*, PEN_T, *>, state: Int): PEN_T {
    val specifier = getSpecifier(state)
    val result = with(specifier) { strategy.newPen().setColor(red, green, blue, alpha) }

    if (!fill) {
      val stroke = when {
        stroke > 0.0 -> stroke
        else         -> parent?.stroke ?: stroke
      }
      result.setStrokeWidth(stroke * specifier.strokeMultiplier)
    }

    if (fontSize.isFinite()) result.setFontSize(fontSize)

    return result
  }


  private fun getSpecifier(state: Int): StateSpecifier {
    parent?.apply { return getSpecifier(state) }

    specifiers.firstOrNull { it.state == state }?.let { return it }

    return specifiers.reduce { a, b ->
      when {
        b.state hasFlag state && b.state > a.state -> b
        else -> a
      }
    }
  }

}

private open class StateSpecifier(val state: Int, val red: Int, val green: Int, val blue: Int, val alpha: Int) {

  open val strokeMultiplier: Double get() = 1.0

}

private class StrokeStateSpecifier(state: Int, r: Int, g: Int, b: Int, a: Int, override val strokeMultiplier: Double) :
  StateSpecifier(state, r, g, b, a)

@Suppress("NOTHING_TO_INLINE")
private inline fun stateStroke(state: Int, r: Int, g: Int, b: Int, a: Int, strokeMultiplier: Double)
  = StrokeStateSpecifier(state, r, g, b, a, strokeMultiplier)

@Suppress("NOTHING_TO_INLINE")
private inline fun state(state: Int, r: Int, g: Int, b: Int) = StateSpecifier(state, r, g, b, 255)

// This method can be useful when colors with alpha are desired.
@Suppress("NOTHING_TO_INLINE")
private inline fun state(state: Int, r: Int, g: Int, b: Int, a: Int) = StateSpecifier(state, r, g, b, a)

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.ifInvalid(alternate:Int): Int = if (this >= 0) this else alternate