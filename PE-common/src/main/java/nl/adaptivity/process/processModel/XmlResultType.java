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
import nl.adaptivity.util.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;


@XmlDeserializer(XmlResultType.Factory.class)
public class XmlResultType extends XPathHolder implements IXmlResultType, XmlSerializable {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public XmlResultType deserialize(@NotNull final XMLStreamReader in) throws XMLStreamException {
      return XmlResultType.deserialize(in);
    }

  }

  @NotNull
  public static XmlResultType deserialize(@NotNull final XMLStreamReader in) throws XMLStreamException {
    return deserialize(in, new XmlResultType());
  }

  public static final String ELEMENTLOCALNAME = "result";
  private static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  public XmlResultType() {}

  @Deprecated
  public XmlResultType(final String name, final String path, @Nullable final DocumentFragment content, final Iterable<Namespace> namespaceContext) {
    this(name, path, content==null ? null : XmlUtil.toString(content).toCharArray(), namespaceContext);
  }

  public XmlResultType(final String name, final String path, final char[] content, final Iterable<Namespace> originalNSContext) {
    super(content, originalNSContext, path, name);
  }

  @Override
  protected void serializeStartElement(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
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
        processData = new ProcessData(getName(), XmlUtil.nodeListToFragment((NodeList) getXPath().evaluate(payload, XPathConstants.NODESET)));
      }
      final char[] content = getContent();
      if (content!=null && content.length>0) {
        final PETransformer transformer = PETransformer.create(SimpleNamespaceContext.from(getOriginalNSContext()), processData);
        final CompactFragment transformed = XmlUtil.siblingsToFragment(transformer.createFilter(getBodyStreamReader()));
        return new ProcessData(getName(), transformed);
      } else {
        return processData;
      }


    } catch (@NotNull XPathExpressionException | XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

}
