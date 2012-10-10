package nl.adaptivity.process.processModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import net.devrieze.util.IdFactory;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;


@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ProcesNode")
@XmlSeeAlso({ Join.class, Activity.class, EndNode.class, StartNode.class })
public abstract class ProcessNode implements Serializable {

  private static final long serialVersionUID = -7745019972129682199L;

  private Collection<ProcessNode> aPredecessors;

  private Collection<ProcessNode> aSuccessors = null;

  private String aId;

  protected ProcessNode() {

  }

  protected ProcessNode(final ProcessNode pPrevious) {
    if (pPrevious == null) {
      if (!((this instanceof StartNode) || (this instanceof Join))) {
        throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
      }
      setPredecessors(Arrays.asList(new ProcessNode[0]));
    } else {
      setPredecessors(Arrays.asList(pPrevious));
    }
  }

  public ProcessNode(final Collection<ProcessNode> pPredecessors) {
    if ((pPredecessors.size() < 1) && (!(this instanceof StartNode))) {
      throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
    }
    if ((pPredecessors.size() > 1) && (!(this instanceof Join))) {
      throw new IllegalProcessModelException("Only join nodes may have multiple predecessors");
    }
    setPredecessors(pPredecessors);
  }

  public Collection<ProcessNode> getPredecessors() {
    if (aPredecessors == null) {
      aPredecessors = new ArrayList<ProcessNode>();
    }
    return aPredecessors;
  }

  public void setPredecessors(final Collection<ProcessNode> predecessors) {
    if (aPredecessors != null) {
      throw new UnsupportedOperationException("Not allowed to change predecessors");
    }
    aPredecessors = predecessors;
  }

  public void addSuccessor(final ProcessNode pNode) {
    if (pNode == null) {
      throw new IllegalProcessModelException("Adding Null process successors is illegal");
    }
    if (aSuccessors == null) {
      aSuccessors = new ArrayList<ProcessNode>(1);
    }
    aSuccessors.add(pNode);
  }

  public Collection<ProcessNode> getSuccessors() {
    return aSuccessors;
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

  /**
   * Take action to make task available
   * 
   * @param pMessageService TODO
   * @param pInstance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically taken
   */
  public abstract <T, U extends IProcessNodeInstance<U>> boolean provideTask(IMessageService<T, U> pMessageService, U pInstance);

  /**
   * Take action to accept the task (but not start it yet)
   * 
   * @param pMessageService TODO
   * @param pInstance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically started
   */
  public abstract <T, U extends IProcessNodeInstance<U>> boolean takeTask(IMessageService<T, U> pMessageService, U pInstance);

  public abstract <T, U extends IProcessNodeInstance<U>> boolean startTask(IMessageService<T, U> pMessageService, U pInstance);

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append(getClass().getName()).append(" (").append(getId());
    if ((this.getPredecessors() == null) || (getPredecessors().size() == 0)) {
      result.append(')');
    }
    if (this.getPredecessors().size() > 1) {
      result.append(", pred={");
      for (final ProcessNode pred : getPredecessors()) {
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

  public boolean isPredecessorOf(final ProcessNode pNode) {
    for (final ProcessNode pred : pNode.getPredecessors()) {
      if (pred == pNode) {
        return true;
      }
      if (isPredecessorOf(pred)) {
        return true;
      }
    }
    return false;
  }

}
