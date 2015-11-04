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
    public ProcessModelImpl unmarshal(final XmlProcessModel pV) throws Exception {
      return pV.toProcessModel();
    }

    @Override
    public XmlProcessModel marshal(final ProcessModelImpl pV) throws Exception {
      return new XmlProcessModel(pV);
    }

  }

  public enum Permissions implements SecurityProvider.Permission {
    INSTANTIATE;
  }

  public static class Factory implements XmlDeserializerFactory {

    @Override
    public ProcessModelImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return ProcessModelImpl.deserialize(in);
    }
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
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
  public boolean deserializeChild(final XMLStreamReader in) throws XMLStreamException {
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
  public boolean deserializeChildText(final String pElementText) {
    return false; // No text expected except whitespace
  }

  @Override
  public boolean deserializeAttribute(final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    switch (pAttributeLocalName) {
      case "name" : setName(pAttributeValue); break;
      case "owner": setOwner(new SimplePrincipal(pAttributeValue)); break;
      case XmlProcessModel.ATTR_ROLES: aRoles.addAll(Arrays.asList(pAttributeValue.split(" *, *"))); break;
      case "uuid": setUuid(UUID.fromString(pAttributeValue)); break;
      default:
        return false;
    }
    return true;
  }

  @Override
  public QName getElementName() {
    return XmlProcessModel.ELEMENTNAME;
  }

  public static ProcessModelImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
    ProcessModelImpl result = new ProcessModelImpl(Collections.<ProcessNodeImpl>emptyList());
    return XmlUtil.deserializeHelper(result, in);
  }

  private static final long serialVersionUID = -4199223546188994559L;

  private ProcessNodeSet<ProcessNodeImpl> aProcessNodes;

  private int aEndNodeCount;

  private String aName;

  private long aHandle;

  private Principal aOwner;

  private Set<String> aRoles;

  private UUID aUuid;

  private List<XmlResultType> aImports = new ArrayList<>();

  private List<XmlDefineType> aExports = new ArrayList<>();

  /**
   * A class handle purely used for caching and special casing the DarwinPrincipal class.
   */
  private static Class<?> _cls_darwin_principal;

  static {
    try {
      _cls_darwin_principal = ClassLoader.getSystemClassLoader().loadClass("uk.ac.bournemouth.darwin.catalina.realm.DarwinPrincipal");
    } catch (ClassNotFoundException e) {
      _cls_darwin_principal = null;
    }
  }

  /**
   * Create a new processModel based on the given nodes. These nodes should be complete
   *
   */
  public ProcessModelImpl(final Collection<? extends ProcessNodeImpl> pProcessNodes) {
    aProcessNodes=ProcessNodeSet.processNodeSet(pProcessNodes);
    int endNodeCount=0;
    for(ProcessNodeImpl node:aProcessNodes) {
      node.setOwnerModel(this);
      if (node instanceof EndNodeImpl) { ++endNodeCount; }
    }
    aEndNodeCount = endNodeCount;
  }

  /**
   * Create a processModel out of the given xml representation.
   *
   * @param pXmlModel The xml representation to base the model on.
   */
  public ProcessModelImpl(final XmlProcessModel pXmlModel) {
    this(pXmlModel.getNodes());

    setName(pXmlModel.getName());
    final String owner = pXmlModel.getOwner();
    aOwner = owner == null ? null : new SimplePrincipal(pXmlModel.getOwner());
    aUuid = pXmlModel.getUuid()==null ? null : pXmlModel.getUuid();
  }

  public void ensureNode(final ProcessNodeImpl pProcessNode) {
    if (aProcessNodes.add(pProcessNode)) {
      pProcessNode.setOwnerModel(this);
    }
  }

  public void removeNode(final ProcessNodeImpl pProcessNode) {
    throw new UnsupportedOperationException("This will break in all kinds of ways");
  }

  @Override
  @XmlAttribute(name="uuid")
  public UUID getUuid() {
    return aUuid;
  }

  public void setUuid(UUID pUuid) {
    aUuid = pUuid;
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
   * @param pProcessNodes The process nodes to base the model on.
   */
  public void setModelNodes(final Collection<? extends ProcessNodeImpl> pProcessNodes) {
    aProcessNodes = ProcessNodeSet.processNodeSet(pProcessNodes);
    int endNodeCount = 0;
    for (final ProcessNodeImpl n : pProcessNodes) {
      if (n instanceof EndNodeImpl) {
        ++endNodeCount;
      }
    }
    aEndNodeCount = endNodeCount;
  }

  /**
   * Helper method that helps enumerating all elements in the model
   *
   * @param pTo The collection that will contain the result.
   * @param pSeen A set of process names that have already been seen (and should
   *          not be added again.
   * @param pNode The node to start extraction from. This will go on to the
   *          successors.
   */
  private static void extractElements(final Collection<? super ProcessNodeImpl> pTo, final HashSet<String> pSeen, final ProcessNodeImpl pNode) {
    if (pSeen.contains(pNode.getId())) {
      return;
    }
    pTo.add(pNode);
    pSeen.add(pNode.getId());
    for (final ProcessNodeImpl node : pNode.getSuccessors()) {
      extractElements(pTo, pSeen, node);
    }
  }

  /**
   * Get the startnodes for this model.
   *
   * @return The start nodes.
   */
  @Override
  public Collection<StartNodeImpl> getStartNodes() {
    return Collections.unmodifiableCollection(CollectionUtil.addInstancesOf(new ArrayList(), aProcessNodes, StartNodeImpl.class));
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
  public void setHandle(final long pHandle) {
    aHandle = pHandle;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessModel#getRef()
   */
  @Override
  public IProcessModelRef<ProcessNodeImpl> getRef() {
    return new ProcessModelRef(getName(), aHandle, getUuid());
  }

  @Override
  public Principal getOwner() {
    return aOwner;
  }

  /**
   * @param pOwner
   * @return
   * @todo add security checks.
   */
  public void setOwner(final Principal pOwner) {
    aOwner = pOwner;
  }

  @Override
  public Set<String> getRoles() {
    if (aRoles == null) {
      aRoles = new HashSet<>();
    }
    return aRoles;
  }

  @Override
  public Collection<? extends IXmlResultType> getImports() {
    return aImports;
  }

  @Override
  public Collection<? extends IXmlDefineType> getExports() {
    return aExports;
  }

  public void cacheStrings(final StringCache pStringCache) {
    if (aOwner instanceof SimplePrincipal) {
      aOwner = new SimplePrincipal(pStringCache.lookup(aOwner.getName()));
    } else if (_cls_darwin_principal!=null) {
      if (_cls_darwin_principal.isInstance(aOwner)) {
        try {
          Method cacheStrings = _cls_darwin_principal.getMethod("cacheStrings", StringCache.class);
          if (cacheStrings!=null) {
            aOwner = (Principal) cacheStrings.invoke(aOwner, pStringCache);
          }
        } catch (Exception e) {
          // Ignore
        }
      }
    }
    aName = pStringCache.lookup(aName);
    if ((aRoles != null) && (aRoles.size() > 0)) {
      final Set<String> roles = aRoles;
      aRoles = new HashSet<>(aRoles.size() + (aRoles.size() >> 1));
      for (final String role : roles) {
        aRoles.add(pStringCache.lookup(role));
      }
    }
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.ProcessModel#getNode(java.lang.String)
   */
  @Override
  public ProcessNodeImpl getNode(Identifiable pNodeId) {
    if (pNodeId instanceof ProcessModelImpl) { return (ProcessNodeImpl) pNodeId; }
    return aProcessNodes.get(pNodeId);
  }

  /**
   * Faster method that doesn't require an {@link nl.adaptivity.process.util.Identifier intermediate}
   * @param pNodeId
   * @return
   */
  public ProcessNodeImpl getNode(String pNodeId) {
    return aProcessNodes.get(pNodeId);
  }

  public List<ProcessData> toInputs(Node pPayload) {
    // TODO make this work properly
    Collection<? extends IXmlResultType> imports = getImports();
    ArrayList<ProcessData> result = new ArrayList<>(imports.size());
    for(IXmlResultType import_:imports) {
      result.add(XmlResultType.get(import_).apply(pPayload));
    }
    return result;
  }

  public List<ProcessData> toOutputs(Node pPayload) {
    // TODO make this work properly
    Collection<? extends IXmlDefineType> exports = getExports();
    ArrayList<ProcessData> result = new ArrayList<>(exports.size());
    for(IXmlDefineType export:exports) {
//      result.add(XmlDefineType.get(export).apply(pPayload));
    }
    return result;
  }

}
