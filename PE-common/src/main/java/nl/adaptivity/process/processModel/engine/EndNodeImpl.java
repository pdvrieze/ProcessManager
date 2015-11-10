package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.util.*;


@XmlDeserializer(EndNodeImpl.Factory.class)
@XmlRootElement(name = EndNodeImpl.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "EndNode")
public class EndNodeImpl extends ProcessNodeImpl implements EndNode<ProcessNodeImpl>, SimpleXmlDeserializable {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public EndNodeImpl deserialize(@NotNull final XMLStreamReader in) throws XMLStreamException {
      return EndNodeImpl.deserialize(null, in);
    }
  }

  @NotNull
  public static EndNodeImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XMLStreamReader in) throws XMLStreamException {
    return XmlUtil.deserializeHelper(new EndNodeImpl(ownerModel), in);
  }

  private static final long serialVersionUID = 220908810658246960L;

  public static final String ELEMENTLOCALNAME = "end";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private List<XmlDefineType> mExports;

  public EndNodeImpl(final ProcessModelImpl ownerModel, final ProcessNodeImpl previous) {
    super(ownerModel, Collections.singletonList(previous));
  }

  public EndNodeImpl(final ProcessModelImpl ownerModel) {
    super(ownerModel);
  }

  @Override
  public boolean deserializeChild(@NotNull final XMLStreamReader in) throws XMLStreamException {
    if (ProcessConsts.Engine.NAMESPACE.equals(in.getNamespaceURI())) {
      switch (in.getLocalName()) {
        case "export":
        case XmlDefineType.ELEMENTLOCALNAME:
          getDefines(); mExports.add(XmlDefineType.deserialize(in)); return true;
      }
    }
    return false;
  }

  @Override
  public boolean deserializeAttribute(final String attributeNamespace, final String attributeLocalName, final String attributeValue) {
    if (ATTR_PREDECESSOR.equals(attributeLocalName)) {
      setPredecessor(new Identifier(attributeValue));
      return true;
    }
    return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
  }

  @Override
  public boolean deserializeChildText(final String elementText) {
    return false;
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    out.writeEndElement();
  }

  protected void serializeChildren(final XMLStreamWriter out) throws XMLStreamException {
    super.serializeChildren(out);
    XmlUtil.writeChildren(out, mExports);
  }

  @Override
  protected void serializeAttributes(@NotNull final XMLStreamWriter out) throws XMLStreamException {
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
   * @see nl.adaptivity.process.processModel.EndNode#getExports()
   */
  @Override
  @XmlElement(name = "export")
  public List<? extends XmlDefineType> getDefines() {
    if (mExports == null) {
      mExports = new ArrayList<>();
    }
    return mExports;
  }

  @Override
  public void setDefines(final Collection<? extends IXmlDefineType> exports) {
    mExports = toExportableDefines(exports);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.EndNode#getSuccessors()
   */
  @NotNull
  @Override
  public Set<? extends ProcessNodeImpl> getSuccessors() {
    return Collections.emptySet();
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
