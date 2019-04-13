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

import nl.adaptivity.diagram.Positioned

interface LayoutStepper<in T: Positioned> {
  fun reportMove(node: DiagramNode<T>, newX: Double, newY: Double) = Unit

  fun reportPass(pass: Int) = Unit

  fun reportLowest(nodes: List<DiagramNode<T>>, node: DiagramNode<T>) = Unit

  fun reportHighest(nodes: List<DiagramNode<T>>, node: DiagramNode<T>) = Unit

  fun reportRightmost(nodes: List<DiagramNode<T>>, node: DiagramNode<T>) = Unit

  fun reportLeftmost(nodes: List<DiagramNode<T>>, node: DiagramNode<T>) = Unit

  fun reportLayoutNode(node: DiagramNode<T>) = Unit

  fun reportMoveX(nodes: List<DiagramNode<T>>, offset: Double) =
    nodes.forEach { node -> reportMove(node, node.x + offset, node.y) }

  fun reportMoveY(nodes: List<DiagramNode<T>>, offset: Double) =
    nodes.forEach { node -> reportMove(node, node.x, node.y + offset) }

  fun reportMinX(nodes: List<DiagramNode<T>>, offset: Double) = Unit

  fun reportMinY(nodes: List<DiagramNode<T>>, offset: Double) = Unit

  fun reportMaxX(nodes: List<DiagramNode<T>>, offset: Double) = Unit

  fun reportMaxY(nodes: List<DiagramNode<T>>, offset: Double) = Unit

  fun reportSiblings(node: DiagramNode<T>, nodes: List<DiagramNode<T>>, above: Boolean) = Unit

}

open class AbstractLayoutStepper<T : Positioned> : LayoutStepper<T>
