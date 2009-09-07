package nl.adaptivity.process.processModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import net.devrieze.util.HandleMap.HandleAware;

//@XmlRootElement(name="processModel")
//@XmlType(name="ProcessModel")
//@XmlAccessorType(XmlAccessType.NONE)
public class ProcessModel implements HandleAware, Serializable{

  private static final long serialVersionUID = -4199223546188994559L;
  private Collection<StartNode> aStartNodes;
  private int aEndNodeCount;
  private String aName;
  private long aHandle;

  public ProcessModel(Collection<EndNode> pEndNodes) {
    aStartNodes = reverseGraph(pEndNodes);
    aEndNodeCount = pEndNodes.size();
  }

  public ProcessModel(EndNode... pEndNodes) {
    this(Arrays.asList(pEndNodes));
  }
  
  public ProcessModel(XmlProcessModel pXmlModel) {
    Collection<EndNode> endNodes = new ArrayList<EndNode>();
    
    for(ProcessNode node:pXmlModel.getNodes()) {
      if (node instanceof EndNode) {
        endNodes.add((EndNode) node);
      }
    }

    aEndNodeCount = endNodes.size();
    
    aStartNodes = reverseGraph(endNodes);
    setName(pXmlModel.getName());
  }

  private static Collection<StartNode> reverseGraph(Collection<EndNode> pEndNodes) {
    
    Collection<StartNode> resultList = new ArrayList<StartNode>();
    for (EndNode endNode:pEndNodes) {
      reverseGraph(resultList, endNode);
    }
    return resultList;
  }

  private static void reverseGraph(Collection<StartNode> pResultList, ProcessNode pNode) {
    Collection<ProcessNode> previous = pNode.getPredecessors();
    for(ProcessNode prev: previous) {
      if (prev instanceof StartNode) {
        if (prev.getSuccessors()==null) {
          pResultList.add((StartNode) prev);
        }
        prev.addSuccessor(pNode);
      } else if (prev.getSuccessors()==null || prev.getSuccessors().size()==0){
        prev.addSuccessor(pNode);
        reverseGraph(pResultList, prev);
      } else {
        prev.addSuccessor(pNode);
      }
    }
  }

//  @XmlElementRefs({
//    @XmlElementRef(name = "end", type = EndNode.class),
//    @XmlElementRef(name = "activity", type = Activity.class),
//    @XmlElementRef(name = "start", type = StartNode.class),
//    @XmlElementRef(name = "join", type = Join.class)
//  })
  public ProcessNode[] getModelNodes() {
    Collection<ProcessNode> list = new ArrayList<ProcessNode>();
    HashSet<String> seen = new HashSet<String>();
    if (aStartNodes!=null) {
      for(StartNode node: aStartNodes) {
        extractElements(list, seen, node);
      }
    }
    return list.toArray(new ProcessNode[list.size()]);
  }
  
  public void setModelNodes(ProcessNode[] pProcessNodes) {
    ArrayList<EndNode> endNodes = new ArrayList<EndNode>();
    for(ProcessNode n:pProcessNodes) {
      if (n instanceof EndNode) {
        endNodes.add((EndNode) n);
      }
    }
    aStartNodes = reverseGraph(endNodes);
    aEndNodeCount = endNodes.size();
  }

  private static void extractElements(Collection<ProcessNode> pTo, HashSet<String> pSeen, ProcessNode pNode) {
    if (pSeen.contains(pNode.getId())) {
      return;
    }
    pTo.add(pNode);
    pSeen.add(pNode.getId());
    for(ProcessNode node:pNode.getSuccessors()) {
      extractElements(pTo, pSeen, node);
    }
  }
  
  public Collection<StartNode> getStartNodes() {
    return aStartNodes;
  }

//  public ProcessInstance createInstance(IProcessEngine pEngine, Payload pPayload) {
//    return new ProcessInstance(this, pEngine, pPayload);
//  }

  public int getEndNodeCount() {
    return aEndNodeCount;
  }
  
  public String getName() {
    return aName;
  }
  
  public void setName(String name) {
    aName = name;
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public void setHandle(long pHandle) {
    aHandle = pHandle;
  }

  public ProcessModelRef getRef() {
    return new ProcessModelRef(getName(), aHandle);
  }

}
