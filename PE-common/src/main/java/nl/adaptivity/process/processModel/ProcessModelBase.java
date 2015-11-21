package nl.adaptivity.process.processModel;

import net.devrieze.util.StringUtil;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.processModel.engine.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlStreaming.EventType;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;

import java.security.Principal;
import java.util.*;


/**
 * Created by pdvrieze on 21/11/15.
 */
public class ProcessModelBase<T extends ProcessNode<? extends T>> implements ProcessModel<T>, XmlSerializable {

  protected interface DeserializationFactory<U extends ProcessNode<? extends U>> {

    EndNode<? extends U> deserializeEndNode(ProcessModelBase<U> ownerModel, XmlReader in) throws XmlException;

    Activity<? extends U> deserializeActivity(ProcessModelBase<U> ownerModel, XmlReader in) throws XmlException;

    StartNode<? extends U> deserializeStartNode(ProcessModelBase<U> ownerModel, XmlReader in) throws XmlException;

    Join<? extends U> deserializeJoin(ProcessModelBase<U> ownerModel, XmlReader in) throws XmlException;

    Split<? extends U> deserializeSplit(ProcessModelBase<U> ownerModel, XmlReader in) throws XmlException;
  }

  private ProcessNodeSet<T> mProcessNodes;
  private String mName;
  private long mHandle;
  @Nullable private Principal mOwner;
  private Set<String> mRoles;
  @Nullable private UUID mUuid;
  @NotNull private List<XmlResultType> mImports = new ArrayList<>();
  @NotNull private List<XmlDefineType> mExports = new ArrayList<>();

  public ProcessModelBase(final Collection<? extends T> processNodes) {
    mProcessNodes=ProcessNodeSet.processNodeSet(processNodes);
  }

  public boolean deserializeChild(final DeserializationFactory<T> factory, @NotNull final XmlReader in) throws XmlException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case EndNodeImpl.ELEMENTLOCALNAME:
          factory.deserializeEndNode(this, in); break;
        case ActivityImpl.ELEMENTLOCALNAME:
          factory.deserializeActivity(this, in); break;
        case StartNodeImpl.ELEMENTLOCALNAME:
          factory.deserializeStartNode(this, in); break;
        case JoinImpl.ELEMENTLOCALNAME:
          factory.deserializeJoin(this, in); break;
        case SplitImpl.ELEMENTLOCALNAME:
          factory.deserializeSplit(this, in); break;
      }
    }
    return false;
  }

  public boolean deserializeAttribute(final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, @NotNull final CharSequence attributeValue) {
    final String value = StringUtil.toString(attributeValue);
    switch (StringUtil.toString(attributeLocalName)) {
      case "name" : setName(value); break;
      case "owner": setOwner(new SimplePrincipal(value)); break;
      case XmlProcessModel.ATTR_ROLES: mRoles.addAll(Arrays.asList(value.split(" *, *"))); break;
      case "uuid": setUuid(UUID.fromString(value)); break;
      default:
        return false;
    }
    return true;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, XmlProcessModel.ELEMENTNAME);
    XmlUtil.writeAttribute(out, "name", getName());
    XmlUtil.writeAttribute(out, "owner", mOwner==null ? null : mOwner.getName());
    if (mRoles!=null && mRoles.size()>0) {
      XmlUtil.writeAttribute(out, XmlProcessModel.ATTR_ROLES,StringUtil.join(",", mRoles));
    }
    if (getUuid() !=null) {
      XmlUtil.writeAttribute(out, "uuid", getUuid().toString());
    }
    XmlUtil.writeChildren(out, getImports());
    XmlUtil.writeChildren(out, getExports());
    XmlUtil.writeChildren(out, mProcessNodes);
  }

  public static <T extends ProcessNode<T>> ProcessModelBase<T> deserialize(final DeserializationFactory<T> factory, final ProcessModelBase<T> processModel, final XmlReader in) throws
          XmlException {

    XmlUtil.skipPreamble(in);
    final QName elementName = XmlProcessModel.ELEMENTNAME;
    assert XmlUtil.isElement(in, elementName): "Expected "+elementName+" but found "+ in.getLocalName();
    for(int i = in.getAttributeCount()-1; i>=0; --i) {
      processModel.deserializeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i), in.getAttributeValue(i));
    }

    EventType event = null;
    loop: while (in.hasNext() && event != XmlStreaming.END_ELEMENT) {
      switch ((event = in.next())) {
        case START_ELEMENT:
          if (processModel.deserializeChild(factory, in)) {
            continue loop;
          }
          XmlUtil.unhandledEvent(in);
          break;
        case TEXT:
        case CDSECT:
          if (false) {
            continue loop;
          }
        default:
          XmlUtil.unhandledEvent(in);
      }
    }

    for(final T node: processModel.mProcessNodes) {
      for(final Identifiable pred: node.getPredecessors()) {
        final T predNode = processModel.getNode(pred);
        predNode.addSuccessor(node);
      }
    }
    return processModel;
  }

  @Nullable
  @Override
  @XmlAttribute(name="uuid")
  public UUID getUuid() {
    return mUuid;
  }

  public void setUuid(final UUID uuid) {
    mUuid = uuid;
  }

  /**
   * Get the name of the model.
   *
   * @return
   */
  @Override
  public String getName() {
    return mName;
  }

  /**
   * Set the name of the model.
   *
   * @param name The name
   */
  public void setName(final String name) {
    mName = name;
  }

  /**
   * Get the handle recorded for this model.
   */
  public long getHandle() {
    return mHandle;
  }

  /**
   * Set the handle for this model.
   */
  public void setHandle(final long handle) {
    mHandle = handle;
  }

  @Nullable
  @Override
  public Principal getOwner() {
    return mOwner;
  }

  /**
   * @param owner
   * @return
   * @todo add security checks.
   */
  public void setOwner(final Principal owner) {
    mOwner = owner;
  }

  @NotNull
  @Override
  public Collection<? extends IXmlResultType> getImports() {
    return mImports;
  }

  @NotNull
  @Override
  public Collection<? extends IXmlDefineType> getExports() {
    return mExports;
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getRef()
     */
  @Nullable
  @Override
  public IProcessModelRef<? extends T> getRef() {
    return new ProcessModelRef(getName(), getHandle(), getUuid());
  }

  @Override
  public Set<String> getRoles() {
    if (mRoles == null) {
      mRoles = new HashSet<>();
    }
    return mRoles;
  }

  public void setRoles(Collection<String> roles) {
    mRoles = new HashSet<>(roles);
  }

  /**
   * Get an array of all process nodes in the model. Used by XmlProcessModel
   *
   * @return An array of all nodes.
   */
  @Override
  public Collection<? extends T> getModelNodes() {
    return Collections.unmodifiableCollection(mProcessNodes);
  }

  /**
   * Set the process nodes for the model. This will actually just retrieve the
   * {@link EndNodeImpl}s and sets the model accordingly. This does mean that only
   * passing {@link EndNodeImpl}s will have the same result, and the other nodes
   * will be pulled in.
   *
   * @param processNodes The process nodes to base the model on.
   */
  public void setModelNodes(@NotNull final Collection<? extends T> processNodes) {
    mProcessNodes = ProcessNodeSet.processNodeSet(processNodes);
  }

  public boolean addNode(T node) {
    return mProcessNodes.add(node);
  }

  public boolean removeNode(T node) {
    return mProcessNodes.remove(node);
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getNode(java.lang.String)
     */
  @Override
  public T getNode(final Identifiable nodeId) {
    if (nodeId instanceof ProcessNode) { return (T) nodeId; }
    return mProcessNodes.get(nodeId);
  }
}
