package nl.adaptivity.process.processModel;

import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlDeserializer;
import org.jetbrains.annotations.Nullable;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;


@XmlDeserializer(ProcessModelImpl.Factory.class)
public interface ProcessModel<T extends ProcessNode<? extends T>> {

  /**
   * Get the UUID for this process model.
   * @return The UUID this process model has.
   */
  @Nullable
  UUID getUuid();

  void setUuid(UUID uUID);

  /**
   * Get a reference node for this model.
   *
   * @return A reference node.
   */
  @Nullable
  IProcessModelRef<? extends T> getRef();

  /**
   * Get the process node with the given id.
   * @param nodeId The node id to look up.
   * @return The process node with the id.
   */
  T getNode(Identifiable nodeId);

  Collection<? extends T> getModelNodes();

  String getName();

  @Nullable
  Principal getOwner();

  Set<String> getRoles();

  Collection<? extends IXmlResultType> getImports();

  Collection<? extends IXmlDefineType> getExports();
}