package nl.adaptivity.process.processModel.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.util.ListFilter;


@XmlRootElement(name = JoinImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class JoinImpl extends
JoinSplitImpl implements Join<ProcessNodeImpl> {

  private static final long serialVersionUID = -8598245023280025173L;

  public static final String ELEMENTNAME = "join";

  private Set<ProcessNodeImpl> aPred;

  public JoinImpl(final Collection<ProcessNodeImpl> pPredecessors, final int pMin, final int pMax) {
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

  // TODO see whether this is still needed
  @Deprecated
  public Set<ProcessNodeImpl> getPred() {
    if (aPred == null) {
      aPred = ProcessNodeSet.processNodeSet();
    }
    return aPred;
  }

  @Override
  public Set<? extends ProcessNodeImpl> getPredecessors() {
    if (aPred != null) {
      setPredecessors(aPred);
      aPred = null;
    }
    return super.getPredecessors();
  }

  List<? extends ProcessNodeImpl> getXmlPrececessors() {
    return new ArrayList<>(getPredecessors());
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

}
