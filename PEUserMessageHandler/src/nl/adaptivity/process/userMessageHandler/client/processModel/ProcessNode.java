package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.Collection;
import java.util.Map;


public abstract class ProcessNode {

  private String aId;

  protected ProcessNode(String pId) {
    aId = pId;
  }

  public String getId() {
    return aId;
  }

  public abstract void resolvePredecessors(Map<String, ProcessNode> pMap);

  public abstract void ensureSuccessor(ProcessNode pNode);

  public abstract Collection<ProcessNode> getSuccessors();

}
