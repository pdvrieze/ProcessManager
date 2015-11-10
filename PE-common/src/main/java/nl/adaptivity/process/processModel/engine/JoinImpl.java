package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@XmlDeserializer(JoinImpl.Factory.class)
@XmlRootElement(name = JoinImpl.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class JoinImpl extends JoinSplitImpl implements Join<ProcessNodeImpl> {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public JoinImpl deserialize(@NotNull final XMLStreamReader in) throws XMLStreamException {
      return JoinImpl.deserialize(null, in);
    }
  }

  @NotNull
  public static JoinImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XMLStreamReader in) throws XMLStreamException {
    return XmlUtil.deserializeHelper(new JoinImpl(ownerModel), in);
  }

  private static final long serialVersionUID = -8598245023280025173L;

  public static final String ELEMENTLOCALNAME = "join";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);
  public static final QName PREDELEMNAME = new QName(Engine.NAMESPACE, "predecessor", Engine.NSPREFIX);

  public JoinImpl(final ProcessModelImpl ownerModel, final Collection<? extends Identifiable> predecessors, final int min, final int max) {
    super(ownerModel, predecessors, min, max);
    if ((getMin() < 1) || (max < min)) {
      throw new IllegalProcessModelException("Join range (" + min + ", " + max + ") must be sane");
    }
  }

  public JoinImpl(final ProcessModelImpl ownerModel) {
    super(ownerModel);
  }

  @NotNull
  public static JoinImpl andJoin(final ProcessModelImpl ownerModel, final ProcessNodeImpl... nodes) {
    return new JoinImpl(ownerModel, Arrays.asList(nodes), Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Override
  public void serialize(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    out.writeEndElement();
  }

  protected void serializeChildren(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    super.serializeChildren(out);
    for(final Identifiable pred: getPredecessors()) {
      XmlUtil.writeSimpleElement(out, PREDELEMNAME, pred.getId());
    }
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean deserializeChild(@NotNull final XMLStreamReader in) throws XMLStreamException {
    if (XmlUtil.isElement(in, PREDELEMNAME)) {
      final String id = XmlUtil.readSimpleElement(in);
      addPredecessor(new Identifier(id));
      return true;
    }
    return super.deserializeChild(in);
  }

  @Override
  public boolean condition(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    return true;
  }

  @Nullable
  Set<? extends Identifiable> getXmlPrececessors() {
    if (getPredecessors()==null) { return null; }
    return getPredecessors();
  }

  @XmlElement(name = "predecessor")
//@XmlJavaTypeAdapter(PredecessorAdapter.class)
  @XmlIDREF
  void setXmlPrececessors(final List<? extends ProcessNodeImpl> pred) {
    swapPredecessors(pred);
  }

  @Override
  public int getMaxPredecessorCount() {
    return Integer.MAX_VALUE;
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
    return true;
  }

  @Override
  public <R> R visit(@NotNull final ProcessNode.Visitor<R> visitor) {
    return visitor.visitJoin(this);
  }

}
