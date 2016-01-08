package nl.adaptivity.process.processModel;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.XmlDeserializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
  private List<XmlDefineType> mDefines;
  private List<XmlResultType> mResults;

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
    XmlUtil.writeAttribute(out, "id", getId());
    XmlUtil.writeAttribute(out, "label", getLabel());
    XmlUtil.writeAttribute(out, "x", getX());
    XmlUtil.writeAttribute(out, "y", getY());
  }

  protected void serializeChildren(final XmlWriter out) throws XmlException {
    XmlUtil.writeChildren(out, getResults());
    XmlUtil.writeChildren(out, getDefines());
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

  public final void setId(final String id) {
    mId = id;
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
    notifyChange();
  }

  @Override
  public double getY() {
    return mY;
  }

  public final void setY(final double y) {
    mY = y;
    notifyChange();
  }

  public void translate(final double dX, final double dY) {
    mX+=dX;
    mY+=dY;
    notifyChange();
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#isPredecessorOf(nl.adaptivity.process.processModel.ProcessNode)
     */
  @Override
  public boolean isPredecessorOf(@NotNull final T node) {
    for (final Identifiable pred : node.getPredecessors()) {
      if (getId().equals(pred.getId())) {
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
    mDefines = exports==null ? new ArrayList<XmlDefineType>(0) : toExportableDefines(exports);
  }

  @Override
  public final List<XmlDefineType> getDefines() {
    if (mDefines==null) {
      mDefines = new ArrayList<>();
    }
    return mDefines;
  }

  protected void setResults(@Nullable final Collection<? extends IXmlResultType> imports) {
    mResults = imports==null ? new ArrayList<XmlResultType>(0) : toExportableResults(imports);
  }

  @Override
  public final List<XmlResultType> getResults() {
    if (mResults==null) {
      mResults = new ArrayList<>();
    }
    return mResults;
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
