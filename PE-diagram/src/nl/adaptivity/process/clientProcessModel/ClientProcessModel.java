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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.clientProcessModel;

import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.diagram.Bounded;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.diagram.DiagramNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.ProcessModelBase;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.util.Identifiable;

import java.util.*;


public abstract class ClientProcessModel<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ProcessModelBase<T, M> {

  public static final String NS_JBI = "http://adaptivity.nl/ProcessEngine/activity";

  public static final String NS_UMH = "http://adaptivity.nl/userMessageHandler";

  public static final String NS_PM = "http://adaptivity.nl/ProcessEngine/";

  static final String PROCESSMODEL_NS = NS_PM;

  private double mTopPadding = 5d;
  private double mLeftPadding = 5d;
  private double mBottomPadding = 5d;
  private double mRightPadding = 5d;

  LayoutAlgorithm<T> mLayoutAlgorithm;

  private boolean mNeedsLayout = false;

  public ClientProcessModel(UUID uuid, final String name, final Collection<? extends T> nodes) {
    this(uuid, name, nodes, new LayoutAlgorithm<T>());
  }

  public ClientProcessModel(UUID uuid, final String name, final Collection<? extends T> nodes, LayoutAlgorithm<T> layoutAlgorithm) {
    super(nodes);
    setName(name);
    mLayoutAlgorithm = layoutAlgorithm == null ? new LayoutAlgorithm<T>() : layoutAlgorithm;
    setNodes(nodes);
    setUuid(uuid == null ? UUID.randomUUID() : uuid);
  }

  protected ClientProcessModel() {
    this(null, null, new ArrayList<T>(), null);
  }

  public abstract T asNode(final Identifiable id);

  public void setNodes(final Collection<? extends T> nodes) {
    super.setModelNodes(ProcessNodeSet.processNodeSet(nodes));
    invalidate();
  }

  public LayoutAlgorithm<T> getLayoutAlgorithm() {
    return mLayoutAlgorithm;
  }

  public void setLayoutAlgorithm(LayoutAlgorithm<T> layoutAlgorithm) {
    mLayoutAlgorithm = layoutAlgorithm;
  }

  public double getVertSeparation() {
    return mLayoutAlgorithm.getVertSeparation();
  }


  public void setVertSeparation(double vertSeparation) {
    if (mLayoutAlgorithm.getVertSeparation()!=vertSeparation) {
      invalidate();
    }
    mLayoutAlgorithm.setVertSeparation(vertSeparation);
  }


  public double getHorizSeparation() {
    return mLayoutAlgorithm.getHorizSeparation();
  }


  public void setHorizSeparation(double horizSeparation) {
    if (mLayoutAlgorithm.getHorizSeparation()!=horizSeparation) {
      invalidate();
    }
    mLayoutAlgorithm.setHorizSeparation(horizSeparation);
  }

  public double getDefaultNodeWidth() {
    return mLayoutAlgorithm.getDefaultNodeWidth();
  }


  public void setDefaultNodeWidth(double defaultNodeWidth) {
    if (mLayoutAlgorithm.getDefaultNodeWidth()!=defaultNodeWidth) {
      invalidate();
    }
    mLayoutAlgorithm.setDefaultNodeWidth(defaultNodeWidth);
  }


  public double getDefaultNodeHeight() {
    return mLayoutAlgorithm.getDefaultNodeHeight();
  }


  public void setDefaultNodeHeight(double defaultNodeHeight) {
    if (mLayoutAlgorithm.getDefaultNodeHeight()!=defaultNodeHeight) {
      invalidate();
    }
    mLayoutAlgorithm.setDefaultNodeHeight(defaultNodeHeight);
  }


  public double getTopPadding() {
    return mTopPadding;
  }


  public void setTopPadding(double topPadding) {
    double offset = topPadding-mTopPadding;
    for(T n: getModelNodes()) {
      n.setY(n.getY()+offset);
    }
    mTopPadding = topPadding;
  }


  public double getLeftPadding() {
    return mLeftPadding;
  }


