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

import android.graphics.Bitmap


interface IAndroidCanvas : nl.adaptivity.diagram.Canvas<AndroidStrategy, AndroidPen, AndroidPath> {
    fun scale(scale: Double): IAndroidCanvas
    fun translate(dx: Double, dy: Double): IAndroidCanvas
    override fun childCanvas(offsetX: Double, offsetY: Double, scale: Double): IAndroidCanvas

    fun drawBitmap(left: Double, top: Double, bitmap: Bitmap, pen: AndroidPen)
}
