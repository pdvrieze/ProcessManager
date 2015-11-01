package nl.adaptivity.process.processModel.engine;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.devrieze.util.IdFactory;
import net.devrieze.util.Transaction;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;


@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ProcesNode")
@XmlSeeAlso({ JoinImpl.class, SplitImpl.class, JoinSplitImpl.class, ActivityImpl.class, EndNodeImpl.class, StartNodeImpl.class })
public abstract class ProcessNodeImpl implements XmlSerializable, Serializable, ProcessNode<ProcessNodeImpl> {

  public static final String ATTR_PREDECESSOR = "predecessor";
  private static final long serialVersionUID = -7745019972129682199L;
  private ProcessModelImpl aOwnerModel;

  private ProcessNodeSet<Identifiable> aPredecessors;

  private ProcessNodeSet<ProcessNodeImpl> aSuccessors = null;

  private String aId;

  private String aLabel;

  private double aX=Double.NaN;
  private double aY=Double.NaN;
//
//  private Collection<? extends IXmlImportType> aImports;
//
//  private Collection<? extends IXmlExportType> aExports;

  protected ProcessNodeImpl(ProcessModelImpl pOwnerModel) {
    aOwnerModel = pOwnerModel;
    if (pOwnerModel!=null) {
      aOwnerModel.ensureNode(this);
    }
  }


  public ProcessNodeImpl(ProcessModelImpl pOwnerModel, final Collection<? extends Identifiable> pPredecessors) {
    this(pOwnerModel);
    if ((pPredecessors.size() < 1) && (!(this instanceof StartNode))) {
      throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
    }
    if ((pPredecessors.size() > 1) && (!(this instanceof Join))) {
      throw new IllegalProcessModelException("Only join nodes may have multiple predecessors");
    }
    setPredecessors(pPredecessors);
  }

  protected void serializeAttributes(final XMLStreamWriter pOut) throws XMLStreamException {
    pOut.writeAttribute("id", getId());
    XmlUtil.writeAttribute(pOut, "label", getLabel());
    XmlUtil.writeAttribute(pOut, "x", getX());
    XmlUtil.writeAttribute(pOut, "y", getY());
  }

  protected void serializeChildren(final XMLStreamWriter pOut) throws XMLStreamException {

  }

