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

package nl.adaptivity.process.diagram

import net.devrieze.util.collection.maxIndex
import nl.adaptivity.diagram.Positioned
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


open class LayoutAlgorithm {

    object NullAlgorithm : LayoutAlgorithm() {
        override fun <T : Positioned> layout(nodes: List<DiagramNode<T>>, layoutStepper: LayoutStepper<T>): Boolean {
            return false
        }

    }

    var vertSeparation = RootDrawableProcessModel.DEFAULT_VERT_SEPARATION

    var horizSeparation = RootDrawableProcessModel.DEFAULT_HORIZ_SEPARATION

    var minHorizSeparation = RootDrawableProcessModel.DEFAULT_HORIZ_SEPARATION - TOLERANCE
    var minVertSeparation = RootDrawableProcessModel.DEFAULT_VERT_SEPARATION - TOLERANCE

    var defaultNodeWidth = 30.0
    var defaultNodeHeight = 30.0

    var gridSize = Double.NaN

    var isTighten = false

    /**
     * Layout the given nodes
     * @param nodes The nodes to layout
     *
     * @return Whether the nodes have changed.
     */
    open fun <T : Positioned> layout(nodes: List<DiagramNode<T>>,
                                     layoutStepper: LayoutStepper<T> = AbstractLayoutStepper()): Boolean {
        var changed = false
        var bottomY = Double.NEGATIVE_INFINITY
        val instance = Instance(layoutStepper)
        for (partition in partition(nodes)) {
            changed = layoutPartition<T>(partition, bottomY, layoutStepper) || changed
            bottomY = with(instance) { partition.getBottomY(vertSeparation) }
        }
        // TODO if needed, lay out single element partitions differently.
        return changed
    }

    open fun <T : Positioned> layoutPartition(nodes: List<DiagramNode<T>>,
                                              topY: Double,
                                              layoutStepper: LayoutStepper<T>): Boolean {
        return Instance(layoutStepper).layoutPartition(nodes, topY)
    }

