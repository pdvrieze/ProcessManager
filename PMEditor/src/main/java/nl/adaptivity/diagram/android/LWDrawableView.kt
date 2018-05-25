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

package nl.adaptivity.diagram.android

import android.graphics.Canvas
import android.graphics.RectF
import nl.adaptivity.diagram.Drawable
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.Theme


open class LWDrawableView(open val item: Drawable) : LightView {
    /** Cached canvas  */
    private var androidCanvas: AndroidCanvas? = null

    override var isFocussed: Boolean
        get() = item.state and Drawable.STATE_FOCUSSED != 0
        set(focussed) = if (focussed) {
            item.state = item.state or Drawable.STATE_FOCUSSED
        } else {
            item.state = item.state and Drawable.STATE_FOCUSSED.inv()
        }

    override var isSelected: Boolean
        get() = item.state and Drawable.STATE_SELECTED != 0
        set(selected) = if (selected) {
            item.state = item.state or Drawable.STATE_SELECTED
        } else {
            item.state = item.state and Drawable.STATE_SELECTED.inv()
        }

    override var isTouched: Boolean
        get() = item.state and Drawable.STATE_TOUCHED != 0
        set(touched) = if (touched) {
            item.state = item.state or Drawable.STATE_TOUCHED
        } else {
            item.state = item.state and Drawable.STATE_TOUCHED.inv()
        }

    override var isActive: Boolean
        get() = item.state and Drawable.STATE_ACTIVE != 0
        set(active) = if (active) {
            item.state = item.state or Drawable.STATE_ACTIVE
        } else {
            item.state = item.state and Drawable.STATE_ACTIVE.inv()
        }

    override fun getBounds(rect: RectF) {
        val bounds = item.bounds
        rect.set(bounds.leftf, bounds.topf, bounds.rightf, bounds.bottomf)
    }


    /**
     * Craw this drawable onto an android canvas. The canvas has an ofset
     * preapplied so the top left of the drawing is 0,0. This method itself,
     * just creates the canvas and passes the work to [.onDraw]
     */
    override fun draw(canvas: Canvas, theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>, scale: Double) {
        if (androidCanvas == null) {
            androidCanvas = AndroidCanvas(canvas, theme)
        } else {
            androidCanvas!!.setCanvas(canvas)
        }
        onDraw(androidCanvas!!.scale(scale), null)
    }

    protected open fun onDraw(androidCanvas: IAndroidCanvas, clipBounds: Rectangle?) {
        item.draw(androidCanvas, clipBounds)
    }
}
