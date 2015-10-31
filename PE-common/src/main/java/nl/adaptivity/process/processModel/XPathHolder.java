package nl.adaptivity.process.processModel;

import nl.adaptivity.xml.GatheringNamespaceContext;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.util.Map;
import java.util.TreeMap;


public abstract class XPathHolder extends XMLContainer {

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

  public XPathHolder(final char[] pContent, final NamespaceContext pOriginalNSContext, final String pPath) {
    super(pOriginalNSContext, pContent);
    pathString = pPath;
  }

  @XmlAttribute(name="xpath")
  public String getPath() {
    return pathString;
  }

  public void setPath(final String value) {
    if (pathString!=null && pathString.equals(value)) { return; }
    path = null;
    pathString = value;
    assert value==null || getXPath()!=null;
  }

  public NamespaceContext getNamespaceContext() {
    return getOriginalNSContext();
  }

  @Deprecated
  public void setNamespaceContext(NamespaceContext pNamespaceContext) {
    setContent(pNamespaceContext, getContent());

    path = null; // invalidate the cached path expression
  }

  String getPathAttr() { return null; }

  void setPathAttr(String path) {
    setPath(path);
  }

  public XPathExpression getXPath() {
    // TODO support a functionresolver
    if (path==null) {
      if (pathString==null) {
        path = SELF_PATH;
      } else {
        XPathFactory f = XPathFactory.newInstance();
        try {
          XPath xPath = f.newXPath();
          if (getOriginalNSContext()!=null) {
            xPath.setNamespaceContext(getOriginalNSContext());
          }
          path = xPath.compile(pathString);
          return path;
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return path;
  }

  protected void serializeAttributes(final XMLStreamWriter out) throws XMLStreamException {
    super.serializeAttributes(out);
    if (pathString!=null) {
      out.writeAttribute("xpath", pathString);
    }
  }

  @Override
  protected Map<String,String> findNamesInAttributeValue(final NamespaceContext referenceContext, final QName owner, final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    if (pAttributeNamespace==null && "xpath".equals(pAttributeLocalName)) {
      Map<String, String> result = new TreeMap<>();
      try {
        addXpathUsedPrefixes(result, pathString, referenceContext);
      } catch (XPathExpressionException pE) {
        throw new RuntimeException(pE);
      }
    }
    return super.findNamesInAttributeValue(referenceContext, owner, pAttributeNamespace, pAttributeLocalName, pAttributeValue);
  }

  public static void addXpathUsedPrefixes(final Map<String, String> pNamespaceMap, final String pPath, final NamespaceContext pNamespaceContext) throws
          XPathExpressionException {
    XPathFactory xpf = XPathFactory.newInstance();
    XPath xpath = xpf.newXPath();
    xpath.setNamespaceContext(new GatheringNamespaceContext(pNamespaceContext, pNamespaceMap));
    xpath.compile(pPath);
  }
}