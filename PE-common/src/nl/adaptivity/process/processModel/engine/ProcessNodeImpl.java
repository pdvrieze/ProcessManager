package nl.adaptivity.process.processModel.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import net.devrieze.util.IdFactory;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IXmlExportType;
import nl.adaptivity.process.processModel.IXmlImportType;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.XmlExportType;
import nl.adaptivity.process.processModel.XmlImportType;


@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ProcesNode")
@XmlSeeAlso({ JoinImpl.class, SplitImpl.class, JoinSplitImpl.class, ActivityImpl.class, EndNodeImpl.class, StartNodeImpl.class })
public abstract class ProcessNodeImpl implements Serializable, ProcessNode<ProcessNodeImpl> {

  private static final long serialVersionUID = -7745019972129682199L;

  private ProcessNodeSet<ProcessNodeImpl> aPredecessors;

  private ProcessNodeSet<ProcessNodeImpl> aSuccessors = null;

  private String aId;

  private String aLabel;

  private double aX=Double.NaN;
  private double aY=Double.NaN;

  protected ProcessNodeImpl() {

  }


  public ProcessNodeImpl(final Collection<? extends ProcessNodeImpl> pPredecessors) {
    if ((pPredecessors.size() < 1) && (!(this instanceof StartNode))) {
      throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
    }
    if ((pPredecessors.size() > 1) && (!(this instanceof Join))) {
      throw new IllegalProcessModelException("Only join nodes may have multiple predecessors");
    }
    setPredecessors(pPredecessors);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessNode#getPredecessors()
   */
  @Override
  public Set<? extends ProcessNodeImpl> getPredecessors() {
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
  public void setPredecessors(final Collection<? extends ProcessNodeImpl> predecessors) {
    if (aPredecessors != null) {
      throw new UnsupportedOperationException("Not allowed to change predecessors");
    }
    aPredecessors = ProcessNodeSet.processNodeSet(predecessors);
  }



  @Override
  public void addPredecessor(ProcessNodeImpl pNode) {
    if (aPredecessors.contains(pNode)) { return; }
    if (aPredecessors.size()+1>getMaxPredecessorCount()) {
      throw new IllegalProcessModelException("Can not add more predecessors");
    }
    if(aPredecessors.add(pNode)) {
      pNode.addSuccessor(this);
    }
  }


  @Override
  public void removePredecessor(ProcessNodeImpl pNode) {
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
   * @param The instance against which the condition should be evaluated.
   * @return <code>true</code> if the node can be started, <code>false</code> if
   *         not.
   */
  public abstract boolean condition(IProcessNodeInstance<?> pInstance);

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
  public abstract <T, U extends IProcessNodeInstance<U>> boolean provideTask(IMessageService<T, U> pMessageService, U pInstance);

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
    if ((this.getPredecessors() == null) || (getPredecessors().size() == 0)) {
      result.append(')');
    }
    final int predCount = this.getPredecessors().size();
    if (predCount != 1) {
      result.append(", pred={");
      for (final ProcessNodeImpl pred : getPredecessors()) {
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
    return result.toString();
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessNode#isPredecessorOf(nl.adaptivity.process.processModel.ProcessNode)
   */
  @Override
  public boolean isPredecessorOf(final ProcessNodeImpl pNode) {
    for (final ProcessNodeImpl pred : pNode.getPredecessors()) {
      if (pred == pNode) {
        return true;
      }
      if (isPredecessorOf(pred)) {
        return true;
      }
    }
    return false;
  }

  protected static List<XmlExportType> toExportableExports(Collection<? extends IXmlExportType> pExports) {
    List<XmlExportType> newExports;
    if (pExports!=null) {
      newExports = new ArrayList<>(pExports.size());
      for(IXmlExportType export:pExports) {
        newExports.add(XmlExportType.get(export));
      }
    } else {
      newExports = new ArrayList<>();
    }
    return newExports;
  }

  protected static List<XmlImportType> toExportableImports(Collection<? extends IXmlImportType> pImports) {
    List<XmlImportType> newImports;
    if (pImports!=null) {
      newImports = new ArrayList<>(pImports.size());
      for(IXmlImportType imp:pImports) {
        newImports.add(XmlImportType.get(imp));
      }
    } else {
      newImports = new ArrayList<>();
    }
    return newImports;
  }

}
