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

package nl.adaptivity.process.processModel;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;

import java.util.*;


/**
 * A base class for process nodes. Works like {@link ProcessModelBase}
 * Created by pdvrieze on 23/11/15.
 */
public abstract class ProcessNodeBase<T extends ProcessNode<T, M>, M extends ProcessModelBase<T, M>> implements ProcessNode<T, M>, XmlDeserializable {

  public static final String ATTR_PREDECESSOR = "predecessor";
  @Nullable private M mOwnerModel;
  @Nullable private ProcessNodeSet<Identifiable> mPredecessors;
  @Nullable private ProcessNodeSet<Identifiable> mSuccessors = null;
  private String mId;
  private String mLabel;
  private double mX=Double.NaN;
  private double mY=Double.NaN;
  @Nullable private List<XmlDefineType> mDefines;
  @Nullable private List<XmlResultType> mResults;
  private int mHashCode = 0;

  public ProcessNodeBase(@Nullable final M ownerModel) {
    if (ownerModel!=null) {
      mOwnerModel = ownerModel;
      mOwnerModel.addNode(this.asT());
    }
  }

  /**
   * Copy constructor
   * @param orig Original
   */
  public ProcessNodeBase(final ProcessNode<?, ?> orig) {
    mPredecessors = toIdentifiers(orig.getPredecessors(), getMaxPredecessorCount());
    mSuccessors = toIdentifiers(orig.getSuccessors(), getMaxSuccessorCount());
    setId(orig.getId());
    setLabel(orig.getLabel());
    setX(orig.getX());
    setY(orig.getY());
    setDefines(orig.getDefines());
    setResults(orig.getResults());
  }

  private ProcessNodeSet<Identifiable> toIdentifiers(final Set<? extends Identifiable> identifiables, int maxSize) {
    if (identifiables==null) { return null; }
    final ProcessNodeSet<Identifiable> result;
    switch (maxSize) {
      case 0: result = ProcessNodeSet.empty(); break;
      case 1: if (identifiables.size() <= 1) { result = ProcessNodeSet.singleton(); break; }
      default: result = ProcessNodeSet.processNodeSet(identifiables.size());
    }
    for(Identifiable pred: identifiables) {
      if (pred instanceof Identifier) {
        result.add(pred);
      } else {
        result.add(new Identifier(pred.getId()));
      }
    }
    return result;
  }

