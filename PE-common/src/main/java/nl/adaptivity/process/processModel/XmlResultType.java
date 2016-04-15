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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.processModel;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.engine.PETransformer;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.xml.XmlSerializable;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.SimpleNamespaceContext;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.Namespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;


@XmlDeserializer(XmlResultType.Factory.class)
public class XmlResultType extends XPathHolder implements IXmlResultType, XmlSerializable {

  public static class Factory implements XmlDeserializerFactory<XmlResultType> {

    @NotNull
    @Override
    public XmlResultType deserialize(@NotNull final XmlReader in) throws XmlException {
      return XmlResultType.deserialize(in);
    }

  }

  @NotNull
  public static XmlResultType deserialize(@NotNull final XmlReader in) throws XmlException {
    return deserialize(in, new XmlResultType());
  }

  public static final String ELEMENTLOCALNAME = "result";
  private static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  public XmlResultType() {}

  @Deprecated
  public XmlResultType(final String name, final String path, @Nullable final DocumentFragment content, final Iterable<nl.adaptivity.xml.Namespace> namespaceContext) {
    this(name, path, content==null ? null : XmlUtil.toString(content).toCharArray(), namespaceContext);
  }

  public XmlResultType(final String name, final String path, final char[] content, final Iterable<Namespace> originalNSContext) {
    super(content, originalNSContext, path, name);
  }

  @Override
  protected void serializeStartElement(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
  }

  @Override
  protected void serializeEndElement(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  public static XmlResultType get(IXmlResultType pImport) {
    if (pImport instanceof XmlResultType) { return (XmlResultType) pImport; }
    return new XmlResultType(pImport.getName(), pImport.getPath(), (char[]) null, pImport.getOriginalNSContext());
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  /**
   * Transform the given payload as specified by tag.
   * @param payload
   * @return
   */
  @NotNull
  @Override
  public ProcessData apply(final Node payload) {
    // TODO add support for variable and function resolvers.
    try {
      // shortcircuit missing path
      final ProcessData processData;
      if (getPath() == null || ".".equals(getPath())) {
        processData = new ProcessData(getName(), XmlUtil.nodeToFragment(payload));
      } else {
        processData = new ProcessData(getName(), XmlUtil.nodeListToFragment((NodeList) getXPath().evaluate(XmlUtil.ensureAttached(payload), XPathConstants.NODESET)));
      }
      final char[] content = getContent();
      if (content!=null && content.length>0) {
        final PETransformer   transformer = PETransformer.create(SimpleNamespaceContext.Companion.from(getOriginalNSContext()), processData);
        XmlReader             reader      = transformer.createFilter(getBodyStreamReader());

        if (reader.hasNext()) reader.next(); // Initialise the reader

        final CompactFragment transformed = XmlUtil.siblingsToFragment(reader);
        return new ProcessData(getName(), transformed);
      } else {
        return processData;
      }


    } catch (@NotNull XPathExpressionException | XmlException e) {
      throw new RuntimeException(e);
    }
  }

}
