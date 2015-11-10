package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.Collection;
import java.util.Map;


public abstract class ProcessNode {

  private static final int HORIZSEP = 100;

  private static final int VERTSEP = 60;

  private final String aId;

  private boolean aPosSet = false;

  private int aX;

  private int aY;

  protected ProcessNode(final String id) {
    aId = id;
  }

  public String getId() {
    return aId;
  }

  public abstract void resolvePredecessors(Map<String, ProcessNode> map);

  public abstract void ensureSuccessor(ProcessNode node);

  public abstract Collection<ProcessNode> getSuccessors();

  public abstract Collection<ProcessNode> getPredecessors();

  public void unsetPos() {
    aPosSet = false;
  }

  public boolean hasPos() {
    return aPosSet;
  }

  public int layout(final int x, final int y, final ProcessNode source, final boolean forward) {
    if (aPosSet) {
      boolean dx = false;
      boolean dy = false;
      if (x != aX) {
        if (forward) {
          if (x > aX) {
            aX = x;
            dx = true;
          } else { // pX < aX
            aX -= (aX - x) / 2; // center
          }
        } else {
          if (x < aX) {
            aX = x;
            dx = true;
          } else { // pX > aX
            aX += (x - aX) / 2; // center
          }
        }
      }
      if (y != aY) {
        if (y > aY) {
          aY = y;
          dy = true;
        } else {

        }
      }
      if (dx || dy) {
        if (forward) {
          return Math.max(aY, layoutSuccessors(this));
        } else {
          return Math.max(layoutPredecessors(this), aY);
        }
      }
      return aY;

    } else {
      aPosSet = true;
      aX = x;
      if (forward) {
        final int cnt = getPredecessors().size();
        int index = -1;
        int i = 0;
        for (final ProcessNode n : getPredecessors()) {
          if (n == source) {
            index = i;
            break;
          }
          ++i;
        }
        if (index >= 0) {
          aY = (y - ((index * VERTSEP))) + (((cnt - 1) * VERTSEP) / 2);
        } else {
          aY = y;
        }
      } else {
        aY = y;
      }
      return Math.max(layoutPredecessors(source), layoutSuccessors(source));
    }
  }

  private int layoutSuccessors(final ProcessNode source) {
    final Collection<ProcessNode> successors = getSuccessors();
    int posY = aY - (((successors.size() - 1) * VERTSEP) / 2);
    final int posX = aX + HORIZSEP;

    for (final ProcessNode successor : successors) {
      if (successor != source) {
        successor.layout(posX, posY, this, true);
      }
      posY += VERTSEP;
    }
    return Math.min(aY, posY - VERTSEP);
  }

  private int layoutPredecessors(final ProcessNode source) {
    final Collection<ProcessNode> predecessors = getPredecessors();
    int posY = aY - (((predecessors.size() - 1) * VERTSEP) / 2);
    final int posX = aX - HORIZSEP;

    for (final ProcessNode predecessor : predecessors) {
      if (predecessor != source) {
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

  public void offset(final int offsetX, final int offsetY) {
    aX += offsetX;
    aY += offsetY;
  }

}