  public void offset(final int offsetX, final int offsetY) {
    setX(getX()+ offsetX);
    setY(getY()+ offsetY);
    notifyChange();
  }

  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    XmlWriterUtil.writeAttribute(out, "id", getId());
    XmlWriterUtil.writeAttribute(out, "label", getLabel());
    XmlWriterUtil.writeAttribute(out, "x", getX());
    XmlWriterUtil.writeAttribute(out, "y", getY());
  }

  protected void serializeChildren(final XmlWriter out) throws XmlException {
    XmlWriterUtil.writeChildren(out, getResults());
    XmlWriterUtil.writeChildren(out, getDefines());
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, final CharSequence attributeValue) {
    String value = StringUtil.toString(attributeValue);
    if (XMLConstants.NULL_NS_URI.equals(attributeNamespace)) {
      switch (attributeLocalName.toString()) {
        case "id": setId(value); return true;
        case "label": setLabel(value); return true;
        case "x": setX(Double.parseDouble(value)); return true;
        case "y": setY(Double.parseDouble(value)); return true;
      }
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader in) {
    // do nothing
  }

  @Deprecated
  protected final void swapPredecessors(@NotNull final Collection<?> predecessors) {
    mHashCode = 0;
    mPredecessors=null;
    final List<ExecutableProcessNode> tmp = new ArrayList<>(predecessors.size());
    for(final Object pred:predecessors) {
      if (pred instanceof ExecutableProcessNode) {
        tmp.add((ExecutableProcessNode) pred);
      }
    }
    setPredecessors(tmp);
  }

  @Override
  public void addPredecessor(@NotNull final Identifiable predId) {
    mHashCode = 0;
    if (predId==this) { throw new IllegalArgumentException(); }
    if (mPredecessors!=null) {
      if (mPredecessors.containsKey(predId.getId())) { return; }
      if (mPredecessors.size() + 1 > getMaxPredecessorCount()) {
        throw new IllegalProcessModelException("Can not add more predecessors");
      }
    } else if (getMaxPredecessorCount()==1) {
      mPredecessors = ProcessNodeSet.singleton();
    } else {
      mPredecessors = ProcessNodeSet.processNodeSet(1);
    }
    if (mPredecessors.add(predId)) {
      M ownerModel = getOwnerModel();

      ProcessNode node = null;
      if (predId instanceof ProcessNode) {
        node = (ProcessNode) predId;
      } else if (ownerModel != null) {
        node = ownerModel.getNode(predId);
      }
      if (node!=null) {
        node.addSuccessor(this);
      }
    }

  }

  @Override
  public final void removePredecessor(final Identifiable predecessorId) {
    mHashCode = 0;
    if (mPredecessors.remove(predecessorId)) {
      M owner = mOwnerModel;
      T predecessor;
      if (owner!=null && (predecessor = owner.getNode(predecessorId))!=null) {
        predecessor.removeSuccessor(this.asT()); }
    }

    // TODO perhaps make this reciprocal
  }

  @Override
  public final void addSuccessor(@Nullable final Identifiable nodeId) {
    if (nodeId == null) {
      throw new IllegalProcessModelException("Adding Null process successors is illegal");
    }
    mHashCode = 0;
    if (mSuccessors == null) {
      mSuccessors = getMaxSuccessorCount()==1 ? ProcessNodeSet.singleton() : ProcessNodeSet.processNodeSet(1);
    } else {
      if (mSuccessors.contains(nodeId)) { return; }
      if (mSuccessors.size()+1>getMaxSuccessorCount()) {
        throw new IllegalProcessModelException("Can not add more successors");
      }
    }
    mSuccessors.add(nodeId);

    ProcessModelBase<T, M> owner = mOwnerModel;
    ProcessNode<?, M> node = null;
    if (owner!=null) {
      node = owner.getNode(nodeId);
    } else if (nodeId instanceof ProcessNode){
      node = (ProcessNode<?, M>) nodeId;
    }
    if (node!=null) {
      Set<? extends Identifiable> predecessors = node.getPredecessors();
      if (predecessors!=null && !predecessors.contains(this)) {
        node.addPredecessor(this.asT());
      }
    }
  }

  @Override
  public final void removeSuccessor(final Identifiable node) {
    if (mSuccessors.remove(node)) {
      mHashCode = 0;
      ProcessNode successorNode = node instanceof ProcessNode ? (ProcessNode) node : (mOwnerModel == null ? null : mOwnerModel.getNode(node));
      if (successorNode!=null) { successorNode.removePredecessor(this.asT()); }
    }
  }

  /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.ProcessNode#getPredecessors()
       */
  @Nullable
  @Override
  public final ProcessNodeSet<? extends Identifiable> getPredecessors() {
    if (mPredecessors == null) {
      mHashCode = 0;
      switch (getMaxPredecessorCount()) {
        case 0: mPredecessors = ProcessNodeSet.empty(); break;
        case 1: mPredecessors = ProcessNodeSet.singleton(); break;
        default: mPredecessors = ProcessNodeSet.processNodeSet(); break;
      }
    }
    return mPredecessors;
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#setPredecessors(java.util.Collection)
     */
  @Override
  public final void setPredecessors(@NotNull final Collection<? extends Identifiable> predecessors) {
    if (predecessors.size()>getMaxPredecessorCount()) {
      throw new IllegalArgumentException();
    }
    mHashCode = 0;

    if (mPredecessors == null) {
      mPredecessors = getMaxPredecessorCount()==1 ? ProcessNodeSet.singleton() : ProcessNodeSet.processNodeSet();
    }

    if (mPredecessors.size()>0) {
      List<Identifiable> toRemove = removeNonShared(mPredecessors, predecessors);
      new ArrayList<>(mPredecessors.size());
      for (Identifiable oldPred : toRemove) {
        removePredecessor(oldPred);
      }
    }
    for(Identifiable pred: predecessors) {
      addPredecessor(pred);
    }

  }

  /**
   * Remove all elements from the baseList that are not also present in others. This
   * will modify both lists. This method is a building block for replacing a list with
   * another one that does not touch overlaps.
   *
   * @param baseList Modified list now only containing the shared elements.
   * @param others Modified list that no longer contains the shared elements.
   * @param <U> The type of the elements
   * @return List of elements removed from baseList (items in baselist but not others)
   */
  public static <U> List<U> removeNonShared(Collection<? extends U> baseList, Collection<? extends U> others) {
    List<U> result = new ArrayList<>(baseList);
    if (others!=null && others.size()>0) {
      result.removeAll(others);
      baseList.removeAll(result);
      others.removeAll(baseList);
    } else {
      baseList.clear();
    }
    return result;
  }

  @Override
  public final void setSuccessors(@NotNull final Collection<? extends Identifiable> successors) {
    if (successors.size()>getMaxSuccessorCount()) {
      throw new IllegalArgumentException();
    }
    mHashCode = 0;
    if (mSuccessors == null) {
      mSuccessors = getMaxSuccessorCount()==1 ? ProcessNodeSet.singleton() : ProcessNodeSet.processNodeSet();
    }

    if (mSuccessors.size()>0) {
      List<Identifiable> toRemove = removeNonShared(mSuccessors, successors);
      new ArrayList<>(mPredecessors.size());
      for (Identifiable oldSuc : toRemove) {
        removeSuccessor(oldSuc);
      }
    }
    for(Identifiable suc: successors) {
      addSuccessor(suc);
    }

  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#getSuccessors()
     */
  @Nullable
  @Override
  public ProcessNodeSet<? extends Identifiable> getSuccessors() {
    if (mSuccessors==null) {
      mHashCode = 0;
      switch (getMaxSuccessorCount()) {
        case 0:
          mSuccessors = ProcessNodeSet.empty();
          break;
        case 1:
          mSuccessors = ProcessNodeSet.singleton();
          break;
        default:
          mSuccessors = ProcessNodeSet.processNodeSet(2);
      }
    }
    return mSuccessors;
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public int getMaxPredecessorCount() {
    return 1;
  }

  @Override
  public final void resolveRefs() {
    ProcessModelBase<T, M> ownerModel = getOwnerModel();
    if (mPredecessors!=null) mPredecessors.resolve(ownerModel);
    if (mSuccessors!=null) mSuccessors.resolve(ownerModel);
  }

  public void unsetPos() {
    setX(Double.NaN);
    setY(Double.NaN);
  }

  @Override
  @Nullable
  public final M getOwnerModel() {
    return mOwnerModel;
  }

  @Override
  public final void setOwnerModel(@Nullable final M ownerModel) {
    if (mOwnerModel!=ownerModel) {
      mHashCode = 0;
      T thisT = this.asT();
      if (mOwnerModel!=null) { mOwnerModel.removeNode(thisT); }
      mOwnerModel = ownerModel;
      if (ownerModel!=null) {
        ownerModel.addNode(thisT);
      }
    }
  }

  public final T asT() {
    return (T) this;
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#getId()
     */
  @Override
  public String getId() {
    return mId;
  }

  @Override
  public int compareTo(final Identifiable o) {
    return mId.compareTo(o.getId());
  }

  public final void setId(final String id) {
    mId = id;
    mHashCode = 0;
    notifyChange();
  }

  @Override
  public String getIdBase() {
    return "id";
  }

  @Override
  public String getLabel() {
    return mLabel;
  }

  protected void notifyChange() {
    if (getOwnerModel() != null) getOwnerModel().notifyNodeChanged(this.asT());
  }

  public final void setLabel(final String label) {
    mLabel = label;
    mHashCode = 0;
    notifyChange();
  }

  @Override
  public double getX() {
    return mX;
  }

  public boolean hasPos() {
    return !Double.isNaN(getX()) && !Double.isNaN(getY());
  }

  public final void setX(final double x) {
    mX = x;
    mHashCode = 0;
    notifyChange();
  }

  @Override
  public double getY() {
    return mY;
  }

  public final void setY(final double y) {
    mY = y;
    mHashCode = 0;
    notifyChange();
  }

  public void translate(final double dX, final double dY) {
    mX+=dX;
    mY+=dY;
    mHashCode = 0;
    notifyChange();
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#isPredecessorOf(nl.adaptivity.process.processModel.ProcessNode)
     */
  @Override
  public boolean isPredecessorOf(@NotNull final T node) {
    for (final Identifiable pred : node.getPredecessors()) {
      if (this==pred) {
        return true;
      }
      if (getId()!=null && getId().equals(pred.getId())) {
        return true;
      }
      if (pred instanceof ProcessNode) {
        return (isPredecessorOf((T)pred));
      } else if (isPredecessorOf(getOwnerModel().getNode(pred))) {
        return true;
      }
    }
    return false;
  }

  protected void setDefines(@Nullable final Collection<? extends IXmlDefineType> exports) {
    mHashCode = 0;
    mDefines = exports==null ? new ArrayList<XmlDefineType>(0) : toExportableDefines(exports);
  }


  public XmlDefineType setDefine(final IXmlDefineType define) {
    if (mDefines==null) {
      mDefines = new ArrayList<>();
    }
    String targetName = define.getName();
    for (int i = 0; i < mDefines.size(); i++) {
      if (mDefines.get(i).getName().equals(targetName)) {
        return mDefines.set(i, XmlDefineType.get(define));
      }
    }
    mDefines.add(XmlDefineType.get(define));
    return null;
  }


  @Override
  public final List<XmlDefineType> getDefines() {
    if (mDefines==null) {
      mHashCode = 0;
      mDefines = new ArrayList<>();
    }
    return mDefines;
  }

  public XmlDefineType getDefine(final String name) {
    if (mDefines!=null && name!=null) {
      for(final XmlDefineType candidate: mDefines) {
        if (candidate.getName().equals(name)) {
          return candidate;
        }
      }
    }
    return null;
  }

  protected void setResults(@Nullable final Collection<? extends IXmlResultType> imports) {
    mHashCode = 0;
    mResults = imports==null ? new ArrayList<XmlResultType>(0) : toExportableResults(imports);
  }

  @Override
  public final List<XmlResultType> getResults() {
    if (mResults==null) {
      mHashCode = 0;
      mResults = new ArrayList<>();
    }
    return mResults;
  }

  public XmlResultType getResult(final String name) {
    if (mResults!=null && name!=null) {
      for(final XmlResultType candidate: mResults) {
        if (candidate.getName().equals(name)) {
          return candidate;
        }
      }
    }
    return null;
  }

  @NotNull
  protected static List<XmlDefineType> toExportableDefines(@Nullable final Collection<? extends IXmlDefineType> exports) {
    final List<XmlDefineType> newExports;
    if (exports!=null) {
      newExports = new ArrayList<>(exports.size());
      for(final IXmlDefineType export:exports) {
        newExports.add(XmlDefineType.get(export));
      }
    } else {
      newExports = new ArrayList<>();
    }
    return newExports;
  }

  @NotNull
  protected static List<XmlResultType> toExportableResults(@Nullable final Collection<? extends IXmlResultType> imports) {
    final List<XmlResultType> newImports;
    if (imports!=null) {
      newImports = new ArrayList<>(imports.size());
      for(final IXmlResultType imp:imports) {
        newImports.add(XmlResultType.get(imp));
      }
    } else {
      newImports = new ArrayList<>();
    }
    return newImports;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    ProcessNodeBase<?, ?> that = (ProcessNodeBase<?, ?>) o;

    if (Double.compare(that.mX, mX) != 0) { return false; }
    if (Double.compare(that.mY, mY) != 0) { return false; }
    if (mId != null ? !mId.equals(that.mId) : that.mId != null) { return false; }
    if (mLabel != null ? !mLabel.equals(that.mLabel) : that.mLabel != null) { return false; }
    if (mDefines != null ? !mDefines.equals(that.mDefines) : that.mDefines != null) { return false; }
    if (mResults != null ? !mResults.equals(that.mResults) : that.mResults != null) { return false; }

    if (mPredecessors==null) {
      if (that.mPredecessors!=null) { return false; }
    } else {
      if (that.mPredecessors==null) { return false; }
      ArrayList<String> thisIds = new ArrayList<>();
      for (Identifiable id : mPredecessors) thisIds.add(getId());

      ArrayList<String> thatIds = new ArrayList<>();
      for (Identifiable id : that.mPredecessors) thatIds.add(getId());
      if (! thisIds.equals(thatIds)) { return false; }
    }

    if (mSuccessors==null) {
      if (that.mSuccessors!=null) { return false; }
    } else {
      if (that.mSuccessors==null) { return false; }
      ArrayList<String> thisIds = new ArrayList<>();
      for (Identifiable id : mSuccessors) thisIds.add(getId());

      ArrayList<String> thatIds = new ArrayList<>();
      for (Identifiable id : that.mSuccessors) thatIds.add(getId());
      if (! thisIds.equals(thatIds)) { return false; }
    }
    if (mSuccessors != null ? !mSuccessors.equals(that.mSuccessors) : that.mSuccessors != null) { return false; }
    return true;
  }

  /**
   * Method to only use the specific ids of predecessors / successors for the hash code. Otherwise there may be an infinite loop.
   * @param c The collection of ids
   * @return The hashcode.
   */
  private static int getHashCode(Collection<Identifiable> c) {
    int result = 1;
    for(Identifiable i: c) {
      String id=i.getId();
      result = result*31 + (id==null ? 1 : id.hashCode());
    }
    return result;
  }

  @Override
  public int hashCode() {
    if (mHashCode!=0) { return mHashCode; }
    int result;
    long temp;
    result = mPredecessors != null ? getHashCode(mPredecessors) : 0;
    result = 31 * result + (mSuccessors != null ? getHashCode(mSuccessors) : 0);
    result = 31 * result + (mId != null ? mId.hashCode() : 0);
    result = 31 * result + (mLabel != null ? mLabel.hashCode() : 0);
    temp = Double.doubleToLongBits(mX);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(mY);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + (mDefines != null ? mDefines.hashCode() : 0);
    result = 31 * result + (mResults != null ? mResults.hashCode() : 0);
    return result;
  }

  public String toString() {
    String name = getClass().getSimpleName();
    if (name.endsWith("Impl")) {
      name = name.substring(0, name.length()-4);
    }
    if (name.endsWith("Node")) {
      name = name.substring(0, name.length()-4);
    }
    {
      for(int i=name.length()-1; i>=0; --i) {
        if (Character.isUpperCase(name.charAt(i)) && (i==0 || ! Character.isUpperCase(name.charAt(i-1)))) {
          name = name.substring(i);
          break;
        }
      }
    }
    StringBuilder result = new StringBuilder();
    result.append(name).append('(');
    if (mId!=null) { result.append(" id=\'").append(mId).append('\''); }
    if (mPredecessors!=null && mPredecessors.size()>0) {
      result.append(" pred=\'");
      Iterator<Identifiable> predIt = mPredecessors.iterator();
      result.append(predIt.next().getId());
      while(predIt.hasNext()) {
        result.append(", ").append(predIt.next().getId());
      }
      result.append('\'');
    }
    if (mOwnerModel!=null && mOwnerModel.getName()!=null && mOwnerModel.getName().length()>0) {
      result.append(" owner='").append(mOwnerModel.getName()).append('\'');
    }
    result.append(" )");
    return result.toString();
  }

}
