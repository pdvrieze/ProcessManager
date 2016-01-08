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

package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.Collection;
import java.util.Map;


public abstract class ProcessNode {

  private static final int HORIZSEP = 100;

  private static final int VERTSEP = 60;

  private final String mId;

  private boolean mPosSet = false;

  private int mX;

  private int mY;

  protected ProcessNode(final String id) {
    mId = id;
  }

  public String getId() {
    return mId;
  }

  public abstract void resolvePredecessors(Map<String, ProcessNode> map);

  public abstract void ensureSuccessor(ProcessNode node);

  public abstract Collection<ProcessNode> getSuccessors();

  public abstract Collection<ProcessNode> getPredecessors();

  public void unsetPos() {
    mPosSet = false;
  }

  public boolean hasPos() {
    return mPosSet;
  }

  public int layout(final int x, final int y, final ProcessNode source, final boolean forward) {
    if (mPosSet) {
      boolean dx = false;
      boolean dy = false;
      if (x != mX) {
        if (forward) {
          if (x > mX) {
            mX = x;
            dx = true;
          } else { // pX < mX
            mX -= (mX - x) / 2; // center
          }
        } else {
          if (x < mX) {
            mX = x;
            dx = true;
          } else { // pX > mX
            mX += (x - mX) / 2; // center
          }
        }
      }
      if (y != mY) {
        if (y > mY) {
          mY = y;
          dy = true;
        } else {

        }
      }
      if (dx || dy) {
        if (forward) {
          return Math.max(mY, layoutSuccessors(this));
        } else {
          return Math.max(layoutPredecessors(this), mY);
        }
      }
      return mY;

    } else {
      mPosSet = true;
      mX = x;
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
          mY = (y - ((index * VERTSEP))) + (((cnt - 1) * VERTSEP) / 2);
        } else {
          mY = y;
        }
      } else {
        mY = y;
      }
      return Math.max(layoutPredecessors(source), layoutSuccessors(source));
    }
  }

  private int layoutSuccessors(final ProcessNode source) {
    final Collection<ProcessNode> successors = getSuccessors();
    int posY = mY - (((successors.size() - 1) * VERTSEP) / 2);
    final int posX = mX + HORIZSEP;

    for (final ProcessNode successor : successors) {
      if (successor != source) {
        successor.layout(posX, posY, this, true);
      }
      posY += VERTSEP;
    }
    return Math.min(mY, posY - VERTSEP);
  }

  private int layoutPredecessors(final ProcessNode source) {
    final Collection<ProcessNode> predecessors = getPredecessors();
    int posY = mY - (((predecessors.size() - 1) * VERTSEP) / 2);
    final int posX = mX - HORIZSEP;

    for (final ProcessNode predecessor : predecessors) {
      if (predecessor != source) {
        predecessor.layout(posX, posY, this, false);
      }
      posY += VERTSEP;
    }
    return Math.min(mY, posY - VERTSEP);
  }

  public int getX() {
    return mX;
  }

  public int getY() {
    return mY;
  }

  public void offset(final int offsetX, final int offsetY) {
    mX += offsetX;
    mY += offsetY;
  }

}
