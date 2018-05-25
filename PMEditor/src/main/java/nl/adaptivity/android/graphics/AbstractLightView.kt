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

package nl.adaptivity.android.graphics


import nl.adaptivity.diagram.Drawable
import nl.adaptivity.diagram.android.LightView


abstract class AbstractLightView : LightView {
    private var state = 0

    override var isFocussed: Boolean
        get() = hasState(Drawable.STATE_FOCUSSED)
        set(focussed) {
            setState(Drawable.STATE_FOCUSSED, focussed)
            state = state or Drawable.STATE_FOCUSSED
        }

    override var isSelected: Boolean
        get() = hasState(Drawable.STATE_SELECTED)
        set(selected) {
            setState(Drawable.STATE_SELECTED, selected)
            state = state or Drawable.STATE_SELECTED
        }

    override var isTouched: Boolean
        get() = hasState(Drawable.STATE_TOUCHED)
        set(touched) {
            setState(Drawable.STATE_TOUCHED, touched)
            state = state or Drawable.STATE_TOUCHED
        }

    override var isActive: Boolean
        get() = hasState(Drawable.STATE_ACTIVE)
        set(active) {
            setState(Drawable.STATE_ACTIVE, active)
            state = state or Drawable.STATE_ACTIVE
        }

    protected fun setState(flag: Int, isFlagged: Boolean) {
        if (isFlagged) {
            state = state or flag
        } else {
            state = state and flag.inv()
        }
    }

    protected fun hasState(state: Int): Boolean {
        return this.state and state != 0
    }

}