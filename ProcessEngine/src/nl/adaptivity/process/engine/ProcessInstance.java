package nl.adaptivity.process.engine;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Node;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.security.SecureObject;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.JoinInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.processModel.*;


public class ProcessInstance implements Serializable, HandleAware<ProcessInstance>, SecureObject {

  @XmlRootElement(name = "processInstance", namespace = "http://adaptivity.nl/ProcessEngine/")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class ProcessInstanceRef implements Handle<ProcessInstance> {

    private long aHandle;

    private long aProcessModel;

    private String aName;

    public ProcessInstanceRef() {
      // empty constructor;
    }

    public ProcessInstanceRef(final ProcessInstance pProcessInstance) {
      setHandle(pProcessInstance.getHandle());
      setProcessModel(pProcessInstance.aProcessModel.getHandle());
      aName = pProcessInstance.getName();
      if ((aName == null) || (aName.trim().length() == 0)) {
        aName = pProcessInstance.aProcessModel.getName() + " instance " + aHandle;
      }
    }

    public void setHandle(final long handle) {
      aHandle = handle;
    }

    @Override
    @XmlAttribute(name = "handle")
    public long getHandle() {
      return aHandle;
    }

    public void setProcessModel(final long processModel) {
      aProcessModel = processModel;
    }

    @XmlAttribute(name = "processModel")
    public long getProcessModel() {
      return aProcessModel;
    }

    @XmlAttribute(name = "name")
    public String getName() {
      return aName;
    }

    public void setName(final String pName) {
      aName = pName;
    }

  }

  private static final long serialVersionUID = 1145452195455018306L;

  private final ProcessModel aProcessModel;

  private final Collection<ProcessNodeInstance> aThreads;

  private HashMap<Join, JoinInstance> aJoins;

  private long aHandle;

  private final ProcessEngine aEngine;

  private Node aPayload;

  private final String aName;

  private final Principal aOwner;

  public ProcessInstance(final Principal pOwner, final ProcessModel pProcessModel, final String pName, final ProcessEngine pEngine) {
    aProcessModel = pProcessModel;
    aName = pName;
    aEngine = pEngine;
    aThreads = new LinkedList<ProcessNodeInstance>();
    aOwner = pOwner;
    for (final StartNode node : aProcessModel.getStartNodes()) {
      final ProcessNodeInstance instance = new ProcessNodeInstance(node, null, this);
      aThreads.add(instance);
    }
    aJoins = new HashMap<Join, JoinInstance>();
  }

  public synchronized void finish() {
    int aFinished = getFinishedCount();
    if (aFinished >= aProcessModel.getEndNodeCount()) {
      aEngine.finishInstance(this);
    }
  }

  private int getFinishedCount() {
    int count = 0;
    for(ProcessNodeInstance thread:aThreads) {
      if (thread.getNode() instanceof EndNode) {
        ++count;
      }
    }
    return count;
  }

  public synchronized JoinInstance getJoinInstance(final Join pJoin, final ProcessNodeInstance pPredecessor) {
    JoinInstance result = aJoins.get(pJoin);
    if (result == null) {
      final Collection<ProcessNodeInstance> predecessors = new ArrayList<ProcessNodeInstance>(pJoin.getPredecessors().size());
      predecessors.add(pPredecessor);
      result = new JoinInstance(pJoin, predecessors, this);
      aJoins.put(pJoin, result);
    } else {
      result.addPredecessor(pPredecessor);
    }
    return result;
  }

