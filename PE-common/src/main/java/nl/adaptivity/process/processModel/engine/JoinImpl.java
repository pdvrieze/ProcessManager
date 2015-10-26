package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.util.xml.XmlDeserializer;
import nl.adaptivity.util.xml.XmlDeserializerFactory;

import javax.xml.bind.annotation.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


@XmlDeserializer(JoinImpl.Factory.class)
@XmlRootElement(name = JoinImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class JoinImpl extends JoinSplitImpl implements Join<ProcessNodeImpl> {

  public class Factory implements XmlDeserializerFactory {

    @Override
    public JoinImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
      return JoinImpl.deserialize(in);
    }
  }

  public static JoinImpl deserialize(final XMLStreamReader in) throws XMLStreamException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private static final long serialVersionUID = -8598245023280025173L;

  public static final String ELEMENTNAME = "join";

  public JoinImpl(final Collection<? extends ProcessNodeImpl> pPredecessors, final int pMin, final int pMax) {
    super(pPredecessors, pMin, pMax);
    if ((getMin() < 1) || (pMax < pMin)) {
      throw new IllegalProcessModelException("Join range (" + pMin + ", " + pMax + ") must be sane");
    }
  }

  public JoinImpl() {}

  public static JoinImpl andJoin(final ProcessNodeImpl... pNodes) {
    return new JoinImpl(Arrays.asList(pNodes), Integer.MAX_VALUE, Integer.MAX_VALUE);
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
