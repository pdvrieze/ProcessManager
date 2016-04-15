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

package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.CollectionUtil;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.StringCache;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.ProcessNode.Visitor;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.xml.XmlDeserializer;
import nl.adaptivity.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;


/**
 * A class representing a process model.
 *
 * @author Paul de Vrieze
 */
@XmlDeserializer(ProcessModelImpl.Factory.class)

@SuppressWarnings("unused")
public class ProcessModelImpl extends ProcessModelBase<ExecutableProcessNode, ProcessModelImpl> implements HandleAware<ProcessModelImpl>, SecureObject {

  public enum Permissions implements SecurityProvider.Permission {
    INSTANTIATE
  }

  public static class Factory implements XmlDeserializerFactory, DeserializationFactory<ExecutableProcessNode, ProcessModelImpl> {

    @NotNull
    @Override
    public ProcessModelImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return ProcessModelImpl.deserialize(in);
    }

    @Override
    public EndNodeImpl deserializeEndNode(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return EndNodeImpl.deserialize(ownerModel, in);
    }

    @Override
    public ActivityImpl deserializeActivity(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return ActivityImpl.deserialize(ownerModel, in);
    }

    @Override
    public StartNodeImpl deserializeStartNode(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return StartNodeImpl.deserialize(ownerModel, in);
    }

    @Override
    public JoinImpl deserializeJoin(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return JoinImpl.deserialize(ownerModel, in);
    }

