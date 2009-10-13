package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.Collection;
import java.util.Map;


public abstract class ProcessNode {

  private static final int HORIZSEP = 100;
  private static final int VERTSEP = 60;
  private String aId;
  private boolean aPosSet = false;
  private int aX;
  private int aY;

  protected ProcessNode(String pId) {
    aId = pId;
  }

  public String getId() {
    return aId;
  }

  public abstract void resolvePredecessors(Map<String, ProcessNode> pMap);

  public abstract void ensureSuccessor(ProcessNode pNode);

  public abstract Collection<ProcessNode> getSuccessors();

  public abstract Collection<ProcessNode> getPredecessors();

  public void unsetPos() {
    aPosSet = false;
  }

  public boolean hasPos() {
    return aPosSet;
  }

  public int layout(int pX, int pY, ProcessNode pSource, boolean pForward) {
    if (aPosSet) {
      boolean dx = false; boolean dy = false;
      if (pX!=aX) {
        if (pForward) {
          if (pX > aX) {
            aX = pX;
            dx = true;
          } else { // pX < aX
            aX -= (aX - pX)/2; // center
          }
        } else {
          if (pX < aX) {
            aX = pX;
            dx = true;
          } else { // pX > aX
            aX += (pX - aX)/2; // center
          }
        }
      }
      if (pY != aY) {
        if (pY>aY) {
          aY = pY;
          dy = true;
        } else {

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
      aPosSet = true;
      aX = pX;
      if (pForward) {
        int cnt = getPredecessors().size();
        int index = -1;
        int i = 0;
        for (ProcessNode n:getPredecessors()) {
          if (n==pSource) {
            index = i;
            break;
          }
          ++i;
        }
        if (index>=0) {
          aY = pY - ((index * VERTSEP)) + ((cnt-1)*VERTSEP/2);
        } else {
          aY = pY;
        }
      } else {
        aY = pY;
      }
      return Math.max(layoutPredecessors(pSource),
                      layoutSuccessors(pSource));
    }
  }

  private int layoutSuccessors(ProcessNode pSource) {
    Collection<ProcessNode> successors = getSuccessors();
    int posY = aY - (((successors.size()-1) * VERTSEP)/2);
    int posX = aX + HORIZSEP;

    for (ProcessNode successor: successors) {
      if (successor !=pSource) {
        successor.layout(posX, posY, this, true);
      }
      posY += VERTSEP;
    }
    return Math.min(aY, posY - VERTSEP);
  }

  private int layoutPredecessors(ProcessNode pSource) {
    Collection<ProcessNode> predecessors = getPredecessors();
    int posY = aY - (((predecessors.size()-1) * VERTSEP)/2);
    int posX = aX - HORIZSEP;

    for (ProcessNode predecessor: predecessors) {
      if (predecessor !=pSource) {
        predecessor.layout(posX, posY, this, false);
      }
      posY += VERTSEP;
    }
    return Math.min(aY, posY - VERTSEP);
  }

  public int getX() {
    return aX;
  }

  public int getY() {
    return aY;
  }

  public void offset(int pOffsetX, int pOffsetY) {
    aX += pOffsetX;
    aY += pOffsetY;
  }

}
