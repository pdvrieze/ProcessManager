package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@XmlDeserializer(JoinImpl.Factory.class)
@XmlRootElement(name = Join.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class JoinImpl extends JoinSplitImpl implements Join<ExecutableProcessNode, ProcessModelImpl> {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public JoinImpl deserialize(@NotNull final XmlReader in) throws XmlException {
      return JoinImpl.deserialize(null, in);
    }
  }

  @NotNull
  public static JoinImpl deserialize(final ProcessModelImpl ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new JoinImpl(ownerModel), in);
  }

  private static final long serialVersionUID = -8598245023280025173L;

  public JoinImpl(final ProcessModelImpl  ownerModel, final Collection<? extends Identifiable> predecessors, final int min, final int max) {
    super(ownerModel, predecessors, min, max);
    if ((getMin() < 1) || (max < min)) {
      throw new IllegalProcessModelException("Join range (" + min + ", " + max + ") must be sane");
    }
  }

  public JoinImpl(final ProcessModelImpl  ownerModel) {
    super(ownerModel);
  }

  @NotNull
  public static JoinImpl andJoin(final ProcessModelImpl ownerModel, final ExecutableProcessNode... predecessors) {
    return new JoinImpl(ownerModel, Arrays.asList(predecessors), Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  protected void serializeChildren(@NotNull final XmlWriter out) throws XmlException {
    super.serializeChildren(out);
    for(final Identifiable pred: getPredecessors()) {
      XmlUtil.writeStartElement(out, PREDELEMNAME);
      out.text(pred.getId());
      XmlUtil.writeEndElement(out, PREDELEMNAME);
    }
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (XmlUtil.isElement(in, PREDELEMNAME)) {
      final String id = XmlUtil.readSimpleElement(in).toString();
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
  void setXmlPrececessors(final List<? extends ExecutableProcessNode> pred) {
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
