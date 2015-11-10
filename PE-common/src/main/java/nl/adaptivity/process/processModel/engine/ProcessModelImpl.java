package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.CollectionUtil;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.StringCache;
import net.devrieze.util.StringUtil;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl.PMXmlAdapter;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.Serializable;
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
public class ProcessModelImpl implements HandleAware<ProcessModelImpl>, SimpleXmlDeserializable, Serializable, SecureObject, ProcessModel<ProcessNodeImpl> {

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

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public ProcessModelImpl deserialize(@NotNull final XMLStreamReader in) throws XMLStreamException {
      return ProcessModelImpl.deserialize(in);
    }
  }

  @Override
  public void serialize(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, XmlProcessModel.ELEMENTNAME);
    XmlUtil.writeAttribute(out, "name", getName());
    XmlUtil.writeAttribute(out, "owner", aOwner==null ? null : aOwner.getName());
    if (aRoles!=null && aRoles.size()>0) {
      XmlUtil.writeAttribute(out, XmlProcessModel.ATTR_ROLES,StringUtil.join(",", aRoles));
    }
    if (aUuid!=null) {
      XmlUtil.writeAttribute(out, "uuid", aUuid.toString());
    }
    XmlUtil.writeChildren(out, aImports);
    XmlUtil.writeChildren(out, aExports);
    XmlUtil.writeChildren(out, aProcessNodes);
  }

  @Override
  public boolean deserializeChild(@NotNull final XMLStreamReader in) throws XMLStreamException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceURI())) {
      switch (in.getLocalName()) {
        case EndNodeImpl.ELEMENTLOCALNAME:
          EndNodeImpl.deserialize(this, in); break;
        case ActivityImpl.ELEMENTLOCALNAME:
          ActivityImpl.deserialize(this, in); break;
        case StartNodeImpl.ELEMENTLOCALNAME:
          StartNodeImpl.deserialize(this, in); break;
        case JoinImpl.ELEMENTLOCALNAME:
          JoinImpl.deserialize(this, in); break;
        case SplitImpl.ELEMENTLOCALNAME:
          SplitImpl.deserialize(this, in); break;
      }
    }
    return false;
  }

  @Override
  public boolean deserializeChildText(final String elementText) {
    return false; // No text expected except whitespace
  }

  @Override
  public boolean deserializeAttribute(final String attributeNamespace, @NotNull final String attributeLocalName, @NotNull final String attributeValue) {
    switch (attributeLocalName) {
      case "name" : setName(attributeValue); break;
      case "owner": setOwner(new SimplePrincipal(attributeValue)); break;
      case XmlProcessModel.ATTR_ROLES: aRoles.addAll(Arrays.asList(attributeValue.split(" *, *"))); break;
      case "uuid": setUuid(UUID.fromString(attributeValue)); break;
      default:
        return false;
    }
    return true;
  }

  @Override
  public void onBeforeDeserializeChildren(final XMLStreamReader in) {
    // do nothing
  }

  @Override
  public QName getElementName() {
    return XmlProcessModel.ELEMENTNAME;
  }

  @NotNull
  public static ProcessModelImpl deserialize(@NotNull final XMLStreamReader in) throws XMLStreamException {
    final ProcessModelImpl processModel = XmlUtil.deserializeHelper(new ProcessModelImpl(Collections.<ProcessNodeImpl>emptyList()), in);
    for(final ProcessNodeImpl node:processModel.aProcessNodes) {
      for(final Identifiable pred: node.getPredecessors()) {
        final ProcessNodeImpl predNode = processModel.getNode(pred);
        predNode.addSuccessor(node);
      }
    }
    return processModel;
  }

  private static final long serialVersionUID = -4199223546188994559L;

  private ProcessNodeSet<ProcessNodeImpl> aProcessNodes;

  private int aEndNodeCount;

  private String aName;

  private long aHandle;

  @Nullable private Principal aOwner;

  private Set<String> aRoles;

  @Nullable private UUID aUuid;

  @NotNull private List<XmlResultType> aImports = new ArrayList<>();

  @NotNull private List<XmlDefineType> aExports = new ArrayList<>();

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
    aProcessNodes=ProcessNodeSet.processNodeSet(processNodes);
    int endNodeCount=0;
    for(final ProcessNodeImpl node:aProcessNodes) {
      node.setOwnerModel(this);
      if (node instanceof EndNodeImpl) { ++endNodeCount; }
    }
    aEndNodeCount = endNodeCount;
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
    aOwner = owner == null ? null : new SimplePrincipal(xmlModel.getOwner());
    aUuid = xmlModel.getUuid()==null ? null : xmlModel.getUuid();
  }

  public void ensureNode(@NotNull final ProcessNodeImpl processNode) {
    if (aProcessNodes.add(processNode)) {
      processNode.setOwnerModel(this);
    }
  }

  public void removeNode(final ProcessNodeImpl processNode) {
    throw new UnsupportedOperationException("This will break in all kinds of ways");
  }

  @Nullable
  @Override
  @XmlAttribute(name="uuid")
  public UUID getUuid() {
    return aUuid;
  }

  public void setUuid(final UUID uuid) {
    aUuid = uuid;
  }

  /**
   * Get an array of all process nodes in the model. Used by XmlProcessModel
   *
   * @return An array of all nodes.
   */
  @Override
  public Collection<? extends ProcessNodeImpl> getModelNodes() {
    return Collections.unmodifiableCollection(aProcessNodes);
  }

  /**
   * Set the process nodes for the model. This will actually just retrieve the
   * {@link EndNodeImpl}s and sets the model accordingly. This does mean that only
   * passing {@link EndNodeImpl}s will have the same result, and the other nodes
   * will be pulled in.
   *
   * @param processNodes The process nodes to base the model on.
   */
  public void setModelNodes(@NotNull final Collection<? extends ProcessNodeImpl> processNodes) {
    aProcessNodes = ProcessNodeSet.processNodeSet(processNodes);
    int endNodeCount = 0;
    for (final ProcessNodeImpl n : processNodes) {
      if (n instanceof EndNodeImpl) {
        ++endNodeCount;
      }
    }
    aEndNodeCount = endNodeCount;
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
    for (final ProcessNodeImpl successor : node.getSuccessors()) {
      extractElements(to, seen, successor);
    }
  }

  /**
   * Get the startnodes for this model.
   *
   * @return The start nodes.
   */
  @Override
  public Collection<StartNodeImpl> getStartNodes() {
    return Collections.unmodifiableCollection(CollectionUtil.addInstancesOf(new ArrayList<StartNodeImpl>(), aProcessNodes, StartNodeImpl.class));
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessModel#getEndNodeCount()
   */
  @Override
  public int getEndNodeCount() {
    return aEndNodeCount;
  }

  /**
   * Get the name of the model.
   *
   * @return
   */
  @Override
  public String getName() {
    return aName;
  }

  /**
   * Set the name of the model.
   *
   * @param name The name
   */
  public void setName(final String name) {
    aName = name;
  }

  /**
   * Get the handle recorded for this model.
   */
  @Override
  public long getHandle() {
    return aHandle;
  }

  /**
   * Set the handle for this model.
   */
  @Override
  public void setHandle(final long handle) {
    aHandle = handle;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessModel#getRef()
   */
  @Nullable
  @Override
  public IProcessModelRef<ProcessNodeImpl> getRef() {
    return new ProcessModelRef(getName(), aHandle, getUuid());
  }

  @Nullable
  @Override
  public Principal getOwner() {
    return aOwner;
  }

  /**
   * @param owner
   * @return
   * @todo add security checks.
   */
  public void setOwner(final Principal owner) {
    aOwner = owner;
  }

  @Override
  public Set<String> getRoles() {
    if (aRoles == null) {
      aRoles = new HashSet<>();
    }
    return aRoles;
  }

  @NotNull
  @Override
  public Collection<? extends IXmlResultType> getImports() {
    return aImports;
  }

  @NotNull
  @Override
  public Collection<? extends IXmlDefineType> getExports() {
    return aExports;
  }

  public void cacheStrings(@NotNull final StringCache stringCache) {
    if (aOwner instanceof SimplePrincipal) {
      aOwner = new SimplePrincipal(stringCache.lookup(aOwner.getName()));
    } else if (_cls_darwin_principal!=null) {
      if (_cls_darwin_principal.isInstance(aOwner)) {
        try {
          final Method cacheStrings = _cls_darwin_principal.getMethod("cacheStrings", StringCache.class);
          if (cacheStrings!=null) {
            aOwner = (Principal) cacheStrings.invoke(aOwner, stringCache);
          }
        } catch (@NotNull final Exception e) {
          // Ignore
        }
      }
    }
    aName = stringCache.lookup(aName);
    if ((aRoles != null) && (aRoles.size() > 0)) {
      final Set<String> roles = aRoles;
      aRoles = new HashSet<>(aRoles.size() + (aRoles.size() >> 1));
      for (final String role : roles) {
        aRoles.add(stringCache.lookup(role));
      }
    }
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessModel#getNode(java.lang.String)
   */
  @Override
  public ProcessNodeImpl getNode(final Identifiable nodeId) {
    if (nodeId instanceof ProcessModelImpl) { return (ProcessNodeImpl) nodeId; }
    return aProcessNodes.get(nodeId);
  }

  /**
   * Faster method that doesn't require an {@link nl.adaptivity.process.util.Identifier intermediate}
   * @param nodeId
   * @return
   */
  public ProcessNodeImpl getNode(final String nodeId) {
    return aProcessNodes.get(nodeId);
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
