package nl.adaptivity.process.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.process.engine.processModel.*;


public class ProcessInstance implements Serializable, HandleAware{

  private static final long serialVersionUID = 1145452195455018306L;

  private final ProcessModel aProcessModel;

  private Collection<ProcessNodeInstance> aThreads;

  private int aFinished = 0;

  private HashMap<Join, JoinInstance> aJoins;

  private long aHandle;

  private final IProcessEngine aEngine;

  private final Payload aPayload;

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

  private void fireNode(Collection<ProcessNodeInstance> pThreads, ProcessNode node, ProcessNodeInstance pPredecessor) {
    if (node.condition()) {
      node.start(pThreads, this, pPredecessor);
    } else {
      node.skip(pThreads, this, pPredecessor);
    }
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
      Collection<ProcessNodeInstance> predecessors = new ArrayList<ProcessNodeInstance>(pJoin.getPrevious().size());
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

  public void finishThread(ProcessNodeInstance pOldInstance) {
    aThreads.remove(pOldInstance);
    for (ProcessNode successor: pOldInstance.getNode().getSuccessors()) {
      successor.start(aThreads, this, pOldInstance);
    }
  }

  public void start() {
    ArrayList<ProcessNodeInstance> copy = new ArrayList<ProcessNodeInstance>(aThreads.size());
    copy.addAll(aThreads);
    for (ProcessNodeInstance node : copy) {
      node.finish(aPayload, this);
    }
  }

  public IProcessEngine getEngine() {
    return aEngine;
  }

}
