package nl.adaptivity.process.clientProcessModel;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.devrieze.util.CollectionUtil;
import nl.adaptivity.diagram.Bounded;
import nl.adaptivity.diagram.Point;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;


public class ClientProcessModel<T extends IClientProcessNode<T>> implements ProcessModel<T>{

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";

  private static final double TOLERANCE = 1d;

  private final String aName;

  private List<T> aNodes;

  private double aVertSeparation = 30d;

  private double aHorizSeparation = 30d;

  private double aTopPadding = 5d;
  private double aLeftPadding = 5d;
  private double aBottomPadding = 5d;
  private double aRightPadding = 5d;

  private double aDefaultNodeWidth = 30d;
  private double aDefaultNodeHeight = 30d;

  public ClientProcessModel(final String pName, final Collection<? extends T> pNodes) {
    aName = pName;
    setNodes(pNodes);
  }

  public void setNodes(final Collection<? extends T> nodes) {
    aNodes = CollectionUtil.copy(nodes);
  }

  public void layout() {
    for (final T node : aNodes) {
      if (Double.isNaN(node.getX()) || Double.isNaN(node.getY())) {
        layoutNode(node, true); // always force as that should be slightly more efficient
      }
    }
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    for (final T node : aNodes) {
      minX = Math.min(node.getX()-leftDistance(node), minX);
      minY = Math.min(node.getY()-topDistance(node), minY);
    }
    final double offsetX = aLeftPadding - minX;
    final double offsetY = aTopPadding - minY;

    if (Math.abs(offsetX)>TOLERANCE || Math.abs(offsetY)>TOLERANCE) {
      for (final T node : aNodes) {
        node.setX(node.getX()+offsetX);
        node.setY(node.getY()+offsetY);
      }
    }
  }


  private void layoutNode(T pNode, boolean force) {
    List<Point> leftPoints = getLeftPoints(pNode);
    List<Point> abovePoints = getAbovePoints(pNode);
    List<Point> rightPoints = getRightPoints(pNode);
    List<Point> belowPoints = getBelowPoints(pNode);
    
    double minY = maxY(abovePoints)+aVertSeparation + topDistance(pNode);
    double maxY = minY(belowPoints)-aVertSeparation - bottomDistance(pNode);
    double minX = maxX(leftPoints)+aHorizSeparation + leftDistance(pNode);
    double maxX = minX(rightPoints)-aHorizSeparation - rightDistance(pNode);
    
    double x = pNode.getX();
    double y = pNode.getY();
    
    if (leftPoints.isEmpty()) {
      if (rightPoints.isEmpty()) {
        if (force || Double.isNaN(x)|| x<minX || x>maxX) {
          x = averageX(abovePoints, belowPoints, 0d);
        }
        if (force || Double.isNaN(y) || y<minY || y>maxY) {
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
        if (force || Double.isNaN(y)|| y<minY || y>maxY) {
          y = Math.max(minY, averageY(rightPoints));
        }
        if (force || Double.isNaN(x)|| x<minX || x>maxX) {
          x = Math.min(averageX(abovePoints, belowPoints,Double.POSITIVE_INFINITY),maxX);
        }
      }
    } else { // leftPoints not empty
      if (force || Double.isNaN(y) || y<minY || y>maxY) {
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
      if (force || Double.isNaN(x)|| x<minX || x>maxX) {
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
        layoutNode(n, yChanged && y<minY);
      }
      for(T n:getFollowingSiblings(pNode)) {
        layoutNode(n, yChanged && y>maxY);
      }
      for(T n:pNode.getPredecessors()) {
        layoutNode(n, xChanged && x<minX);
      }
      for(T n:pNode.getSuccessors()) {
        layoutNode(n, xChanged && x>maxX);
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
    return aDefaultNodeHeight/2;
  }

  private double bottomDistance(T pNode) {
    if ((!Double.isNaN(pNode.getY()))&& pNode instanceof Bounded) {
      return ((Bounded) pNode).getBounds().bottom()-pNode.getY();
    }
    return aDefaultNodeHeight/2;
  }

  private double leftDistance(T pNode) {
    if ((!Double.isNaN(pNode.getX()))&& pNode instanceof Bounded) {
      return pNode.getX()-((Bounded) pNode).getBounds().left;
    }
    return aDefaultNodeWidth/2;
  }

  private double rightDistance(T pNode) {
    if ((!Double.isNaN(pNode.getX()))&& pNode instanceof Bounded) {
      return ((Bounded) pNode).getBounds().right()-pNode.getX();
    }
    return aDefaultNodeWidth/2;
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
          x = n.getX()+(aDefaultNodeWidth/2);
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
          x = n.getX()-(aDefaultNodeWidth/2);
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
          y = n.getY()+(aDefaultNodeHeight/2);
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
          y = n.getY()-(aDefaultNodeHeight/2);
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
            result.add(pNode);
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
            result.add(pNode);
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
            result.add(pNode);
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
            result.add(pNode);
          }
        }
      }
    }
    return result;
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
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
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

}
