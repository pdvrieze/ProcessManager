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

  private int aMin;
  private int aMax;

  public JoinSplitImpl(final ProcessModelImpl pOwnerModel) {
    super(pOwnerModel);
  }

  public JoinSplitImpl(final ProcessModelImpl pOwnerModel, Collection<? extends Identifiable> pPredecessors, int pMin, int pMax) {
    super(pOwnerModel, pPredecessors);
    aMin = pMin;
    aMax = pMax;
  }

  @Override
  public boolean deserializeChild(final XMLStreamReader pIn) throws XMLStreamException {
    return false;
  }

  @Override
  public boolean deserializeChildText(final String pElementText) {
    return false;
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