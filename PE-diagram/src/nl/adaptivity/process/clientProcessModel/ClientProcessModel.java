package nl.adaptivity.process.clientProcessModel;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.diagram.Bounded;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.diagram.DiagramNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;


public class ClientProcessModel<T extends IClientProcessNode<T>> implements ProcessModel<T>{

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";

  private final String aName;

  private List<T> aNodes;

  private double aTopPadding = 5d;
  private double aLeftPadding = 5d;
  private double aBottomPadding = 5d;
  private double aRightPadding = 5d;

  LayoutAlgorithm<T> aLayoutAlgorithm;

  public ClientProcessModel(final String pName, final Collection<? extends T> pNodes) {
    this(pName, pNodes, new LayoutAlgorithm<T>());
  }

  public ClientProcessModel(final String pName, final Collection<? extends T> pNodes, LayoutAlgorithm<T> pLayoutAlgorithm) {
    aName = pName;
    aLayoutAlgorithm = pLayoutAlgorithm == null ? new LayoutAlgorithm<T>() : pLayoutAlgorithm;
    setNodes(pNodes);
  }

  public void setNodes(final Collection<? extends T> nodes) {
    aNodes = CollectionUtil.copy(nodes);
    for(T node: aNodes) {
      node.setOwner(this);
    }
    invalidate();
  }

  public LayoutAlgorithm<T> getLayoutAlgorithm() {
    return aLayoutAlgorithm;
  }

  public void setLayoutAlgorithm(LayoutAlgorithm<T> pLayoutAlgorithm) {
    aLayoutAlgorithm = pLayoutAlgorithm;
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

  /**
   * @param pNode The node that has changed.
   */
  public void nodeChanged(T pNode) {
    // no implementation here
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
  public List<? extends T> getModelNodes() {
    if (aNodes == null) {
      aNodes = new ArrayList<>(0);
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
    List<ClientStartNode<T>> result = new ArrayList<>();
    for(T n:getModelNodes()) {
      if (n instanceof ClientStartNode) {
        result.add((ClientStartNode<T>) n);
      }
    }
    return result;
  }

  public void addNode(T pNode) {
    aNodes.add(pNode);
    // Make sure that children can know of the change.
    nodeChanged(pNode);
  }

  public void removeNode(int pNodePos) {
    T node = aNodes.remove(pNodePos);
    for(T pred: node.getPredecessors()) {
      pred.removeSuccessor(node);
    }
    for(T suc:node.getSuccessors()) {
      suc.removePredecessor(node);
    }
    nodeChanged(node);
  }

  public void layout() {
    final List<DiagramNode<T>> diagramNodes = toDiagramNodes(getModelNodes());
    if(aLayoutAlgorithm.layout(diagramNodes)) {
      double maxX = Double.MIN_VALUE;
      double maxY = Double.MIN_VALUE;
      for(DiagramNode<T> n:diagramNodes) {
        n.getTarget().setX(n.getX()+getLeftPadding());
        n.getTarget().setY(n.getY()+getTopPadding());
        maxX = Math.max(n.getRight(), maxX);
        maxY = Math.max(n.getBottom(), maxY);
      }
    }
  }


  private List<DiagramNode<T>> toDiagramNodes(Collection<? extends T> pModelNodes) {
    HashMap<T,DiagramNode<T>> map = new HashMap<>();
    List<DiagramNode<T>> result = new ArrayList<>();
    for(T node:pModelNodes) {
      final double leftExtend;
      final double rightExtend;
      final double topExtend;
      final double bottomExtend;
      if (node instanceof Bounded) {
        boolean tempCoords = Double.isNaN(node.getX())||Double.isNaN(node.getY());
        double tmpX=0;
        double tmpY=0;
        if (tempCoords) {
          tmpX=node.getX();node.setX(0);
          tmpY=node.getY();node.setY(0);
        }
        Rectangle bounds = ((Bounded)node).getBounds();
        leftExtend = node.getX()-bounds.left;
        rightExtend = bounds.right()-node.getX();
        topExtend = node.getY()-bounds.top;
        bottomExtend = bounds.bottom()-node.getY();
        if (tempCoords) {
          node.setX(tmpX);
          node.setY(tmpY);
        }
      } else {
        leftExtend = rightExtend = aLayoutAlgorithm.getDefaultNodeWidth()/2;
        topExtend = bottomExtend = aLayoutAlgorithm.getDefaultNodeHeight()/2;
      }
      DiagramNode<T> dn = new DiagramNode<>(node, leftExtend, rightExtend, topExtend, bottomExtend);
      if (node.getId()!=null) {
        map.put(node, dn);
      }
      result.add(dn);
    }

    for(DiagramNode<T> dn:result) {
      T mn = dn.getTarget();
      for(T successor:mn.getSuccessors()) {
        DiagramNode<T> rightdn = map.get(successor);
        if (rightdn!=null) {
          dn.getRightNodes().add(rightdn);
        }
      }
      for(T predecessor:mn.getPredecessors()) {
        DiagramNode<T> leftdn = map.get(predecessor);
        if (leftdn!=null) {
          dn.getLeftNodes().add(leftdn);
        }
      }
    }
    return result;
  }

}
