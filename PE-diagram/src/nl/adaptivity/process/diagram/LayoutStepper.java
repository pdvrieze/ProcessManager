package nl.adaptivity.process.diagram;

import java.util.List;

import nl.adaptivity.diagram.Positioned;


@SuppressWarnings("unused")
public class LayoutStepper<T extends Positioned> {

  public void reportMove(DiagramNode<T> pNode, double newX, double newY) {
    // By default empty
  }

  public void reportPass(int pass) {
    // no implementation
  }

  public void reportLowest(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    // empty
  }

  public void reportHighest(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    // empty
  }

  public void reportRightmost(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    // empty
  }

  public void reportLeftmost(List<? extends DiagramNode<T>> pNodes, DiagramNode<T> pNode) {
    // empty
  }

  public void reportLayoutNode(DiagramNode<T> pNode) {
    // empty
  }

  public void reportMoveX(List<? extends DiagramNode<T>> pNodes, double offset) {
    // empty
  }

  public void reportMoveY(List<? extends DiagramNode<T>> pNodes, double offset) {
    // empty
  }

  public void reportMinX(List<? extends DiagramNode<T>> pNodes, double offset) {
    // empty
  }

  public void reportMinY(List<? extends DiagramNode<T>> pNodes, double offset) {
    // empty
  }

  public void reportMaxX(List<? extends DiagramNode<T>> pNodes, double offset) {
    // empty
  }

  public void reportMaxY(List<? extends DiagramNode<T>> pNodes, double offset) {
    // empty
  }

}
