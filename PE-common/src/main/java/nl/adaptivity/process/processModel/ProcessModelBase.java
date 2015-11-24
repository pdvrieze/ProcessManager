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

  public interface SplitFactory<U extends ProcessNode<? extends U>> {

    /**
     * Create a new join node. This must register the node with the owner, and mark the join as successor to
     * the predecessors. If appropriate, this should also generate an id for the node, and must verify that it
     * is not duplcated in the model.
     * @param ownerModel The owner
     * @param successors The predecessors
     * @return The resulting join node.
     */
    Split<? extends U> createSplit(ProcessModelBase<U> ownerModel, Collection<? extends Identifiable> successors);
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
        case EndNode.ELEMENTLOCALNAME:
          factory.deserializeEndNode(this, in); return true;
        case Activity.ELEMENTLOCALNAME:
          factory.deserializeActivity(this, in); return true;
        case StartNode.ELEMENTLOCALNAME:
          factory.deserializeStartNode(this, in); return true;
        case Join.ELEMENTLOCALNAME:
          factory.deserializeJoin(this, in); return true;
        case Split.ELEMENTLOCALNAME:
          factory.deserializeSplit(this, in); return true;
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
    XmlUtil.writeEndElement(out, XmlProcessModel.ELEMENTNAME);
  }

  public T removeNode(final int nodePos) {
    return mProcessNodes.remove(nodePos);
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

  public T getNode(final int pos) {
    return mProcessNodes.get(pos);
  }

  /**
   * Initiate the notification that a node has changed. Actual implementations can override this.
   * @param node The node that has changed.
   */
  public void notifyNodeChanged(T node) {
    // no implementation here
  }

  /**
   * Normalize the process model. By default this may do nothing.
   * @return The model (this).
   */
  public ProcessModelBase<T> normalize(SplitFactory<T> splitFactory) {
    // Make all nodes directly refer to other nodes.
    for(T childNode: mProcessNodes) {
      childNode.resolveRefs();
    }
    for(T childNode: mProcessNodes) {
      // Create a copy as we are actually going to remove all successors, but need to keep the list
      ArrayList<Identifiable> successors = new ArrayList<>(childNode.getSuccessors());
      if (successors.size()>1 && ! (childNode instanceof Split)) {
        for(Identifiable suc2: successors) { // Remove the current node as predecessor.
          ProcessNode<?> suc = (ProcessNode) suc2;
          suc.removePredecessor(childNode);
          childNode.removeSuccessor(suc); // remove the predecessor from the current node
        }
        // create a new join, this should
        Split<? extends T> newSplit = splitFactory.createSplit(this, successors);
        childNode.addSuccessor(newSplit);
      }
    }
    return this;
  }
}
