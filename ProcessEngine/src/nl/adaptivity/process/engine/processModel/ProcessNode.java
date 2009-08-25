package nl.adaptivity.process.engine.processModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import nl.adaptivity.process.engine.ProcessInstance;


public abstract class ProcessNode implements Serializable {

  private static final long serialVersionUID = -7745019972129682199L;
  
  private final Collection<ProcessNode> aPrevious;

  private Collection<ProcessNode> aSuccessors = null;
  
  protected ProcessNode(ProcessNode pPrevious) {
    if (pPrevious == null) {
      if (! (this instanceof StartNode || this instanceof Join)) {
        throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
      }
      aPrevious = Arrays.asList(new ProcessNode[0]);
    } else {
      aPrevious = Arrays.asList(pPrevious);
    }
  }
  
  public ProcessNode(Collection<ProcessNode> pPrevious) {
    if (pPrevious.size()<1 && (! (this instanceof StartNode))) {
      throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
    }
    if (pPrevious.size()>1 && (! (this instanceof Join))) {
      throw new IllegalProcessModelException("Only join nodes may have multiple predecessors");
    }
    aPrevious = pPrevious;
  }

  public final Collection<ProcessNode> getPrevious() {
    return aPrevious;
  }

  public void addSuccessor(ProcessNode pNode) {
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

  public abstract boolean condition();

  public abstract void start(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance);

  public void skip(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance) {
    for(ProcessNode successor: aSuccessors) {
      successor.skip(pThreads, pProcessInstance);
    }
  }

  protected Collection<ProcessNodeInstance> startSuccessors(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance) {
    Collection<ProcessNodeInstance> result = new ArrayList<ProcessNodeInstance>();
    for (ProcessNode node:getSuccessors()) {
      if (node.condition()) {
        node.start(pThreads, pProcessInstance);
      } else {
        node.skip(pThreads, pProcessInstance);
      }
    }
    return result;
  }

}
