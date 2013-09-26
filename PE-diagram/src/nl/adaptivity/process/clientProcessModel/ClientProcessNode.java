package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.IXmlImportType;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public abstract class ClientProcessNode<T extends IClientProcessNode<T>> implements IClientProcessNode<T>{

  private static final int HORIZSEP = 100;

  private static final int VERTSEP = 60;

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

  protected abstract void setPredecessor(T pPredecessor);

  @Override
  public void setSuccessors(Collection<? extends T> pSuccessors) {
    if (pSuccessors.size()!=1) {
      throw new IllegalArgumentException();
    }
    setSuccessor(pSuccessors.iterator().next());
  }

  protected abstract void setSuccessor(T pSuccessor);



  @Override
  public abstract ProcessNodeSet<? extends T> getPredecessors();

  @Override
  public abstract ProcessNodeSet<? extends T> getSuccessors();

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
    return aX!=Double.NaN && aY!=Double.NaN;
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

}
