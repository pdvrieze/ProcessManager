package net.devrieze.test;

import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import net.devrieze.util.JAXBCollectionWrapper;


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

  @Before
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
    assertEquals(mExpectedMarshalling, out.toString());
  }

}