package nl.adaptivity.process.processModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.bind.annotation.*;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;

@XmlRootElement(name = "join")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Join")
public class Join extends ProcessNode{

  private static final long serialVersionUID = -8598245023280025173L;

  private int aMin;
  private int aMax;

  private ArrayList<Object> aPred;

  public Join(Collection<ProcessNode> pNodes, int pMin, int pMax) {
    super(pNodes);
    setMin(pMin);
    setMax(Math.min(pNodes.size(), pMax));
    if (getMin() < 1 || getMin() > pNodes.size() || pMax < pMin) {
      throw new IllegalProcessModelException("Join range ("+pMin+", "+pMax+") must be sane");
    }
  }

  public Join() {
  }

  public static Join andJoin(ProcessNode... pNodes) {
    return new Join(Arrays.asList(pNodes), pNodes.length, pNodes.length);
  }

  public void setMax(int max) {
    aMax = max;
  }

  @XmlAttribute(required=true)
  public int getMax() {
    return aMax;
  }

  public void setMin(int min) {
    aMin = min;
  }

  @XmlAttribute(required=true)
  public int getMin() {
    return aMin;
  }

  @Override
  public boolean condition(Task<?> pInstance) {
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

  @XmlElement(name="predecessor")
  @XmlIDREF
  public Collection<Object> getPred() {
    if (aPred == null) {
      aPred = new ArrayList<Object>();
    }
    return aPred;
  }

  @XmlElement(name="predecessor")
  @XmlIDREF
  @Override
  public Collection<ProcessNode> getPredecessors() {
    if (aPred!=null) {
      Collection<ProcessNode> pred = super.getPredecessors();
      for(Object o:aPred) {
        if (o instanceof ProcessNode) {
          pred.add((ProcessNode) o);
        }
      }
      aPred = null;
      return pred;
    }
    return super.getPredecessors();
  }

  @Override
  public <T, U extends Task<U>> boolean provideTask(IMessageService<T, U> pMessageService, U pInstance) {
    return true;
  }

  @Override
  public <T, U extends Task<U>> boolean takeTask(IMessageService<T, U> pMessageService, U pInstance) {
    return true;
  }

  @Override
  public <T, U extends Task<U>> boolean startTask(IMessageService<T, U> pMessageService, U pInstance) {
    return true;
  }

}
