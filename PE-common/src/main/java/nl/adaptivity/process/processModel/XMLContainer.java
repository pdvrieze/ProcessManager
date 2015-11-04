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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
public abstract class XMLContainer implements XmlSerializable {

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

  public XMLContainer(final NamespaceContext pOriginalNSContext, final char[] pContent) {
    setContent(pOriginalNSContext, pContent);
  }

  public XMLContainer(final Source pSource) throws XMLStreamException {
    setContent(pSource);
  }

  public void deserializeChildren(XMLStreamReader in) throws XMLStreamException {
    content = XmlUtil.childrenToCharArray(in);
  }

  public char[] getContent() {
    return content;
  }

  public NamespaceContext getOriginalNSContext() {
    return originalNSContext;
  }

  public void setContent(final NamespaceContext pOriginalNSContext, final char[] pContent) {
    content = pContent;
    updateNamespaceContext(pOriginalNSContext);
  }

  protected void updateNamespaceContext(final NamespaceContext pAdditionalContext) {
    Map<String, String> nsmap = new TreeMap<>();
    NamespaceContext context = originalNSContext == null ? pAdditionalContext : new CombiningNamespaceContext(pAdditionalContext, originalNSContext);
    try {
      visitNamespaces(new GatheringNamespaceContext(context, nsmap));
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }
    originalNSContext = new SimpleNamespaceContext(nsmap);
  }

  public void setContent(final Source pContent) throws XMLStreamException {
    content = XmlUtil.toCharArray(pContent);
    updateNamespaceContext(new SimpleNamespaceContext(new TreeMap<String, String>()));
  }

  void addNamespaceContext(final SimpleNamespaceContext pNamespaceContext) {
    originalNSContext = (originalNSContext==null || originalNSContext.size()==0) ? pNamespaceContext: originalNSContext.combine(pNamespaceContext);
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
    Map<String,String> missingNamespaces = new TreeMap<>();
    NamespaceContext context = new CombiningNamespaceContext(new GatheringNamespaceContext(getOriginalNSContext(), missingNamespaces), out.getNamespaceContext());
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

      XMLStreamReader xsr = new NamespaceAddingStreamReader(baseContext, XMLFragmentStreamReader.from(xif, new CharArrayReader(content)));

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
    if (content!=null && content.length>0) {
      XMLOutputFactory xof = XMLOutputFactory.newFactory();
      xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,true); // Make sure to repair namespaces when writing
      XMLEventWriter xew = xof instanceof XMLOutputFactory2 ? ((XMLOutputFactory2)xof).createXMLEventWriter(pOut) : xof.createXMLEventWriter(new StAXResult(pOut));

      XMLEventReader contentReader = getBodyEventReader();
      xew.add(contentReader);
    }

  }

  public XMLStreamReader getBodyStreamReader() throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    NamespaceContext nsContext = this.originalNSContext==null ? BASE_NS_CONTEXT : new CombiningNamespaceContext(BASE_NS_CONTEXT, originalNSContext);
    NamespaceAddingStreamReader streamReader = new NamespaceAddingStreamReader(nsContext, XMLFragmentStreamReader.from(xif, new CharArrayReader(content)));
    return streamReader;
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