  protected boolean deserializeAttribute(final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    if (XMLConstants.NULL_NS_URI.equals(pAttributeNamespace)) {
      switch (pAttributeLocalName) {
        case "id": setId(pAttributeValue); return true;
        case "label": setLabel(pAttributeValue); return true;
        case "x": setX(Double.parseDouble(pAttributeValue)); return true;
        case "y": setY(Double.parseDouble(pAttributeValue)); return true;
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessNode#getPredecessors()
   */
  @Override
  public Set<? extends Identifiable> getPredecessors() {
    if (aPredecessors == null) {
      aPredecessors = ProcessNodeSet.processNodeSet();
    }
    return aPredecessors;
  }

  protected void swapPredecessors(final Collection<?> predecessors) {
    aPredecessors=null;
    List<ProcessNodeImpl> tmp = new ArrayList<>(predecessors.size());
    for(Object pred:predecessors) {
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
    if (aPredecessors != null) {
      throw new UnsupportedOperationException("Not allowed to change predecessors");
    }
    aPredecessors = ProcessNodeSet.processNodeSet(predecessors);
  }


  @Override
  public void addPredecessor(Identifiable pNode) {
    if (aPredecessors.containsKey(pNode.getId())) { return; }
    if (aPredecessors.size()+1>getMaxPredecessorCount()) {
      throw new IllegalProcessModelException("Can not add more predecessors");
    }
    if(aPredecessors.add(pNode)) {
      getOwnerModel().getNode(pNode).addSuccessor(this);
    }
  }

  @Override
  public void removePredecessor(Identifiable pNode) {
    aPredecessors.remove(pNode);
    // TODO perhaps make this reciprocal
  }



  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessNode#addSuccessor(nl.adaptivity.process.processModel.ProcessNodeImpl)
   */
  @Override
  public void addSuccessor(final ProcessNodeImpl pNode) {
    if (pNode == null) {
      throw new IllegalProcessModelException("Adding Null process successors is illegal");
    }
    if (aSuccessors == null) {
      aSuccessors = ProcessNodeSet.processNodeSet(1);
    }
    aSuccessors.add(pNode);
  }


  @Override
  public void setSuccessors(Collection<? extends ProcessNodeImpl> pSuccessors) {
    for(ProcessNodeImpl n: pSuccessors) {
      addSuccessor(n);
    }
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessNode#getSuccessors()
   */
  @Override
  public Set<? extends ProcessNodeImpl> getSuccessors() {
    return aSuccessors;
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
  public void removeSuccessor(ProcessNodeImpl pNode) {
    aSuccessors.remove(pNode);
  }

  /**
   * Should this node be able to be provided?
   *
   * @param pInstance The instance against which the condition should be evaluated.
   * @return <code>true</code> if the node can be started, <code>false</code> if
   *         not.
   */
  public abstract boolean condition(IProcessNodeInstance<?> pInstance);

  public ProcessModelImpl getOwnerModel() {
    return aOwnerModel;
  }

  public void setOwnerModel(final ProcessModelImpl pOwnerModel) {
    if (aOwnerModel!=pOwnerModel) {
      if (aOwnerModel!=null) { aOwnerModel.removeNode(this); }
      aOwnerModel = pOwnerModel;
      pOwnerModel.ensureNode(this);
    }
  }

  @Deprecated
  public void skip() {
    //    for(ProcessNode successor: aSuccessors) {
    //      successor.skip(pThreads, pProcessInstance, pPredecessor);
    //    }
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
    if (aId == null) {
      aId = IdFactory.create();
    }
    return aId;
  }

  public void setId(final String id) {
    aId = id;
  }

  @Override
  @XmlAttribute
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  public String getLabel() {
    return aLabel;
  }

  public void setLabel(final String label) {
    aLabel = label;
  }

  @XmlAttribute(name="x")
  @Override
  public double getX() {
    return aX;
  }

  public void setX(double pX) {
    aX = pX;
  }

  @XmlAttribute(name="y")
  @Override
  public double getY() {
    return aY;
  }

  public void setY(double pY) {
    aY = pY;
  }

  /**
   * Take action to make task available
   *
   * @param pMessageService The message service to use for the communication.
   * @param pInstance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically taken
   */
  public abstract <T, U extends IProcessNodeInstance<U>> boolean provideTask(Transaction pTransaction, IMessageService<T, U> pMessageService, U pInstance) throws SQLException;

  /**
   * Take action to accept the task (but not start it yet)
   *
   * @param pMessageService The message service to use for the communication.
   * @param pInstance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically started
   */
  public abstract <T, U extends IProcessNodeInstance<U>> boolean takeTask(IMessageService<T, U> pMessageService, U pInstance);

  public abstract <T, U extends IProcessNodeInstance<U>> boolean startTask(IMessageService<T, U> pMessageService, U pInstance);

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
  public boolean isPredecessorOf(final ProcessNodeImpl pNode) {
    for (final Identifiable pred : pNode.getPredecessors()) {
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
//    if (aImports==null) { aImports = new ArrayList<>(); }
//    return aImports;
  }


  @Override
  public Collection<? extends XmlDefineType> getDefines() {
    return Collections.emptyList();
//    if (aExports==null) { aExports = new ArrayList<>(); }
//    return aExports;
  }


  protected static List<XmlDefineType> toExportableDefines(Collection<? extends IXmlDefineType> pExports) {
    List<XmlDefineType> newExports;
    if (pExports!=null) {
      newExports = new ArrayList<>(pExports.size());
      for(IXmlDefineType export:pExports) {
        newExports.add(XmlDefineType.get(export));
      }
    } else {
      newExports = new ArrayList<>();
    }
    return newExports;
  }

  protected static List<XmlResultType> toExportableResults(Collection<? extends IXmlResultType> pImports) {
    List<XmlResultType> newImports;
    if (pImports!=null) {
      newImports = new ArrayList<>(pImports.size());
      for(IXmlResultType imp:pImports) {
        newImports.add(XmlResultType.get(imp));
      }
    } else {
      newImports = new ArrayList<>();
    }
    return newImports;
  }

}
