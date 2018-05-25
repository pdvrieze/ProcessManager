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

import android.app.Activity
import android.app.DialogFragment
import android.content.Context
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import nl.adaptivity.android.graphics.BackgroundDrawable
import nl.adaptivity.android.graphics.LineView
import nl.adaptivity.diagram.android.AndroidDrawableLightView
import nl.adaptivity.diagram.android.DiagramView
import nl.adaptivity.diagram.android.LightView
import nl.adaptivity.diagram.android.RelativeLightView
import nl.adaptivity.diagram.android.RelativeLightView.BOTTOM
import nl.adaptivity.diagram.android.RelativeLightView.HGRAVITY
import nl.adaptivity.process.diagram.*
import nl.adaptivity.process.processModel.*
import java.util.*

/**
 * The MyDiagramAdapter to use for the editor.
 * @author Paul de Vrieze
 */
class MyDiagramAdapter(private val context: Context, diagram: DrawableProcessModel.Builder) :
    BaseProcessAdapter(diagram) {

    override var overlay: LightView? = null
    private val cachedDecorations = arrayOfNulls<RelativeLightView>(3)
    private val cachedStartDecorations = arrayOfNulls<RelativeLightView>(2)
    private val cachedEndDecorations = arrayOfNulls<RelativeLightView>(1)
    private var cachedDecorationItem: DrawableProcessNode.Builder<*>? = null
    private var connectingItem = -1

    override fun getRelativeDecorations(position: Int, scale: Double, selected: Boolean): List<RelativeLightView> {
        if (!selected) {
            return emptyList()
        }

        val drawableProcessNode = getItem(position)

        val decorations: Array<RelativeLightView>
        if (drawableProcessNode is StartNode.Builder<*, *>) {
            decorations = getStartDecorations(drawableProcessNode, scale)
        } else if (drawableProcessNode is EndNode.Builder<*, *>) {
            decorations = getEndDecorations(drawableProcessNode, scale)
        } else {
            decorations = getDefaultDecorations(drawableProcessNode, scale)
        }

        val centerX = drawableProcessNode.x
        val topY = drawableProcessNode.bounds.bottom + DECORATION_VSPACING / scale
        layoutHorizontal(centerX, topY, scale, decorations)
        return Arrays.asList(*decorations)
    }

    private fun getDefaultDecorations(item: DrawableProcessNode.Builder<*>, scale: Double): Array<RelativeLightView> {
        if (item != cachedDecorationItem) {
            cachedDecorationItem = item
            cachedDecorations[0] = RelativeLightView(
                AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM or HGRAVITY)
            cachedDecorations[1] = RelativeLightView(
                AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_edit), scale), BOTTOM or HGRAVITY)
            cachedDecorations[2] = RelativeLightView(
                AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), scale), BOTTOM or HGRAVITY)
        }
        @Suppress("UNCHECKED_CAST")
        return cachedDecorations as Array<RelativeLightView>
    }

    private fun getStartDecorations(item: DrawableProcessNode.Builder<*>, scale: Double): Array<RelativeLightView> {
        if (item != cachedDecorationItem) {
            cachedDecorationItem = item
            // Assign to both caches to allow click to remain working.
            cachedStartDecorations[0] = RelativeLightView(
                AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM or HGRAVITY)
            cachedDecorations[0] = cachedStartDecorations[0]
            cachedStartDecorations[1] = RelativeLightView(
                AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), scale), BOTTOM or HGRAVITY)
            cachedDecorations[2] = cachedStartDecorations[1]
        }
        @Suppress("UNCHECKED_CAST")
        return cachedStartDecorations as Array<RelativeLightView>
    }

    private fun getEndDecorations(item: DrawableProcessNode.Builder<*>, scale: Double): Array<RelativeLightView> {
        if (item != cachedDecorationItem) {
            cachedDecorationItem = item
            // Assign to both caches to allow click to remain working.
            cachedEndDecorations[0] = RelativeLightView(
                AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM or HGRAVITY)
            cachedDecorations[0] = cachedEndDecorations[0]
        }
        @Suppress("UNCHECKED_CAST")
        return cachedEndDecorations as Array<RelativeLightView>
    }

    private fun loadDrawable(resId: Int): Drawable {
        // TODO get the button drawable out of the style.
        return BackgroundDrawable(context, R.drawable.btn_context, resId)
    }

    fun notifyDatasetChanged() {
        invalidate()
    }

    override fun onDecorationClick(view: DiagramView, position: Int, decoration: LightView) {
        if (decoration === cachedDecorations[0]) {
            removeNode(position)
            view.invalidate()
        } else if (decoration === cachedDecorations[1]) {
            doEditNode(position)
        } else if (decoration === cachedDecorations[2]) {
            if (overlay is LineView) {
                view.invalidate(overlay!!)
                overlay = null
            } else {
                decoration.isActive = true
                connectingItem = position
            }
        }
    }

    private fun removeNode(position: Int) {
        val item = getItem(position)
        mViewCache.remove(item)
        diagram.nodes.remove(item)
        if (item == cachedDecorationItem) {
            cachedDecorationItem = null
        }
    }

    private fun doEditNode(position: Int) {
        if (context is Activity) {
            val node = diagram.childElements.get(position)
            val fragment: DialogFragment
            if (node is DrawableJoinSplit) {
                fragment = JoinSplitNodeEditDialogFragment.newInstance(position)
            } else if (node is DrawableActivity) {
                fragment = ActivityEditDialogFragment.newInstance(position)
            } else {
                fragment = NodeEditDialogFragment.newInstance(position)
            }
            fragment.show(context.fragmentManager, "editNode")
        }
    }

    override fun onDecorationMove(view: DiagramView, position: Int, decoration: RelativeLightView, x: Float, y: Float) {
        if (decoration === cachedDecorations[2]) {
            val start = cachedDecorationItem
            val x1 = (start!!.bounds.right - RootDrawableProcessModel.STROKEWIDTH).toFloat()
            val y1 = start.y.toFloat()

            if (overlay is LineView) {
                view.invalidate(overlay!!) // invalidate both old
                (overlay as LineView).setPos(x1, y1, x, y)
            } else {
                overlay = LineView(x1, y1, x, y)
            }
            view.invalidate(overlay!!) // and new bounds
        }
    }

    override fun onDecorationUp(view: DiagramView,
                                position: Int,
                                decoration: RelativeLightView,
                                x: Float,
                                y: Float) {
        if (decoration === cachedDecorations[2]) {
            if (overlay is LineView) {
                view.invalidate(overlay!!)
                overlay = null
            }

            var next: DrawableProcessNode.Builder<*>? = null
            for (item in diagram.childElements) {
                if (item.getItemAt(x.toDouble(), y.toDouble()) != null) {
                    next = item
                    break
                }
            }
            if (next != null) {
                tryAddSuccessor(getItem(position), next)
            }
        }
    }

    override fun onNodeClickOverride(diagramView: DiagramView, touchedElement: Int, e: MotionEvent): Boolean {
        if (connectingItem >= 0) {
            val prev = getItem(connectingItem)
            val next = getItem(touchedElement)
            tryAddSuccessor(prev, next)
            connectingItem = -1
            cachedDecorations[2]!!.isActive = false
            diagramView.invalidate()
            return true
        }
        return false
    }

    fun tryAddSuccessor(prev: DrawableProcessNode.Builder<DrawableProcessNode>, next: DrawableProcessNode.Builder<*>) {
        if (prev.successors.any { it.id == next.id }) {
            prev.removeSuccessor(next)
        } else {

            if (prev.successors.size < prev.maxSuccessorCount && next.predecessors.size < next.maxPredecessorCount) {
                try {
                    if (prev is Split<*, *>) {
                        val split = prev as Split<*, *>
                        if (split.min >= split.max) {
                            split.min = split.min + 1
                        }
                        if (split.max >= prev.successors.size) {
                            split.max = split.max + 1
                        }
                    }
                    if (next is Join<*, *>) {
                        val join = next as Join<*, *>
                        if (join.min >= join.max) {
                            join.min = join.min + 1
                        }
                        if (join.max >= prev.predecessors.size) {
                            join.max = join.max + 1
                        }
                    }
                    prev.addSuccessor(next.identifier!!)
                } catch (e: IllegalProcessModelException) {
                    Log.w(MyDiagramAdapter::class.java.name, e.message, e)
                    Toast.makeText(context, "These can not be connected", Toast.LENGTH_LONG).show()
                }

            } else {
                Toast.makeText(context, "These can not be connected", Toast.LENGTH_LONG).show()
                // TODO Better errors
            }
        }
    }

    companion object {


        private val DECORATION_VSPACING = 12.0
        private val DECORATION_HSPACING = 12.0

        private fun layoutHorizontal(centerX: Double,
                                     top: Double,
                                     scale: Double,
                                     decorations: Array<RelativeLightView>) {
            if (decorations.size == 0) {
                return
            }
            val hspacing = DECORATION_HSPACING / scale
            var totalWidth = -hspacing

            val bounds = RectF()
            for (decoration in decorations) {
                decoration.getBounds(bounds)
                totalWidth += bounds.width() + hspacing
            }
            var leftF = (centerX - totalWidth / 2).toFloat()
            val topF = top.toFloat()
            for (decoration in decorations) {
                decoration.setPos(leftF, topF)
                decoration.getBounds(bounds)
                leftF += (bounds.width() + hspacing).toFloat()
            }
        }
    }

}