  public void setLeftPadding(double leftPadding) {
    double offset = leftPadding-mLeftPadding;
    for(T n: getModelNodes()) {
      n.setX(n.getX()+offset);
    }
    mLeftPadding = leftPadding;
  }


  public double getBottomPadding() {
    return mBottomPadding;
  }


  public void setBottomPadding(double bottomPadding) {
    mBottomPadding = bottomPadding;
  }


  public double getRightPadding() {
    return mRightPadding;
  }


  public void setRightPadding(double rightPadding) {
    mRightPadding = rightPadding;
  }


  public void invalidate() {
    mNeedsLayout  = true;
  }

  public void resetLayout() {
    for (T n: getModelNodes()) {
      n.setX(Double.NaN);
      n.setY(Double.NaN);
    }
    invalidate();
  }

  public boolean isInvalid() {
    return mNeedsLayout;
  }

  public int getEndNodeCount() {
    int i=0;
    for(T node: getModelNodes()) {
      if (node instanceof EndNode) { ++i; }
    }
    return i;
  }

  @Override
  public IProcessModelRef<T, M> getRef() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public T getNode(String nodeId) {
    for(T n: getModelNodes()) {
      if (nodeId.equals(n.getId())) {
        return n;
      }
    }
    return null;
  }

  public void setOwner(String owner) {
    setOwner(new SimplePrincipal(owner));
  }

  public Collection<? extends ClientStartNode<? extends T, M>> getStartNodes() {
    List<ClientStartNode<? extends T, M>> result = new ArrayList<>();
    for(T n:getModelNodes()) {
      if (n instanceof ClientStartNode) {
        result.add((ClientStartNode<? extends T, M>) n);
      }
    }
    return result;
  }

  public boolean addNode(T node) {
    if (super.addNode(node)) {
      node.setOwnerModel(asM());
      // Make sure that children can know of the change.
      notifyNodeChanged(node);
      return true;
    }
    return false;
  }

  public T removeNode(int nodePos) {
    T node = super.removeNode(nodePos);
    disconnectNode(node);
    return node;
  }

  @Override
  public boolean removeNode(T node) {
    if (node==null) {
      return false;
    }
    if (getModelNodes().remove(node)) {
      disconnectNode(node);
      return true;
    }
    return false;
  }

  private void disconnectNode(T node) {
    node.setPredecessors(Collections.<Identifiable>emptyList());
    node.setSuccessors(Collections.<Identifiable>emptyList());
    notifyNodeChanged(node);
  }

  public void layout() {
    final List<DiagramNode<T>> diagramNodes = toDiagramNodes(getModelNodes());
    if(mLayoutAlgorithm.layout(diagramNodes)) {
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

  private List<DiagramNode<T>> toDiagramNodes(Collection<? extends T> modelNodes) {
    HashMap<T,DiagramNode<T>> map = new HashMap<>();
    List<DiagramNode<T>> result = new ArrayList<>();
    for(T node:modelNodes) {
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
          mNeedsLayout=true; // we need layout as we have undefined coordinates.
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
        leftExtend = rightExtend = mLayoutAlgorithm.getDefaultNodeWidth()/2;
        topExtend = bottomExtend = mLayoutAlgorithm.getDefaultNodeHeight()/2;
      }
      DiagramNode<T> dn = new DiagramNode<>(node, leftExtend, rightExtend, topExtend, bottomExtend);
      if (node.getId()!=null) {
        map.put(node, dn);
      }
      result.add(dn);
    }

    for(DiagramNode<T> dn:result) {
      T mn = dn.getTarget();
      for(Identifiable successor:mn.getSuccessors()) {
        DiagramNode<T> rightdn = map.get(successor);
        if (rightdn!=null) {
          dn.getRightNodes().add(rightdn);
        }
      }
      for(Identifiable predecessorId:mn.getPredecessors()) {
        T predecessor = getNode(predecessorId);
        DiagramNode<T> leftdn = map.get(predecessor);
        if (leftdn!=null) {
          dn.getLeftNodes().add(leftdn);
        }
      }
    }
    return result;
  }

}