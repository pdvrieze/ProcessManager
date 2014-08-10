package nl.adaptivity.process.processModel;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import nl.adaptivity.process.processModel.engine.IProcessModelRef;


public interface ProcessModel<T extends ProcessNode<? extends T>> {

  /**
   * Get the UUID for this process model.
   * @return The UUID this process model has.
   */
  public UUID getUuid();

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
  public IProcessModelRef<? extends T> getRef();

  /**
   * Get the process node with the given id.
   * @param pNodeId The node id to look up.
   * @return The process node with the id.
   */
  public T getNode(String pNodeId);

  public Collection<? extends T> getModelNodes();

  public String getName();

  public Principal getOwner();

  public Set<String> getRoles();

  public Collection<? extends StartNode<? extends T>> getStartNodes();

  public Collection<? extends IXmlResultType> getImports();

  public Collection<? extends IXmlDefineType> getExports();

}