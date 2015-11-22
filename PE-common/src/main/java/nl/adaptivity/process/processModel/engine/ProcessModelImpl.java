package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.CollectionUtil;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.StringCache;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl.PMXmlAdapter;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;


/**
 * A class representing a process model. This is too complex to directly support
 * JAXB serialization, so the {@link ProcessModelXmlAdapter} does that.
 *
 * @author Paul de Vrieze
 */
@XmlJavaTypeAdapter(PMXmlAdapter.class)
@XmlDeserializer(ProcessModelImpl.Factory.class)

@SuppressWarnings("unused")
public class ProcessModelImpl extends ProcessModelBase<ProcessNodeImpl> implements HandleAware<ProcessModelImpl>, SecureObject {

  static class PMXmlAdapter extends XmlAdapter<XmlProcessModel, ProcessModelImpl> {

    @Override
    public ProcessModelImpl unmarshal(@NotNull final XmlProcessModel v) throws Exception {
      return v.toProcessModel();
    }

    @NotNull
    @Override
    public XmlProcessModel marshal(final ProcessModelImpl v) throws Exception {
      return new XmlProcessModel(v);
    }

  }

  public enum Permissions implements SecurityProvider.Permission {
    INSTANTIATE
  }

  public static class Factory implements XmlDeserializerFactory, DeserializationFactory<ProcessNodeImpl> {

    @NotNull
    @Override
    public ProcessModelImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return ProcessModelImpl.deserialize(in);
    }

    @Override
    public EndNodeImpl deserializeEndNode(final ProcessModelBase<ProcessNodeImpl> ownerModel, final XmlReader in) throws
            XmlException {
      return EndNodeImpl.deserialize(ownerModel, in);
    }

    @Override
    public ActivityImpl deserializeActivity(final ProcessModelBase<ProcessNodeImpl> ownerModel, final XmlReader in) throws
            XmlException {
      return ActivityImpl.deserialize(ownerModel, in);
    }

    @Override
    public StartNodeImpl deserializeStartNode(final ProcessModelBase<ProcessNodeImpl> ownerModel, final XmlReader in) throws
            XmlException {
      return StartNodeImpl.deserialize(ownerModel, in);
    }

    @Override
    public JoinImpl deserializeJoin(final ProcessModelBase<ProcessNodeImpl> ownerModel, final XmlReader in) throws
            XmlException {
      return JoinImpl.deserialize(ownerModel, in);
    }

    @Override
    public SplitImpl deserializeSplit(final ProcessModelBase<ProcessNodeImpl> ownerModel, final XmlReader in) throws
            XmlException {
      return SplitImpl.deserialize(ownerModel, in);
    }
  }

  private int mEndNodeCount;

  @NotNull
  public static ProcessModelImpl deserialize(@NotNull final XmlReader in) throws XmlException {
    return deserialize(new Factory(), in);
  }

    @NotNull
  public static ProcessModelImpl deserialize(@NotNull Factory factory, @NotNull final XmlReader in) throws XmlException {
    return (ProcessModelImpl) ProcessModelBase.deserialize(factory, new ProcessModelImpl(Collections.<ProcessNodeImpl>emptyList()), in);
  }

  private static final long serialVersionUID = -4199223546188994559L;

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
  public ProcessModelImpl(final Collection<? extends ProcessNodeImpl> processNodes) {
    super(processNodes);
    int endNodeCount=0;
    for(final ProcessNodeImpl node:getModelNodes()) {
      node.setOwnerModel(this);
      if (node instanceof EndNodeImpl) { ++endNodeCount; }
    }
    mEndNodeCount = endNodeCount;
  }

  /**
   * Create a processModel out of the given xml representation.
   *
   * @param xmlModel The xml representation to base the model on.
   */
  public ProcessModelImpl(@NotNull final XmlProcessModel xmlModel) {
    this(xmlModel.getNodes());

    setName(xmlModel.getName());
    final String owner = xmlModel.getOwner();
    setOwner(owner == null ? null : new SimplePrincipal(xmlModel.getOwner()));
    setUuid(xmlModel.getUuid()==null ? null : xmlModel.getUuid());
  }

  /**
   * Ensure that the given node is owned by this model.
   * @param processNode
   */
  public boolean addNode(@NotNull final ProcessNodeImpl processNode) {
    if (super.addNode(processNode)) {
      processNode.setOwnerModel(this);
      return true;
    }
    return false;
  }

  public boolean removeNode(final ProcessNodeImpl processNode) {
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
  private static void extractElements(final Collection<? super ProcessNodeImpl> to, final HashSet<String> seen, final ProcessNodeImpl node) {
    if (seen.contains(node.getId())) {
      return;
    }
    to.add(node);
    seen.add(node.getId());
    for (final Identifiable successor : node.getSuccessors()) {
      extractElements(to, seen, (ProcessNodeImpl) successor);
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
  public void setModelNodes(@NotNull final Collection<? extends ProcessNodeImpl> processNodes) {
    super.setModelNodes(processNodes);
    int endNodeCount = 0;
    for (final ProcessNodeImpl n : processNodes) {
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
  public ProcessNodeImpl getNode(final String nodeId) {
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
