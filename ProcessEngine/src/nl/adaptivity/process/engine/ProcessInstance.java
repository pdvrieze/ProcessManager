package nl.adaptivity.process.engine;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Node;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.JoinInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;


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

  private final ProcessModelImpl aProcessModel;

  private final Collection<ProcessNodeInstance> aThreads;

  private final Collection<ProcessNodeInstance> aEndResults;

  private HashMap<Join, JoinInstance> aJoins;

  private long aHandle;

  private final ProcessEngine aEngine;

  private Node aPayload;

  private final String aName;

  private final Principal aOwner;

  ProcessInstance(final long pHandle, final Principal pOwner, final ProcessModelImpl pProcessModel, final String pName, final ProcessEngine pEngine) {
    aHandle = pHandle;
    aProcessModel = pProcessModel;
    aOwner = pOwner;
    aEngine = pEngine;
    aName =pName;
    aThreads = new LinkedList<>();
    aJoins = new HashMap<>();
    aEndResults = new ArrayList<>();
  }

  void setThreads(final Collection<? extends Handle<? extends ProcessNodeInstance>> pThreads) {
    for(Handle<? extends ProcessNodeInstance> thread:pThreads) {
      aThreads.add(aEngine.getNodeInstance(thread.getHandle(), SecurityProvider.SYSTEMPRINCIPAL));
    }
  }

  public ProcessInstance(final Principal pOwner, final ProcessModelImpl pProcessModel, final String pName, final ProcessEngine pEngine) {
    aProcessModel = pProcessModel;
    aName = pName;
    aEngine = pEngine;
    aThreads = new LinkedList<>();
    aOwner = pOwner;
    for (final StartNode node : aProcessModel.getStartNodes()) {
      final ProcessNodeInstance instance = new ProcessNodeInstance((StartNodeImpl) node, null, this);
      aThreads.add(instance);
    }
    aJoins = new HashMap<>();
    aEndResults = new ArrayList<>();
  }

  public synchronized void finish() {
    int aFinished = getFinishedCount();
    if (aFinished >= aProcessModel.getEndNodeCount()) {
      aEngine.finishInstance(this);
    }
  }

  private int getFinishedCount() {
    return aEndResults.size();
  }

  public synchronized JoinInstance getJoinInstance(final JoinImpl pJoin, final ProcessNodeInstance pPredecessor) {
    JoinInstance result = aJoins.get(pJoin);
    if (result == null) {
      final Collection<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>(pJoin.getPredecessors().size());
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

  public ProcessModelImpl getProcessModel() {
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
      aEndResults.add(pNode);
      aThreads.remove(pNode);
      finish();
    } else {
      aThreads.remove(pNode);
      final List<ProcessNodeInstance> startedTasks = new ArrayList<>(pNode.getNode().getSuccessors().size());
      for (final ProcessNode successorNode : pNode.getNode().getSuccessors()) {
        final ProcessNodeInstance instance = getProcessNodeInstance(pNode, successorNode);
        aThreads.add(instance);
        startedTasks.add(instance);
      }
      for (final ProcessNodeInstance task : startedTasks) {
        provideTask(pMessageService, task);
      }
    }
  }

  private ProcessNodeInstance getProcessNodeInstance(final ProcessNodeInstance pPredecessor, final ProcessNode pNode) {
    if (pNode instanceof Join) {
      final JoinImpl join = (JoinImpl) pNode;
      if (aJoins == null) {
        aJoins = new HashMap<>();
      }
      JoinInstance instance = aJoins.get(join);
      if (instance == null) {

        final Collection<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>(pNode.getPredecessors().size());
        predecessors.add(pPredecessor);
        instance = new JoinInstance(join, predecessors, this);
        aJoins.put(join, instance);
      } else {
        instance.addPredecessor(pPredecessor);
      }
      return instance;

    } else {
      return new ProcessNodeInstance((ProcessNodeImpl) pNode, pPredecessor, this);
    }
  }

  public synchronized void failTask(@SuppressWarnings("unused") final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode, final Throwable pCause) {
    pNode.failTask(pCause);
  }

  public synchronized void cancelTask(@SuppressWarnings("unused") final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) {
    pNode.cancelTask();
  }

  public synchronized Collection<ProcessNodeInstance> getActivePredecessorsFor(final Join pJoin) {
    final ArrayList<ProcessNodeInstance> activePredecesors = new ArrayList<>(Math.min(pJoin.getPredecessors().size(), aThreads.size()));
    for (final ProcessNodeInstance node : aThreads) {
      if (node.getNode().isPredecessorOf(pJoin)) {
        activePredecesors.add(node);
      }
    }
    return activePredecesors;
  }

  public synchronized Collection<? extends Handle<? extends ProcessNodeInstance>> getDirectSuccessors(final ProcessNodeInstance pPredecessor) {
    final ArrayList<Handle<? extends ProcessNodeInstance>> result = new ArrayList<>(pPredecessor.getNode().getSuccessors().size());
    for (final ProcessNodeInstance candidate : aThreads) {
      addDirectSuccessor(result, candidate, pPredecessor);
    }
    return result;
  }

  private void addDirectSuccessor(final ArrayList<Handle<? extends ProcessNodeInstance>> pResult, ProcessNodeInstance pCandidate, final Handle<? extends ProcessNodeInstance> pPredecessor) {
    // First look for this node, before diving into it's children
    for (final Handle<? extends ProcessNodeInstance> node : pCandidate.getDirectPredecessors()) {
      if (node.getHandle() == pPredecessor.getHandle()) {
        pResult.add(pCandidate);
        return; // Assume that there is no further "successor" down the chain
      }
    }
    for (final Handle<? extends ProcessNodeInstance> hnode : pCandidate.getDirectPredecessors()) {
      // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
      ProcessNodeInstance node = pCandidate.getProcessInstance().getEngine().getNodeInstance(hnode.getHandle(), SecurityProvider.SYSTEMPRINCIPAL);
      addDirectSuccessor(pResult, node, pPredecessor);
    }
  }

}
