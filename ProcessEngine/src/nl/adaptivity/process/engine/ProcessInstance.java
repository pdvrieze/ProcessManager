package nl.adaptivity.process.engine;

import java.io.Serializable;
import java.util.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Node;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.JoinInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.processModel.*;


public class ProcessInstance implements Serializable, HandleAware<ProcessInstance>{

  @XmlRootElement(name="processInstance", namespace="http://adaptivity.nl/ProcessEngine/")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class ProcessInstanceRef implements Handle<ProcessInstance> {

    private long aHandle;
    private long aProcessModel;
    private String aName;

    public ProcessInstanceRef() {
      // empty constructor;
    }

    public ProcessInstanceRef(ProcessInstance pProcessInstance) {
      setHandle(pProcessInstance.getHandle());
      setProcessModel(pProcessInstance.aProcessModel.getHandle());
      aName = pProcessInstance.getName();
      if (aName == null || aName.trim().length()==0) {
        aName = pProcessInstance.aProcessModel.getName()+" instance " + aHandle;
      }
    }

    public void setHandle(long handle) {
      aHandle = handle;
    }

    @XmlAttribute(name="handle")
    public long getHandle() {
      return aHandle;
    }

    public void setProcessModel(long processModel) {
      aProcessModel = processModel;
    }

    @XmlAttribute(name="processModel")
    public long getProcessModel() {
      return aProcessModel;
    }

    @XmlAttribute(name="name")
    public String getName() {
      return aName;
    }

    public void setName(String pName) {
      aName = pName;
    }

  }

  private static final long serialVersionUID = 1145452195455018306L;

  private final ProcessModel aProcessModel;

  private Collection<ProcessNodeInstance> aThreads;

  private int aFinished = 0;

  private HashMap<Join, JoinInstance> aJoins;

  private long aHandle;

  private final ProcessEngine aEngine;

  private Node aPayload;

  private final String aName;

  public ProcessInstance(ProcessModel pProcessModel, String pName, ProcessEngine pEngine) {
    aProcessModel = pProcessModel;
    aName = pName;
    aEngine = pEngine;
    aThreads = new LinkedList<ProcessNodeInstance>();
    for (StartNode node: aProcessModel.getStartNodes()) {
      ProcessNodeInstance instance = new ProcessNodeInstance(node, null, this);
      aThreads.add(instance);
    }
    aJoins = new HashMap<Join, JoinInstance>();
  }

  public void finish() {
    aFinished++;
    if (aFinished>=aProcessModel.getEndNodeCount()) {
      aEngine.finishInstance(this);
    }
  }

  public JoinInstance getJoinInstance(Join pJoin, ProcessNodeInstance pPredecessor) {
    JoinInstance result = aJoins.get(pJoin);
    if (result == null) {
      Collection<ProcessNodeInstance> predecessors = new ArrayList<ProcessNodeInstance>(pJoin.getPredecessors().size());
      predecessors.add(pPredecessor);
      result = new JoinInstance(pJoin, predecessors, this);
      aJoins.put(pJoin, result);
    } else {
      result.addPredecessor(pPredecessor);
    }
    return result;
  }

