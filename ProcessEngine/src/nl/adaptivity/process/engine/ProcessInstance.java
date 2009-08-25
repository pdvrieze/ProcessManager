package nl.adaptivity.process.engine;

import java.io.Serializable;
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

  public ProcessInstance(ProcessModel pProcessModel, IProcessEngine pEngine) {
    aProcessModel = pProcessModel;
    aEngine = pEngine;
    aThreads = new LinkedList<ProcessNodeInstance>();
    aJoins = new HashMap<Join, JoinInstance>();
  }

  private void fireNode(Collection<ProcessNodeInstance> pThreads, ProcessNode node) {
    if (node.condition()) {
      node.start(pThreads, this);
    } else {
      node.skip(pThreads, this);
    }
  }

  public void finish(EndNode pEndNode) {
    aFinished++;
    if (aFinished>=aProcessModel.getEndNodeCount()) {
      aEngine.finishInstance(this);
    }
  }

  public JoinInstance getInstance(Join pJoin) {
    JoinInstance result = aJoins.get(pJoin);
    if (result == null) {
      result = new JoinInstance(pJoin);
      aJoins.put(pJoin, result);
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
      successor.start(aThreads, this);
    }
  }

  public void start() {
    for (ProcessNode node: aProcessModel.getStartNodes()) {
      fireNode(aThreads, node);
    }
  }

  public IProcessEngine getEngine() {
    return aEngine;
  }

}
