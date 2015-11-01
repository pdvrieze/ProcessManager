package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.Split;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.util.Collections;


@XmlDeserializer(SplitImpl.Factory.class)
@XmlRootElement(name = SplitImpl.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Split")
public class SplitImpl extends JoinSplitImpl implements Split<ProcessNodeImpl> {

  public class Factory implements XmlDeserializerFactory {

    @Override
    public SplitImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return SplitImpl.deserialize(null, in);
    }
  }

  public static SplitImpl deserialize(final ProcessModelImpl pOwnerModel, final XMLStreamReader in) throws XMLStreamException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private static final long serialVersionUID = -8598245023280025173L;

  public static final String ELEMENTLOCALNAME = "split";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  public SplitImpl(final ProcessModelImpl pOwnerModel, final ProcessNodeImpl pPredecessor, final int pMin, final int pMax) {
    super(pOwnerModel, Collections.singleton(pPredecessor), pMin, pMax);
    if ((getMin() < 1) || (pMax < pMin)) {
      throw new IllegalProcessModelException("Join range (" + pMin + ", " + pMax + ") must be sane");
    }
  }

  public SplitImpl(final ProcessModelImpl pOwnerModel) {
    super(pOwnerModel);
  }

  public static SplitImpl andSplit(final ProcessModelImpl pOwnerModel, final ProcessNodeImpl pPredecessor) {
    return new SplitImpl(pOwnerModel, pPredecessor, Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    out.writeEndElement();
  }

  protected void serializeChildren(final XMLStreamWriter pOut) throws XMLStreamException {
  }

  @Override
  protected void serializeAttributes(final XMLStreamWriter pOut) throws XMLStreamException {
    super.serializeAttributes(pOut);
    if (getPredecessor()!=null) {
      XmlUtil.writeAttribute(pOut, ATTR_PREDECESSOR, getPredecessor().getId());
    }
  }

  @Override
  public boolean condition(final IProcessNodeInstance<?> pInstance) {
    return true;
  }

  @Override
  public void skip() {
    //    JoinInstance j = pProcessInstance.getJoinInstance(this, pPredecessor);
    //    if (j.isFinished()) {
    //      return;
    //    }
    //    j.incSkipped();
    //    throw new UnsupportedOperationException("Not yet correct");
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(Transaction pTransaction, final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean takeTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @Override
  public <T, U extends IProcessNodeInstance<U>> boolean startTask(final IMessageService<T, U> pMessageService, final U pInstance) {
    return true;
  }

  @XmlAttribute(name="predecessor")
  @XmlIDREF
  Identifiable getPredecessor() {
    int c = getPredecessors().size();
    if (c>1) { throw new IllegalStateException("Too many predecessors"); }
    if (c==0) { return null; }
    return getPredecessors().iterator().next();
  }

  void setPredecessor(ProcessNodeImpl pred) {
    setPredecessors(Collections.singleton(pred));
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitSplit(this);
  }

}
