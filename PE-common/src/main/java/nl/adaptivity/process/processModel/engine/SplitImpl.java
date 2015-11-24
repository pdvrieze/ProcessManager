package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.StringUtil;
import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.Split;
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

import java.util.Collections;


@XmlDeserializer(SplitImpl.Factory.class)
@XmlRootElement(name = Split.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Split")
public class SplitImpl extends JoinSplitImpl implements Split<ExecutableProcessNode, ProcessModelImpl> {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public SplitImpl deserialize(final XmlReader in) throws XmlException {
      return SplitImpl.deserialize(null, in);
    }
  }

  @NotNull
  public static SplitImpl deserialize(final ProcessModelImpl ownerModel, final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new SplitImpl(ownerModel), in);
  }

  private static final long serialVersionUID = -8598245023280025173L;

  public SplitImpl(final ProcessModelImpl  ownerModel, final ExecutableProcessNode predecessor, final int min, final int max) {
    super(ownerModel, Collections.singleton(predecessor), min, max);
    if ((getMin() < 1) || (max < min)) {
      throw new IllegalProcessModelException("Join range (" + min + ", " + max + ") must be sane");
    }
  }

  public SplitImpl(final ProcessModelImpl  ownerModel) {
    super(ownerModel);
  }

  @NotNull
  public static SplitImpl andSplit(final ProcessModelImpl ownerModel, final ExecutableProcessNode predecessor) {
    return new SplitImpl(ownerModel, predecessor, Integer.MAX_VALUE, Integer.MAX_VALUE);
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

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    if (ATTR_PREDECESSOR.equals(attributeLocalName)) {
      setPredecessor(new Identifier(StringUtil.toString(attributeValue)));
      return true;
    }
    return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
  }

  @Override
  public boolean condition(final Transaction transaction, final IProcessNodeInstance<?> instance) {
    return true;
  }

  @Override
  public int getMaxSuccessorCount() {
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

  @Nullable
  @XmlAttribute(name="predecessor")
  @XmlIDREF
  Identifiable getPredecessor() {
    final int c = getPredecessors().size();
    if (c>1) { throw new IllegalStateException("Too many predecessors"); }
    if (c==0) { return null; }
    return getPredecessors().iterator().next();
  }

  void setPredecessor(final Identifier pred) {
    setPredecessors(Collections.singleton(pred));
  }

  @Override
  public <R> R visit(@NotNull final ProcessNode.Visitor<R> visitor) {
    return visitor.visitSplit(this);
  }

}
