package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.GatheringNamespaceContext;
import org.codehaus.stax2.XMLOutputFactory2;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;

import java.io.CharArrayReader;
import java.util.*;
import java.util.Map.Entry;


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
@XmlDeserializer(XMLContainer.Factory.class)
public abstract class XMLContainer implements XmlSerializable {

  public static class Factory<T extends XMLContainer> implements XmlDeserializerFactory<T> {

    @Override
    public T deserialize(final XMLStreamReader in) throws XMLStreamException {
      return null;
    }
  }

  private char[] content;
  private NamespaceContext originalNSContext;

  public XMLContainer() {
  }

  public XMLContainer(final NamespaceContext pOriginalNSContext, final char[] pContent) {
    setContent(pOriginalNSContext, pContent);
  }

  public XMLContainer(final Source pSource) throws XMLStreamException {
    setContent(pSource);
  }

  public char[] getContent() {
    return content;
  }

  public NamespaceContext getOriginalNSContext() {
    return originalNSContext;
  }

  public void setContent(final NamespaceContext pOriginalNSContext, final char[] pContent) {
    content = pContent;
    TreeMap<String, String> nsmap = new TreeMap<>();
    GatheringNamespaceContext gatheringNamespaceContext = new GatheringNamespaceContext(pOriginalNSContext, nsmap);

    try {
      missingNamespaces(gatheringNamespaceContext);
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }

    originalNSContext = new SimpleNamespaceContext(nsmap);
  }

  public void setContent(final Source pContent) throws XMLStreamException {
    content = XmlUtil.toCharArray(pContent);
    Map<String, String> nsmap = new TreeMap<>();

    try {
      XMLInputFactory xif = XMLInputFactory.newFactory();
      XMLStreamReader xsr = xif.createXMLStreamReader(pContent);
      nsmap = missingNamespaces(new SimpleNamespaceContext(new TreeMap<String, String>()), xsr, nsmap, null);
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }

    originalNSContext = new SimpleNamespaceContext(nsmap);
  }

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {

    Map<String,String> missingNamespaces = missingNamespaces(out.getNamespaceContext());
    serializeStartElement(out);
    for(Entry<String, String> name:missingNamespaces.entrySet()) {
      if (XMLConstants.DEFAULT_NS_PREFIX.equals(name.getKey())||name.getKey()==null) {
        out.writeDefaultNamespace(name.getValue());
      } else {
        out.writeNamespace(name.getKey(), name.getValue());
      }
    }
    serializeAttributes(out);
    serializeBody(out);
    out.writeEndElement();
  }

  protected Map<String,String> findNamesInTag(XMLStreamReader source) {
    assert source.getEventType()==XMLStreamConstants.START_ELEMENT;
    Map<String,String> result = new TreeMap<>();
    result.put(source.getPrefix(), source.getNamespaceURI());

    for(int i=source.getAttributeCount()-1; i>=0; --i ) {
      String ns = source.getAttributeNamespace(i);
      if (ns!=null && ns.length()>0) {
        result.put(source.getAttributePrefix(i), source.getAttributeNamespace(i));
      }
      Map<String, String> v;
      if (! (v= findNamesInAttributeValue(source.getNamespaceContext(), source.getName(), source.getAttributeNamespace(i), source.getAttributeLocalName(i), source.getAttributeValue(i))).isEmpty()){
        result.putAll(v);
      }
    }
    return result;
  }

  protected Map<String,String> findNamesInAttributeValue(final NamespaceContext referenceContext, final QName owner, final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    return Collections.emptyMap();
  }

  protected List<QName> findNamesInTextContent(QName parent, CharSequence textContent) {
    ArrayList<QName> result = new ArrayList<>();



    return result;
  }

  private Map<String, String> missingNamespaces(NamespaceContext baseContext) throws XMLStreamException {
    Map<String, String> result = new TreeMap<>();
    XMLInputFactory xif = XMLInputFactory.newFactory();
    XMLOutputFactory xof = XMLOutputFactory.newFactory();

    XMLStreamReader xsr = xif.createXMLStreamReader(new CharArrayReader(content));
    return missingNamespaces(baseContext, xsr, result, null);

  }

  public Map<String, String> undeclaredNamespaces() throws XMLStreamException {
    return missingNamespaces(new SimpleNamespaceContext(new String[0], new String[0]));
  }

  private Map<String, String> missingNamespaces(final NamespaceContext baseContext, final XMLStreamReader xsr, final Map<String, String> result, final QName parent) throws
          XMLStreamException {
    while (xsr.hasNext()) {
      switch(xsr.next()) {
        case XMLStreamConstants.START_ELEMENT: {
          NamespaceContext newBaseContext;
          if (xsr.getNamespaceCount() > 0) {
            String[] prefixes = new String[xsr.getNamespaceCount()];
            String[] namespaces = new String[prefixes.length];
            for(int i=namespaces.length-1; i>=0; --i) {
              prefixes[i]=xsr.getNamespacePrefix(i);
              namespaces[i]=xsr.getNamespaceURI(i);
            }
            newBaseContext = new CombiningNamespaceContext(new SimpleNamespaceContext(prefixes, namespaces), baseContext);
          } else {
            newBaseContext=baseContext;
          }

          Map<String, String> namesInTag = findNamesInTag(xsr);
          for(Entry<String, String> name:namesInTag.entrySet()) {
            if(! Objects.equals(newBaseContext.getNamespaceURI(name.getKey()), name.getValue())) {
              result.put(name.getKey(), name.getValue());
            }
          }

          missingNamespaces(newBaseContext, xsr, result, xsr.getName());
          break;
        }
        case XMLStreamConstants.CHARACTERS: {
          List<QName> namesInTag = findNamesInTextContent(parent, xsr.getText());
          for(QName name:namesInTag) {
            if(! Objects.equals(baseContext.getNamespaceURI(name.getPrefix()), name.getNamespaceURI())) {
              result.put(name.getPrefix(), name.getNamespaceURI());
            }
          }
          break;
        }

        default:
          //ignore
      }
    }

    return result;

  }

  private void serializeBody(final XMLStreamWriter pOut) throws XMLStreamException {
    if (content!=null && content.length>0) {
      XMLOutputFactory xof = XMLOutputFactory.newFactory();
      XMLEventWriter xew = xof instanceof XMLOutputFactory2 ? ((XMLOutputFactory2)xof).createXMLEventWriter(pOut) : xof.createXMLEventWriter(new StAXResult(pOut));

      XMLEventReader contentReader = getBodyEventReader();
      xew.add(contentReader);
    }

  }

  public XMLStreamReader getBodyStreamReader() throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return new NamespaceAddingStreamReader(originalNSContext,xif.createXMLStreamReader(new CharArrayReader(content)));
  }

  public XMLEventReader getBodyEventReader() throws XMLStreamException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    return xif.createXMLEventReader(getBodyStreamReader());
  }

  protected void serializeAttributes(final XMLStreamWriter pOut) throws XMLStreamException {
    // No attributes by default
  }

  protected abstract void serializeStartElement(final XMLStreamWriter pOut) throws XMLStreamException;


}
