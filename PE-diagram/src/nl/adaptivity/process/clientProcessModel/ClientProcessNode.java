package nl.adaptivity.process.clientProcessModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

  protected ClientProcessNode() {
    this((String) null);
  }

  protected ClientProcessNode(final String pId) {
    aId = pId;
    aOwner = null;
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
    this(pOrig.aId);
    aOwner = null;
    aX = pOrig.aX;
    aY = pOrig.aY;
    aImports = CollectionUtil.copy(pOrig.aImports);
    aExports = CollectionUtil.copy(pOrig.aExports);
    aLabel = pOrig.aLabel;

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
    List<T> toRemove = new ArrayList<>(aPredecessors.size());
    for(Iterator<T> it = aPredecessors.iterator();it.hasNext(); ) {
      T item = it.next();
      if (pPredecessors.contains(item)) {
        pPredecessors.remove(item);
      } else {
        toRemove.add(item);
        it.remove();
      }
    }
    for(T oldPred: toRemove) {
      removePredecessor(oldPred);
    }
    for(T pred: pPredecessors) {
      addPredecessor(pred);
    }
  }

  @Override
  public final void setSuccessors(Collection<? extends T> pSuccessors) {
    if (pSuccessors.size()>getMaxSuccessorCount()) {
      throw new IllegalArgumentException();
    }
    List<T> toRemove = new ArrayList<>(aSuccessors.size());
    for(Iterator<T> it = aSuccessors.iterator();it.hasNext(); ) {
      T item = it.next();
      if (pSuccessors.contains(item)) {
        pSuccessors.remove(item);
      } else {
        toRemove.add(item);
        it.remove();
      }
    }
    for(T oldSuc: toRemove) {
      removeSuccessor(oldSuc);
    }
    for(T suc: pSuccessors) {
      addSuccessor(suc);
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
      pred.addSuccessor(this.asT());
    }
    if (aOwner!=null) {
      aOwner.addNode(pred);
    }
  }

  @Override
  public final void addSuccessor(T pNode) {
    if (aSuccessors.contains(pNode)) { return; }
    if (aSuccessors.size()+1>getMaxSuccessorCount()) {
      throw new IllegalProcessModelException("Can not add more successors");
    }

    aSuccessors.add(pNode);
    if (!pNode.getPredecessors().contains(this)) {
      pNode.addPredecessor(this.asT());
    }
    if (aOwner!=null) {
      aOwner.addNode(pNode);
    }



    if (aSuccessors.add(pNode)) {
      @SuppressWarnings("unchecked")
      final T pred = (T) this;
      if (!pNode.getPredecessors().contains(pred)) {
        pNode.addPredecessor(pred);
      }
    }
  }

  @Override
  public final ProcessNodeSet<T> getPredecessors() {
    return aPredecessors.readOnly();
  }

  @Override
  public final ProcessNodeSet<T> getSuccessors() {
    return aSuccessors.readOnly();
  }

  @Override
  public final void removePredecessor(T pNode) {
    if (aPredecessors.remove(pNode)) {
      pNode.removeSuccessor(this.asT());
    }
  }

  @Override
  public final void removeSuccessor(T pNode) {
    if (aSuccessors.remove(pNode)) {
      pNode.removePredecessor(this.asT());
    }
  }

  @Override
  public void disconnect() {
    final T me = this.asT();
    for(Iterator<T> it=aPredecessors.iterator(); it.hasNext();) {
      T pred = it.next();
      it.remove(); // Remove first, otherwise we get strange iterator concurrent modification effects.
      pred.removeSuccessor(me);
    }
    for(Iterator<? extends T> it = aSuccessors.iterator(); it.hasNext();) {
      T suc = it.next();
      it.remove();
      suc.removePredecessor(me);
    }
  }

  @Override
  public final boolean isPredecessorOf(T pNode) {
    return aSuccessors.contains(pNode);
  }

  @Override
  public int getMaxPredecessorCount() {
    return 1;
  }

  @Override
  public int getMaxSuccessorCount() {
    return 1;//Integer.MAX_VALUE;
  }

  @Override
  public List<IXmlImportType> getImports() {
    return aImports;
  }

  protected void setImports(Collection<? extends IXmlImportType> pImports) {
    aImports = CollectionUtil.copy(pImports);
  }

  @Override
  public List<IXmlExportType> getExports() {
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
  public final ClientProcessModel<T> getOwner() {
    return aOwner;
  }

  public void serializeCommonAttrs(SerializerAdapter pOut) {
    pOut.addAttribute(null, "id", aId);
    if (aLabel!=null) { pOut.addAttribute(null, "label", aLabel); }
    if (!Double.isNaN(aX)) { pOut.addAttribute(null, "x", Double.toString(aX)); }
    if (!Double.isNaN(aY)) { pOut.addAttribute(null, "y", Double.toString(aY)); }
    if (getMaxPredecessorCount()==1 && aPredecessors.size()==1) {
      pOut.addAttribute(null, "predecessor", aPredecessors.get(0).getId());
    }
  }

  public void serializeCommonChildren(SerializerAdapter pOut) {
    // TODO handle imports and exports.
  }
}
