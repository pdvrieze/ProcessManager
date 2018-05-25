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

package nl.adaptivity.process.editor.android


import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import nl.adaptivity.android.graphics.AbstractLightView
import nl.adaptivity.android.graphics.LineView
import nl.adaptivity.diagram.Bounded
import nl.adaptivity.diagram.Point
import nl.adaptivity.diagram.Theme
import nl.adaptivity.diagram.android.*
import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.diagram.ProcessThemeItems
import java.util.*


open class BaseProcessAdapter(val diagram: DrawableProcessModel.Builder) : DiagramAdapter<LWDrawableView, DrawableProcessNode.Builder<*>> {

    protected val mViewCache: MutableMap<DrawableProcessNode.Builder<*>, LWDrawableView> = HashMap()
    private var _background: LightView? = null
    
    private val bounds = RectF()
    
    var isInvalid = true
        private set
    
    private var _theme: AndroidTheme? = null

    override val theme: AndroidTheme
        get() {
            return _theme ?: AndroidTheme(AndroidStrategy.INSTANCE).also { _theme = it }
        }

    protected class ConnectorView(parent: BaseProcessAdapter) : AbstractLightView() {

        private var pen: Paint? = null
        private val bounds = RectF()
        private val diagram: DrawableProcessModel.Builder?

        init {
            parent.getBounds(bounds)
            diagram = parent.diagram
        }

        override fun getBounds(target: RectF) {
            target.set(bounds)
        }

        override fun draw(canvas: Canvas, theme: Theme<AndroidStrategy, AndroidPen, AndroidPath>, scale: Double) {
            if (diagram == null) {
                return
            }
            if (pen == null) {
                pen = theme.getPen(ProcessThemeItems.LINE, nl.adaptivity.diagram.Drawable.STATE_DEFAULT).paint
            }
            for (start in diagram.childElements.toList()) { // defensive copy
                if (!(java.lang.Double.isNaN(start.x) || java.lang.Double.isNaN(start.y))) {
                    for (endId in start.successors) {
                        val end = diagram.getNode(endId.id)
                        if (end != null && !(java.lang.Double.isNaN(end.x) || java.lang.Double.isNaN(end.y))) {
                            val x1 = ((start.bounds.right/*-DrawableProcessModel.STROKEWIDTH*/ - bounds.left) * scale).toFloat()
                            val y1 = ((start.y - bounds.top) * scale).toFloat()
                            val x2 = ((end.bounds.left/*+DrawableProcessModel.STROKEWIDTH*/ - bounds.left) * scale).toFloat()
                            val y2 = ((end.y - bounds.top) * scale).toFloat()
                            //              pCanvas.drawLine(x1, y1, x2, y2, mPen);
                            LineView.drawArrow(canvas, theme, x1, y1, x2, y2, scale)
                        }
                    }
                }
            }
        }

    }

    fun updateItem(pos: Int, newValue: DrawableProcessNode.Builder<*>) {
        // TODO do this better
        diagram.nodes[pos] = newValue
        invalidate()
    }

    override val count: Int get() = diagram.nodes.size

    override fun getItem(position: Int): DrawableProcessNode.Builder<*> {
        // TODO do this better
        return diagram.nodes[position] as DrawableProcessNode.Builder<*>
    }

    override fun getView(position: Int): LWDrawableView {
        val item = getItem(position)
        var result: LWDrawableView? = mViewCache[item]
        if (result != null) {
            return result
        }
        result = LWProcessDrawableView(item)
        mViewCache[item] = result
        return result
    }

    override fun getRelativeDecorations(position: Int, scale: Double, selected: Boolean): List<RelativeLightView> {
        return emptyList()
    }

    override val background: LightView 
        get() = _background ?: ConnectorView(this).also { _background = it }

    override val overlay: LightView?
        get() = null

    override fun getBounds(diagramBounds: RectF) {
        if (isInvalid) {
            val len = count
            if (len == 0) {
                diagramBounds.set(0f, 0f, 0f, 0f)
                return
            }
            var item: Bounded = getItem(0)
            var bounds = item.bounds
            this.bounds.set(bounds.left.toFloat(), bounds.top.toFloat(), bounds.rightf, bounds.bottomf)
            for (i in 1 until len) {
                item = getItem(i)
                bounds = item.bounds
                this.bounds.left = Math.min(this.bounds.left, bounds.leftf)
                this.bounds.top = Math.min(this.bounds.top, bounds.topf)
                this.bounds.right = Math.max(this.bounds.right, bounds.rightf)
                this.bounds.bottom = Math.max(this.bounds.bottom, bounds.bottomf)

            }
            isInvalid = false
        }
        diagramBounds.set(bounds)
    }

    override fun onDecorationClick(view: DiagramView, position: Int, decoration: LightView) {
        // ignore
    }

    override fun onDecorationMove(view: DiagramView, position: Int, decoration: RelativeLightView, x: Float, y: Float) {
        //ignore
    }

    override fun onDecorationUp(view: DiagramView, position: Int, decoration: RelativeLightView, x: Float, y: Float) {
        //ignore
    }

    override fun onNodeClickOverride(diagramView: DiagramView, touchedElement: Int, e: MotionEvent): Boolean {
        //ignore
        return false
    }

    override fun getGravityX(pos: Int): Double {
        return getItem(pos).x
    }

    override fun getGravityY(pos: Int): Double {
        return getItem(pos).y
    }

    override fun closestAttractor(element: Int, x: Double, y: Double): Point? {
        val node = getItem(element)
        var attrY = java.lang.Double.NaN
        var minDy = java.lang.Double.POSITIVE_INFINITY
        for (predId in (node.predecessors + node.successors)) {
            val pred = diagram.getNode(predId.id)
            val dy = Math.abs(pred!!.y - y)
            if (dy < minDy) {
                minDy = dy
                attrY = pred.y
            }
        }
        return if (!java.lang.Double.isNaN(attrY)) {
            Point(node.x, attrY)
        } else null
    }

    override fun setPos(element: Int, diagx: Double, diagy: Double) {
        val item = getItem(element)
        item.x = diagx
        item.y = diagy
    }


    fun invalidate() {
        isInvalid = true
    }

}