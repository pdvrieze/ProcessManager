package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;
import nl.adaptivity.util.xml.XmlUtil;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


@XmlDeserializer(JoinImpl.Factory.class)
@XmlRootElement(name = JoinImpl.ELEMENTLOCALNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class JoinImpl extends JoinSplitImpl implements Join<ProcessNodeImpl> {

  public class Factory implements XmlDeserializerFactory {

    @Override
    public JoinImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return JoinImpl.deserialize(null, in);
    }
  }

  public static JoinImpl deserialize(final ProcessModelImpl pOwnerModel, final XMLStreamReader in) throws XMLStreamException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private static final long serialVersionUID = -8598245023280025173L;

  public static final String ELEMENTLOCALNAME = "join";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);
  public static final QName PREDELEMNAME = new QName(Engine.NAMESPACE, "predecessor", Engine.NSPREFIX);

  public JoinImpl(final ProcessModelImpl pOwnerModel, final Collection<? extends Identifiable> pPredecessors, final int pMin, final int pMax) {
    super(pOwnerModel, pPredecessors, pMin, pMax);
    if ((getMin() < 1) || (pMax < pMin)) {
      throw new IllegalProcessModelException("Join range (" + pMin + ", " + pMax + ") must be sane");
    }
  }

  public JoinImpl(final ProcessModelImpl pOwnerModel) {
    super(pOwnerModel);
  }

  public static JoinImpl andJoin(final ProcessModelImpl pOwnerModel, final ProcessNodeImpl... pNodes) {
    return new JoinImpl(pOwnerModel, Arrays.asList(pNodes), Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    out.writeEndElement();
  }

  protected void serializeChildren(final XMLStreamWriter pOut) throws XMLStreamException {
    super.serializeChildren(pOut);
    for(Identifiable pred: getPredecessors()) {
      XmlUtil.writeSimpleElement(pOut, PREDELEMNAME, pred.getId());
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

  List<? extends ProcessNodeImpl> getXmlPrececessors() {
    if (getPredecessors()==null) { return null; }
    return (ProcessNodeSet<? extends ProcessNodeImpl>) getPredecessors();
  }

  @XmlElement(name = "predecessor")
//@XmlJavaTypeAdapter(PredecessorAdapter.class)
  @XmlIDREF
  void setXmlPrececessors(List<? extends ProcessNodeImpl> pred) {
    swapPredecessors(pred);
  }

  @Override
  public int getMaxPredecessorCount() {
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

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitJoin(this);
  }

}
