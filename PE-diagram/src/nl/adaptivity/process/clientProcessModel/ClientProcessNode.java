package nl.adaptivity.process.clientProcessModel;

import net.devrieze.util.CollectionUtil;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.util.Identifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


public abstract class ClientProcessNode<T extends IClientProcessNode<T>> implements IClientProcessNode<T>{

  private String aId;

  private String aLabel;

  private double aX=Double.NaN;

  private double aY=Double.NaN;

  private List<IXmlResultType> aResults;

  private List<IXmlDefineType> aDefines;

  private ClientProcessModel<T> aOwner;

  private final ProcessNodeSet<Identifiable> aPredecessors;

  private final ProcessNodeSet<T> aSuccessors;

  protected ClientProcessNode() {
    this((String) null);
  }

  protected ClientProcessNode(final String id) {
    aId = id;
    aOwner = null;
    switch (getMaxPredecessorCount()) {
      case 0: aPredecessors = ProcessNodeSet.empty(); break;
      case 1: aPredecessors = ProcessNodeSet.singleton(); break;
      default: aPredecessors = ProcessNodeSet.processNodeSet();
    }
    switch (getMaxSuccessorCount()) {
      case 0: aSuccessors = ProcessNodeSet.<T>empty(); break;
      case 1: aSuccessors = ProcessNodeSet.<T>singleton(); break;
      default: aSuccessors = ProcessNodeSet.<T>processNodeSet();
    }
  }

  protected ClientProcessNode(final ClientProcessNode<T> orig) {
    this(orig.aId);
    aOwner = null;
    aX = orig.aX;
    aY = orig.aY;
    aResults = CollectionUtil.copy(orig.aResults);
    aDefines = CollectionUtil.copy(orig.aDefines);
    aLabel = orig.aLabel;

    aPredecessors.addAll(orig.aPredecessors);
    aSuccessors.addAll(orig.aSuccessors);
  }

  @Override
  public String getId() {
    return aId;
  }

  public void setId(String id) {
    aId = id;
  }

  @Override
  public String getLabel() {
    return aLabel;
  }

  public void setLabel(String label) {
    aLabel = label;
  }

  @Override
  public final void setPredecessors(Collection<? extends Identifiable> predecessors) {
    if (predecessors.size()>getMaxPredecessorCount()) {
      throw new IllegalArgumentException();
    }
    List<Identifiable> toRemove = new ArrayList<>(aPredecessors.size());
    for(Iterator<Identifiable> it = aPredecessors.iterator(); it.hasNext(); ) {
      Identifiable item = it.next();
      if (predecessors.contains(item)) {
        predecessors.remove(item);
      } else {
        toRemove.add(item);
        it.remove();
      }
    }
    for(Identifiable oldPred: toRemove) {
      removePredecessor(oldPred);
    }
    for(Identifiable pred: predecessors) {
      addPredecessor(pred);
    }
  }

  @Override
  public final void setSuccessors(Collection<? extends T> successors) {
    if (successors.size()>getMaxSuccessorCount()) {
      throw new IllegalArgumentException();
    }
    List<T> toRemove = new ArrayList<>(aSuccessors.size());
    for(Iterator<T> it = aSuccessors.iterator();it.hasNext(); ) {
      T item = it.next();
      if (successors.contains(item)) {
        successors.remove(item);
      } else {
        toRemove.add(item);
        it.remove();
      }
    }
    for(T oldSuc: toRemove) {
      removeSuccessor(oldSuc);
    }
    for(T suc: successors) {
      addSuccessor(suc);
    }
  }

  @Override
  public final void addPredecessor(Identifiable predId) {
    if (aPredecessors.contains(predId)) { return; }
    if (aPredecessors.size()+1>getMaxPredecessorCount()) {
      throw new IllegalProcessModelException("Can not add more predecessors");
    }

    aPredecessors.add(predId);
    T pred = getOwner().getNode(predId);
    if (!pred.getSuccessors().contains(this)) {
      pred.addSuccessor(this.asT());
    }
    if (aOwner!=null) {
      aOwner.addNode(pred);
    }
  }

  @Override
  public final void addSuccessor(T node) {
    if (aSuccessors.contains(node)) { return; }
    if (aSuccessors.size()+1>getMaxSuccessorCount()) {
      throw new IllegalProcessModelException("Can not add more successors");
    }

    aSuccessors.add(node);
    if (!node.getPredecessors().contains(this)) {
      node.addPredecessor(this.asT());
    }
    if (aOwner!=null) {
      aOwner.addNode(node);
    }



    if (aSuccessors.add(node)) {
      @SuppressWarnings("unchecked")
      final T pred = (T) this;
      if (!node.getPredecessors().contains(pred)) {
        node.addPredecessor(pred);
      }
    }
  }

  @Override
  public final ProcessNodeSet<? extends Identifiable> getPredecessors() {
    return aPredecessors.readOnly();
  }

  @Override
  public final ProcessNodeSet<T> getSuccessors() {
    return aSuccessors.readOnly();
  }

  @Override
  public final void removePredecessor(Identifiable node) {
    if (aPredecessors.remove(node)) {
      getOwner().getNode(node).removeSuccessor(this.asT());
    }
  }

  @Override
  public final void removeSuccessor(T node) {
    if (aSuccessors.remove(node)) {
      node.removePredecessor(this.asT());
    }
  }

  @Override
  public void disconnect() {
    final T me = this.asT();
    for(Iterator<Identifiable> it=aPredecessors.iterator(); it.hasNext();) {
      Identifiable pred = it.next();
      it.remove(); // Remove first, otherwise we get strange iterator concurrent modification effects.
      getOwner().getNode(pred).removeSuccessor(me);
    }
    for(Iterator<? extends T> it = aSuccessors.iterator(); it.hasNext();) {
      T suc = it.next();
      it.remove();
      suc.removePredecessor(me);
    }
  }

  @Override
  public final boolean isPredecessorOf(T node) {
    return aSuccessors.contains(node);
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
  public List<IXmlResultType> getResults() {
    return aResults;
  }

  protected void setResults(Collection<? extends IXmlResultType> exports) {
    aResults = CollectionUtil.copy(exports);
  }

  @Override
  public List<IXmlDefineType> getDefines() {
    return aDefines;
  }

  protected void setDefines(Collection<? extends IXmlDefineType> imports) {
    aDefines = CollectionUtil.copy(imports);
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
  public void setX(double x) {
    aX = x;
    if (aOwner!=null) aOwner.nodeChanged(this.asT());
  }

  @Override
  public void setY(double y) {
    aY = y;
    if (aOwner!=null) aOwner.nodeChanged(this.asT());
  }

  public void offset(final int offsetX, final int offsetY) {
    aX += offsetX;
    aY += offsetY;
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
  public void setOwner(ClientProcessModel<T> owner) {
    aOwner = owner;
  }

  @Override
  public final ClientProcessModel<T> getOwner() {
    return aOwner;
  }

  public void serializeCommonAttrs(SerializerAdapter out) {
    out.addAttribute(null, "id", aId);
    if (aLabel!=null) { out.addAttribute(null, "label", aLabel); }
    if (!Double.isNaN(aX)) { out.addAttribute(null, "x", Double.toString(aX)); }
    if (!Double.isNaN(aY)) { out.addAttribute(null, "y", Double.toString(aY)); }
    if (getMaxPredecessorCount()==1 && aPredecessors.size()==1) {
      out.addAttribute(null, "predecessor", aPredecessors.get(0).getId());
    }
  }

  public void serializeCommonChildren(SerializerAdapter out) {
    // TODO handle imports and exports.
  }
}
