package net.devrieze.util;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.namespace.QName;


@XmlAccessorType(XmlAccessType.NONE)
public class JAXBCollectionWrapper {

  private final Collection<?> mCollection;

  private final Class<?> mElementType;

  public JAXBCollectionWrapper() {
    mCollection = new ArrayList<>();
    mElementType = Object.class;
  }

  public JAXBCollectionWrapper(final Collection<?> pCollection, final Class<?> pElementType) {
    mCollection = pCollection;
    mElementType = pElementType;
  }

  @XmlMixed
  @XmlAnyElement(lax = true)
  public Collection<?> getElements() {
    return mCollection;
  }

  public JAXBElement<JAXBCollectionWrapper> getJAXBElement(final QName pName) {
    return new JAXBElement<>(pName, JAXBCollectionWrapper.class, this);
  }

  public Class<?> getElementType() {
    return mElementType;
  }

}
