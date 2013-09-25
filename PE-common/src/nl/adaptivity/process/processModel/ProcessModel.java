package nl.adaptivity.process.processModel;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;


public interface ProcessModel {

  /**
   * Get the amount of end nodes in the model
   *
   * @return The amount of end nodes.
   */
  public int getEndNodeCount();

  /**
   * Get a reference node for this model.
   *
   * @return A reference node.
   */
  public ProcessModelRef getRef();

  /**
   * Get the process node with the given id.
   * @param pNodeId The node id to look up.
   * @return The process node with the id.
   */
  public ProcessNode getNode(String pNodeId);

  public Collection<? extends ProcessNode> getModelNodes();

  public String getName();

  public Principal getOwner();

  public Set<String> getRoles();

  public Collection<StartNode> getStartNodes();

}