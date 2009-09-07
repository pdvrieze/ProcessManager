package nl.adaptivity.process.processModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import net.devrieze.util.IdFactory;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name="ProcesNode")
@XmlSeeAlso({Join.class, Activity.class, EndNode.class, StartNode.class})
public abstract class ProcessNode implements Serializable {

  private static final long serialVersionUID = -7745019972129682199L;
  
  private Collection<ProcessNode> aPredecessors;

  private Collection<ProcessNode> aSuccessors = null;

  private String aId;
  
  protected ProcessNode() {
    
  }
  
  protected ProcessNode(ProcessNode pPrevious) {
    if (pPrevious == null) {
      if (! (this instanceof StartNode || this instanceof Join)) {
        throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
      }
      setPredecessors(Arrays.asList(new ProcessNode[0]));
    } else {
      setPredecessors(Arrays.asList(pPrevious));
    }
  }
  
  public ProcessNode(Collection<ProcessNode> pPredecessors) {
    if (pPredecessors.size()<1 && (! (this instanceof StartNode))) {
      throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
    }
    if (pPredecessors.size()>1 && (! (this instanceof Join))) {
      throw new IllegalProcessModelException("Only join nodes may have multiple predecessors");
    }
    setPredecessors(pPredecessors);
  }

  public Collection<ProcessNode> getPredecessors() {
    if (aPredecessors==null) {
      aPredecessors = new ArrayList<ProcessNode>();
    }
    return aPredecessors;
  }

  public void setPredecessors(Collection<ProcessNode> predecessors) {
    if (aPredecessors!=null) {
      throw new UnsupportedOperationException("Not allowed to change predecessors");
    }
    aPredecessors = predecessors;
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

  @Deprecated
  public abstract void start();

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
  public String getId(){
    if (aId==null) {
      aId=IdFactory.create();
    }
    return aId;
  }

  public void setId(String id) {
    aId = id;
  }

}
