package nl.adaptivity.process.processModel.engine;


import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import nl.adaptivity.process.processModel.JoinSplit;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;


@XmlAccessorType(XmlAccessType.NONE)
public abstract class JoinSplitImpl extends ProcessNodeImpl implements JoinSplit<ProcessNodeImpl>, SimpleXmlDeserializable {

  private static final long serialVersionUID = -4343040873373817308L;

  private int mMin;
  private int mMax;

  public JoinSplitImpl(final ProcessModelImpl ownerModel) {
    super(ownerModel);
  }

  public JoinSplitImpl(final ProcessModelImpl ownerModel, final Collection<? extends Identifiable> predecessors, final int min, final int max) {
    super(ownerModel, predecessors);
    mMin = min;
    mMax = max;
  }

  @Override
  public boolean deserializeChild(final XMLStreamReader in) throws XMLStreamException {
    return false;
  }

  @Override
  public boolean deserializeChildText(final String elementText) {
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