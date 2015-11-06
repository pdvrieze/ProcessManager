package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.GatheringNamespaceContext;
import org.codehaus.stax2.XMLOutputFactory2;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;

import java.io.CharArrayReader;
import java.util.*;
import java.util.Map.Entry;


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
public abstract class XMLContainer implements ExtXmlDeserializable {

  public static class Factory<T extends XMLContainer> implements XmlDeserializerFactory<T> {

    @Override
    public T deserialize(final XMLStreamReader in) throws XMLStreamException {
      return null;
    }
  }

  private static final SimpleNamespaceContext BASE_NS_CONTEXT = new SimpleNamespaceContext(new String[]{""}, new String[]{""});

  private char[] content;
  private SimpleNamespaceContext originalNSContext;

  public XMLContainer() {
  }

  public XMLContainer(final Iterable<Namespace> pOriginalNSContext, final char[] pContent) {
    setContent(pOriginalNSContext, pContent);
  }

  public XMLContainer(final Source pSource) throws XMLStreamException {
    setContent(pSource);
  }

  public void deserializeChildren(XMLStreamReader in) throws XMLStreamException {
    if (in.hasNext()) {
      if (in.next() != XMLStreamConstants.END_ELEMENT) {
        CompactFragment content = XmlUtil.siblingsToFragment(in);
        setContent(content);
      }
    }
  }

  @Override
  public void onBeforeDeserializeChildren(final XMLStreamReader pIn) {
    for(int i=pIn.getNamespaceCount()-1; i>=0; --i) {
      visitNamespace(pIn, pIn.getNamespacePrefix(i));
    }
  }

  public char[] getContent() {
    return content;
  }

  public Iterable<Namespace> getOriginalNSContext() {
    return originalNSContext !=null ? originalNSContext : Collections.<Namespace>emptyList();
  }

  public void setContent(final Iterable<Namespace> pOriginalNSContext, final char[] pContent) {
    originalNSContext = SimpleNamespaceContext.from(pOriginalNSContext);
    content = pContent;
  }

  public void setContent(final CompactFragment pContent) {
    setContent(pContent.getNamespaces(), pContent.getContent());
  }

  protected void updateNamespaceContext(final Iterable<Namespace> pAdditionalContext) {
    Map<String, String> nsmap = new TreeMap<>();
    SimpleNamespaceContext context = originalNSContext == null ? SimpleNamespaceContext.from(pAdditionalContext) : originalNSContext.combine(pAdditionalContext);
    try {
      visitNamespaces(new GatheringNamespaceContext(context, nsmap));
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }
    originalNSContext = new SimpleNamespaceContext(nsmap);
  }

  public void setContent(final Source pContent) throws XMLStreamException {
    setContent(Collections.<Namespace>emptyList(), XmlUtil.toCharArray(pContent));
  }

  void addNamespaceContext(final SimpleNamespaceContext pNamespaceContext) {
    originalNSContext = (originalNSContext==null || originalNSContext.size()==0) ? pNamespaceContext: originalNSContext.combine(pNamespaceContext);
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
    Map<String,String> missingNamespaces = new TreeMap<>();
    NamespaceContext context = new CombiningNamespaceContext(new GatheringNamespaceContext(originalNSContext, missingNamespaces), out.getNamespaceContext());
    visitNamespaces(context);

    serializeStartElement(out);
    for(Entry<String, String> name:missingNamespaces.entrySet()) {
      out.writeNamespace(name.getKey(), name.getValue());
    }
    serializeAttributes(out);
    serializeBody(out);
    out.writeEndElement();
  }

  protected static void visitNamespace(XMLStreamReader in, String prefix) {
    in.getNamespaceURI(prefix);
  }

  protected void visitNamesInElement(XMLStreamReader source) {
    assert source.getEventType()==XMLStreamConstants.START_ELEMENT;
    visitNamespace(source, source.getPrefix());

    for(int i=source.getAttributeCount()-1; i>=0; --i ) {
      String attrns = source.getNamespaceURI(source.getAttributePrefix(i));
      visitNamesInAttributeValue(source.getNamespaceContext(), source.getName(), attrns, source.getAttributeLocalName(i), source.getAttributeValue(i));
    }
  }

  protected void visitNamesInAttributeValue(final NamespaceContext referenceContext, final QName owner, final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    // By default there are no special attributes
  }

  protected List<QName> visitNamesInTextContent(QName parent, CharSequence textContent) {
    ArrayList<QName> result = new ArrayList<>();



    return result;
  }

  protected void visitNamespaces(NamespaceContext baseContext) throws XMLStreamException {
    if (content != null) {
      XMLInputFactory xif = XMLInputFactory.newFactory();

      XMLStreamReader xsr = new NamespaceAddingStreamReader(baseContext, XMLFragmentStreamReader.from(xif, new CharArrayReader(content), originalNSContext));

      visitNamespacesInContent(xsr, null);
    }
  }

  private void visitNamespacesInContent(final XMLStreamReader xsr, final QName parent) throws
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

  private void serializeBody(final XMLStreamWriter pOut) throws XMLStreamException {
    if (content !=null && content.length>0) {
      XMLOutputFactory xof = XMLOutputFactory.newFactory();
      xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,true); // Make sure to repair namespaces when writing
      XMLEventWriter xew = xof instanceof XMLOutputFactory2 ? ((XMLOutputFactory2)xof).createXMLEventWriter(pOut) : xof.createXMLEventWriter(new StAXResult(pOut));

      XMLEventReader contentReader = getBodyEventReader();
      xew.add(contentReader);
    }

  }

  public XMLStreamReader getBodyStreamReader() throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return XMLFragmentStreamReader.from(xif, new CharArrayReader(content), originalNSContext);
  }

  public XMLEventReader getBodyEventReader() throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return XmlUtil.filterSubstream(xif.createXMLEventReader(getBodyStreamReader()));
  }

  protected void serializeAttributes(final XMLStreamWriter pOut) throws XMLStreamException {
    // No attributes by default
  }

  protected abstract void serializeStartElement(final XMLStreamWriter pOut) throws XMLStreamException;


}
