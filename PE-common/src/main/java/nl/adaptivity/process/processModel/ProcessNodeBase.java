package nl.adaptivity.process.processModel;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import nl.adaptivity.process.util.Identifiable;
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
public abstract class ProcessNodeBase<T extends ProcessNode<T>> implements ProcessNode<T>, XmlDeserializable {

  public static final String ATTR_PREDECESSOR = "predecessor";
  @Nullable protected ProcessModelBase<T> mOwnerModel;
  @Nullable private ProcessNodeSet<Identifiable> mPredecessors;
  @Nullable private ProcessNodeSet<Identifiable> mSuccessors = null;
  private String mId;
  private String mLabel;
  private double mX=Double.NaN;
  private double mY=Double.NaN;
  private List<XmlDefineType> mDefines;
  private List<XmlResultType> mResults;

  public ProcessNodeBase(@Nullable final ProcessModelBase<T> ownerModel) {mOwnerModel = ownerModel;}

  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    out.attribute(null, "id", null, getId());
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

  protected final void swapPredecessors(@NotNull final Collection<?> predecessors) {
    mPredecessors=null;
    final List<ProcessNodeImpl> tmp = new ArrayList<>(predecessors.size());
    for(final Object pred:predecessors) {
      if (pred instanceof ProcessNodeImpl) {
        tmp.add((ProcessNodeImpl) pred);
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
      ProcessModelBase<T> ownerModel = getOwnerModel();

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
      ProcessModelBase<T> owner = mOwnerModel;
      T predecessor;
      if (owner!=null && (predecessor = owner.getNode(predecessorId))!=null) {
        predecessor.removeSuccessor(this.asT()); }
    }

    // TODO perhaps make this reciprocal
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#addSuccessor(nl.adaptivity.process.processModel.ProcessNodeImpl)
     */
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

    ProcessModelBase<T> owner = mOwnerModel;
    ProcessNode<?> node = null;
    if (owner!=null) {
      T node2 = owner.getNode(nodeId);
      owner.addNode((T) node2);
      node = node2;
    } else if (nodeId instanceof ProcessNode){
      node = (ProcessNode<?>) nodeId;
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

  @Override
  public final void resolveRefs() {
    ProcessModelBase<T> ownerModel = getOwnerModel();
    if (mPredecessors!=null) mPredecessors.resolve(ownerModel);
    if (mSuccessors!=null) mSuccessors.resolve(ownerModel);
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
  public final void setPredecessors(final Collection<? extends Identifiable> predecessors) {
    if (mPredecessors == null) {
      mPredecessors = getMaxPredecessorCount()==1 ? ProcessNodeSet.singleton() : ProcessNodeSet.processNodeSet();
    }

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
  public final void setSuccessors(@NotNull final Collection<? extends Identifiable> successors) {
    if (successors.size()>getMaxSuccessorCount()) {
      throw new IllegalArgumentException();
    }
    if (mSuccessors!=null) {
      List<Identifiable> toRemove = new ArrayList<>(mSuccessors.size());
      for (Iterator<Identifiable> it = mSuccessors.iterator(); it.hasNext(); ) {
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

  @Nullable
  public ProcessModelBase<T> getOwnerModel() {
    return mOwnerModel;
  }

  public void setOwnerModel(@NotNull final ProcessModelBase<T> ownerModel) {
    if (mOwnerModel!=ownerModel) {
      T thisT = this.asT();
      if (mOwnerModel!=null) { mOwnerModel.removeNode(thisT); }
      mOwnerModel = ownerModel;
      if (ownerModel!=null) {
        ownerModel.addNode(thisT);
      }
    }
  }

  public T asT() {
    return (T) this;
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#getId()
     */
  @Override
  @XmlAttribute
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  @XmlID
  @XmlSchemaType(name = "ID")
  public String getId() {
    return mId;
  }

  public void setId(final String id) {
    mId = id;
  }

  @Override
  @XmlAttribute
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  public String getLabel() {
    return mLabel;
  }

  public void setLabel(final String label) {
    mLabel = label;
  }

  @XmlAttribute(name="x")
  @Override
  public double getX() {
    return mX;
  }

  public void setX(final double x) {
    mX = x;
  }

  @XmlAttribute(name="y")
  @Override
  public double getY() {
    return mY;
  }

  public void setY(final double y) {
    mY = y;
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
      if (isPredecessorOf(getOwnerModel().getNode(pred))) {
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
}
