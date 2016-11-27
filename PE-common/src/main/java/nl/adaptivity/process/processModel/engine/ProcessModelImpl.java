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
import net.devrieze.util.MutableHandleAware;
import net.devrieze.util.StringCache;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.processModel.*;
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
public class ProcessModelImpl extends ProcessModelBase<XmlProcessNode, ProcessModelImpl> implements MutableHandleAware<ProcessModelImpl>, SecureObject<ProcessModelImpl> {

  public static class Factory implements XmlDeserializerFactory<ProcessModelImpl>, DeserializationFactory<XmlProcessNode,ProcessModelImpl> {

    @NotNull
    @Override
    public ProcessModelImpl deserialize(@NotNull final XmlReader reader) throws XmlException {
      return ProcessModelImpl.deserialize(reader);
    }

    @Override
    public XmlEndNode deserializeEndNode(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return XmlEndNode.deserialize(ownerModel, in);
    }

    @Override
    public XmlActivity deserializeActivity(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return XmlActivity.deserialize(ownerModel, in);
    }

    @Override
    public XmlStartNode deserializeStartNode(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return XmlStartNode.deserialize(ownerModel, in);
    }

    @Override
    public XmlJoin deserializeJoin(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return XmlJoin.deserialize(ownerModel, in);
    }

    @Override
    public XmlSplit deserializeSplit(final ProcessModelImpl ownerModel, final XmlReader in) throws
            XmlException {
      return XmlSplit.deserialize(ownerModel, in);
    }
  }

  private volatile int mEndNodeCount = -1;

  public ProcessModelImpl(final ProcessModelBase<?, ?> basepm, final Collection<? extends XmlProcessNode> modelNodes) {
    super(basepm, modelNodes);
  }



  public static ProcessModelImpl from(final ProcessModelBase<?, ? extends ProcessNode<?,?>> basepm) {
    return new ProcessModelImpl(basepm, new ArrayList(basepm.getModelNodes()));
  }

  @NotNull
  public static ProcessModelImpl deserialize(@NotNull final XmlReader in) throws XmlException {
    return deserialize(new Factory(), in);
  }

    @NotNull
    @Deprecated
  public static ProcessModelImpl deserialize(@NotNull Factory factory, @NotNull final XmlReader in) throws XmlException {
    return ProcessModelBase.deserialize(factory, new ProcessModelImpl(Collections.<XmlProcessNode>emptyList()), in);
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
  public ProcessModelImpl(final Collection<? extends XmlProcessNode> processNodes) {
    super(new ArrayList<XmlProcessNode>(processNodes));
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
