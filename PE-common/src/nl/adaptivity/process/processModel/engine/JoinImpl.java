package nl.adaptivity.process.processModel.engine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessNode;


@XmlRootElement(name = JoinImpl.ELEMENTNAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class JoinImpl extends ProcessNodeImpl implements Join {

  private static final long serialVersionUID = -8598245023280025173L;

  public static final String ELEMENTNAME = "join";

  private int aMin;

  private int aMax;

  private Set<ProcessNode> aPred;

  public JoinImpl(final Collection<ProcessNodeImpl> pNodes, final int pMin, final int pMax) {
    super(pNodes);
    setMin(pMin);
    setMax(Math.min(pNodes.size(), pMax));
    if ((getMin() < 1) || (getMin() > pNodes.size()) || (pMax < pMin)) {
      throw new IllegalProcessModelException("Join range (" + pMin + ", " + pMax + ") must be sane");
    }
  }

  public JoinImpl() {}

  public static Join andJoin(final ProcessNodeImpl... pNodes) {
    return new JoinImpl(Arrays.asList(pNodes), pNodes.length, pNodes.length);
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.Join#setMax(int)
   */
  @Override
  public void setMax(final int max) {
    aMax = max;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.Join#getMax()
   */
  @Override
  @XmlAttribute(required = true)
  public int getMax() {
    return aMax;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.Join#setMin(int)
   */
  @Override
  public void setMin(final int min) {
    aMin = min;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.Join#getMin()
   */
  @Override
  @XmlAttribute(required = true)
  public int getMin() {
    return aMin;
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
  @XmlElement(name = "predecessor")
  @XmlIDREF
  public Set<ProcessNode> getPred() {
    if (aPred == null) {
      aPred = new ProcessNodeSet();
    }
    return aPred;
  }

  @XmlElement(name = "predecessor")
  @XmlIDREF
  @Override
  public Set<ProcessNode> getPredecessors() {
    if (aPred != null) {
      final Set<ProcessNode> pred = super.getPredecessors();
      for (final Object o : aPred) {
        if (o instanceof ProcessNode) {
          pred.add((ProcessNodeImpl) o);
        }
      }
      aPred = null;
      return pred;
    }
    return super.getPredecessors();
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
