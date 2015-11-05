package nl.adaptivity.process.processModel;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CombiningNamespaceContext;
import nl.adaptivity.util.xml.SimpleNamespaceContext;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.GatheringNamespaceContext;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class XPathHolder extends XMLContainer {

  private static final XPathExpression SELF_PATH;
  private String name;

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

  public XPathHolder(final char[] pContent, final NamespaceContext pOriginalNSContext, final String pPath, final String pName) {
    super();
    setName(pName);
    setPath(pOriginalNSContext, pPath);
    setContent(pOriginalNSContext, pContent);
  }

  @XmlAttribute(name="xpath")
  public String getPath() {
    return pathString;
  }

  public void setPath(final NamespaceContext baseNsContext, final String value) {
    if (pathString!=null && pathString.equals(value)) { return; }
    path = null;
    pathString = value;
    assert value==null || getXPath()!=null;
    updateNamespaceContext(baseNsContext);
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

  public NamespaceContext getNamespaceContext() {
    return getOriginalNSContext();
  }

  @Deprecated
  public void setNamespaceContext(NamespaceContext pNamespaceContext) {
    setContent(pNamespaceContext, getContent());

    path = null; // invalidate the cached path expression
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.IXmlResultType#getName()
     */
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.IXmlResultType#setName(java.lang.String)
     */
  public void setName(final String value) {
    this.name = value;
  }

  protected boolean deserializeAttribute(final String pAttributeLocalName, final String pAttributeValue) {
    switch(pAttributeLocalName) {
      case "name":
        setName(pAttributeValue);
        return true;
      case "path":
      case "xpath":
        pathString=pAttributeValue;
        return true;
      case XMLConstants.XMLNS_ATTRIBUTE:
        return true;
      default:
        return false;
    }
  }

  protected static <T extends XPathHolder> T deserialize(final XMLStreamReader in, final T pResult) throws
          XMLStreamException {
    XmlUtil.skipPreamble(in);
    if (in.getEventType()!= XMLStreamConstants.START_ELEMENT) { in.nextTag(); }
    for(int i=in.getAttributeCount()-1; i>=0;--i) {
      String prefix = in.getAttributePrefix(i);
      if (prefix==null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
        if (pResult.deserializeAttribute(in.getAttributeLocalName(i), in.getAttributeValue(i)) ) {
          continue;
        }
      } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
        continue;
      }
      Logger.getAnonymousLogger().log(Level.FINER, "Unsupported attribute in result: "+in.getAttributeName(i), in);
    }

    Map<String, String> namespaceMap = new TreeMap<>();
    String path = pResult.getPath();
    if (path!=null) {
      visitXpathUsedPrefixes(path, new GatheringNamespaceContext(in.getNamespaceContext(), namespaceMap));
    }
    if (in.hasNext()) {
      if (in.next()!=XMLStreamConstants.END_ELEMENT) {
        pResult.setContent(in.getNamespaceContext(), XmlUtil.siblingsToCharArray(in));
      }
    }

    if (! (in.getEventType()==XMLStreamConstants.END_ELEMENT|| in.getEventType()==XMLStreamConstants.END_DOCUMENT)) {
      throw new RuntimeException("Missing end tag");
    }

    if (namespaceMap.size()>0) {
      pResult.addNamespaceContext(new SimpleNamespaceContext(namespaceMap));
    }
    return pResult;
  }

  protected void serializeAttributes(final XMLStreamWriter out) throws XMLStreamException {
    super.serializeAttributes(out);
    if (pathString!=null) {
      Map<String, String> namepaces = new TreeMap<>();
      // Have a namespace that gathers those namespaces that are not known already in the outer context
      NamespaceContext referenceContext = out.getNamespaceContext();
      // TODO streamline this, the right context should not require the filtering on the output context later.
      NamespaceContext nsc = new GatheringNamespaceContext(new CombiningNamespaceContext(referenceContext, getNamespaceContext()), namepaces);
      visitXpathUsedPrefixes(pathString, nsc);
      for(Entry<String, String> ns: namepaces.entrySet()) {
        if (! ns.getValue().equals(referenceContext.getNamespaceURI(ns.getKey()))) {
          out.writeNamespace(ns.getKey(), ns.getValue());
        }
      }
      out.writeAttribute("xpath", pathString);

    }
    XmlUtil.writeAttribute(out, "name", name);
  }

  @Override
  protected void visitNamespaces(final NamespaceContext pBaseContext) throws XMLStreamException {
    path = null;
    if (pathString!=null) { visitXpathUsedPrefixes(pathString, pBaseContext); }
    super.visitNamespaces(pBaseContext);
  }

  @Override
  protected void visitNamesInAttributeValue(final NamespaceContext referenceContext, final QName owner, final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    if (Constants.MODIFY_NS_STR.equals(owner.getNamespaceURI()) && pAttributeNamespace=="" && "xpath".equals(pAttributeLocalName)) {
      visitXpathUsedPrefixes(pAttributeValue, referenceContext);
    }
  }

  protected static void visitXpathUsedPrefixes(final String pPath, final NamespaceContext pNamespaceContext) {
    if (pPath!=null && pPath.length()>0) {
      try {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(pNamespaceContext);
        xpath.compile(pPath);
      } catch (XPathExpressionException pE) {
        throw new RuntimeException(pE);
      }
    }
  }
}