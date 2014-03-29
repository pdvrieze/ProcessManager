package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.List;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.IXmlImportType;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public abstract class ClientProcessNode<T extends IClientProcessNode<T>> implements IClientProcessNode<T>{

  private String aId;

  private String aLabel;

  private double aX=Double.NaN;

  private double aY=Double.NaN;

  private List<IXmlImportType> aImports;

  private List<IXmlExportType> aExports;

  private ClientProcessModel<T> aOwner;

  private final ProcessNodeSet<T> aPredecessors;

  private final ProcessNodeSet<T> aSuccessors;


  protected ClientProcessNode(ClientProcessModel<T> pOwner) {
    this(null, pOwner);
  }

  protected ClientProcessNode(final String pId, ClientProcessModel<T> pOwner) {
    aId = pId;
    aOwner = pOwner;
    switch (getMaxPredecessorCount()) {
      case 0: aPredecessors = ProcessNodeSet.<T>empty(); break;
      case 1: aPredecessors = ProcessNodeSet.<T>singleton(); break;
      default: aPredecessors = ProcessNodeSet.<T>processNodeSet();
    }
    switch (getMaxSuccessorCount()) {
      case 0: aSuccessors = ProcessNodeSet.<T>empty(); break;
      case 1: aSuccessors = ProcessNodeSet.<T>singleton(); break;
      default: aSuccessors = ProcessNodeSet.<T>processNodeSet();
    }
  }

  protected ClientProcessNode(final ClientProcessNode<T> pOrig) {
    this(pOrig.aId, pOrig.aOwner);
    aX = pOrig.aX;
    aY = pOrig.aY;
    aImports = CollectionUtil.copy(pOrig.aImports);
    aExports = CollectionUtil.copy(pOrig.aExports);

    aPredecessors.addAll(pOrig.aPredecessors);
    aSuccessors.addAll(pOrig.aSuccessors);
  }

  @Override
  public String getId() {
    return aId;
  }

  public void setId(String pId) {
    aId = pId;
  }

  @Override
  public String getLabel() {
    return aLabel;
  }

  public void setLabel(String pLabel) {
    aLabel = pLabel;
  }

  @Override
  public final void setPredecessors(Collection<? extends T> pPredecessors) {
    if (pPredecessors.size()>getMaxPredecessorCount()) {
      throw new IllegalArgumentException();
    }
    aPredecessors.retainAll(pPredecessors);
    pPredecessors.removeAll(aPredecessors);
    for(T pred: pPredecessors) {
      addPredecessor(pred);
    }
  }

  @Override
  public final void addPredecessor(T pred) {
    if (aPredecessors.contains(pred)) { return; }
    if (aPredecessors.size()+1>getMaxPredecessorCount()) {
      throw new IllegalProcessModelException("Can not add more predecessors");
    }

    aPredecessors.add(pred);
    if (!pred.getSuccessors().contains(this)) {
      @SuppressWarnings("unchecked")
      T suc = (T) this;
      pred.addSuccessor(suc);
    }
  }

  @Deprecated
  public final void setPredecessor(T pPredecessor) {
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
  public final ProcessNodeSet<T> getPredecessors() {
    return aPredecessors;
  }

  @Override
  public final void removePredecessor(T pNode) {
    if (aPredecessors.remove(pNode)) {
      @SuppressWarnings("unchecked")
      T suc = (T) this;
      pNode.removeSuccessor(suc);
    }
  }

  @Override
  public int getMaxPredecessorCount() {
    return 1;
  }

  @Override
  public final void setSuccessors(Collection<? extends T> pSuccessors) {
    if (pSuccessors.size()>getMaxSuccessorCount()) {
      throw new IllegalArgumentException();
    }
    aSuccessors.retainAll(pSuccessors);
    pSuccessors.removeAll(aSuccessors);
    for(T pred: pSuccessors) {
      addSuccessor(pred);
    }
    if (pSuccessors.size()!=1) {
      throw new IllegalArgumentException();
    }
    addSuccessor(pSuccessors.iterator().next());
  }

  @Deprecated
  public final void setSuccessor(T pSuccessor) {
    if (aSuccessors.size()==1 && aSuccessors.get(0).equals(pSuccessor)) {
      return; // Don't change
    }
    getSuccessors().clear();
    getPredecessors().add(pSuccessor);
    if(! pSuccessor.getPredecessors().contains(this)) {
      @SuppressWarnings("unchecked")
      T pred = (T) this;
      pSuccessor.addPredecessor(pred);
    }
  }



  @Override
  public final ProcessNodeSet<T> getSuccessors() {
    return aSuccessors;
  }


  @Override
  public final boolean isPredecessorOf(T pNode) {
    return getSuccessors().contains(pNode);
  }

  @Override
  public final void addSuccessor(T pNode) {
    if (aSuccessors.add(pNode)) {
      @SuppressWarnings("unchecked")
      final T pred = (T) this;
      if (!pNode.getPredecessors().contains(pred)) {
        pNode.addPredecessor(pred);
      }
    }
  }

  @Override
  public final void removeSuccessor(T pNode) {
    if (aSuccessors.remove(pNode)) {
      @SuppressWarnings("unchecked")
      T pred = (T) this;
      pNode.removePredecessor(pred);
    }
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

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
    if (aOwner!=null) aOwner.nodeChanged(this.asT());
  }

  @Override
  public void setY(double pY) {
    aY = pY;
    if (aOwner!=null) aOwner.nodeChanged(this.asT());
  }

  public void offset(final int pOffsetX, final int pOffsetY) {
    aX += pOffsetX;
    aY += pOffsetY;
    if (aOwner!=null) aOwner.nodeChanged(this.asT());
  }

  @Override
  public String toString() {
    String nm = getClass().getSimpleName();
    if (nm.startsWith("Client")) { nm = nm.substring(6); }
    if (nm.startsWith("Drawable")) { nm = nm.substring(8); }
    if (nm.endsWith("Node")) { nm = nm.substring(0, nm.length()-4); }

    return nm+"[id=" + aId + '(' + aX + ", " + aY + ")";
  }

  @SuppressWarnings("unchecked")
  public T asT() {
    return (T) this;
  }

  @Override
  public void setOwner(ClientProcessModel<T> pOwner) {
    aOwner = pOwner;
  }

  @Override
  public ClientProcessModel<T> getOwner() {
    return aOwner;
  }
}
