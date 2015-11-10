package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.IdFactory;
import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlDeserializable;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;


@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ProcesNode")
@XmlSeeAlso({ JoinImpl.class, SplitImpl.class, JoinSplitImpl.class, ActivityImpl.class, EndNodeImpl.class, StartNodeImpl.class })
public abstract class ProcessNodeImpl implements XmlDeserializable, Serializable, ProcessNode<ProcessNodeImpl> {

  public static final String ATTR_PREDECESSOR = "predecessor";
  private static final long serialVersionUID = -7745019972129682199L;
  @Nullable private ProcessModelImpl mOwnerModel;

  @Nullable private ProcessNodeSet<Identifiable> mPredecessors;

  @Nullable private ProcessNodeSet<ProcessNodeImpl> mSuccessors = null;

  private String mId;

  private String mLabel;

  private double mX=Double.NaN;
  private double mY=Double.NaN;
//
//  private Collection<? extends IXmlImportType> mImports;
//
//  private Collection<? extends IXmlExportType> mExports;

  protected ProcessNodeImpl(@Nullable final ProcessModelImpl ownerModel) {
    mOwnerModel = ownerModel;
    if (ownerModel!=null) {
      mOwnerModel.ensureNode(this);
    }
  }


  public ProcessNodeImpl(final ProcessModelImpl ownerModel, @NotNull final Collection<? extends Identifiable> predecessors) {
    this(ownerModel);
    if ((predecessors.size() < 1) && (!(this instanceof StartNode))) {
      throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
    }
    if ((predecessors.size() > 1) && (!(this instanceof Join))) {
      throw new IllegalProcessModelException("Only join nodes may have multiple predecessors");
    }
    setPredecessors(predecessors);
  }

  protected void serializeAttributes(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    out.writeAttribute("id", getId());
    XmlUtil.writeAttribute(out, "label", getLabel());
    XmlUtil.writeAttribute(out, "x", getX());
    XmlUtil.writeAttribute(out, "y", getY());
  }

  protected void serializeChildren(final XMLStreamWriter out) throws XMLStreamException {

  }

  @Override
  public boolean deserializeAttribute(final String attributeNamespace, @NotNull final String attributeLocalName, final String attributeValue) {
    if (XMLConstants.NULL_NS_URI.equals(attributeNamespace)) {
      switch (attributeLocalName) {
        case "id": setId(attributeValue); return true;
        case "label": setLabel(attributeValue); return true;
        case "x": setX(Double.parseDouble(attributeValue)); return true;
        case "y": setY(Double.parseDouble(attributeValue)); return true;
      }
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XMLStreamReader in) {
    // do nothing
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessNode#getPredecessors()
     */
  @Nullable
  @Override
  public Set<? extends Identifiable> getPredecessors() {
    if (mPredecessors == null) {
      mPredecessors = ProcessNodeSet.processNodeSet();
    }
    return mPredecessors;
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
  public void addPredecessor(@NotNull final Identifiable node) {
    if (mPredecessors.containsKey(node.getId())) { return; }
    if (mPredecessors.size()+1>getMaxPredecessorCount()) {
      throw new IllegalProcessModelException("Can not add more predecessors");
    }
    if(mPredecessors.add(node)) {
      getOwnerModel().getNode(node).addSuccessor(this);
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
  public void addSuccessor(@Nullable final ProcessNodeImpl node) {
    if (node == null) {
      throw new IllegalProcessModelException("Adding Null process successors is illegal");
    }
    if (mSuccessors == null) {
      mSuccessors = ProcessNodeSet.processNodeSet(1);
    }
    mSuccessors.add(node);
  }


  @Override
  public void setSuccessors(@NotNull final Collection<? extends ProcessNodeImpl> successors) {
    for(final ProcessNodeImpl n: successors) {
      addSuccessor(n);
    }
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessNode#getSuccessors()
   */
  @Nullable
  @Override
  public Set<? extends ProcessNodeImpl> getSuccessors() {
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
  public void removeSuccessor(final ProcessNodeImpl node) {
    mSuccessors.remove(node);
  }

  /**
   * Should this node be able to be provided?
   *
   *
   * @param transaction
   * @param instance The instance against which the condition should be evaluated.
   * @return <code>true</code> if the node can be started, <code>false</code> if
   *         not.
   */
  public abstract boolean condition(final Transaction transaction, IProcessNodeInstance<?> instance);

  @Nullable
  public ProcessModelImpl getOwnerModel() {
    return mOwnerModel;
  }

  public void setOwnerModel(@NotNull final ProcessModelImpl ownerModel) {
    if (mOwnerModel!=ownerModel) {
      if (mOwnerModel!=null) { mOwnerModel.removeNode(this); }
      mOwnerModel = ownerModel;
      ownerModel.ensureNode(this);
    }
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

  /**
   * Take action to make task available
   *
   * @param messageService The message service to use for the communication.
   * @param instance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically taken
   */
  public abstract <T, U extends IProcessNodeInstance<U>> boolean provideTask(Transaction transaction, IMessageService<T, U> messageService, U instance) throws SQLException;

  /**
   * Take action to accept the task (but not start it yet)
   *
   * @param messageService The message service to use for the communication.
   * @param instance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically started
   */
  public abstract <T, U extends IProcessNodeInstance<U>> boolean takeTask(IMessageService<T, U> messageService, U instance);

  public abstract <T, U extends IProcessNodeInstance<U>> boolean startTask(IMessageService<T, U> messageService, U instance);

  @NotNull
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append(getClass().getSimpleName()).append(" (").append(getId());
    if ((this.getPredecessors() == null) || (getMaxPredecessorCount()==0)) {
      result.append(')');
    } else {
      final int predCount = this.getPredecessors().size();
      if (predCount != 1) {
        result.append(", pred={");
        for (final Identifiable pred : getPredecessors()) {
          result.append(pred.getId()).append(", ");
        }
        if (result.charAt(result.length() - 2) == ',') {
          result.setLength(result.length() - 2);
        }
        result.append("})");
      } else {
        result.append(", pred=").append(getPredecessors().iterator().next().getId());
        result.append(')');
      }
    }
    return result.toString();
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessNode#isPredecessorOf(nl.adaptivity.process.processModel.ProcessNode)
   */
  @Override
  public boolean isPredecessorOf(@NotNull final ProcessNodeImpl node) {
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
