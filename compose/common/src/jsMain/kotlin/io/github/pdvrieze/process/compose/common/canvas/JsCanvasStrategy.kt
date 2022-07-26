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

package io.github.pdvrieze.process.compose.common.canvas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import nl.adaptivity.diagram.DrawingStrategy
import org.w3c.dom.CanvasRenderingContext2D

class JsCanvasStrategy(private val context2D: CanvasRenderingContext2D) : DrawingStrategy<JsCanvasStrategy, JsCanvasPen, JsCanvasPath> {

    override fun newPen(): JsCanvasPen {
        return JsCanvasPen(context2D)
    }

    override fun newPath(): JsCanvasPath {
        return JsCanvasPath()
    }

}
