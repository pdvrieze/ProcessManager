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

package nl.adaptivity.process.processModel;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CombiningNamespaceContext;
import nl.adaptivity.xml.Namespace;
import nl.adaptivity.xml.SimpleNamespaceContext;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
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

  @Nullable private volatile XPathExpression path; // This is merely a cache.
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
    final SimpleNamespaceContext context = SimpleNamespaceContext.Companion.from(originalNSContext);
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
            xPath.setNamespaceContext(SimpleNamespaceContext.Companion.from(getOriginalNSContext()));
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

  public boolean deserializeAttribute(final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, final CharSequence attributeValue) {
    switch(attributeLocalName.toString()) {
      case "name":
        setName(StringUtil.toString(attributeValue));
        return true;
      case "path":
      case "xpath":
        pathString=StringUtil.toString(attributeValue);
        return true;
      case XMLConstants.XMLNS_ATTRIBUTE:
        return true;
      default:
        return false;
    }
  }

  @Override
  public void deserializeChildren(@NotNull final XmlReader in) throws XmlException {
    final NamespaceContext origContext = in.getNamespaceContext();
    super.deserializeChildren(in);
    final Map<String, String> namespaces = new TreeMap<>();
    final NamespaceContext gatheringNamespaceContext = new CombiningNamespaceContext(SimpleNamespaceContext.Companion.from(getOriginalNSContext()), new GatheringNamespaceContext(in.getNamespaceContext(), namespaces));
    visitNamespaces(gatheringNamespaceContext);
    if (namespaces.size() > 0) {
      addNamespaceContext(new SimpleNamespaceContext(namespaces));
    }
  }

  @NotNull
  protected static <T extends XPathHolder> T deserialize(@NotNull final XmlReader in, @NotNull final T result) throws
          XmlException {
    return nl.adaptivity.xml.XmlUtil.<T>deserializeHelper(result, in);
  }

  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    if (pathString!=null) {
      final Map<String, String> namepaces = new TreeMap<>();
      // Have a namespace that gathers those namespaces that are not known already in the outer context
      final NamespaceContext referenceContext = out.getNamespaceContext();
      // TODO streamline this, the right context should not require the filtering on the output context later.
      final NamespaceContext nsc = new GatheringNamespaceContext(new CombiningNamespaceContext(referenceContext, SimpleNamespaceContext.Companion
                                                                                                                         .from(getOriginalNSContext())), namepaces);
      visitXpathUsedPrefixes(pathString, nsc);
      for(final Entry<String, String> ns: namepaces.entrySet()) {
        if (! ns.getValue().equals(referenceContext.getNamespaceURI(ns.getKey()))) {
          out.namespaceAttr(ns.getKey(), ns.getValue());
        }
      }
      out.attribute(null, "xpath", null, pathString);

    }
    XmlWriterUtil.writeAttribute(out, "name", name);
  }

  @Override
  protected void visitNamespaces(final NamespaceContext baseContext) throws XmlException {
    path = null;
    if (pathString!=null) { visitXpathUsedPrefixes(pathString, baseContext); }
    super.visitNamespaces(baseContext);
  }

  @Override
  protected void visitNamesInAttributeValue(final NamespaceContext referenceContext, @NotNull final QName owner, @NotNull final QName attributeName, final CharSequence attributeValue) {
    if (Constants.MODIFY_NS_STR.equals(owner.getNamespaceURI()) && (XMLConstants.NULL_NS_URI.equals(attributeName.getNamespaceURI())||XMLConstants.DEFAULT_NS_PREFIX.equals(attributeName.getPrefix())) && "xpath".equals(attributeName.getLocalPart())) {
      visitXpathUsedPrefixes(attributeValue, referenceContext);
    }
  }

  protected static void visitXpathUsedPrefixes(@Nullable final CharSequence path, final NamespaceContext namespaceContext) {
    if (path!=null && path.length()>0) {
      try {
        final XPathFactory xpf = XPathFactory.newInstance();
        final XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(namespaceContext);
        xpath.compile(path.toString());
      } catch (@NotNull final XPathExpressionException e) {
        Logger.getLogger(XPathHolder.class.getSimpleName()).log(Level.WARNING, "The path used is not valid");
      }
    }
  }
}