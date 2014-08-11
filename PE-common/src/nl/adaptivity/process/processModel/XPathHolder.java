package nl.adaptivity.process.processModel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


public class XPathHolder {

  protected XPathExpression path;

  public XPathHolder() {
    super();
  }

  @XmlAttribute
  public String getPath() {
    return path.toString();
  }

  public void setPath(final String value) {
    if (value==null) {
      path = null;
    } else {
      XPathFactory f = XPathFactory.newInstance();
      try {
        path = f.newXPath().compile(value);
      } catch (XPathExpressionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public XPathExpression getXPath() { return path; }

  public void setXPath(XPathExpression xpath) { path = xpath; }

}