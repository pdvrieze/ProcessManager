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

import net.devrieze.util.StringUtil;
import net.devrieze.util.security.SimplePrincipal;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.processModel.engine.EndNodeImpl;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelRef;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.xml.XmlSerializable;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlStreaming.EventType;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.security.Principal;
import java.util.*;


/**
 * Created by pdvrieze on 21/11/15.
 */
public class ProcessModelBase<T extends ProcessNode<? extends T, M>, M extends ProcessModelBase<T, M>> implements ProcessModel<T, M>, XmlSerializable {

  protected interface DeserializationFactory<U extends ProcessNode<? extends U, M>, M extends ProcessModelBase<U, M>> {

    EndNode<? extends U, M> deserializeEndNode(M ownerModel, XmlReader in) throws XmlException;

    Activity<? extends U, M> deserializeActivity(M ownerModel, XmlReader in) throws XmlException;

    StartNode<? extends U, M> deserializeStartNode(M ownerModel, XmlReader in) throws XmlException;

    Join<? extends U, M> deserializeJoin(M ownerModel, XmlReader in) throws XmlException;

    Split<? extends U, M> deserializeSplit(M ownerModel, XmlReader in) throws XmlException;
  }

  public interface SplitFactory<U extends ProcessNode<? extends U, M>, M extends ProcessModel<U, M>> {

    /**
     * Create a new join node. This must register the node with the owner, and mark the join as successor to
     * the predecessors. If appropriate, this should also generate an id for the node, and must verify that it
     * is not duplcated in the model.
     * @param ownerModel The owner
     * @param successors The predecessors
     * @return The resulting join node.
     */
    Split<? extends U, M> createSplit(M ownerModel, Collection<? extends Identifiable> successors);
  }

