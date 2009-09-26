package nl.adaptivity.process.engine;

import java.io.Serializable;
import java.util.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.JoinInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;


public class ProcessInstance implements Serializable, HandleAware<ProcessInstance>{

  @XmlRootElement(name="processInstance", namespace="http://adaptivity.nl/ProcessEngine/")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class ProcessInstanceRef implements Handle<ProcessInstance> {

    @XmlAttribute(name="handle")
    private long aHandle;
    @XmlAttribute(name="processModel")
    private long aProcessModel;

    public ProcessInstanceRef() {
      // empty constructor;
    }

    public ProcessInstanceRef(ProcessInstance pProcessInstance) {
      setHandle(pProcessInstance.aHandle);
      setProcessModel(pProcessInstance.aProcessModel.getHandle());
    }

    public void setHandle(long handle) {
      aHandle = handle;
    }

    public long getHandle() {
      return aHandle;
    }

    public void setProcessModel(long processModel) {
      aProcessModel = processModel;
    }

    public long getProcessModel() {
      return aProcessModel;
    }

  }

  private static final long serialVersionUID = 1145452195455018306L;

  private final ProcessModel aProcessModel;

  private Collection<ProcessNodeInstance> aThreads;

  private int aFinished = 0;

  private HashMap<Join, JoinInstance> aJoins;

  private long aHandle;

  private final IProcessEngine aEngine;

  private final Payload aPayload;

  private Payload aResult;

  public ProcessInstance(ProcessModel pProcessModel, IProcessEngine pEngine, Payload pPayload) {
    aProcessModel = pProcessModel;
    aEngine = pEngine;
    aPayload = pPayload;
    aThreads = new LinkedList<ProcessNodeInstance>();
    for (StartNode node: aProcessModel.getStartNodes()) {
      ProcessNodeInstance instance = new ProcessNodeInstance(node, null, null);
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
      result = new JoinInstance(pJoin, predecessors);
      aJoins.put(pJoin, result);
    } else {
      result.addPredecessor(pPredecessor);
    }
    return result;
  }

  public void removeJoin(JoinInstance pJ) {
    aJoins.remove(pJ.getNode());
  }

  public void fireMessage(InternalMessage pMessage) {
    aEngine.fireMessage(pMessage);
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public void setHandle(long pHandle) {

    aHandle = pHandle;
  }

  public ProcessNodeInstance getProcesNodeInstanceFor(InternalMessage pRepliedMessage) {
    for(ProcessNodeInstance thread:aThreads) {
      if (thread.getMessage()==pRepliedMessage) {
        return thread;
      }
    }
    return null;
  }

  public IProcessEngine getEngine() {
    return aEngine;
  }

  public ProcessInstanceRef getRef() {
    return new ProcessInstanceRef(this);
  }

  public void start(IMessageService pMessageService) {
    for(ProcessNodeInstance node:aThreads) {
      provideTask(pMessageService, node);
    }
  }

  private void provideTask(IMessageService<?> pMessageService, ProcessNodeInstance pNode) {
    if (pNode.provideTask(pMessageService)) {
      takeTask(pMessageService, pNode);
    }
  }

  private void takeTask(IMessageService<?> pMessageService, ProcessNodeInstance pNode) {
    if (pNode.takeTask(pMessageService)) {
      startTask(pMessageService, pNode);
    }
  }

  private void startTask(IMessageService<?> pMessageService, ProcessNodeInstance pNode) {
    if (pNode.startTask(pMessageService)) {
      finishTask(pMessageService, pNode, null);
    }
  }

  private void finishTask(IMessageService<?> pMessageService, ProcessNodeInstance pNode, Payload pPayload) {
    pNode.finishTask(pPayload);
    aThreads.remove(pNode);
    List<ProcessNodeInstance> startedTasks = new ArrayList<ProcessNodeInstance>(pNode.getNode().getSuccessors().size());
    final List<ProcessNodeInstance> nodelist = Arrays.asList(pNode);
    for (ProcessNode successorNode: pNode.getNode().getSuccessors()) {
      ProcessNodeInstance instance = new ProcessNodeInstance(successorNode, null, nodelist);
      aThreads.add(instance);
      startedTasks.add(instance);
    }
    for (ProcessNodeInstance task:startedTasks) {
      provideTask(pMessageService, task);
    }
  }

}
