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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.test;

import net.devrieze.util.JAXBCollectionWrapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;

import java.io.StringWriter;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;


public class JAXBCollectionWrapperTest {


  private static final String NS = "http://example.org/test";

  @XmlRootElement(name = "xItem", namespace = "http://example.org/test")
  static class XItem {

    @XmlValue
    public String value;

    public XItem() { value = null; }

    public XItem(final String string) {
      value = string;
    }
  }

  @XmlRootElement(name = "yItem", namespace = "http://example.org/test")
  static class YItem {

    public YItem(final String string) {
      value = string;
    }

    public YItem() { value = null; }

    @XmlValue
    public String value;
  }

  private ArrayList<Object> mCollection;

  private final String mExpectedMarshalling = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><list xmlns=\"http://example.org/test\"><xItem>x</xItem>blabla<yItem>y</yItem></list>";

  @BeforeMethod
  public void setUp() {
    mCollection = new ArrayList<>();
    mCollection.add(new XItem("x"));
    mCollection.add("blabla");
    mCollection.add(new YItem("y"));

  }

  @Test
  public void testGetJAXBElement() throws JAXBException {
    final JAXBContext context = JAXBContext.newInstance(XItem.class, YItem.class, JAXBCollectionWrapper.class);
    final JAXBCollectionWrapper wrapper = new JAXBCollectionWrapper(mCollection, Object.class);
    final JAXBElement<JAXBCollectionWrapper> element = wrapper.getJAXBElement(new QName(NS, "list"));
    final StringWriter out = new StringWriter();
    context.createMarshaller().marshal(element, out);
    assertEquals(out.toString(), mExpectedMarshalling);
  }

}