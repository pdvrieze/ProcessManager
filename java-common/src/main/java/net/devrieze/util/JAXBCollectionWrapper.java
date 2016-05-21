/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

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
