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

import android.graphics.Paint
import nl.adaptivity.diagram.DrawingStrategy

enum class AndroidStrategy : DrawingStrategy<AndroidStrategy, AndroidPen, AndroidPath> {
    INSTANCE;

    override fun newPen(): AndroidPen {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.SQUARE
            isAntiAlias = true
        }
        return AndroidPen(paint)
    }

    override fun newPath(): AndroidPath {
        return AndroidPath()
    }

}