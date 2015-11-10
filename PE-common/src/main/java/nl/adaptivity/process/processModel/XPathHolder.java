package nl.adaptivity.process.processModel;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.GatheringNamespaceContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
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


public abstract class XPathHolder extends XMLContainer {

  private static final XPathExpression SELF_PATH;
  private String name;

  @Nullable private XPathExpression path;
  @Nullable private String pathString;

  static {
    try {
      SELF_PATH = XPathFactory.newInstance().newXPath().compile(".");
    } catch (@NotNull final XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  public XPathHolder() {
    super();
  }

  public XPathHolder(final char[] content, final Iterable<Namespace> originalNSContext, final String path, final String name) {
    super();
    setName(name);
    final SimpleNamespaceContext context = SimpleNamespaceContext.from(originalNSContext);
    setPath(context, path);
    setContent(context, content);
  }

  @Nullable
  @XmlAttribute(name="xpath")
  public String getPath() {
    return pathString;
  }

  public void setPath(final Iterable<Namespace> baseNsContext, @Nullable final String value) {
    if (pathString!=null && pathString.equals(value)) { return; }
    path = null;
    pathString = value;
    updateNamespaceContext(baseNsContext);
    assert value==null || getXPath()!=null;
  }

  @Nullable
  public XPathExpression getXPath() {
    // TODO support a functionresolver
    if (path==null) {
      if (pathString==null) {
        path = SELF_PATH;
      } else {
        final XPathFactory f = XPathFactory.newInstance();
        try {
          final XPath xPath = f.newXPath();
          if (getOriginalNSContext()!=null) {
            xPath.setNamespaceContext(SimpleNamespaceContext.from(getOriginalNSContext()));
          }
          path = xPath.compile(pathString);
          return path;
        } catch (@NotNull final XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return path;
  }

  @Deprecated
  public void setNamespaceContext(final Iterable<Namespace> namespaceContext) {
    setContent(namespaceContext, getContent());

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

  public boolean deserializeAttribute(final String attributeNamespace, @NotNull final String attributeLocalName, final String attributeValue) {
    switch(attributeLocalName) {
      case "name":
        setName(attributeValue);
        return true;
      case "path":
      case "xpath":
        pathString=attributeValue;
        return true;
      case XMLConstants.XMLNS_ATTRIBUTE:
        return true;
      default:
        return false;
    }
  }

  @Override
  public void deserializeChildren(@NotNull final XMLStreamReader in) throws XMLStreamException {
    final NamespaceContext origContext = in.getNamespaceContext();
    super.deserializeChildren(in);
    final Map<String, String> namespaces = new TreeMap<>();
    final NamespaceContext gatheringNamespaceContext = new CombiningNamespaceContext(SimpleNamespaceContext.from(getOriginalNSContext()), new GatheringNamespaceContext(in.getNamespaceContext(), namespaces));
    visitNamespaces(gatheringNamespaceContext);
    if (namespaces.size() > 0) {
      addNamespaceContext(new SimpleNamespaceContext(namespaces));
    }
  }

  @NotNull
  protected static <T extends XPathHolder> T deserialize(@NotNull final XMLStreamReader in, @NotNull final T result) throws
          XMLStreamException {
    return XmlUtil.deserializeHelper(result, in);
  }

  protected void serializeAttributes(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    super.serializeAttributes(out);
    if (pathString!=null) {
      final Map<String, String> namepaces = new TreeMap<>();
      // Have a namespace that gathers those namespaces that are not known already in the outer context
      final NamespaceContext referenceContext = out.getNamespaceContext();
      // TODO streamline this, the right context should not require the filtering on the output context later.
      final NamespaceContext nsc = new GatheringNamespaceContext(new CombiningNamespaceContext(referenceContext, SimpleNamespaceContext.from(getOriginalNSContext())), namepaces);
      visitXpathUsedPrefixes(pathString, nsc);
      for(final Entry<String, String> ns: namepaces.entrySet()) {
        if (! ns.getValue().equals(referenceContext.getNamespaceURI(ns.getKey()))) {
          out.writeNamespace(ns.getKey(), ns.getValue());
        }
      }
      out.writeAttribute("xpath", pathString);

    }
    XmlUtil.writeAttribute(out, "name", name);
  }

  @Override
  protected void visitNamespaces(final NamespaceContext baseContext) throws XMLStreamException {
    path = null;
    if (pathString!=null) { visitXpathUsedPrefixes(pathString, baseContext); }
    super.visitNamespaces(baseContext);
  }

  @Override
  protected void visitNamesInAttributeValue(final NamespaceContext referenceContext, @NotNull final QName owner, @NotNull final QName attributeName, final String attributeValue) {
    if (Constants.MODIFY_NS_STR.equals(owner.getNamespaceURI()) && (XMLConstants.NULL_NS_URI.equals(attributeName.getNamespaceURI())||XMLConstants.DEFAULT_NS_PREFIX.equals(attributeName.getPrefix())) && "xpath".equals(attributeName.getLocalPart())) {
      visitXpathUsedPrefixes(attributeValue, referenceContext);
    }
  }

  protected static void visitXpathUsedPrefixes(@Nullable final String path, final NamespaceContext namespaceContext) {
    if (path!=null && path.length()>0) {
      try {
        final XPathFactory xpf = XPathFactory.newInstance();
        final XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(namespaceContext);
        xpath.compile(path);
      } catch (@NotNull final XPathExpressionException e) {
        throw new RuntimeException(e);
      }
    }
  }
}