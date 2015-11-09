package nl.adaptivity.process.processModel;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlDeserializer;


@XmlDeserializer(ProcessModelImpl.Factory.class)
public interface ProcessModel<T extends ProcessNode<? extends T>> {

  /**
   * Get the UUID for this process model.
   * @return The UUID this process model has.
   */
  UUID getUuid();

  void setUuid(UUID pUUID);

  /**
   * Get the amount of end nodes in the model
   *
   * @return The amount of end nodes.
   */
  int getEndNodeCount();

  /**
   * Get a reference node for this model.
   *
   * @return A reference node.
   */
  IProcessModelRef<? extends T> getRef();

  /**
   * Get the process node with the given id.
   * @param pNodeId The node id to look up.
   * @return The process node with the id.
   */
  T getNode(Identifiable pNodeId);

  Collection<? extends T> getModelNodes();

  String getName();

  Principal getOwner();

  Set<String> getRoles();

  Collection<? extends StartNode<? extends T>> getStartNodes();

  Collection<? extends IXmlResultType> getImports();

  Collection<? extends IXmlDefineType> getExports();
}