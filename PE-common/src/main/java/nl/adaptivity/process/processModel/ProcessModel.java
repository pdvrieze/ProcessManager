/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel;

import net.devrieze.util.security.SecureObject;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlDeserializer;
import org.jetbrains.annotations.Nullable;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;


@XmlDeserializer(ProcessModelImpl.Factory.class)
public interface ProcessModel<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends SecureObject<M> {

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
  IProcessModelRef<T, M> getRef();

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