    private inner class Instance<T : Positioned>(val layoutStepper: LayoutStepper<T>) {

        val Iterable<DiagramNode<T>>.minX get() = leftMost()?.left
        val Iterable<DiagramNode<T>>.maxX get() = rightMost()?.right

        val Iterable<DiagramNode<T>>.minY get() = highest()?.top
        val Iterable<DiagramNode<T>>.maxY get() = lowest()?.bottom

        /**
         * The horizontal exclusion range of the node
         */
        val DiagramNode<*>.horizExRange get() = (left - minHorizSeparation)..(right + minHorizSeparation)

        /**
         * The vertical exclusion range of the node
         */
        val DiagramNode<*>.vertExRange get() = (top - minVertSeparation)..(bottom + minVertSeparation)

        private val DiagramNode<T>.preceedingSiblings: List<DiagramNode<T>>
            get() = getSiblings(this, true)

        private val DiagramNode<T>.followingSiblings: List<DiagramNode<T>>
            get() = getSiblings(this, false)

        /**
         * Layout the given nodes
         * @param nodes The nodes to layout
         *
         * @return Whether the nodes have changed.
         */
        fun layout(nodes: List<DiagramNode<T>>): Boolean {
            var changed = false
            var bottomY = Double.NEGATIVE_INFINITY
            for (partition in partition(nodes)) {
                changed = layoutPartition(partition, bottomY) || changed
                bottomY = partition.getBottomY(vertSeparation)
            }
            // TODO if needed, lay out single element partitions differently.
            return changed
        }

        fun layoutPartition(nodes: List<DiagramNode<T>>, topY: Double): Boolean {
            var changed = false
            for (node in nodes) {
                if (!node.hasValidPosition) {
                    changed = layoutNodeInitial(nodes, node,
                                                topY) or changed // always force as that should be slightly more efficient
                }
                changed = changed || (!node.target.hasValidPosition)
            }

            if (!changed) {
                changed = if (isTighten) {
                    nodes.tightenPositions()
                } else {
                    nodes.verifyPositions()
                }
            }

            if (isTighten || changed) {
                val nodeListCopy = nodes.toList()
                var nodesChanged = true
                var pass = 0
                while (nodesChanged && pass < PASSCOUNT) {
                    layoutStepper.reportPass(pass)
                    nodesChanged = false
                    if (pass % 2 == 0) {
                        //          Collections.sort(nodes, LEFT_TO_RIGHT);
                        for (node in nodeListCopy) {
                            if (node.leftNodes.isEmpty()) {
                                nodesChanged = nodesChanged or layoutNodeRight(nodes, node, pass)
                            }
                        }
                    } else {
                        //          Collections.sort(nodes, RIGHT_TO_LEFT);
                        for (node in nodeListCopy) {
                            if (node.rightNodes.isEmpty()) {
                                nodesChanged = nodesChanged or layoutNodeLeft(nodes, node, pass)
                            }
                        }
                    }
                    ++pass
                }
                changed = changed or nodesChanged
            }
            if (changed) {
                val newMinX = nodes.minX ?: 0.0
                val newMinY = nodes.minY ?: 0.0

                val offsetX = 0 - newMinX
                val offsetY = 0 - newMinY

                if (abs(offsetX) > TOLERANCE || abs(offsetY) > TOLERANCE) {
                    for (node in nodes) {
                        node.x += offsetX
                        node.y += offsetY
                    }
                }
            }
            return changed
        }

        /**
         * @param nodes The nodes in the diagram that could be layed out.
         *
         * @param baseNode The node to focus on.
         */
        private fun layoutNodeInitial(nodes: List<DiagramNode<T>>,
                                      baseNode: DiagramNode<T>,
                                      minY: Double): Boolean {
            val leftNodes = baseNode.leftNodes
            val aboveNodes = baseNode.preceedingSiblings

            val origX = baseNode.x // store the initial coordinates
            val origY = baseNode.y

            // set temporary coordinates to prevent infinite recursion
            val x: Double
            val y: Double

            // Ensure that both the leftNodes and aboveNodes have set coordinates.
            (leftNodes.asSequence() + aboveNodes.asSequence())
                .filter { !it.hasValidPosition }
                .forEach { layoutNodeInitial(nodes, it, minY) }


            val newMinY = maxOf(aboveNodes.getBottomY(vertSeparation + baseNode.topExtent), minY + baseNode.topExtent)
            val newMinX = leftNodes.getMinX(horizSeparation + baseNode.leftExtent)

            if (leftNodes.isEmpty()) {
                x = if (aboveNodes.isEmpty()) baseNode.leftExtent else averageX(aboveNodes)
                y = if (aboveNodes.isEmpty()) baseNode.topExtent else newMinY
            } else { // leftPoints not empty, minX must be set
                x = newMinX
                y = max(newMinY, averageY(leftNodes))
            }

            //    if (Double.isNaN(x)) { x = 0d; }
            //    if (Double.isNaN(y)) { y = 0d; }
            val xChanged = changed(x, origX, TOLERANCE)
            val yChanged = changed(y, origY, TOLERANCE)
            if (yChanged || xChanged) {
                layoutStepper.reportMove(baseNode, x, y)
                //      System.err.println("Moving node "+pNode.getTarget()+ "to ("+x+", "+y+')');
                baseNode.x = x
                baseNode.y = y
                return true
            }
            return false
        }

        private fun List<DiagramNode<T>>.tightenPositions(): Boolean {
            val minX = getValidLeftBound(this, 0.0)
            var minY = getValidTopBound(this, 0.0)

            var changed = false
            for (partition in partition(this)) {
                changed = tightenPartitionPositions(partition, minX, minY) or changed
                minY = partition.getBottomY(vertSeparation) // put the partitions below each other
            }
            return changed
        }

        /**
         * Get a minX value that is a valid number (not NaN or infinity). The fallback is 0.
         * @param nodes The nodes to try to find the mimimum of.
         *
         * @return The minimum left of all the nodes, or 0 if none exists.
         */
        protected fun getValidLeftBound(nodes: List<DiagramNode<T>>,
                                        fallback: Double): Double {
            val minX = left(nodes.leftMost(), fallback)
            layoutStepper.reportMinX(nodes, minX)
            return minX
        }

        /**
         * Get a minX value that is a valid number (not NaN or infinity). The fallback is 0.
         * @param nodes The nodes to try to find the mimimum of.
         *
         * @return The minimum left of all the nodes, or 0 if none exists.
         */
        protected fun getValidTopBound(nodes: List<DiagramNode<T>>,
                                       fallback: Double): Double {
            val minY = top(nodes.highest(), fallback)
            layoutStepper.reportMinY(nodes, minY)
            return minY
        }

        private fun tightenPartitionPositions(nodes: List<DiagramNode<T>>,
                                              leftMostPos: Double,
                                              minY: Double): Boolean {
            var leftMostPos = leftMostPos
            var leftMostNode: Int
            val len = nodes.size
            val minXs = DoubleArray(len)
            unset(minXs)
            val maxXs = DoubleArray(len)
            unset(maxXs)
            val newYs = DoubleArray(len)
            unset(newYs)
            run {
                leftMostNode = -1
                leftMostPos = Double.MAX_VALUE
                for (i in 0..len - 1) {
                    val node = nodes[i]
                    if (node.leftNodes.isEmpty() && node.left < leftMostPos) {
                        leftMostNode = i
                        leftMostPos = node.left
                    }
                }
                if (leftMostNode >= 0) {
                    minXs[leftMostNode] = nodes[leftMostNode].x
                    maxXs[leftMostNode] = minXs[leftMostNode]
                }
                for (node in nodes[leftMostNode].rightNodes) {
                    val newX = minXs[leftMostNode] + nodes[leftMostNode].rightExtent + horizSeparation + node.leftExtent
                    layoutStepper.reportLayoutNode(node)
                    layoutStepper.reportMinX(listOf(nodes[leftMostNode]), newX)
                    updateXPosLR(newX, node, nodes, minXs)
                }
            }
            val mostRightNodePos = minXs.maxIndex()
            val mostRightNode = nodes[mostRightNodePos]
            maxXs[mostRightNodePos] = minXs[mostRightNodePos]
            val changed: Boolean
            if (abs(mostRightNode.x - minXs[mostRightNodePos]) > TOLERANCE) {
                changed = true
                layoutStepper.reportLayoutNode(mostRightNode)
                layoutStepper.reportMove(mostRightNode, minXs[mostRightNodePos], mostRightNode.y)
                nodes[mostRightNodePos].x = minXs[mostRightNodePos]
            } else {
                changed = false
            }
            for (node in nodes[mostRightNodePos].leftNodes) {
                val newX = minXs[mostRightNodePos] - nodes[mostRightNodePos].leftExtent - horizSeparation - node.rightExtent
                layoutStepper.reportLayoutNode(node)
                layoutStepper.reportMaxX(listOf(nodes[mostRightNodePos]), newX)
                updateXPosRL(newX, node, nodes, minXs, maxXs)
            }
            return updateXPos(nodes, minXs, maxXs) || changed
        }

        private fun updateXPosLR(newX: Double,
                                 node: DiagramNode<T>,
                                 nodes: List<DiagramNode<T>>,
                                 newXs: DoubleArray) {
            val len = nodes.size
            for (i in 0..len - 1) {
                if (node == nodes[i]) {
                    val oldX = newXs[i]
                    if (newX > oldX) { // Use the negative way to handle NaN, don't go on when there is already a value that wasn't changed.
                        if (!newXs[i].isNaN()) {
                            layoutStepper.reportMaxX(listOf(node), newXs[i])
                        }
                        newXs[i] = newX
                        //          mLayoutStepper.reportMove(pNode, newXs[i], pNode.getY());
                        for (rightNode in node.rightNodes) {
                            val updatedNewX = newX + node.rightExtent + horizSeparation + rightNode.leftExtent
                            layoutStepper.reportLayoutNode(rightNode)
                            layoutStepper.reportMinX(listOf(node), updatedNewX)
                            updateXPosLR(updatedNewX, rightNode, nodes, newXs)
                        }
                    } // ignore the rest
                    break
                }
            }
        }

        private fun updateXPosRL(maxX: Double,
                                 node: DiagramNode<T>,
                                 nodes: List<DiagramNode<T>>,
                                 minXs: DoubleArray,
                                 maxXs: DoubleArray) {
            val len = nodes.size
            for (i in 0..len - 1) { // loop to find the node position
                if (node == nodes[i]) { // found the position, now use stuff
                    layoutStepper.reportMinX(listOf(node), minXs[i])
                    if (maxXs[i].isNaN() || maxXs[i] - TOLERANCE > maxX) {
                        maxXs[i] = maxX
                        for (leftNode in node.leftNodes) {
                            val newX = maxX - node.leftExtent - horizSeparation - leftNode.rightExtent
                            layoutStepper.reportLayoutNode(leftNode)
                            layoutStepper.reportMaxX(listOf(node), newX)
                            updateXPosRL(newX, leftNode, nodes, minXs, maxXs)
                        }
                    }
                    break
                }
            }
        }

        private fun updateXPos(nodes: List<DiagramNode<T>>,
                               minXs: DoubleArray,
                               maxXs: DoubleArray): Boolean {
            var changed = false
            val len = nodes.size
            for (i in 0..len - 1) {
                val node = nodes[i]
                val minX = minXs[i]
                val maxX = maxXs[i]
                layoutStepper.reportLayoutNode(node)
                layoutStepper.reportMinX(emptyList<DiagramNode<T>>(), minX)
                layoutStepper.reportMaxX(emptyList<DiagramNode<T>>(), maxX)
                val x = node.x
                if (x + TOLERANCE < minX) {
                    layoutStepper.reportMove(node, minX, node.y)
                    changed = true
                    node.x = minX
                } else if (x - TOLERANCE > maxX) {
                    layoutStepper.reportMove(node, maxX, node.y)
                    changed = true
                    node.x = maxX
                }
            }

            return changed
        }

        /** Just ensure that the positions of all the nodes are valid.
         * This means that all nodes are checked on whether they are at least horizseparation and vertseparation from each other.
         * This method does **not** take into account the grid. In most cases this method should not change the layout.
         * @param this@verifyPositions The nodes to verify (or move)
         *
         * @return `true` if at least one node changed position, `false` if not.
         */
        private fun List<DiagramNode<T>>.verifyPositions(): Boolean {
            var changed = false
            for (node in this) {
                // For every node determine the minimum X position
                val minX = right(
                    nodesLeftPos(this, node).rightMost(),
                    Double.NEGATIVE_INFINITY) + horizSeparation + node.leftExtent
                // If our coordinate is lower than needed, move the node and all "within the area"
                if (minX + TOLERANCE > node.x) {
                    changed = moveToRight(this, node) || changed
                }
                val minY = bottom(nodesAbovePos(this, node).lowest(),
                                  Double.NEGATIVE_INFINITY) + horizSeparation + node.topExtent

                if (minY + TOLERANCE > node.y) {
                    changed = moveDown(this, node) || changed
                }
            }
            return changed
        }

        private fun layoutNodeRight(nodes: List<DiagramNode<T>>,
                                    node: DiagramNode<T>,
                                    pass: Int): Boolean {
            layoutStepper.reportLayoutNode(node)
            var changed = false
            val leftNodes = node.leftNodes
            val rightNodes = node.rightNodes
            val aboveSiblings = node.preceedingSiblings
            val belowSiblings = node.followingSiblings

            val minY = nodesAbove(node).getBottomY(vertSeparation + node.topExtent)
            val maxY = belowSiblings.getTopY(vertSeparation + node.bottomExtent)

            val minX = leftNodes.getMinX(horizSeparation + node.leftExtent)
            val maxX = rightNodes.getMaxX(horizSeparation + node.rightExtent)

            var x = node.x
            var y = node.y

            run {
                // ensure that there is space for the node. If not, move all right nodes to the right
                val missingSpace = minX - maxX
                if (missingSpace > TOLERANCE) {
                    x = minX
                    nodesRightPos(nodes, node).moveX(missingSpace)
                    changed = true
                }
            }

            run {
                val missingSpace = minY - maxY
                if (missingSpace > TOLERANCE) {
                    y = minY
                    belowSiblings.moveY(missingSpace)
                    changed = true
                }
            }

            // If we have nodes left and right position this one in the middle
            if (!(leftNodes.isEmpty() || rightNodes.isEmpty())) {
                x = (leftNodes.rightMost()!!.x + rightNodes.leftMost()!!.x) / 2
            }
            if (!(aboveSiblings.isEmpty() || belowSiblings.isEmpty())) {
                y = (aboveSiblings.lowest()!!.y + belowSiblings.highest()!!.y) / 2
            } else if (leftNodes.size > 1) {
                y = (leftNodes.highest()!!.y + leftNodes.lowest()!!.y) / 2

                // If we are not a sibling, just align with the previous node
            } else if (leftNodes.size == 1 && rightNodes.size < 2 && leftNodes[0].rightNodes.size == 1) {
                y = leftNodes[0].y
            }

            x = x.coerceIn(minX, maxX)
            y = y.coerceIn(minY, maxY)

            val xChanged = changed(x, node.x, TOLERANCE)
            val yChanged = changed(y, node.y, TOLERANCE)

            if (rightNodes.size > 1 && (pass < 2 || yChanged)) {
                /* If we have multiple nodes branching of this one determine the center. Move that
                 * so that this node is the vertical center.
                 */
                val rightCenterY = (rightNodes.highest()!!.y + rightNodes.lowest()!!.y) / 2
                if (y - rightCenterY > TOLERANCE) {
                    // if the center of the right nodes is above this one, move the right nodes down.
                    // the reverse should be handled in the left pass
                    rightNodes.moveY(y - rightCenterY)
                }
            }

            if (yChanged || xChanged) {
                layoutStepper.reportMove(node, x, y)
                changed = true
                //      System.err.println("Moving node "+pNode+ "to ("+x+", "+y+')');
                node.x = x
                node.y = y
            }
            for (rightNode in rightNodes) {
                changed = changed or layoutNodeRight(nodes, rightNode, pass)
            }
            return changed
        }

        internal fun List<DiagramNode<T>>.getBottomY(add: Double): Double {
            return (lowest(false)?.let { (it.bottom + add).also { layoutStepper.reportMinY(this, it) } })
                   ?: Double.NEGATIVE_INFINITY
        }

        private fun List<DiagramNode<T>>.getTopY(subtract: Double): Double {
            return (highest(false)?.let { (it.top - subtract).also { layoutStepper.reportMaxY(this, it) } })
                   ?: Double.POSITIVE_INFINITY
        }

        private fun List<DiagramNode<T>>.getMinX(add: Double): Double {
            return (rightMost(false)?.let { (it.right + add).also { layoutStepper.reportMinX(this, it) } })
                   ?: Double.NEGATIVE_INFINITY
        }

        private fun List<DiagramNode<T>>.getMaxX(subtract: Double): Double {
            return (leftMost(false)?.let { (it.left - subtract).also { layoutStepper.reportMaxX(this, it) } })
                   ?: Double.POSITIVE_INFINITY
        }

        private fun layoutNodeLeft(nodes: List<DiagramNode<T>>, node: DiagramNode<T>, pass: Int): Boolean {
            layoutStepper.reportLayoutNode(node)
            var changed = false
            val leftNodes = node.leftNodes
            val rightNodes = node.rightNodes
            val aboveSiblings = node.preceedingSiblings
            val belowSiblings = node.followingSiblings

            val minY = bottom(aboveSiblings.lowest(), Double.NEGATIVE_INFINITY) + vertSeparation + node.topExtent
            if (minY.isFinite()) {
                layoutStepper.reportMinY(aboveSiblings, minY)
            }

            val nodesBelow = nodesBelow(node)
            val maxY = top(nodesBelow.highest(), Double.POSITIVE_INFINITY) - vertSeparation - node.bottomExtent
            if (minY.isFinite()) {
                layoutStepper.reportMaxY(nodesBelow, maxY)
            }

            val minX = right(leftNodes.rightMost(), Double.NEGATIVE_INFINITY) + horizSeparation + node.leftExtent
            if (minX.isFinite()) {
                layoutStepper.reportMinX(leftNodes, minX)
            }

            val maxX = left(rightNodes.leftMost(), Double.POSITIVE_INFINITY) - horizSeparation - node.rightExtent
            if (maxX.isFinite()) {
                layoutStepper.reportMaxX(rightNodes, maxX)
            }

            var x = node.x
            var y = node.y

            run {
                // ensure that there is space for the node. If not, move all right nodes to the right
                val missingSpace = minX - maxX
                if (missingSpace > TOLERANCE) {
                    x = minX
                    nodesLeftPos(nodes, node).moveX(-missingSpace)
                    changed = true
                }
            }

            run {
                val missingSpace = minY - maxY
                if (missingSpace > TOLERANCE) {
                    y = minY
                    nodesAbovePos(nodes, node).moveY(-missingSpace)
                    changed = true
                }
            }

            // If we have nodes left and right position this one in the middle
            if (!(leftNodes.isEmpty() || rightNodes.isEmpty())) {
                x = (leftNodes.rightMost()!!.right + rightNodes.leftMost()!!.left) / 2
            }

            // If we have siblings above and below, center in the middle between the siblings
            if (!(aboveSiblings.isEmpty() || belowSiblings.isEmpty())) {
                y = (aboveSiblings.lowest()!!.bottom + belowSiblings.highest()!!.top) / 2

                // If we have multiple nodes to our right, but one to our left, center in the middle between the nodes.
            } else if (rightNodes.size > 1 && leftNodes.size < 2) {
                y = (rightNodes.highest()!!.y + rightNodes.lowest()!!.y) / 2

                // If we have one node to the right and to the left either nothing, or we have siblings, use the right node to
                // set the position
            } else if (rightNodes.size == 1 && (leftNodes.isEmpty() ||
                                                (leftNodes.size == 1 && leftNodes[0].rightNodes.size > 1))) {
                y = rightNodes[0].y
            }

            x = max(min(maxX, x), minX)
            y = max(min(maxY, y), minY)

            val xChanged = changed(x, node.x, TOLERANCE)
            val yChanged = changed(y, node.y, TOLERANCE)

            if (leftNodes.size > 1 && (pass < 2 || yChanged)) {
                /* If we have multiple nodes branching of this one determine the center. Move that
                 * so that this node is the vertical center.
                 */
                val leftCenterY = (leftNodes.highest()!!.y + leftNodes.lowest()!!.y) / 2
                // if the center of the left nodes is below this one, move the left nodes up.
                // the reverse should be handled in the right pass
                if (y - leftCenterY > TOLERANCE) {
                    leftNodes.moveY(y - leftCenterY)
                }
            }

            if (yChanged || xChanged) {
                layoutStepper.reportMove(node, x, y)
                changed = true

                node.x = x
                node.y = y
            }
            for (leftNode in leftNodes) {
                changed = changed or layoutNodeLeft(nodes, leftNode, pass)
            }
            return changed
        }


        private fun Iterable<DiagramNode<T>>.lowest(report: Boolean = true) = maxByOrNull {
            it.bottom.onNaN(Double.NEGATIVE_INFINITY)
        }?.also { if (report) layoutStepper.reportLowest(this.toList(), it) }


        private fun Iterable<DiagramNode<T>>.highest(report: Boolean = true) = minByOrNull {
            it.top.onNaN(Double.POSITIVE_INFINITY)
        }?.also { if (report) layoutStepper.reportHighest(this.toList(), it) }

        private fun Iterable<DiagramNode<T>>.leftMost(report: Boolean = true) = minByOrNull {
            it.left.onNaN(Double.NEGATIVE_INFINITY)
        }?.also { if (report) layoutStepper.reportLeftmost(this.toList(), it) }


        private fun Iterable<DiagramNode<T>>.rightMost(report: Boolean = true) = maxByOrNull {
            it.right.onNaN(Double.POSITIVE_INFINITY)
        }?.also { if (report) layoutStepper.reportRightmost(this.toList(), it) }

        private fun nodesAbove(node: DiagramNode<T>): List<DiagramNode<T>> {
            val result = LinkedHashSet<DiagramNode<T>>()
            for (pred in node.leftNodes) {
                addNodesAbove(result, pred, node)
            }
            removeTransitiveRight(result, node)
            return result.toList()
        }

        private fun nodesBelow(node: DiagramNode<T>): List<DiagramNode<T>> {
            val result = LinkedHashSet<DiagramNode<T>>()
            for (pred in node.leftNodes) {
                addNodesBelow(result, pred, node)
            }
            removeTransitiveRight(result, node)
            return result.toList()
        }

        private fun addNodesAbove(result: LinkedHashSet<DiagramNode<T>>, left: DiagramNode<T>, ref: DiagramNode<T>) {
            if (left.y < ref.y) {
                for (candidate in left.rightNodes) {
                    if (candidate == ref) {
                        break
                    } else {
                        addTransitiveRight(result, candidate)
                    }
                }
                for (pred in left.leftNodes) {
                    addNodesAbove(result, pred, left)
                }
            }
        }

        private fun addNodesBelow(result: LinkedHashSet<DiagramNode<T>>, left: DiagramNode<T>, ref: DiagramNode<T>) {
            if (left.y > ref.y) {
                var found = false
                for (candidate in left.rightNodes) {
                    if (candidate == ref) {
                        found = true
                    } else if (found) {
                        addTransitiveRight(result, candidate)
                    }
                }
                for (pred in left.leftNodes) {
                    addNodesBelow(result, pred, left)
                }
            }
        }

        private fun addTransitiveRight(result: LinkedHashSet<DiagramNode<T>>, node: DiagramNode<T>) {
            if (result.add(node)) {
                for (right in node.rightNodes) {
                    addTransitiveRight(result, right)
                }
            }
        }


        private fun removeTransitiveRight(result: LinkedHashSet<DiagramNode<T>>, node: DiagramNode<T>) {
            result.remove(node)
            for (right in node.rightNodes) {
                removeTransitiveRight(result, right)
            }
        }

        private fun nodesAbovePos(nodes: List<DiagramNode<T>>,
                                  node: DiagramNode<T>) = nodes.filter { n -> n upOverlaps node }

        private fun nodesBelowPos(nodes: List<DiagramNode<T>>,
                                  node: DiagramNode<T>) = nodes.filter { it downOverlaps node }

        private fun nodesLeftPos(nodes: List<DiagramNode<T>>,
                                 node: DiagramNode<T>) = nodes.filter { it leftOverlaps node }

        private fun nodesRightPos(nodes: List<DiagramNode<T>>,
                                  node: DiagramNode<T>) = nodes.filter { it rightOverlaps node }

        private inline fun moveCommon(nodes: List<DiagramNode<T>>,
                                      filter: (DiagramNode<T>) -> Boolean,
                                      update: (DiagramNode<T>) -> Unit): Boolean {
            val overlaps = nodes.filter(filter)
            for (n in overlaps) {
                update(n)
                layoutStepper.reportMove(n, n.x, n.y)
                moveToRight(nodes, n)
                moveDown(nodes, n)
            }
            return overlaps.isNotEmpty()

        }

        private fun moveToRight(nodes: List<DiagramNode<T>>, freeRegion: DiagramNode<T>): Boolean {
            return moveCommon(nodes, { it rightOverlaps freeRegion },
                              { it.x = freeRegion.right + horizSeparation + it.leftExtent })
        }

        private fun moveDown(nodes: List<DiagramNode<T>>, freeRegion: DiagramNode<T>): Boolean {
            return moveCommon(nodes, { it downOverlaps freeRegion },
                              { it.y = freeRegion.bottom + vertSeparation + it.topExtent })
        }

        private fun List<DiagramNode<T>>.moveX(distance: Double) {
            layoutStepper.reportMoveX(this, distance)
            forEach { it.x += distance }
        }

        private fun List<DiagramNode<T>>.moveY(distance: Double) {
            layoutStepper.reportMoveY(this, distance)
            forEach { it.y += distance }
        }

        private fun getSiblings(node: DiagramNode<T>, above: Boolean): List<DiagramNode<T>> {
            val result = ArrayList<DiagramNode<T>>()
            val y = node.y
            run {
                var seenNode = false
                for (pred in node.leftNodes) {
                    if (pred.rightNodes.contains(node)) {
                        for (sibling in pred.rightNodes) {
                            if (sibling == node) {
                                seenNode = true
                            }
                            if (sibling.y.isNaN() || y.isNaN()) { // no coordinate
                                if (above xor seenNode) {
                                    result.add(sibling)
                                }
                            } else {
                                if (if (above) sibling.y < y else sibling.y > y) {
                                    result.add(sibling)
                                }
                            }
                        }
                    }
                }
            }
            run {
                var seenNode = false
                for (pred in node.rightNodes) {
                    if (pred.leftNodes.contains(node)) {
                        for (sibling in pred.leftNodes) {
                            if (sibling == node) {
                                seenNode = true
                            }
                            if (sibling.y.isNaN() || y.isNaN()) { // no coordinate
                                if (above xor seenNode) {
                                    result.add(sibling)
                                }
                            } else {
                                if (if (above) sibling.y < y else sibling.y > y) {
                                    result.add(sibling)
                                }
                            }
                        }
                    }
                }
            }
            if (result.size > 0) {
                layoutStepper.reportSiblings(node, result, above)
            }
            return result
        }

        infix fun DiagramNode<T>.rightOverlaps(other: DiagramNode<T>) =
            this != other && rightOverlaps(other, minHorizSeparation, minVertSeparation)

        infix fun DiagramNode<T>.leftOverlaps(other: DiagramNode<T>) =
            this != other && leftOverlaps(other, minHorizSeparation, minVertSeparation)

        infix fun DiagramNode<T>.upOverlaps(other: DiagramNode<T>) =
            this != other && upOverlaps(other, minHorizSeparation, minVertSeparation)

        infix fun DiagramNode<T>.downOverlaps(other: DiagramNode<T>) =
            this != other && downOverlaps(other, minHorizSeparation, minVertSeparation)

    }

