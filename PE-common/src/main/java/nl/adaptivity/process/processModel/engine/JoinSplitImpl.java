package nl.adaptivity.process.processModel.engine;


import nl.adaptivity.process.processModel.JoinSplit;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import java.util.Collection;


@XmlAccessorType(XmlAccessType.NONE)
public abstract class JoinSplitImpl extends ProcessNodeImpl implements JoinSplit<ExecutableProcessNode, ProcessModelImpl>, SimpleXmlDeserializable, ExecutableProcessNode {

  private static final long serialVersionUID = -4343040873373817308L;

  private int mMin;
  private int mMax;

  public JoinSplitImpl(final ProcessModelImpl ownerModel) {
    super(ownerModel);
  }

  public JoinSplitImpl(final ProcessModelImpl  ownerModel, final Collection<? extends Identifiable> predecessors, final int min, final int max) {
    super(ownerModel, predecessors);
    mMin = min;
    mMax = max;
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public void setMax(final int max) {
    mMax = max;
  }

  @Override
  @XmlAttribute(required = true)
  public int getMax() {
    return mMax;
  }

  @Override
  public void setMin(final int min) {
    mMin = min;
  }

  @Override
  @XmlAttribute(required = true)
  public int getMin() {
    return mMin;
  }

}