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

package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Positioned;

import java.util.List;


@SuppressWarnings("unused")
public class LayoutStepper<T extends Positioned> {

  public void reportMove(DiagramNode<T> node, double newX, double newY) {
    // By default empty
  }

  public void reportPass(int pass) {
    // no implementation
  }

  public void reportLowest(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node) {
    // empty
  }

  public void reportHighest(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node) {
    // empty
  }

  public void reportRightmost(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node) {
    // empty
  }

  public void reportLeftmost(List<? extends DiagramNode<T>> nodes, DiagramNode<T> node) {
    // empty
  }

  public void reportLayoutNode(DiagramNode<T> node) {
    // empty
  }

  public void reportMoveX(List<? extends DiagramNode<T>> nodes, double offset) {
    for(DiagramNode<T> node: nodes) {
      reportMove(node, node.getX()+offset, node.getY());
    }
  }

  public void reportMoveY(List<? extends DiagramNode<T>> nodes, double offset) {
    for(DiagramNode<T> node: nodes) {
      reportMove(node, node.getX(), node.getY()+offset);
    }
  }

  public void reportMinX(List<? extends DiagramNode<T>> nodes, double offset) {
    // empty
  }

  public void reportMinY(List<? extends DiagramNode<T>> nodes, double offset) {
    // empty
  }

  public void reportMaxX(List<? extends DiagramNode<T>> nodes, double offset) {
    // empty
  }

  public void reportMaxY(List<? extends DiagramNode<T>> nodes, double offset) {
    // empty
  }
  
  public void reportSiblings(DiagramNode<T> node, List<? extends DiagramNode<T>> nodes, boolean above) {
    // empty
  }

}
