package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import java.util.*;


@XmlDeserializer(EndNodeImpl.Factory.class)
@XmlRootElement(name = EndNode.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "EndNode")
public class EndNodeImpl extends ProcessNodeImpl implements EndNode<ExecutableProcessNode>, SimpleXmlDeserializable {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public EndNodeImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return EndNodeImpl.deserialize(null, in);
    }
  }

  @NotNull
  public static EndNodeImpl deserialize(final ProcessModelBase<ExecutableProcessNode> ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new EndNodeImpl(ownerModel), in);
  }

  private static final long serialVersionUID = 220908810658246960L;

  public EndNodeImpl(final ProcessModelBase<ExecutableProcessNode> ownerModel, final ExecutableProcessNode previous) {
    super(ownerModel, Collections.singletonList(previous));
  }

  public EndNodeImpl(final ProcessModelBase<ExecutableProcessNode> ownerModel) {
    super(ownerModel);
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case "export":
        case XmlDefineType.ELEMENTLOCALNAME:
          getDefines().add(XmlDefineType.deserialize(in)); return true;
      }
    }
    return false;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    if (ATTR_PREDECESSOR.equals(attributeLocalName)) {
      setPredecessor(new Identifier(attributeValue.toString()));
      return true;
    }
    return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  @Override
  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    if (getPredecessor()!=null) {
      XmlUtil.writeAttribute(out, ATTR_PREDECESSOR, getPredecessor().getId());
    }
  }

  @Override
  public boolean condition(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    return true;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.EndNode#getPredecessor()
   */
  @Nullable
  @Override
  @XmlAttribute(name = "predecessor", required = true)
  @XmlIDREF
  public Identifiable getPredecessor() {
    final Collection<? extends Identifiable> ps = getPredecessors();
    if ((ps == null) || (ps.size() != 1)) {
      return null;
    }
    return ps.iterator().next();
  }

  @Override
  public void setDefines(@Nullable final Collection<? extends IXmlDefineType> exports) {
    super.setDefines(exports);
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.EndNode#setPredecessor(nl.adaptivity.process.processModel.ProcessNode)
     */
  @Override
  public void setPredecessor(final Identifier predecessor) {
    setPredecessors(Arrays.asList(predecessor));
  }

  @Override
  public int getMaxSuccessorCount() {
    return 0;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.EndNode#getSuccessors()
   */
  @NotNull
  @Override
  public ProcessNodeSet<? extends Identifiable> getSuccessors() {
    return ProcessNodeSet.empty();
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(final Transaction transaction, final IMessageService<T, U> messageService, final U instance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean takeTask(final IMessageService<T, U> messageService, final U instance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean startTask(final IMessageService<T, U> messageService, final U instance) {
    //    pProcessInstance.finish();
    return true;
  }

  @Override
  public <R> R visit(@NotNull final ProcessNode.Visitor<R> visitor) {
    return visitor.visitEndNode(this);
  }

}