    @Override
    public SplitImpl deserializeSplit(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return SplitImpl.deserialize(ownerModel, in);
    }
  }

  private volatile int mEndNodeCount = -1;

  public ProcessModelImpl(final ProcessModelBase<?, ?> basepm) {
    super(basepm, toExecutableNodes(basepm.getModelNodes()));
  }

  private static Collection<? extends ExecutableProcessNode> toExecutableNodes(final Collection<? extends ProcessNode<?,?>> modelNodes) {
    List<ExecutableProcessNode> result = new ArrayList<>();
    for(ProcessNode<?,?> node: modelNodes) {
      result.add(node.visit(new Visitor<ExecutableProcessNode>() {
        @Override
        public ExecutableProcessNode visitStartNode(final StartNode<?, ?> startNode) {
          return new StartNodeImpl(startNode);
        }

        @Override
        public ExecutableProcessNode visitActivity(final Activity<?, ?> activity) {
          return new ActivityImpl(activity);
        }

        @Override
        public ExecutableProcessNode visitSplit(final Split<?, ?> split) {
          return new SplitImpl(split);
        }

        @Override
        public ExecutableProcessNode visitJoin(final Join<?, ?> join) {
          return new JoinImpl(join);
        }

        @Override
        public ExecutableProcessNode visitEndNode(final EndNode<?, ?> endNode) {
          return new EndNodeImpl(endNode);
        }
      }));
    }
    return result;
  }

  public static ProcessModelImpl from(final ProcessModelBase<?, ?> basepm) {
    return new ProcessModelImpl(basepm);
  }

  @NotNull
  public static ProcessModelImpl deserialize(@NotNull final XmlReader in) throws XmlException {
    return deserialize(new Factory(), in);
  }

    @NotNull
  public static ProcessModelImpl deserialize(@NotNull Factory factory, @NotNull final XmlReader in) throws XmlException {
    return ProcessModelBase.deserialize(factory, new ProcessModelImpl(Collections.<ExecutableProcessNode>emptyList()), in);
  }

  /**
   * A class handle purely used for caching and special casing the DarwinPrincipal class.
   */
  @Nullable private static Class<?> _cls_darwin_principal;

  static {
    try {
      _cls_darwin_principal = ClassLoader.getSystemClassLoader().loadClass("uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal");
    } catch (@NotNull final ClassNotFoundException e) {
      _cls_darwin_principal = null;
    }
  }

  /**
   * Create a new processModel based on the given nodes. These nodes should be complete
   *
   */
  public ProcessModelImpl(final Collection<? extends ExecutableProcessNode> processNodes) {
    super(processNodes);
  }

  /**
   * Ensure that the given node is owned by this model.
   * @param processNode
   */
  public boolean addNode(@NotNull final ExecutableProcessNode processNode) {
    if (super.addNode(processNode)) {
      processNode.setOwnerModel(this);
      return true;
    }
    return false;
  }

  public boolean removeNode(final ExecutableProcessNode processNode) {
    throw new UnsupportedOperationException("This will break in all kinds of ways");
  }

  /**
   * Helper method that helps enumerating all elements in the model
   *
   * @param to The collection that will contain the result.
   * @param seen A set of process names that have already been seen (and should
   *          not be added again.
   * @param node The node to start extraction from. This will go on to the
   *          successors.
   */
  private static void extractElements(final Collection<? super ExecutableProcessNode> to, final HashSet<String> seen, final ExecutableProcessNode node) {
    if (seen.contains(node.getId())) {
      return;
    }
    to.add(node);
    seen.add(node.getId());
    for (final Identifiable successor : node.getSuccessors()) {
      extractElements(to, seen, (ExecutableProcessNode) successor);
    }
  }

  /**
   * Get the startnodes for this model.
   *
   * @return The start nodes.
   */
  public Collection<StartNodeImpl> getStartNodes() {
    return Collections.unmodifiableCollection(CollectionUtil.addInstancesOf(new ArrayList<StartNodeImpl>(), getModelNodes(), StartNodeImpl.class));
  }

  @Override
  public void setModelNodes(@NotNull final Collection<? extends ExecutableProcessNode> processNodes) {
    super.setModelNodes(processNodes);
    int endNodeCount = 0;
    for (final ExecutableProcessNode n : processNodes) {
      if (n instanceof EndNodeImpl) {
        ++endNodeCount;
      }
    }
    mEndNodeCount = endNodeCount;
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getEndNodeCount()
     */
  public int getEndNodeCount() {
    if (mEndNodeCount<0) {
      int endNodeCount = 0;
      for (final ExecutableProcessNode node : getModelNodes()) {
        node.setOwnerModel(this);
        if (node instanceof EndNodeImpl) { ++endNodeCount; }
      }
      mEndNodeCount = endNodeCount;
    }

    return mEndNodeCount;
  }

  public void cacheStrings(@NotNull final StringCache stringCache) {
    if (getOwner() instanceof SimplePrincipal) {
      setOwner(new SimplePrincipal(stringCache.lookup(getOwner().getName())));
    } else if (_cls_darwin_principal!=null) {
      if (_cls_darwin_principal.isInstance(getOwner())) {
        try {
          final Method cacheStrings = _cls_darwin_principal.getMethod("cacheStrings", StringCache.class);
          if (cacheStrings!=null) {
            setOwner((Principal) cacheStrings.invoke(getOwner(), stringCache));
          }
        } catch (@NotNull final Exception e) {
          // Ignore
        }
      }
    }
    setName(stringCache.lookup(getName()));
    Set<String> oldRoles = getRoles();
    if ((oldRoles != null) && (oldRoles.size() > 0)) {
      HashSet<String> newRoles = new HashSet<>(oldRoles.size() + (oldRoles.size() >> 1));
      for (final String role : oldRoles) {
        newRoles.add(stringCache.lookup(role));
      }
      setRoles(newRoles);
    }
  }

  /**
   * Faster method that doesn't require an {@link nl.adaptivity.process.util.Identifier intermediate}
   * @param nodeId
   * @return
   */
  public ExecutableProcessNode getNode(final String nodeId) {
    return getNode(new Identifier(nodeId));
  }

  @NotNull
  public List<ProcessData> toInputs(final Node payload) {
    // TODO make this work properly
    final Collection<? extends IXmlResultType> imports = getImports();
    final ArrayList<ProcessData> result = new ArrayList<>(imports.size());
    for(final IXmlResultType import_:imports) {
      result.add(XmlResultType.get(import_).apply(payload));
    }
    return result;
  }

  @NotNull
  public List<ProcessData> toOutputs(final Node payload) {
    // TODO make this work properly
    final Collection<? extends IXmlDefineType> exports = getExports();
    final ArrayList<ProcessData> result = new ArrayList<>(exports.size());
    for(final IXmlDefineType export:exports) {
//      result.add(XmlDefineType.get(export).apply(pPayload));
    }
    return result;
  }
}