  public synchronized void removeJoin(final JoinInstance pJ) {
    aJoins.remove(pJ.getNode());
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public void setHandle(final long pHandle) {

    aHandle = pHandle;
  }

  public String getName() {
    return aName;
  }

  public Principal getOwner() {
    return aOwner;
  }

  public ProcessEngine getEngine() {
    return aEngine;
  }

  public ProcessInstanceRef getRef() {
    return new ProcessInstanceRef(this);
  }

  public ProcessModel getProcessModel() {
    return aProcessModel;
  }

  public Node getPayload() {
    return aPayload;
  }

  public synchronized void start(final IMessageService<?, ProcessNodeInstance> pMessageService, final Node pPayload) {
    if (aThreads.size() == 0) {
      throw new IllegalStateException("No starting nodes in process");
    }
    aPayload = pPayload;
    for (final ProcessNodeInstance node : aThreads) {
      provideTask(pMessageService, node);
    }
  }

  public synchronized void provideTask(final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) {
    if (pNode.provideTask(pMessageService)) {
      takeTask(pMessageService, pNode);
    }
  }

  public synchronized void takeTask(final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) {
    if (pNode.takeTask(pMessageService)) {
      startTask(pMessageService, pNode);
    }
  }

  public synchronized void startTask(final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) {
    if (pNode.startTask(pMessageService)) {
      finishTask(pMessageService, pNode, null);
    }
  }

  public synchronized void finishTask(final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode, final Node pPayload) {
    pNode.finishTask(pPayload);
    if (pNode.getNode() instanceof EndNode) {
      finish();
      aThreads.remove(pNode);
    } else {
      aThreads.remove(pNode);
      final List<ProcessNodeInstance> startedTasks = new ArrayList<ProcessNodeInstance>(pNode.getNode().getSuccessors().size());
      for (final ProcessNode successorNode : pNode.getNode().getSuccessors()) {
        final ProcessNodeInstance instance = getProcessInstance(pNode, successorNode);
        aThreads.add(instance);
        startedTasks.add(instance);
      }
      for (final ProcessNodeInstance task : startedTasks) {
        provideTask(pMessageService, task);
      }
    }
  }

  private ProcessNodeInstance getProcessInstance(final ProcessNodeInstance pPredecessor, final ProcessNode pNode) {
    if (pNode instanceof Join) {
      final Join join = (Join) pNode;
      if (aJoins == null) {
        aJoins = new HashMap<Join, JoinInstance>();
      }
      JoinInstance instance = aJoins.get(join);
      if (instance == null) {

        final Collection<ProcessNodeInstance> predecessors = new ArrayList<ProcessNodeInstance>(pNode.getPredecessors().size());
        predecessors.add(pPredecessor);
        instance = new JoinInstance(join, predecessors, this);
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
  public void failTask(final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) {
    pNode.failTask(null);
  }

  public synchronized void failTask(final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode, final Throwable pCause) {
    pNode.failTask(pCause);
  }

  public synchronized void cancelTask(final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) {
    pNode.cancelTask();
  }

  public synchronized Collection<ProcessNodeInstance> getActivePredecessorsFor(final Join pJoin) {
    final ArrayList<ProcessNodeInstance> activePredecesors = new ArrayList<ProcessNodeInstance>(Math.min(pJoin.getPredecessors().size(), aThreads.size()));
    for (final ProcessNodeInstance node : aThreads) {
      if (node.getNode().isPredecessorOf(pJoin)) {
        activePredecesors.add(node);
      }
    }
    return activePredecesors;
  }

  public synchronized Collection<ProcessNodeInstance> getDirectSuccessors(final ProcessNodeInstance pPredecessor) {
    final ArrayList<ProcessNodeInstance> result = new ArrayList<ProcessNodeInstance>(pPredecessor.getNode().getSuccessors().size());
    for (final ProcessNodeInstance candidate : aThreads) {
      addDirectSuccessor(result, candidate, pPredecessor);
    }
    return result;
  }

  private void addDirectSuccessor(final ArrayList<ProcessNodeInstance> pResult, final ProcessNodeInstance pCandidate, final ProcessNodeInstance pPredecessor) {
    // First look for this node, before diving into it's children
    for (final ProcessNodeInstance node : pCandidate.getDirectPredecessors()) {
      if (node == pPredecessor) {
        pResult.add(pCandidate);
        return; // Assume that there is no further "successor" down the chain
      }
    }
    for (final ProcessNodeInstance node : pCandidate.getDirectPredecessors()) {
      addDirectSuccessor(pResult, node, pPredecessor);
    }
  }

}
