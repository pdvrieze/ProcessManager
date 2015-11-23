package nl.adaptivity.process.processModel;

import net.devrieze.util.IdFactory;
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
public abstract class ProcessNodeBase<T extends ProcessNodeBase<T>> implements ProcessNode<T>, XmlDeserializable {

  public static final String ATTR_PREDECESSOR = "predecessor";
  @Nullable protected ProcessModelBase<T> mOwnerModel;
  @Nullable private ProcessNodeSet<Identifiable> mPredecessors;
  @Nullable private ProcessNodeSet<Identifiable> mSuccessors = null;
  private String mId;
  private String mLabel;
  private double mX=Double.NaN;
  private double mY=Double.NaN;

  public ProcessNodeBase(@Nullable final ProcessModelBase<T> ownerModel) {mOwnerModel = ownerModel;}

  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    out.attribute(null, "id", null, getId());
    XmlUtil.writeAttribute(out, "label", getLabel());
    XmlUtil.writeAttribute(out, "x", getX());
    XmlUtil.writeAttribute(out, "y", getY());
  }

  protected void serializeChildren(final XmlWriter out) throws XmlException {

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

  protected void swapPredecessors(@NotNull final Collection<?> predecessors) {
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
  public void addPredecessor(@NotNull final Identifiable node) {
    if (mPredecessors!=null) {
      if (mPredecessors.containsKey(node.getId())) { return; }
      if (mPredecessors.size() + 1 > getMaxPredecessorCount()) {
        throw new IllegalProcessModelException("Can not add more predecessors");
      }
      ProcessModelBase<T> ownerModel = getOwnerModel();
      if (mPredecessors.add(node) && ownerModel !=null) {
        ownerModel.getNode(node).addSuccessor(this);
      }
    } else if (getMaxPredecessorCount()==1) {
      mPredecessors = ProcessNodeSet.singleton(node);
    } else {
      mPredecessors = ProcessNodeSet.processNodeSet(1);
      mPredecessors.add(node);
    }
  }

  @Override
  public void removePredecessor(final Identifiable node) {
    mPredecessors.remove(node);
    // TODO perhaps make this reciprocal
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#addSuccessor(nl.adaptivity.process.processModel.ProcessNodeImpl)
     */
  @Override
  public void addSuccessor(@Nullable final Identifiable node) {
    if (node == null) {
      throw new IllegalProcessModelException("Adding Null process successors is illegal");
    }
    if (mSuccessors == null) {
      mSuccessors = ProcessNodeSet.processNodeSet(1);
    }
    mSuccessors.add(node);
  }

  @Override
  public void removeSuccessor(final Identifiable node) {
    mSuccessors.remove(node);
  }

  @Override
  public void resolveRefs() {
    ProcessModelBase<T> ownerModel = getOwnerModel();
    mPredecessors.resolve(ownerModel);
    mSuccessors.resolve(ownerModel);
  }

  /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.ProcessNode#getPredecessors()
       */
  @Nullable
  @Override
  public final Set<? extends Identifiable> getPredecessors() {
    if (mPredecessors == null) {
      mPredecessors = getMaxPredecessorCount()==1 ? ProcessNodeSet.singleton() : ProcessNodeSet.processNodeSet();
    }
    return mPredecessors;
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#setPredecessors(java.util.Collection)
     */
  @Override
  public void setPredecessors(final Collection<? extends Identifiable> predecessors) {
    if (mPredecessors != null) {
      throw new UnsupportedOperationException("Not allowed to change predecessors");
    }
    mPredecessors = ProcessNodeSet.processNodeSet(predecessors);
  }

  @Override
  public void setSuccessors(@NotNull final Collection<? extends Identifiable> successors) {
    for(final Identifiable n: successors) {
      addSuccessor(n);
    }
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#getSuccessors()
     */
  @Nullable
  @Override
  public Set<? extends Identifiable> getSuccessors() {
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
      ownerModel.addNode(thisT);
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
    if (mId == null) {
      mId = IdFactory.create();
    }
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

  @Override
  public Collection<? extends IXmlResultType> getResults() {
    return Collections.emptyList();
//    if (mImports==null) { mImports = new ArrayList<>(); }
//    return mImports;
  }

  @Override
  public Collection<? extends XmlDefineType> getDefines() {
    return Collections.emptyList();
//    if (mExports==null) { mExports = new ArrayList<>(); }
//    return mExports;
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
