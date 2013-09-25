package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.XmlExportType;
import nl.adaptivity.process.processModel.XmlImportType;


public abstract class ClientProcessNode implements ProcessNode{

  private static final int HORIZSEP = 100;

  private static final int VERTSEP = 60;

  private String aId;

  private boolean aPosSet = false;

  private double aX;

  private double aY;

  private List<XmlImportType> aImports;

  private List<XmlExportType> aExports;;

  protected ClientProcessNode() {

  }

  protected ClientProcessNode(final String pId) {
    aId = pId;
  }

  @Override
  public String getId() {
    return aId;
  }

  public void setId(String pId) {
    aId = pId;
  }

  @Override
  public void setPredecessors(Collection<? extends ProcessNode> pPredecessors) {
    if (pPredecessors.size()!=1) {
      throw new IllegalArgumentException();
    }
    setPredecessor(pPredecessors.iterator().next());
  }

  protected abstract void setPredecessor(ProcessNode pPredecessor);

  @Override
  public void setSuccessors(Collection<? extends ProcessNode> pSuccessors) {
    if (pSuccessors.size()!=1) {
      throw new IllegalArgumentException();
    }
    setSuccessor(pSuccessors.iterator().next());
  }

  protected abstract void setSuccessor(ProcessNode pSuccessor);



  @Override
  public abstract Set<? extends ClientProcessNode> getPredecessors();

  @Override
  public abstract Set<? extends ClientProcessNode> getSuccessors();

  protected List<XmlImportType> getImports() {
    return aImports;
  }

  protected void setImports(Collection<? extends XmlImportType> pImports) {
    aImports = CollectionUtil.copy(pImports);
  }

  protected List<XmlExportType> getExports() {
    return aExports;
  }

  protected void setExports(Collection<? extends XmlExportType> pExports) {
    aExports = CollectionUtil.copy(pExports);
  }

  public void unsetPos() {
    aPosSet = false;
  }

  public boolean hasPos() {
    return aPosSet;
  }

  public double layout(final double pX, final double pY, final ClientProcessNode pSource, final boolean pForward) {
    if (aPosSet) {
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
        final int cnt = getPredecessors().size();
        int index = -1;
        int i = 0;
        for (final ProcessNode n : getPredecessors()) {
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

  private double layoutSuccessors(final ProcessNode pSource) {
    final Collection<DrawableProcessNode> successors = getSuccessors();
    double posY = aY - (((successors.size() - 1) * VERTSEP) / 2);
    final double posX = aX + HORIZSEP;

    for (final ProcessNode successor : successors) {
      if (successor != pSource) {
        successor.layout(posX, posY, this, true);
      }
      posY += VERTSEP;
    }
    return Math.min(aY, posY - VERTSEP);
  }

  private double layoutPredecessors(final ProcessNode pSource) {
    final Collection<ProcessNode> predecessors = getPredecessors();
    int posY = aY - (((predecessors.size() - 1) * VERTSEP) / 2);
    final int posX = aX - HORIZSEP;

    for (final ProcessNode predecessor : predecessors) {
      if (predecessor != pSource) {
        predecessor.layout(posX, posY, this, false);
      }
      posY += VERTSEP;
    }
    return Math.min(aY, posY - VERTSEP);
  }

  @Override
  public double getX() {
    return aX;
  }

  @Override
  public double getY() {
    return aY;
  }

  public void setX(double pX) {
    aX = pX;
  }

  public void setY(double pY) {
    aY = pY;
  }

  public void offset(final int pOffsetX, final int pOffsetY) {
    aX += pOffsetX;
    aY += pOffsetY;
  }

}
