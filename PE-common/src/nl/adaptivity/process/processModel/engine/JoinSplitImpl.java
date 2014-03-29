package nl.adaptivity.process.processModel.engine;


import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;

import nl.adaptivity.process.processModel.JoinSplit;


public abstract class JoinSplitImpl extends ProcessNodeImpl implements JoinSplit<ProcessNodeImpl>{

  private static final long serialVersionUID = -4343040873373817308L;

  private int aMin;
  private int aMax;

  public JoinSplitImpl() {
    // default empty constructor for serialization (and xml work)
  }

  public JoinSplitImpl(Collection<ProcessNodeImpl> pPredecessors, int pMin, int pMax) {
    super(pPredecessors);
    aMin = pMin;
    aMax = pMax;
  }

  @Override
  public void setMax(final int max) {
    aMax = max;
  }

  @Override
  @XmlAttribute(required = true)
  public int getMax() {
    return aMax;
  }

  @Override
  public void setMin(final int min) {
    aMin = min;
  }

  @Override
  @XmlAttribute(required = true)
  public int getMin() {
    return aMin;
  }

}