    companion object {

        @JvmField
        val NULLALGORITHM = NullAlgorithm

        // We know that nullalgorithm does nothing and doesn't care about types.
        @JvmStatic
        fun <T : Positioned> nullalgorithm(): LayoutAlgorithm = NullAlgorithm

        const val TOLERANCE = 0.1

        private const val PASSCOUNT = 9

        private fun unset(array: DoubleArray) = {
            for (i in array.indices) {
                array[i] = Double.NaN
            }
        }

        @Deprecated("Use directly", ReplaceWith("array.maxIndex()", "net.devrieze.util.collection.maxIndex"))
        private fun maxPos(array: DoubleArray) = array.maxIndex()

        protected fun <T : Positioned> partition(nodes: List<DiagramNode<T>>): List<List<DiagramNode<T>>> {
            val partitions = ArrayList<List<DiagramNode<T>>>()
            val nodesCopy = nodes.toMutableList()

            fun addToPartition(node: DiagramNode<T>, partition: ArrayList<DiagramNode<T>>) {
                if (!partition.contains(node)) {
                    partition.add(node)
                    if (nodesCopy.remove(node)) {
                        for (left in node.leftNodes) {
                            addToPartition(left, partition)
                        }
                        for (right in node.rightNodes) {
                            addToPartition(right, partition)
                        }
                    }
                }
            }


            while (!nodesCopy.isEmpty()) {
                val partition = ArrayList<DiagramNode<T>>()
                addToPartition(nodesCopy[0], partition)
                partitions.add(partition)
            }
            return partitions
        }

        private fun top(node: DiagramNode<*>?, fallback: Double): Double {
            return node?.top ?: fallback
        }

        private fun bottom(node: DiagramNode<*>?, fallback: Double): Double {
            return node?.bottom ?: fallback
        }

        private fun left(node: DiagramNode<*>?, fallback: Double): Double {
            return node?.left ?: fallback
        }

        private fun right(node: DiagramNode<*>?, fallback: Double): Double {
            return node?.right ?: fallback
        }


        private fun changed(a: Double, b: Double, tolerance: Double): Boolean {
            if (a.isNaN()) {
                return !b.isNaN()
            }
            if (b.isNaN()) {
                return true
            }
            return abs(a - b) > tolerance
        }

        private fun <T : Positioned> averageY(nodes: List<DiagramNode<T>>): Double {
            if (nodes.isEmpty()) {
                return Double.NaN
            } else {
                var total = 0.0
                for (p in nodes) {
                    total += p.y
                }
                return total / nodes.size
            }
        }

        private fun <T : Positioned> averageY(nodes1: List<DiagramNode<T>>,
                                              nodes2: List<DiagramNode<T>>,
                                              fallback: Double): Double {
            if (nodes1.isEmpty() && nodes2.isEmpty()) {
                return fallback
            } else {
                var total = 0.0
                for (p in nodes1) {
                    total += p.y
                }
                for (p in nodes2) {
                    total += p.y
                }
                return total / (nodes1.size + nodes2.size)
            }
        }


        private fun <T : Positioned> averageX(nodes: List<DiagramNode<T>>): Double {
            if (nodes.isEmpty()) {
                return Double.NaN
            } else {
                var total = 0.0
                for (p in nodes) {
                    total += p.x
                }
                return total / nodes.size
            }
        }

        private fun <T : Positioned> averageX(nodes1: List<DiagramNode<T>>,
                                              nodes2: List<DiagramNode<T>>,
                                              fallback: Double): Double {
            if (nodes1.isEmpty() && nodes2.isEmpty()) {
                return fallback
            } else {
                var total = 0.0
                for (p in nodes1) {
                    total += p.x
                }
                for (p in nodes2) {
                    total += p.y
                }
                return total / (nodes1.size + nodes2.size)
            }
        }
    }

}

internal val Positioned.hasValidPosition get() = x.isFinite() && y.isFinite()

internal fun Double.onNaN(fallback: Double): Double = if (isNaN()) fallback else this

inline fun Double.whenFinite(body: (Double) -> Unit): Double = apply {
    if (isFinite()) body(this)
}
