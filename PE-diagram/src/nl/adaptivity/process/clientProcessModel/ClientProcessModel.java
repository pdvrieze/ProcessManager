package nl.adaptivity.process.clientProcessModel;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.diagram.Bounded;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;


public class ClientProcessModel<T extends IClientProcessNode<T>> implements ProcessModel<T>{

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";

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
      if (node.getX()==Double.NaN || node.getY()==Double.NaN) {
        layoutNode(node);
      }
    }
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    for (final T node : aNodes) {
      if (node instanceof Bounded) {
        Rectangle bounds = ((Bounded) node).getBounds();
        minX = Math.min(bounds.top, minX);
        minY = Math.min(bounds.top, minY);
      } else {
        minX = Math.min(node.getX()-(aDefaultNodeWidth/2), minX);
        minY = Math.min(node.getY()-(aDefaultNodeHeight/2), minY);
      }
    }
    final double offsetX = aLeftPadding - minX;
    final double offsetY = aTopPadding - minY;

    for (final T node : aNodes) {
      node.setX(node.getX()+offsetX);
      node.setY(node.getY()+offsetY);
    }
  }


  private void layoutNode(T pNode) {
    ProcessNodeSet<? extends T> predecessors = pNode.getPredecessors();
    if (predecessors.size()==0) {
      pNode.setX(0d);
      pNode.setY(0d);
    } else {
      for(T predecessor:predecessors) {

      }
    }
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public double layout(final double pX, final double pY, final IClientProcessNode<?> pSource, final boolean pForward) {
    if (hasPos()) {
      boolean dx = false;
      boolean dy = false;
      if (pX != aX) {
        if (pForward) {
          if (pX > aX) {
            aX = pX;
            dx = true;
          } else { // pX < aX
            aX -= (aX - pX) / 2; // center
          }
        } else {
          if (pX < aX) {
            aX = pX;
            dx = true;
          } else { // pX > aX
            aX += (pX - aX) / 2; // center
          }
        }
      }
      if (pY != aY) {
        if (pY > aY) {
          aY = pY;
          dy = true;
        } else {
          aY -= (aY - pY)/2;
        }
      }
      if (dx || dy) {
        if (pForward) {
          return Math.max(aY, layoutSuccessors(this));
        } else {
          return Math.max(layoutPredecessors(this), aY);
        }
      }
      return aY;

    } else {
      aX = pX;
      if (pForward) {
        final int cnt = getPredecessors().size();
        int index = -1;
        int i = 0;
        for (final T n : getPredecessors()) {
          if (n == pSource) {
            index = i;
            break;
          }
          ++i;
        }
        if (index >= 0) {
          aY = (pY - ((index * VERTSEP))) + (((cnt - 1) * VERTSEP) / 2);
        } else {
          aY = pY;
        }
      } else {
        aY = pY;
      }
      return Math.max(layoutPredecessors(pSource), layoutSuccessors(pSource));
    }
  }

  private double layoutSuccessors(final IClientProcessNode<?> pSource) {
    final Collection<? extends T> successors = getSuccessors();
    double posY = aY - (((successors.size() - 1) * aVertSeparation) / 2);
    final double posX = aX + aHorizSeparation;

    for (final IClientProcessNode<?> successor : successors) {
      if (successor != pSource) {
        successor.layout(posX, posY, this, true);
      }
      posY += VERTSEP;
    }
    return Math.min(aY, posY - VERTSEP);
  }

  private double layoutPredecessors(final IClientProcessNode<?> pSource) {
    final Set<? extends T> predecessors = getPredecessors();
    double posY = aY - (((predecessors.size() - 1) * VERTSEP) / 2);
    final double posX = aX - HORIZSEP;

    for (final T predecessor : predecessors) {
      if (predecessor != pSource) {
        predecessor.layout(posX, posY, this, false);
      }
      posY += VERTSEP;
    }
    return Math.min(aY, posY - VERTSEP);
  }

  @Override
  public int getEndNodeCount() {
    // TODO Auto-generated method stub
    // return 0;
    throw new UnsupportedOperationException("Not yet implemented");
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
