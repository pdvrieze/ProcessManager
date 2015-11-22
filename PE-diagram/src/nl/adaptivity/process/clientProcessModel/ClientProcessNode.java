package nl.adaptivity.process.clientProcessModel;

import net.devrieze.util.CollectionUtil;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public abstract class ClientProcessNode<T extends IClientProcessNode<T>> implements IClientProcessNode<T>{

  private final boolean mCompat;

  private String mId;

  private String mLabel;

  private double mX=Double.NaN;

  private double mY=Double.NaN;

  private List<IXmlResultType> mResults;

  private List<IXmlDefineType> mDefines;

  private ClientProcessModel<T> mOwner;

  private final ProcessNodeSet<Identifiable> mPredecessors;

  private final ProcessNodeSet<Identifiable> mSuccessors;

  protected ClientProcessNode(final boolean compat) {
    this((String) null, compat);
  }

  protected ClientProcessNode(final String id, final boolean compat) {
    mId = id;
    mCompat = compat;
    mOwner = null;
    switch (getMaxPredecessorCount()) {
      case 0: mPredecessors = ProcessNodeSet.empty(); break;
      case 1: mPredecessors = ProcessNodeSet.singleton(); break;
      default: mPredecessors = ProcessNodeSet.processNodeSet();
    }
    switch (getMaxSuccessorCount()) {
      case 0: mSuccessors = ProcessNodeSet.empty(); break;
      case 1: mSuccessors = ProcessNodeSet.singleton(); break;
      default: mSuccessors = ProcessNodeSet.processNodeSet();
    }
  }

  protected ClientProcessNode(final ClientProcessNode<T> orig, final boolean compat) {
    this(orig.mId, compat);
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
  public final void setSuccessors(@NotNull Collection<? extends Identifiable> successors) {
    if (successors.size()>getMaxSuccessorCount()) {
      throw new IllegalArgumentException();
    }
    List<Identifiable> toRemove = new ArrayList<>(mSuccessors.size());
    for(Iterator<Identifiable> it = mSuccessors.iterator(); it.hasNext(); ) {
      Identifiable item = it.next();
      if (successors.contains(item)) {
        successors.remove(item);
      } else {
        toRemove.add(item);
        it.remove();
      }
    }
    for(Identifiable oldSuc: toRemove) {
      removeSuccessor(oldSuc);
    }
    for(Identifiable suc: successors) {
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
    T pred = predId instanceof ClientProcessNode ? (T) ((ClientProcessNode) predId).asT() : (T) null;

    if (pred==null && mOwner!=null) {
      pred = getOwner().getNode(predId);
      mOwner.addNode(pred);
    }
    if (pred!=null) {
      if (!pred.getSuccessors().contains(this)) {
        pred.addSuccessor(this.asT());
      }
    }
  }

  @Override
  public final void addSuccessor(Identifiable id) {
    if (mSuccessors.contains(id)) { return; }
    if (mSuccessors.size()+1>getMaxSuccessorCount()) {
      throw new IllegalProcessModelException("Can not add more successors");
    }

    mSuccessors.add(id);
    ClientProcessModel<T> owner = getOwner();
    ProcessNode<?> node = null;
    if (owner!=null) {
      T node2 = owner.asNode(id);
      owner.addNode((T) node2);
      node = node2;
    } else if (id instanceof ProcessNode){
      node = (ProcessNode<?>) id;
    }
    if (node!=null) {
      Set<? extends Identifiable> predecessors = node.getPredecessors();
      if (predecessors!=null && !predecessors.contains(this)) {
        node.addPredecessor(this.asT());
      }
    }

  }

  @Override
  public final ProcessNodeSet<? extends Identifiable> getPredecessors() {
    return mPredecessors.readOnly();
  }

  @Override
  public final Set<? extends Identifiable> getSuccessors() {
    return mSuccessors.readOnly();
  }

  @Override
  public final void removePredecessor(Identifiable node) {
    if (mPredecessors.remove(node)) {
      ClientProcessModel<T> owner = getOwner();
      T drawableNode;
      if (owner!=null && (drawableNode = owner.getNode(node))!=null) {
        drawableNode.removeSuccessor(this.asT()); }
    }
  }

  @Override
  public final void removeSuccessor(Identifiable node) {
    if (mSuccessors.remove(node)) {
      ProcessNode successorNode = node instanceof ProcessNode ? (ProcessNode) node : (mOwner==null ? null : mOwner.asNode(node));
      if (successorNode!=null) { successorNode.removePredecessor(this.asT()); }
    }
  }

  @Override
  public void disconnect() {
    final T me = this.asT();
    for (Iterator<Identifiable> it = mPredecessors.iterator(); it.hasNext(); ) {
      Identifiable pred = it.next();
      it.remove(); // Remove first, otherwise we get strange iterator concurrent modification effects.
      getOwner().getNode(pred).removeSuccessor(me);
    }
    for (Iterator<Identifiable> it = mSuccessors.iterator(); it.hasNext(); ) {
      Identifiable sucId = it.next();
      it.remove();
      T suc = getOwner().asNode(sucId);
      if (suc != null) { suc.removePredecessor(me); }
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
    return mCompat ? Integer.MAX_VALUE : 1;
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

  public boolean isCompat() {
    return mCompat;
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
  public void setOwner(ProcessModelBase<T> owner) {
    if (mOwner!=null) {
      if (mOwner.equals(owner)) return; // If they are the same, do nothing

      ClientProcessModel<T> oldOwner = mOwner;
      mOwner.removeNode(this.asT());
    }
    mOwner = (ClientProcessModel<T>) owner;
    if (mOwner!=null) {
      mOwner.addNode(this.asT());
    }
  }

  @Override
  public final ClientProcessModel<T> getOwner() {
    return mOwner;
  }

  public void serializeCommonAttrs(XmlWriter out) throws XmlException {
    out.attribute(null, "id", null, mId);
    if (mLabel!=null) { out.attribute(null, "label", null, mLabel); }
    if (!Double.isNaN(mX)) { out.attribute(null, "x", null, Double.toString(mX)); }
    if (!Double.isNaN(mY)) { out.attribute(null, "y", null, Double.toString(mY)); }
    if (getMaxPredecessorCount()==1 && mPredecessors.size()==1) {
      out.attribute(null, "predecessor", null, mPredecessors.get(0).getId());
    }
  }

  public void serializeCommonChildren(XmlWriter out) {
    // TODO handle imports and exports.
  }

  @Override
  public void resolveRefs() {
    mPredecessors.resolve(getOwner());
  }
}
