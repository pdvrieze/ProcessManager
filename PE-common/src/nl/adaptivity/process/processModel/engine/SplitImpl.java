package nl.adaptivity.process.processModel.engine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;


@XmlRootElement(name = SplitImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Split")
public class SplitImpl extends
JoinSplitImpl implements Join<ProcessNodeImpl> {

  private static final long serialVersionUID = -8598245023280025173L;

  public static final String ELEMENTNAME = "split";

  public SplitImpl(final ProcessNodeImpl pPredecessor, final int pMin, final int pMax) {
    super(Collections.singleton(pPredecessor), pMin, pMax);
    if ((getMin() < 1) || (pMax < pMin)) {
      throw new IllegalProcessModelException("Join range (" + pMin + ", " + pMax + ") must be sane");
    }
  }

  public SplitImpl() {}

  public static SplitImpl andSplit(final ProcessNodeImpl pPredecessor) {
    return new SplitImpl(pPredecessor, Integer.MAX_VALUE, Integer.MAX_VALUE);
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
  public <T, U extends IProcessNodeInstance<U>> boolean provideTask(final IMessageService<T, U> pMessageService, final U pInstance) {
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
  ProcessNodeImpl getPredecessor() {
    int c = getPredecessors().size();
    if (c>1) { throw new IllegalStateException("Too many predecessors"); }
    if (c==0) { return null; }
    return getPredecessors().iterator().next();
  }

  void setPredecessor(ProcessNodeImpl pred) {
    setPredecessors(Collections.singleton(pred));
  }

}
