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

    @Override
    public JoinImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return JoinImpl.deserialize(null, in);
    }
  }

  public static JoinImpl deserialize(final ProcessModelImpl pOwnerModel, final XMLStreamReader in) throws XMLStreamException {
    return XmlUtil.deserializeHelper(new JoinImpl(pOwnerModel), in);
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
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean deserializeChild(final XMLStreamReader pIn) throws XMLStreamException {
    if (XmlUtil.isElement(pIn, PREDELEMNAME)) {
      String id = XmlUtil.readSimpleElement(pIn);
      addPredecessor(new Identifier(id));
      return true;
    }
    return super.deserializeChild(pIn);
  }

  @Override
  public boolean condition(final IProcessNodeInstance<?> pInstance) {
    return true;
  }

  /**
   * @deprecated  Should be removed
   */
  @Deprecated
  @Override
  public void skip() {
    //    JoinInstance j = pProcessInstance.getJoinInstance(this, pPredecessor);
    //    if (j.isFinished()) {
    //      return;
    //    }
    //    j.incSkipped();
    //    throw new UnsupportedOperationException("Not yet correct");
  }

  Set<? extends Identifiable> getXmlPrececessors() {
    if (getPredecessors()==null) { return null; }
    return getPredecessors();
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
