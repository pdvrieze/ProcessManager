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

package nl.adaptivity.process.editor.android

import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.diagram.android.IAndroidCanvas
import nl.adaptivity.diagram.android.LWDrawableView
import nl.adaptivity.process.diagram.DrawableProcessNode


/**
 * A lightweight drawable view that adds label drawing for process nodes.
 * Created by pdvrieze on 10/01/16.
 */
class LWProcessDrawableView(item: DrawableProcessNode.Builder<*>) : LWDrawableView(item) {
    override val item get() = super.item as DrawableProcessNode.Builder<*>

    override fun onDraw(androidCanvas: IAndroidCanvas, clipBounds: Rectangle?) {
        super.onDraw(androidCanvas, clipBounds)
        item.drawLabel(androidCanvas, clipBounds, 0.0, 0.0)
    }

}
