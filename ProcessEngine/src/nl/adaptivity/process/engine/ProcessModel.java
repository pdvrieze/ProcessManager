package nl.adaptivity.process.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import nl.adaptivity.process.engine.processModel.EndNode;
import nl.adaptivity.process.engine.processModel.ProcessNode;
import nl.adaptivity.process.engine.processModel.StartNode;


public class ProcessModel implements Serializable{

  private static final long serialVersionUID = -4199223546188994559L;
  private Collection<StartNode> aStartNodes;
  private int aEndNodeCount;

  public ProcessModel(Collection<EndNode> pEndNodes) {
    aStartNodes = reverseGraph(pEndNodes);
    aEndNodeCount = pEndNodes.size();
  }

  public ProcessModel() {
    
  }
  
  public ProcessModel(EndNode... pEndNodes) {
    this(Arrays.asList(pEndNodes));
  }

  private Collection<StartNode> reverseGraph(Collection<EndNode> pEndNodes) {
    
    Collection<StartNode> resultList = new ArrayList<StartNode>();
    for (EndNode endNode:pEndNodes) {
      reverseGraph(resultList, endNode);
    }
    return resultList;
  }

  private void reverseGraph(Collection<StartNode> pResultList, ProcessNode pNode) {
    Collection<ProcessNode> previous = pNode.getPrevious();
    for(ProcessNode prev: previous) {
      if (prev instanceof StartNode) {
        if (prev.getSuccessors()==null) {
          pResultList.add((StartNode) prev);
        }
        prev.addSuccessor(pNode);
      } else if (prev.getSuccessors()==null){
        prev.addSuccessor(pNode);
        reverseGraph(pResultList, prev);
      } else {
        prev.addSuccessor(pNode);
      }
    }
  }

  public Collection<StartNode> getStartNodes() {
    return aStartNodes;
  }

  public ProcessInstance createInstance(IProcessEngine pEngine) {
    return new ProcessInstance(this, pEngine);
  }

  public int getEndNodeCount() {
    return aEndNodeCount;
  }
  

}