  public void removeJoin(JoinInstance pJ) {
    aJoins.remove(pJ.getNode());
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public void setHandle(long pHandle) {

    aHandle = pHandle;
  }

  public String getName() {
    return aName;
  }

  public ProcessEngine getEngine() {
    return aEngine;
  }

  public ProcessInstanceRef getRef() {
    return new ProcessInstanceRef(this);
  }

  public Node getPayload() {
    return aPayload;
  }

  public void start(IMessageService<?, ProcessNodeInstance> pMessageService, Node pPayload) {
    if (aThreads.size()==0) { throw new IllegalStateException("No starting nodes in process"); }
    aPayload = pPayload;
    for(ProcessNodeInstance node:aThreads) {
      provideTask(pMessageService, node);
    }
  }

  public void provideTask(IMessageService<?, ProcessNodeInstance> pMessageService, ProcessNodeInstance pNode) {
    if (pNode.provideTask(pMessageService)) {
      takeTask(pMessageService, pNode);
    }
  }

  public void takeTask(IMessageService<?, ProcessNodeInstance> pMessageService, ProcessNodeInstance pNode) {
    if (pNode.takeTask(pMessageService)) {
      startTask(pMessageService, pNode);
    }
  }

  public void startTask(IMessageService<?, ProcessNodeInstance> pMessageService, ProcessNodeInstance pNode) {
    if (pNode.startTask(pMessageService)) {
      finishTask(pMessageService, pNode, null);
    }
  }

  public void finishTask(IMessageService<?, ProcessNodeInstance> pMessageService, ProcessNodeInstance pNode, Node pPayload) {
    pNode.finishTask(pPayload);
    if (pNode.getNode() instanceof EndNode) {
      finish();
      aThreads.remove(pNode);
    } else {
      aThreads.remove(pNode);
      List<ProcessNodeInstance> startedTasks = new ArrayList<ProcessNodeInstance>(pNode.getNode().getSuccessors().size());
      for (ProcessNode successorNode: pNode.getNode().getSuccessors()) {
        ProcessNodeInstance instance = getProcessInstance(pNode, successorNode);
        aThreads.add(instance);
        startedTasks.add(instance);
      }
      for (ProcessNodeInstance task:startedTasks) {
        provideTask(pMessageService, task);
      }
    }
  }

  private ProcessNodeInstance getProcessInstance(final ProcessNodeInstance pPredecessor, ProcessNode pNode) {
    if (pNode instanceof Join) {
      Join join = (Join) pNode;
      if (aJoins==null) {
        aJoins = new HashMap<Join, JoinInstance>();
      }
      JoinInstance instance = aJoins.get(join);
      if (instance==null) {

        Collection<ProcessNodeInstance> predecessors = new ArrayList<ProcessNodeInstance>(pNode.getPredecessors().size());
        predecessors.add(pPredecessor);
        instance = new JoinInstance(join, predecessors , this);
        aJoins.put(join, instance);
      } else {
        instance.addPredecessor(pPredecessor);
      }
      return instance;

    } else {
      return new ProcessNodeInstance(pNode, pPredecessor, this);
    }
  }

  @Deprecated
  public void failTask(IMessageService<?, ProcessNodeInstance> pMessageService, ProcessNodeInstance pNode) {
    pNode.failTask(null);
  }

  public void failTask(IMessageService<?, ProcessNodeInstance> pMessageService, ProcessNodeInstance pNode, Throwable pCause) {
    pNode.failTask(pCause);
    
  }

  public void cancelTask(IMessageService<?, ProcessNodeInstance> pMessageService, ProcessNodeInstance pT) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  public Collection<ProcessNodeInstance> getActivePredecessorsFor(Join pJoin) {
    ArrayList<ProcessNodeInstance> activePredecesors=new ArrayList<ProcessNodeInstance>(Math.min(pJoin.getPredecessors().size(), aThreads.size()));
    for(ProcessNodeInstance node:aThreads) {
      if (node.getNode().isPredecessorOf(pJoin)) {
        activePredecesors.add(node);
      }
    }


    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");

  }

  public Collection<ProcessNodeInstance> getDirectSuccessors(ProcessNodeInstance pPredecessor) {
    ArrayList<ProcessNodeInstance> result = new ArrayList<ProcessNodeInstance>(pPredecessor.getNode().getSuccessors().size());
    for (ProcessNodeInstance candidate:aThreads) {
      addDirectSuccessor(result, candidate, pPredecessor);
    }
    return result;
  }

  private void addDirectSuccessor(ArrayList<ProcessNodeInstance> pResult, ProcessNodeInstance pCandidate, ProcessNodeInstance pPredecessor) {
    // First look for this node, before diving into it's children
    for(ProcessNodeInstance node: pCandidate.getDirectPredecessors()) {
      if (node == pPredecessor) {
        pResult.add(pCandidate);
        return; // Assume that there is no further "succcesor" down the chain
      }
    }
    for(ProcessNodeInstance node: pCandidate.getDirectPredecessors()) {
      addDirectSuccessor(pResult, node, pPredecessor);
    }
  }

}
