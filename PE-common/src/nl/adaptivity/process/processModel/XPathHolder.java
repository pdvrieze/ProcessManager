package nl.adaptivity.process.processModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

@XmlAccessorType(XmlAccessType.NONE)
public class XPathHolder {

  private static final XPathExpression SELF_PATH;
  private XPathExpression path;

  static {
    try {
      SELF_PATH = XPathFactory.newInstance().newXPath().compile(".");
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

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

  public XPathExpression getXPath() {
    return path !=null ? path : SELF_PATH;
  }

  public void setXPath(XPathExpression xpath) { path = xpath; }

}