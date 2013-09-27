package nl.adaptivity.process.clientProcessModel;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.diagram.Bounded;
import nl.adaptivity.diagram.Point;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;


public class ClientProcessModel<T extends IClientProcessNode<T>> implements ProcessModel<T>{

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";

  private static final double TOLERANCE = 1d;

  private final String aName;

  private List<T> aNodes;

  private double aTopPadding = 5d;
  private double aLeftPadding = 5d;
  private double aBottomPadding = 5d;
  private double aRightPadding = 5d;

  private double aInnerWidth = Double.NaN;

  private double aInnerHeight = Double.NaN;

  LayoutAlgorithm aLayoutAlgorithm = new LayoutAlgorithm();

  public ClientProcessModel(final String pName, final Collection<? extends T> pNodes) {
    aName = pName;
    setNodes(pNodes);
  }

  public void setNodes(final Collection<? extends T> nodes) {
    aNodes = CollectionUtil.copy(nodes);
    invalidate();
  }


  public double getVertSeparation() {
    return aLayoutAlgorithm.getVertSeparation();
  }


  public void setVertSeparation(double pVertSeparation) {
    if (aLayoutAlgorithm.getVertSeparation()!=pVertSeparation) {
      invalidate();
    }
    aLayoutAlgorithm.setVertSeparation(pVertSeparation);
  }


  public double getHorizSeparation() {
    return aLayoutAlgorithm.getHorizSeparation();
  }


  public void setHorizSeparation(double pHorizSeparation) {
    if (aLayoutAlgorithm.getHorizSeparation()!=pHorizSeparation) {
      invalidate();
    }
    aLayoutAlgorithm.setHorizSeparation(pHorizSeparation);
  }

  public double getDefaultNodeWidth() {
    return aLayoutAlgorithm.getDefaultNodeWidth();
  }


  public void setDefaultNodeWidth(double pDefaultNodeWidth) {
    if (aLayoutAlgorithm.getDefaultNodeWidth()!=pDefaultNodeWidth) {
      invalidate();
    }
    aLayoutAlgorithm.setDefaultNodeWidth(pDefaultNodeWidth);
  }


  public double getDefaultNodeHeight() {
    return aLayoutAlgorithm.getDefaultNodeHeight();
  }


  public void setDefaultNodeHeight(double pDefaultNodeHeight) {
    if (aLayoutAlgorithm.getDefaultNodeHeight()!=pDefaultNodeHeight) {
      invalidate();
    }
    aLayoutAlgorithm.setDefaultNodeHeight(pDefaultNodeHeight);
  }


  public double getTopPadding() {
    return aTopPadding;
  }


  public void setTopPadding(double pTopPadding) {
    double offset = pTopPadding-aTopPadding;
    for(T n:aNodes) {
      n.setY(n.getY()+offset);
    }
    aTopPadding = pTopPadding;
  }


  public double getLeftPadding() {
    return aLeftPadding;
  }


  public void setLeftPadding(double pLeftPadding) {
    double offset = pLeftPadding-aLeftPadding;
    for(T n:aNodes) {
      n.setX(n.getX()+offset);
    }
    aLeftPadding = pLeftPadding;
  }


  public double getBottomPadding() {
    return aBottomPadding;
  }


  public void setBottomPadding(double pBottomPadding) {
    aBottomPadding = pBottomPadding;
  }


  public double getRightPadding() {
    return aRightPadding;
  }


  public void setRightPadding(double pRightPadding) {
    aRightPadding = pRightPadding;
  }


  public void invalidate() {
    for (T n:aNodes) {
      n.setX(Double.NaN);
      n.setY(Double.NaN);
    }
  }

  @Override
  public int getEndNodeCount() {
    int i=0;
    for(T node: getModelNodes()) {
      if (node instanceof EndNode) { ++i; }
    }
    return i;
  }

  @Override
  public IProcessModelRef<T> getRef() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public T getNode(String pNodeId) {
    for(T n: getModelNodes()) {
      if (pNodeId.equals(n.getId())) {
        return n;
      }
    }
    return null;
  }

  @Override
  public Collection<? extends T> getModelNodes() {
    if (aNodes == null) {
      aNodes = new ArrayList<T>(0);
    }
    return aNodes;
  }

  @Override
  public String getName() {
    return aName;
  }

