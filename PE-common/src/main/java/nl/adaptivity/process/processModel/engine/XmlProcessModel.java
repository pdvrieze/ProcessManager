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

import kotlin.jvm.functions.Function2;
import net.devrieze.util.CollectionUtil;
import net.devrieze.util.MutableHandleAware;
import net.devrieze.util.StringCache;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.EndNode.Builder;
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
@XmlDeserializer(XmlProcessModel.Factory.class)

@SuppressWarnings("unused")
public class XmlProcessModel extends ProcessModelBase<XmlProcessNode, XmlProcessModel> implements MutableHandleAware<XmlProcessModel>, SecureObject<XmlProcessModel> {

  public static class Builder extends ProcessModelBase.Builder<XmlProcessNode, XmlProcessModel> {

    public Builder(@NotNull final Set<ProcessNode.Builder<XmlProcessNode, XmlProcessModel>> nodes, @Nullable final String name, final long handle, @NotNull final Principal owner, @NotNull final List<String> roles, @Nullable final UUID uuid, @NotNull final List<IXmlResultType> imports, @NotNull final List<IXmlDefineType> exports) {
      super(nodes, name, handle, owner, roles, uuid, imports, exports);
    }

    public Builder() {
    }

    public Builder(@NotNull final ProcessModelBase<XmlProcessNode, XmlProcessModel> base) {
      super(base);
    }

    @NotNull
    @Override
    public XmlProcessModel build() {
      return new XmlProcessModel(this);
    }

    @NotNull
    @Override
    protected XmlStartNode.Builder startNodeBuilder() { return new XmlStartNode.Builder(); }

    @NotNull
    @Override
    protected StartNode.Builder<XmlProcessNode, XmlProcessModel> startNodeBuilder(@NotNull final StartNode<?, ?> startNode) { return new XmlStartNode.Builder(startNode); }

    @NotNull
    @Override
    protected XmlSplit.Builder splitBuilder() { return new XmlSplit.Builder(); }

    @NotNull
    @Override
    protected Split.Builder<XmlProcessNode, XmlProcessModel> splitBuilder(@NotNull final Split<?, ?> split) { return new XmlSplit.Builder(split); }

    @NotNull
    @Override
    protected XmlJoin.Builder joinBuilder() { return new XmlJoin.Builder(); }

    @NotNull
    @Override
    protected XmlJoin.Builder joinBuilder(@NotNull final Join<?, ?> join) {
      return new XmlJoin.Builder(join);
    }

    @NotNull
    @Override
    protected XmlActivity.Builder activityBuilder() { return new XmlActivity.Builder(); }

    @NotNull
    @Override
    protected XmlActivity.Builder activityBuilder(@NotNull final Activity<?, ?> activity) {
      return new XmlActivity.Builder(activity);
    }

    @NotNull
    @Override
    protected XmlEndNode.Builder endNodeBuilder() { return new XmlEndNode.Builder(); }

    @NotNull
    @Override
    protected XmlEndNode.Builder endNodeBuilder(@NotNull final EndNode<?, ?> endNode) {
      return new XmlEndNode.Builder(endNode);
    }

    public static Builder deserialize(final XmlReader reader) throws XmlException {
      return ProcessModelBase.Builder.deserialize(new Builder(), reader);
    }
  }

  public static class Factory implements XmlDeserializerFactory<XmlProcessModel> {

