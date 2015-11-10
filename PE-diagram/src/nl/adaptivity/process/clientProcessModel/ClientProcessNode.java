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

  private String mId;

  private String mLabel;

  private double mX=Double.NaN;

  private double mY=Double.NaN;

  private List<IXmlResultType> mResults;

  private List<IXmlDefineType> mDefines;

  private ClientProcessModel<T> mOwner;

  private final ProcessNodeSet<Identifiable> mPredecessors;

  private final ProcessNodeSet<T> mSuccessors;

  protected ClientProcessNode() {
    this((String) null);
  }

  protected ClientProcessNode(final String id) {
    mId = id;
    mOwner = null;
    switch (getMaxPredecessorCount()) {
      case 0: mPredecessors = ProcessNodeSet.empty(); break;
      case 1: mPredecessors = ProcessNodeSet.singleton(); break;
      default: mPredecessors = ProcessNodeSet.processNodeSet();
    }
    switch (getMaxSuccessorCount()) {
      case 0: mSuccessors = ProcessNodeSet.<T>empty(); break;
      case 1: mSuccessors = ProcessNodeSet.<T>singleton(); break;
      default: mSuccessors = ProcessNodeSet.<T>processNodeSet();
    }
  }

  protected ClientProcessNode(final ClientProcessNode<T> orig) {
    this(orig.mId);
    mOwner = null;
    mX = orig.mX;
    mY = orig.mY;
    mResults = CollectionUtil.copy(orig.mResults);
    mDefines = CollectionUtil.copy(orig.mDefines);
    mLabel = orig.mLabel;

    mPredecessors.addAll(orig.mPredecessors);
    mSuccessors.addAll(orig.mSuccessors);
  }

  @Override
  public String getId() {
    return mId;
  }

  public void setId(String id) {
    mId = id;
  }

  @Override
  public String getLabel() {
    return mLabel;
  }

  public void setLabel(String label) {
    mLabel = label;
  }

  @Override
  public final void setPredecessors(Collection<? extends Identifiable> predecessors) {
    if (predecessors.size()>getMaxPredecessorCount()) {
      throw new IllegalArgumentException();
    }
    List<Identifiable> toRemove = new ArrayList<>(mPredecessors.size());
    for(Iterator<Identifiable> it = mPredecessors.iterator(); it.hasNext(); ) {
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
    List<T> toRemove = new ArrayList<>(mSuccessors.size());
    for(Iterator<T> it = mSuccessors.iterator();it.hasNext(); ) {
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
    if (mPredecessors.contains(predId)) { return; }
    if (mPredecessors.size()+1>getMaxPredecessorCount()) {
      throw new IllegalProcessModelException("Can not add more predecessors");
    }

    mPredecessors.add(predId);
    T pred = getOwner().getNode(predId);
    if (!pred.getSuccessors().contains(this)) {
      pred.addSuccessor(this.asT());
    }
    if (mOwner!=null) {
      mOwner.addNode(pred);
    }
  }

  @Override
  public final void addSuccessor(T node) {
    if (mSuccessors.contains(node)) { return; }
    if (mSuccessors.size()+1>getMaxSuccessorCount()) {
      throw new IllegalProcessModelException("Can not add more successors");
    }

    mSuccessors.add(node);
    if (!node.getPredecessors().contains(this)) {
      node.addPredecessor(this.asT());
    }
    if (mOwner!=null) {
      mOwner.addNode(node);
    }



    if (mSuccessors.add(node)) {
      @SuppressWarnings("unchecked")
      final T pred = (T) this;
      if (!node.getPredecessors().contains(pred)) {
        node.addPredecessor(pred);
      }
    }
  }

  @Override
  public final ProcessNodeSet<? extends Identifiable> getPredecessors() {
    return mPredecessors.readOnly();
  }

  @Override
  public final ProcessNodeSet<T> getSuccessors() {
    return mSuccessors.readOnly();
  }

  @Override
  public final void removePredecessor(Identifiable node) {
    if (mPredecessors.remove(node)) {
      getOwner().getNode(node).removeSuccessor(this.asT());
    }
  }

  @Override
  public final void removeSuccessor(T node) {
    if (mSuccessors.remove(node)) {
      node.removePredecessor(this.asT());
    }
  }

  @Override
  public void disconnect() {
    final T me = this.asT();
    for(Iterator<Identifiable> it=mPredecessors.iterator(); it.hasNext();) {
      Identifiable pred = it.next();
      it.remove(); // Remove first, otherwise we get strange iterator concurrent modification effects.
      getOwner().getNode(pred).removeSuccessor(me);
    }
    for(Iterator<? extends T> it = mSuccessors.iterator(); it.hasNext();) {
      T suc = it.next();
      it.remove();
      suc.removePredecessor(me);
    }
  }

  @Override
  public final boolean isPredecessorOf(T node) {
    return mSuccessors.contains(node);
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
    return mResults;
  }

  protected void setResults(Collection<? extends IXmlResultType> exports) {
    mResults = CollectionUtil.copy(exports);
  }

  @Override
  public List<IXmlDefineType> getDefines() {
    return mDefines;
  }

  protected void setDefines(Collection<? extends IXmlDefineType> imports) {
    mDefines = CollectionUtil.copy(imports);
  }

  public void unsetPos() {
    mX = Double.NaN;
    mY = Double.NaN;
  }

  public boolean hasPos() {
    return !Double.isNaN(mX) && !Double.isNaN(mY);
  }

  @Override
  public double getX() {
    return mX;
  }

  @Override
  public double getY() {
    return mY;
  }

  @Override
  public void setX(double x) {
    mX = x;
    if (mOwner!=null) mOwner.nodeChanged(this.asT());
  }

  @Override
  public void setY(double y) {
    mY = y;
    if (mOwner!=null) mOwner.nodeChanged(this.asT());
  }

  public void offset(final int offsetX, final int offsetY) {
    mX += offsetX;
    mY += offsetY;
    if (mOwner!=null) mOwner.nodeChanged(this.asT());
  }

  @Override
  public String toString() {
    String nm = getClass().getSimpleName();
    if (nm.startsWith("Client")) { nm = nm.substring(6); }
    if (nm.startsWith("Drawable")) { nm = nm.substring(8); }
    if (nm.endsWith("Node")) { nm = nm.substring(0, nm.length()-4); }

    return nm+"[id=" + mId + '(' + mX + ", " + mY + ")";
  }

  @SuppressWarnings("unchecked")
  public T asT() {
    return (T) this;
  }

  @Override
  public void setOwner(ClientProcessModel<T> owner) {
    mOwner = owner;
  }

  @Override
  public final ClientProcessModel<T> getOwner() {
    return mOwner;
  }

  public void serializeCommonAttrs(SerializerAdapter out) {
    out.addAttribute(null, "id", mId);
    if (mLabel!=null) { out.addAttribute(null, "label", mLabel); }
    if (!Double.isNaN(mX)) { out.addAttribute(null, "x", Double.toString(mX)); }
    if (!Double.isNaN(mY)) { out.addAttribute(null, "y", Double.toString(mY)); }
    if (getMaxPredecessorCount()==1 && mPredecessors.size()==1) {
      out.addAttribute(null, "predecessor", mPredecessors.get(0).getId());
    }
  }

  public void serializeCommonChildren(SerializerAdapter out) {
    // TODO handle imports and exports.
  }
}