  @Override
  public Principal getOwner() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Set<String> getRoles() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Collection<? extends ClientStartNode<T>> getStartNodes() {
    List<ClientStartNode<T>> result = new ArrayList<ClientStartNode<T>>();
    for(T n:getModelNodes()) {
      if (n instanceof ClientStartNode) {
        result.add((ClientStartNode<T>) n);
      }
    }
    return result;
  }

  public double getInnerWidth() {
    if (Double.isNaN(aInnerWidth)) {
      layout();
    }
    return aInnerWidth;
  }

  public double getWidth() {
    return aLeftPadding+getInnerWidth()+aRightPadding;
  }

  public double getInnerHeight() {
    if (Double.isNaN(aInnerHeight)) {
      layout();
    }
    return aInnerHeight;
  }

  public double getHeight() {
    return aTopPadding+getInnerHeight()+aBottomPadding;
  }

  protected void layout() {
    boolean changed = Double.isNaN(aInnerHeight)|| Double.isNaN(aInnerWidth);
    for (final T node : aNodes) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        changed = true;
        layoutNode(node, true, true); // always force as that should be slightly more efficient
      }
    }
    if (changed) {
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double maxX = Double.MIN_VALUE;
      double maxY = Double.MIN_VALUE;
      for (final T node : aNodes) {
        minX = Math.min(node.getX()-leftDistance(node), minX);
        minY = Math.min(node.getY()-topDistance(node), minY);
        maxX = Math.max(node.getX()+rightDistance(node), maxX);
        maxY = Math.max(node.getY()+bottomDistance(node), maxY);
      }
      final double offsetX = aLeftPadding - minX;
      final double offsetY = aTopPadding - minY;

      aInnerWidth = maxX - minX;
      aInnerHeight = maxY - minY;

      if (Math.abs(offsetX)>TOLERANCE || Math.abs(offsetY)>TOLERANCE) {
        for (final T node : aNodes) {
          node.setX(node.getX()+offsetX);
          node.setY(node.getY()+offsetY);
        }
      }
    }
  }


  private void layoutNode(T pNode, boolean forceX, boolean forceY) {
    List<Point> leftPoints = getLeftPoints(pNode);
    List<Point> abovePoints = getAbovePoints(pNode);
    List<Point> rightPoints = getRightPoints(pNode);
    List<Point> belowPoints = getBelowPoints(pNode);

    double minY = maxY(abovePoints)+getVertSeparation() + topDistance(pNode);
    double maxY = minY(belowPoints)-getVertSeparation() - bottomDistance(pNode);
    double minX = maxX(leftPoints)+getHorizSeparation() + leftDistance(pNode);
    double maxX = minX(rightPoints)-getHorizSeparation() - rightDistance(pNode);

    double x = pNode.getX();
    double y = pNode.getY();

    if (leftPoints.isEmpty()) {
      if (rightPoints.isEmpty()) {
        if (forceX || Double.isNaN(x)|| x<minX || x>maxX) {
          x = averageX(abovePoints, belowPoints, 0d);
        }
        if (forceY || Double.isNaN(y) || y<minY || y>maxY) {
          if (abovePoints.isEmpty()) {
            if (! belowPoints.isEmpty()) {
              y = maxY;
            } // otherwise keep it where it is
          } else { // abovePoints not empty
            if (belowPoints.isEmpty()) {
              y = minY;
            } else {
              y = Math.max(minY, (minY+maxY)/2);
            }
          }
        }

      } else { // leftPoints empty, rightPoints not empty
        if (forceY || Double.isNaN(y)|| y<minY || y>maxY) {
          y = Math.max(minY, averageY(rightPoints));
        }
        if (forceX || Double.isNaN(x)|| x<minX || x>maxX) {
          x = Math.min(averageX(abovePoints, belowPoints,Double.POSITIVE_INFINITY),maxX);
        }
      }
    } else { // leftPoints not empty
      if (forceY || Double.isNaN(y) || y<minY || y>maxY) {
        if (leftPoints.size()==1) {
          y = Math.max(minY, averageY(leftPoints));
        } else {
          if (rightPoints.size()==1) {
            y = Math.max(minY, averageY(rightPoints));
          } else {
            y = Math.max(minY, averageY(leftPoints, rightPoints, 0));
          }
        }
      }
      if (forceX || Double.isNaN(x)|| x<minX || x>maxX) {
        if (rightPoints.isEmpty()) {
          x = Math.max(averageX(abovePoints, belowPoints,Double.NEGATIVE_INFINITY),minX);
        } else {
          x = Math.max(minX, (minX+maxX)/2);
        }
      }
    }
    if (Double.isNaN(x)) { x = 0d; }
    if (Double.isNaN(y)) { y = 0d; }
    boolean xChanged = changed(x, pNode.getX(), TOLERANCE);
    boolean yChanged = changed(y, pNode.getY(), TOLERANCE);
    if (yChanged || xChanged) {
      System.err.println("Moving node "+pNode+ "to ("+x+", "+y+')');
      pNode.setX(x);
      pNode.setY(y);
      for(T n:getPrecedingSiblings(pNode)) {
        layoutNode(n, xChanged, yChanged && y<minY);
      }
      for(T n:getFollowingSiblings(pNode)) {
        layoutNode(n, xChanged, yChanged && y>maxY);
      }
      for(T n:pNode.getPredecessors()) {
        layoutNode(n, xChanged && x<minX, yChanged);
      }
      for(T n:pNode.getSuccessors()) {
        layoutNode(n, xChanged && x>maxX, yChanged);
      }
    }
  }

  private static boolean changed(double pA, double pB, double pTolerance) {
    if (Double.isNaN(pA)) { return !Double.isNaN(pB); }
    if (Double.isNaN(pB)) { return true; }
    return Math.abs(pA-pB)>pTolerance;
  }

  private double topDistance(T pNode) {
    if ((!Double.isNaN(pNode.getY())) && pNode instanceof Bounded) {
      return pNode.getY()-((Bounded) pNode).getBounds().top;
    }
    return aLayoutAlgorithm.getDefaultNodeHeight()/2;
  }

  private double bottomDistance(T pNode) {
    if ((!Double.isNaN(pNode.getY()))&& pNode instanceof Bounded) {
      return ((Bounded) pNode).getBounds().bottom()-pNode.getY();
    }
    return aLayoutAlgorithm.getDefaultNodeHeight()/2;
  }

  private double leftDistance(T pNode) {
    if ((!Double.isNaN(pNode.getX()))&& pNode instanceof Bounded) {
      return pNode.getX()-((Bounded) pNode).getBounds().left;
    }
    return aLayoutAlgorithm.getDefaultNodeWidth()/2;
  }

  private double rightDistance(T pNode) {
    if ((!Double.isNaN(pNode.getX()))&& pNode instanceof Bounded) {
      return ((Bounded) pNode).getBounds().right()-pNode.getX();
    }
    return aLayoutAlgorithm.getDefaultNodeWidth()/2;
  }

  private static double averageY(List<Point> pPoints) {
    if (pPoints.isEmpty()) {
      return Double.NaN;
    } else {
      double total = 0;
      for(Point p: pPoints) { total+=p.y; }
      return total/pPoints.size();
    }
  }

  private static double averageY(List<Point> pPoints1, List<Point> pPoints2, double fallback) {
    if (pPoints1.isEmpty() && pPoints2.isEmpty()) {
      return fallback;
    } else {
      double total = 0;
      for(Point p: pPoints1) { total+=p.y; }
      for(Point p: pPoints2) { total+=p.y; }
      return total/(pPoints1.size()+pPoints2.size());
    }
  }

  private static double averageX(List<Point> pPoints) {
    if (pPoints.isEmpty()) {
      return Double.NaN;
    } else {
      double total = 0;
      for(Point p: pPoints) { total+=p.x; }
      return total/pPoints.size();
    }
  }

  private static double averageX(List<Point> pPoints1, List<Point> pPoints2, double fallback) {
    if (pPoints1.isEmpty() && pPoints2.isEmpty()) {
      return fallback;
    } else {
      double total = 0;
      for(Point p: pPoints1) { total+=p.x; }
      for(Point p: pPoints2) { total+=p.x; }
      return total/(pPoints1.size()+pPoints2.size());
    }
  }

  private static double maxY(List<Point> pPoints) {
    double result = Double.NEGATIVE_INFINITY;
    for(Point p: pPoints) {
      if (p.y>result) {
        result = p.y;
      }
    }
    return result;
  }

  private static double minY(List<Point> pPoints) {
    double result = Double.POSITIVE_INFINITY;
    for(Point p: pPoints) {
      if (p.y<result) {
        result = p.y;
      }
    }
    return result;
  }

  private static double maxX(List<Point> pPoints) {
    double result = Double.NEGATIVE_INFINITY;
    for(Point p: pPoints) {
      if (p.x>result) {
        result = p.x;
      }
    }
    return result;
  }

  private static double minX(List<Point> pPoints) {
    double result = Double.POSITIVE_INFINITY;
    for(Point p: pPoints) {
      if (p.x<result) {
        result = p.x;
      }
    }
    return result;
  }

  private List<Point> getLeftPoints(T pNode) {
    List<Point> result = new ArrayList<Point>();
    for(T n:pNode.getPredecessors()) {
      if (!(Double.isNaN(n.getX()) || Double.isNaN(n.getY()))) {
        double x;
        double y = n.getY();
        if (n instanceof Bounded) {
          x = ((Bounded) n).getBounds().right();
        } else {
          x = n.getX()+(aLayoutAlgorithm.getDefaultNodeWidth()/2);
        }
        result.add(new Point(x,y));
      }
    }
    return result;
  }

  private List<Point> getRightPoints(T pNode) {
    List<Point> result = new ArrayList<Point>();
    for(T n:pNode.getSuccessors()) {
      if (!(Double.isNaN(n.getX()) || Double.isNaN(n.getY()))) {
        double x;
        double y = n.getY();
        if (n instanceof Bounded) {
          x = ((Bounded) n).getBounds().left;
        } else {
          x = n.getX()-(aLayoutAlgorithm.getDefaultNodeWidth()/2);
        }
        result.add(new Point(x,y));
      }
    }
    return result;
  }

  private List<Point> getAbovePoints(T pNode) {
    List<Point> result = new ArrayList<Point>();
    for(T n:getPrecedingSiblings(pNode)) {
      if (!(Double.isNaN(n.getX()) || Double.isNaN(n.getY()))) {
        double x = n.getX();
        double y;
        if (n instanceof Bounded) {
          y = ((Bounded) n).getBounds().bottom();
        } else {
          y = n.getY()+(aLayoutAlgorithm.getDefaultNodeHeight()/2);
        }
        result.add(new Point(x,y));
      }
    }
    return result;
  }

  private List<Point> getBelowPoints(T pNode) {
    List<Point> result = new ArrayList<Point>();
    for(T n:getFollowingSiblings(pNode)) {
      if (!(Double.isNaN(n.getX()) || Double.isNaN(n.getY()))) {
        double x = n.getX();
        double y;
        if (n instanceof Bounded) {
          y = ((Bounded) n).getBounds().top;
        } else {
          y = n.getY()-(aLayoutAlgorithm.getDefaultNodeHeight()/2);
        }
        result.add(new Point(x,y));
      }
    }
    return result;
  }

  private List<T> getPrecedingSiblings(T pNode) {
    List<T> result = new ArrayList<T>();
    for(T pred:pNode.getPredecessors()) {
      if (pred.getSuccessors().contains(pNode)) {
        for(T sibling: pred.getSuccessors()) {
          if (sibling==pNode) {
            break;
          } else {
            result.add(sibling);
          }
        }
      }
    }
    for(T pred:pNode.getSuccessors()) {
      if (pred.getPredecessors().contains(pNode)) {
        for(T sibling: pred.getPredecessors()) {
          if (sibling==pNode) {
            break;
          } else {
            result.add(sibling);
          }
        }
      }
    }
    return result;
  }

  private List<T> getFollowingSiblings(T pNode) {
    List<T> result = new ArrayList<T>();
    for(T successor:pNode.getPredecessors()) {
      if (successor.getSuccessors().contains(pNode)) {
        boolean following = false;
        for(T sibling: successor.getSuccessors()) {
          if (sibling==pNode) {
            following = true;
          } else if (following){
            result.add(sibling);
          }
        }
      }
    }
    for(T successor:pNode.getSuccessors()) {
      if (successor.getPredecessors().contains(pNode)) {
        boolean following = false;
        for(T sibling: successor.getPredecessors()) {
          if (sibling==pNode) {
            following = true;
          } else if (following){
            result.add(sibling);
          }
        }
      }
    }
    return result;
  }

}