    @NotNull
    @Override
    public XmlProcessModel deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlProcessModel.deserialize(reader);
    }
  }

  private volatile int mEndNodeCount = -1;

  /**
   * Create a new processModel based on the given nodes. These nodes should be complete
   *
   */
  public XmlProcessModel(final Collection<? extends XmlProcessNode> processNodes) {
    super(new ArrayList<>(processNodes), null, -1L, SecurityProvider.SYSTEMPRINCIPAL, Collections.<String>emptyList(), null, Collections.<IXmlResultType>emptyList(), Collections.<IXmlDefineType>emptyList(), XML_NODE_FACTORY);
  }

  private static final Function2<XmlProcessModel, ProcessNode<?, ?>, XmlProcessNode> XML_NODE_FACTORY = new Function2<XmlProcessModel, ProcessNode<?, ?>, XmlProcessNode>() {
    @Override
    public XmlProcessNode invoke(final XmlProcessModel newOwner, final ProcessNode<?, ?> processNode) {
      return toXmlNode(newOwner, processNode);
    }
  };

  public XmlProcessModel(final ProcessModelBase<?, ?> basepm) {
    super(basepm, XML_NODE_FACTORY);
  }

  public XmlProcessModel(@NotNull final ProcessModelBase.Builder<XmlProcessNode, XmlProcessModel> builder) {
    this(builder, false);
  }

  public XmlProcessModel(@NotNull final ProcessModelBase.Builder<XmlProcessNode, XmlProcessModel> builder, boolean pedantic) {
    super(builder, pedantic);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }


  private static Collection<? extends XmlProcessNode> toXmlNodes(final Collection<? extends ProcessNode<?,?>> modelNodes) {
    List<XmlProcessNode> result = new ArrayList<>(modelNodes.size());
    final XmlProcessModel newOwner = null;

    for(ProcessNode<?, ?> node: modelNodes) {
      result.add(toXmlNode(newOwner, node));
    }

    return result;
  }

  private static XmlProcessNode toXmlNode(final XmlProcessModel newOwner, final ProcessNode<?, ?> node) {
    return node.visit(new Visitor<XmlProcessNode>() {
      @Override
      public XmlStartNode visitStartNode(final StartNode<?, ?> startNode) {
        return new XmlStartNode(startNode, newOwner);
      }

      @Override
      public XmlActivity visitActivity(final Activity<?, ?> activity) {
        return new XmlActivity(activity, newOwner);
      }

      @Override
      public XmlSplit visitSplit(final Split<?, ?> split) {
        return new XmlSplit(split, newOwner);
      }

      @Override
      public XmlJoin visitJoin(final Join<?, ?> join) {
        return new XmlJoin(join, newOwner);
      }

      @Override
      public XmlProcessNode visitEndNode(final EndNode<?, ?> endNode) {
        return new XmlEndNode(endNode, newOwner);
      }
    });
  }

  /**
   * Normalize the process model. By default this may do nothing.
   * @return The model (this).
   */
  public XmlProcessModel normalized(boolean pedantic) {
    final Builder builder = builder();
    builder.normalize(pedantic);
    return builder.build();
  }

  @NotNull
  public static XmlProcessModel deserialize(@NotNull final XmlReader reader) throws XmlException {
    return Builder.deserialize(reader).build().asM();
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
   * Ensure that the given node is owned by this model.
   * @param processNode
   */
  public boolean addNode(@NotNull final XmlProcessNode processNode) {
    if (super.addNode(processNode)) {
      processNode.setOwnerModel(this.asM());
      return true;
    }
    return false;
  }

  public boolean removeNode(final XmlProcessNode processNode) {
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
  private static void extractElements(final Collection<? super XmlProcessNode> to, final HashSet<String> seen, final XmlProcessNode node) {
    if (seen.contains(node.getId())) {
      return;
    }
    to.add(node);
    seen.add(node.getId());
    for (final Identifiable successor : node.getSuccessors()) {
      extractElements(to, seen, (XmlProcessNode) successor);
    }
  }

  /**
   * Get the startnodes for this model.
   *
   * @return The start nodes.
   */
  public Collection<XmlStartNode> getStartNodes() {
    return Collections.unmodifiableCollection(CollectionUtil.addInstancesOf(new ArrayList<XmlStartNode>(), getModelNodes(), XmlStartNode.class));
  }

  @Override
  public void setModelNodes(@NotNull final Collection<? extends XmlProcessNode> processNodes) {
    super.setModelNodes(processNodes);
    int endNodeCount = 0;
    for (final XmlProcessNode n : processNodes) {
      if (n instanceof XmlEndNode) {
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
      for (final XmlProcessNode node : getModelNodes()) {
        node.setOwnerModel(this.asM());
        if (node instanceof XmlEndNode) { ++endNodeCount; }
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
  public XmlProcessNode getNode(final String nodeId) {
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
