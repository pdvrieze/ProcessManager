package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;

import net.devrieze.util.CollectionUtil;
import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.IXmlImportType;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public abstract class ClientProcessNode<T extends IClientProcessNode<T>> implements IClientProcessNode<T>{

  private String aId;

  private double aX=Double.NaN;

  private double aY=Double.NaN;

  private List<IXmlImportType> aImports;

  private List<IXmlExportType> aExports;;

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
  public void setPredecessors(Collection<? extends T> pPredecessors) {
    if (pPredecessors.size()!=1) {
      throw new IllegalArgumentException();
    }
    setPredecessor(pPredecessors.iterator().next());
  }

  public void setPredecessor(T pPredecessor) {
    if (getPredecessors().size()==1 && getPredecessors().get(0).equals(pPredecessor)) {
      return; // Don't change
    }
    getPredecessors().clear();
    getPredecessors().add(pPredecessor);
    if(! pPredecessor.getSuccessors().contains(this)) {
      @SuppressWarnings("unchecked")
      T suc = (T) this;
      pPredecessor.addSuccessor(suc);
    }
  }

  @Override
  public void setSuccessors(Collection<? extends T> pSuccessors) {
    if (pSuccessors.size()!=1) {
      throw new IllegalArgumentException();
    }
    addSuccessor(pSuccessors.iterator().next());
  }

  public void setSuccessor(T pSuccessor) {
    if (getSuccessors().size()==1 && getSuccessors().get(0).equals(pSuccessor)) {
      return; // Don't change
    }
    getSuccessors().clear();
    addSuccessor(pSuccessor);
  }



  @Override
  public abstract ProcessNodeSet<T> getPredecessors();

  @Override
  public abstract ProcessNodeSet<T> getSuccessors();

  protected List<IXmlImportType> getImports() {
    return aImports;
  }

  protected void setImports(Collection<? extends IXmlImportType> pImports) {
    aImports = CollectionUtil.copy(pImports);
  }

  protected List<IXmlExportType> getExports() {
    return aExports;
  }

  protected void setExports(Collection<? extends IXmlExportType> pExports) {
    aExports = CollectionUtil.copy(pExports);
  }

  public void unsetPos() {
    aX = Double.NaN;
    aY = Double.NaN;
  }

  public boolean hasPos() {
    return !Double.isNaN(aX) && !Double.isNaN(aY);
  }

  @Override
  public double getX() {
    return aX;
  }

  @Override
  public double getY() {
    return aY;
  }

  @Override
  public void setX(double pX) {
    aX = pX;
  }

  @Override
  public void setY(double pY) {
    aY = pY;
  }

  public void offset(final int pOffsetX, final int pOffsetY) {
    aX += pOffsetX;
    aY += pOffsetY;
  }

  @Override
  public String toString() {
    String nm = getClass().getSimpleName();
    if (nm.startsWith("Client")) { nm = nm.substring(6); }
    if (nm.startsWith("Drawable")) { nm = nm.substring(8); }
    if (nm.endsWith("Node")) { nm = nm.substring(0, nm.length()-4); }
    
    return nm+"[id=" + aId + '(' + aX + ", " + aY + ")";
  }
  
  

}
