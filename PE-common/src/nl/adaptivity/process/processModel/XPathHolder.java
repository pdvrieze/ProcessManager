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
  private String pathString;

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
    return pathString;
  }

  public void setPath(final String value) {
    if (pathString!=null && pathString.equals(value)) { return; }
    path = null;
    pathString = value;
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
    if (path==null) {
      if (pathString==null) {
        path = SELF_PATH;
      } else {
        XPathFactory f = XPathFactory.newInstance();
        try {
          path = f.newXPath().compile(pathString);
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return path;
  }

}