package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.GatheringNamespaceContext;
import org.codehaus.stax2.XMLOutputFactory2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;

import java.io.CharArrayReader;
import java.util.*;


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
public abstract class XMLContainer implements ExtXmlDeserializable {

  public static class Factory<T extends XMLContainer> implements XmlDeserializerFactory<T> {

    @Nullable
    @Override
    public T deserialize(final XMLStreamReader in) throws XMLStreamException {
      return null;
    }
  }

  private static final SimpleNamespaceContext BASE_NS_CONTEXT = new SimpleNamespaceContext(new String[]{""}, new String[]{""});

  private char[] content;
  @Nullable private SimpleNamespaceContext originalNSContext;

  public XMLContainer() {
  }

  public XMLContainer(final Iterable<Namespace> originalNSContext, final char[] content) {
    setContent(originalNSContext, content);
  }

  public XMLContainer(final Source source) throws XMLStreamException {
    setContent(source);
  }

  public void deserializeChildren(@NotNull final XMLStreamReader in) throws XMLStreamException {
    if (in.hasNext()) {
      if (in.next() != XMLStreamConstants.END_ELEMENT) {
        final CompactFragment content = XmlUtil.siblingsToFragment(in);
        setContent(content);
      }
    }
  }

  @Override
  public void onBeforeDeserializeChildren(@NotNull final XMLStreamReader in) {
    for(int i=in.getNamespaceCount()-1; i>=0; --i) {
      visitNamespace(in, in.getNamespacePrefix(i));
    }
  }

  public char[] getContent() {
    return content;
  }

  @Nullable
  public Iterable<Namespace> getOriginalNSContext() {
    return originalNSContext !=null ? originalNSContext : Collections.<Namespace>emptyList();
  }

  public void setContent(final Iterable<Namespace> originalNSContext, final char[] content) {
    this.originalNSContext = SimpleNamespaceContext.from(originalNSContext);
    this.content = content;
  }

  public void setContent(@NotNull final CompactFragment content) {
    setContent(content.getNamespaces(), content.getContent());
  }

  protected void updateNamespaceContext(final Iterable<Namespace> additionalContext) {
    final Map<String, String> nsmap = new TreeMap<>();
    final SimpleNamespaceContext context = originalNSContext == null ? SimpleNamespaceContext.from(additionalContext) : originalNSContext.combine(additionalContext);
    try {
      final GatheringNamespaceContext gatheringNamespaceContext = new GatheringNamespaceContext(context, nsmap);
      visitNamespaces(gatheringNamespaceContext);
    } catch (@NotNull final XMLStreamException e) {
      throw new RuntimeException(e);
    }
    originalNSContext = new SimpleNamespaceContext(nsmap);
  }

  public void setContent(final Source content) throws XMLStreamException {
    setContent(XmlUtil.siblingsToFragment(XmlUtil.createXMLStreamReader(XMLInputFactory.newFactory(), content)));
  }

  void addNamespaceContext(@NotNull final SimpleNamespaceContext namespaceContext) {
    originalNSContext = (originalNSContext==null || originalNSContext.size()==0) ? namespaceContext: originalNSContext.combine(namespaceContext);
  }

  @Override
  public void serialize(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    serializeStartElement(out);
    serializeAttributes(out);
    final NamespaceContext outNs = out.getNamespaceContext();
    if (originalNSContext!=null) {
      for (final Namespace ns : originalNSContext) {
        if (!ns.getNamespaceURI().equals(outNs.getNamespaceURI(ns.getPrefix()))) {
          out.writeNamespace(ns.getPrefix(), ns.getNamespaceURI());
        }
      }
    }
    serializeBody(out);
    out.writeEndElement();
  }

  protected static void visitNamespace(@NotNull final XMLStreamReader in, final String prefix) {
    in.getNamespaceURI(prefix);
  }

  protected void visitNamesInElement(@NotNull final XMLStreamReader source) {
    assert source.getEventType()==XMLStreamConstants.START_ELEMENT;
    visitNamespace(source, source.getPrefix());

    for(int i=source.getAttributeCount()-1; i>=0; --i ) {
      final QName attrName = source.getAttributeName(i);
      visitNamesInAttributeValue(source.getNamespaceContext(), source.getName(), attrName, source.getAttributeValue(i));
    }
  }

  protected void visitNamesInAttributeValue(final NamespaceContext referenceContext, final QName owner, final QName attributeName, final String attributeValue) {
    // By default there are no special attributes
  }

  @SuppressWarnings("UnusedReturnValue")
  @NotNull
  protected List<QName> visitNamesInTextContent(final QName parent, final CharSequence textContent) {
    return Collections.emptyList();
  }

  protected void visitNamespaces(final NamespaceContext baseContext) throws XMLStreamException {
    if (content != null) {
      final XMLInputFactory xif = XMLInputFactory.newFactory();

      final XMLStreamReader xsr = new NamespaceAddingStreamReader(baseContext, XMLFragmentStreamReader.from(xif, new CharArrayReader(content), originalNSContext));

      visitNamespacesInContent(xsr, null);
    }
  }

  private void visitNamespacesInContent(@NotNull final XMLStreamReader xsr, final QName parent) throws
          XMLStreamException {
    while (xsr.hasNext()) {
      switch(xsr.next()) {
        case XMLStreamConstants.START_ELEMENT: {
          visitNamesInElement(xsr);
          visitNamespacesInContent(xsr, xsr.getName());
          break;
        }
        case XMLStreamConstants.CHARACTERS: {
          visitNamesInTextContent(parent, xsr.getText());
          break;
        }

        default:
          //ignore
      }
    }
  }

  private void serializeBody(@NotNull final XMLStreamWriter out) throws XMLStreamException {
    if (content !=null && content.length>0) {
      final XMLOutputFactory xof = XMLOutputFactory.newFactory();
      xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,true); // Make sure to repair namespaces when writing
      final XMLEventWriter xew = xof instanceof XMLOutputFactory2 ? ((XMLOutputFactory2)xof).createXMLEventWriter(out) : xof.createXMLEventWriter(new StAXResult(out));

      final XMLEventReader contentReader = getBodyEventReader();
      xew.add(contentReader);
    }

  }

  @NotNull
  public XMLStreamReader getBodyStreamReader() throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    return XMLFragmentStreamReader.from(xif, new CharArrayReader(content), originalNSContext);
  }

  public XMLEventReader getBodyEventReader() throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    return XmlUtil.filterSubstream(xif.createXMLEventReader(getBodyStreamReader()));
  }

  protected void serializeAttributes(final XMLStreamWriter out) throws XMLStreamException {
    // No attributes by default
  }

  protected abstract void serializeStartElement(final XMLStreamWriter out) throws XMLStreamException;


}