  public static final String ELEMENTLOCALNAME = "processModel";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);
  public static final String ATTR_ROLES = "roles";
  public static final String ATTR_NAME = "name";

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

  /**
   * Copy constructor, but generics mean that the right type of child needs to be provided as parameter
   * @param basepm The base process model
   * @param modelNodes The "converted" model nodes.
   */
  protected ProcessModelBase(final ProcessModelBase<?, ?> basepm, final Collection<? extends T> modelNodes) {
    setModelNodes(modelNodes);
    setName(basepm.getName());
    setHandle(basepm.getHandle());
    setOwner(basepm.getOwner());
    setRoles(basepm.getRoles());
    setUuid(basepm.getUuid());
    setImports(basepm.getImports());
    setExports(basepm.getExports());
  }

  public boolean deserializeChild(final DeserializationFactory<T, M> factory, @NotNull final XmlReader in) throws XmlException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case EndNode.ELEMENTLOCALNAME:
          factory.deserializeEndNode(asM(), in); return true;
        case Activity.ELEMENTLOCALNAME:
          factory.deserializeActivity(asM(), in); return true;
        case StartNode.ELEMENTLOCALNAME:
          factory.deserializeStartNode(asM(), in); return true;
        case Join.ELEMENTLOCALNAME:
          factory.deserializeJoin(asM(), in); return true;
        case Split.ELEMENTLOCALNAME:
          factory.deserializeSplit(asM(), in); return true;
      }
    }
    return false;
  }

  public boolean deserializeAttribute(final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, @NotNull final CharSequence attributeValue) {
    final String value = StringUtil.toString(attributeValue);
    switch (StringUtil.toString(attributeLocalName)) {
      case "name" : setName(value); break;
      case "owner": setOwner(new SimplePrincipal(value)); break;
      case ATTR_ROLES: mRoles.addAll(Arrays.asList(value.split(" *, *"))); break;
      case "uuid": setUuid(UUID.fromString(value)); break;
      default:
        return false;
    }
    return true;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    ensureIds();
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    XmlUtil.writeAttribute(out, "name", getName());
    XmlUtil.writeAttribute(out, "owner", mOwner==null ? null : mOwner.getName());
    if (mRoles!=null && mRoles.size()>0) {
      XmlUtil.writeAttribute(out, ATTR_ROLES,StringUtil.join(",", mRoles));
    }
    if (getUuid() !=null) {
      XmlUtil.writeAttribute(out, "uuid", getUuid().toString());
    }
    XmlUtil.writeChildren(out, getImports());
    XmlUtil.writeChildren(out, getExports());
    XmlUtil.writeChildren(out, mProcessNodes);
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  public void ensureIds() {
    Set<String> ids = new HashSet<>();
    List<ProcessNode<?,?>> unnamedNodes = new ArrayList<>();
    for(ProcessNode<?,?> node: getModelNodes()) {
      String id = node.getId();
      if (id==null) {
        unnamedNodes.add(node);
      } else {
        ids.add(id);
      }
    }
    Map<String, Integer> startCounts = new HashMap<>();
    for(ProcessNode<?,?> unnamed: unnamedNodes) {
      String idBase = unnamed.getIdBase();
      int startCount = getOrDefault(startCounts, idBase, 1);
      for(String id=idBase+Integer.toString(startCount);
          ids.contains(id);
          id=idBase+Integer.toString(startCount)) {
        ++startCount;
      }
      unnamed.setId(idBase+Integer.toString(startCount));
      startCounts.put(idBase, startCount+1);
    }
  }

  public void setImports(@NotNull final Collection<? extends IXmlResultType> imports) {
    mImports = ProcessNodeBase.toExportableResults(imports);
  }

  public void setExports(@NotNull final Collection<? extends IXmlDefineType> exports) {
    mExports = ProcessNodeBase.toExportableDefines(exports);
  }

  private static int getOrDefault(final Map<String, Integer> map, final String key, final int defaultValue) {
    final Integer val = map.get(key);
    return val == null ? defaultValue : val.intValue();
  }

  public T removeNode(final int nodePos) {
    return mProcessNodes.remove(nodePos);
  }

  public boolean hasUnpositioned() {
    for(ProcessNode<?,?> node: getModelNodes()) {
      if (! node.hasPos()) {
        return true;
      }
    }
    return false;
  }

  public static <T extends ProcessNode<T, M>, M extends ProcessModelBase<T, M>> M deserialize(final DeserializationFactory<T, M> factory, final M processModel, final XmlReader in) throws
          XmlException {

    XmlUtil.skipPreamble(in);
    final QName elementName = ELEMENTNAME;
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

    for(final T node: processModel.getModelNodes()) {
      for(final Identifiable pred: node.getPredecessors()) {
        final T predNode = processModel.getNode(pred);
        if (predNode!=null) {
          predNode.addSuccessor(node);
        }
      }
    }
    return processModel;
  }

  @Nullable
  @Override
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
  public IProcessModelRef<? extends T, M> getRef() {
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
    if(mProcessNodes.add(node)) {
      node.setOwnerModel(this.asM());
      return true;
    }
    return false;
  }

  public boolean removeNode(T node) {
    return mProcessNodes.remove(node);
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.ProcessModel#getNode(java.lang.String)
     */
  @Override
  public final T getNode(final Identifiable nodeId) {
    if (nodeId instanceof ProcessNode) { return (T) nodeId; }
    return mProcessNodes.get(nodeId);
  }

  public T getNode(final int pos) {
    return mProcessNodes.get(pos);
  }

  public T setNode(final int pos, final T newValue) {
    final T oldValue = mProcessNodes.set(pos, newValue);
    newValue.setOwnerModel(asM());
    oldValue.setSuccessors(Collections.<Identifiable>emptySet());
    oldValue.setPredecessors(Collections.<Identifiable>emptySet());
    oldValue.setOwnerModel(null);
    newValue.resolveRefs();
    for(Identifiable pred: newValue.getPredecessors()) {
      getNode(pred).addSuccessor(newValue);
    }
    for(Identifiable suc: newValue.getSuccessors()) {
      getNode(suc).addPredecessor(newValue);
    }
    return oldValue;
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
  public M normalize(SplitFactory<? extends T, M> splitFactory) {
    ensureIds();
    // Make all nodes directly refer to other nodes.
    for(T childNode: mProcessNodes) {
      childNode.resolveRefs();
    }
    for(T childNode: mProcessNodes) {
      // Create a copy as we are actually going to remove all successors, but need to keep the list
      ArrayList<Identifiable> successors = new ArrayList<>(childNode.getSuccessors());
      if (successors.size()>1 && ! (childNode instanceof Split)) {
        for(Identifiable suc2: successors) { // Remove the current node as predecessor.
          ProcessNode<?, ?> suc = (ProcessNode) suc2;
          suc.removePredecessor(childNode);
          childNode.removeSuccessor(suc); // remove the predecessor from the current node
        }
        // create a new join, this should
        Split<? extends T, M> newSplit = splitFactory.createSplit(asM(), successors);
        childNode.addSuccessor(newSplit);
      }
    }
    return this.asM();
  }

  @SuppressWarnings("unchecked")
  public final M asM() {
    return (M) this;
  }
}
