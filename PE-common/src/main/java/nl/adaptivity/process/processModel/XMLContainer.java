package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.*;
import org.codehaus.stax2.XMLOutputFactory2;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stax.StAXResult;

import java.io.CharArrayReader;
import java.util.*;


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
@XmlDeserializer(XMLContainer.Factory.class)
public abstract class XMLContainer implements XmlSerializable{

  public static class Factory<T extends XMLContainer> implements XmlDeserializerFactory<T> {

    @Override
    public T deserialize(final XMLStreamReader in) throws XMLStreamException {
      return null;
    }
  }

  private char[] content;
  private SimpleNamespaceContext originalNSContext;

  @Override
  public void serialize(final XMLStreamWriter out) throws XMLStreamException {
    serializeStartElement(out);
    serializeAttributes(out);
    serializeBody(out);
    out.writeEndElement();
  }

  protected List<QName> findNamesInTag(XMLStreamReader source) {
    assert source.getEventType()==XMLStreamConstants.START_ELEMENT;
    ArrayList<QName> result = new ArrayList<>(1);
    result.add(source.getName());

    for(int i=source.getAttributeCount()-1; i>=0; --i ) {
      String ns = source.getAttributeNamespace(i);
      if (ns!=null && ns.length()>0) {
        result.add(source.getAttributeName(i));
      }
      Collection<QName> v;
      if (! (v= findNamesInAttributeValue(source.getAttributeNamespace(i), source.getAttributeLocalName(i), source.getAttributeValue(i))).isEmpty()){
        result.addAll(v);
      }
    }
    return result;
  }

  protected Collection<QName> findNamesInAttributeValue(final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    return Collections.emptyList();
  }

  protected List<QName> findNamesInTextContent(QName parent, CharSequence textContent) {
    ArrayList<QName> result = new ArrayList<>();



    return result;
  }

  private Set<QName> missingNamespaces(NamespaceContext baseContext) throws XMLStreamException {
    TreeSet<QName> result = new TreeSet<>();
    XMLInputFactory xif = XMLInputFactory.newFactory();
    XMLOutputFactory xof = XMLOutputFactory.newFactory();

    XMLStreamReader xsr = xif.createXMLStreamReader(new CharArrayReader(content));
    return missingNamespaces(baseContext, xsr, result, null);

  }

  private Set<QName> missingNamespaces(final NamespaceContext baseContext, final XMLStreamReader xsr, final TreeSet<QName> result, final QName parent) throws
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

          List<QName> namesInTag = findNamesInTag(xsr);
          for(QName name:namesInTag) {
            if(! Objects.equals(newBaseContext.getNamespaceURI(name.getPrefix()), name.getNamespaceURI())) {
              result.add(name);
            }
          }

          missingNamespaces(newBaseContext, xsr, result, xsr.getName());
          break;
        }
        case XMLStreamConstants.CHARACTERS: {
          List<QName> namesInTag = findNamesInTextContent(parent, xsr.getText());
          for(QName name:namesInTag) {
            if(! Objects.equals(baseContext.getNamespaceURI(name.getPrefix()), name.getNamespaceURI())) {
              result.add(name);
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
      XMLInputFactory xif = XMLInputFactory.newFactory();
      XMLOutputFactory xof = XMLOutputFactory.newFactory();

      XMLEventWriter xew = xof instanceof XMLOutputFactory2 ? ((XMLOutputFactory2)xof).createXMLEventWriter(pOut) : xof.createXMLEventWriter(new StAXResult(pOut));

      XMLEventFactory xef = XMLEventFactory.newFactory();



      pOut.getNamespaceContext();
      SimpleNamespaceContext nscontext = originalNSContext;
      List<Namespace> namespaces = new ArrayList<>(nscontext.size());
      for(int i=nscontext.size()-1; i>=0; --i) {
        String prefix = nscontext.getPrefix(i);
        String namespace = nscontext.getNamespaceURI(i);
        if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
          xew.setDefaultNamespace(namespace);
          namespaces.add(xef.createNamespace(namespace));
        } else if (! (XMLConstants.XML_NS_URI.equals(namespace)||
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace))){
          xew.setPrefix(prefix, namespace);
          namespaces.add(xef.createNamespace(prefix, namespace));
        }
      }

      XMLEventReader contentReader = xif.createXMLEventReader(new CharArrayReader(content));
      while(contentReader.hasNext()) { // loop until the first start element event
        XMLEvent event = contentReader.nextEvent();
        if (event.isStartElement()) { // Add all namespace declarations that are used to this element
          StartElement startEvent = event.asStartElement();
//          for(Iterator<Namespace> it = startEvent.getNamespaces(); it.hasNext(); ) {
//            namespaces.add(it.next());
//          }
          xew.add(xef.createStartElement(startEvent.getName(), startEvent.getAttributes(), namespaces.iterator()));
          break;
        } else {
          xew.add(contentReader);
        }
      }
      xew.add(contentReader);
    }

  }

  private void serializeAttributes(final XMLStreamWriter pOut) {
    // No attributes by default
  }

  protected abstract void serializeStartElement(final XMLStreamWriter pOut) throws XMLStreamException;


}
