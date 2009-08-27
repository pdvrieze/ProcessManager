package nl.adaptivity.process.engine;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.Serializable;
import java.util.*;

import javax.xml.bind.JAXB;

import nl.adaptivity.process.engine.processModel.*;
import nl.adaptivity.process.processmodel.jaxb.*;


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
  
  public ProcessModel(XmlProcessModel pXmlModel) {
    Map<String, ProcessNode> seenNodes = new HashMap<String, ProcessNode>();
    Map<String,XmlProcessModelNode> unseenNodes = new HashMap<String, XmlProcessModelNode>();
    Collection<XmlEndNode> endNodes = new ArrayList<XmlEndNode>();
    
    for(XmlProcessModelNode node:pXmlModel.getNodes()) {
      unseenNodes.put(node.getId(), node);
      if (node instanceof XmlEndNode) {
        endNodes.add((XmlEndNode) node);
      }
    }

    Collection<EndNode> result = new ArrayList<EndNode>(endNodes.size());
    aEndNodeCount = endNodes.size();
    
    for(XmlEndNode endNode:endNodes) {
      XmlProcessModelNode pred = endNode.getPredecessor();
      ProcessNode predNode = toProcessNode(seenNodes, pred);
      result.add(new EndNode(predNode));
    }
    
    aStartNodes = reverseGraph(result);
  }

  private static ProcessNode toProcessNode(Map<String, ProcessNode> pSeenNodes, XmlProcessModelNode node) {
    ProcessNode result = pSeenNodes.get(node.getId());
    if (result != null) { return result; }
    if (node instanceof XmlStartNode) {
      result = new StartNode();
    } else if (node instanceof XmlActivity) {
      result = toProcessNode2(pSeenNodes, (XmlActivity) node);
    } else if (node instanceof XmlJoin) {
      result = toProcessNode2(pSeenNodes, (XmlJoin) node);
    }
    pSeenNodes.put(node.getId(), result);
    return result;
  }

  private static Join toProcessNode2(Map<String, ProcessNode> pSeenNodes, XmlJoin pNode) {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  private static Activity toProcessNode2(Map<String, ProcessNode> pSeenNodes, XmlActivity pNode) {
    ProcessNode predecessor = toProcessNode(pSeenNodes, pNode.getPredecessor());
    String name = pNode.getName();
    String condition = pNode.getCondition();
    List<XmlImportType> imports = pNode.getImports();
    List<XmlExportType> exports = pNode.getExports();
    
    final Activity result = new Activity(predecessor);
    result.setName(name);
    result.setCondition(condition);
    result.setImports(imports);
    result.setExports(exports);
    return result;
  }

  private static Collection<StartNode> reverseGraph(Collection<EndNode> pEndNodes) {
    
    Collection<StartNode> resultList = new ArrayList<StartNode>();
    for (EndNode endNode:pEndNodes) {
      reverseGraph(resultList, endNode);
    }
    return resultList;
  }

  private static void reverseGraph(Collection<StartNode> pResultList, ProcessNode pNode) {
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

  public ProcessInstance createInstance(IProcessEngine pEngine, Payload pPayload) {
    return new ProcessInstance(this, pEngine, pPayload);
  }

  public int getEndNodeCount() {
    return aEndNodeCount;
  }
  
  public static void main(String[] pArgs) {
    if (pArgs.length!=1) {
      System.out.println("Give process model file as parameter");
      System.exit(1);
    }
    XmlProcessModel pm = JAXB.unmarshal(new File(pArgs[0]), XmlProcessModel.class);
    CharArrayWriter out = new CharArrayWriter();
    JAXB.marshal(pm, out);
    System.out.println(out.toString());
    System.out.println(pm.toString());
  }

}
