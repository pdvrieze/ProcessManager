package nl.adaptivity.process.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.AbstractBufferedEventReader;
import nl.adaptivity.util.xml.NodeEventReader;
import nl.adaptivity.util.xml.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;


public class PETransformer {





  public static class MyFilter extends AbstractBufferedEventReader {

    private PETransformerContext aContext;
    private XMLEventReader aInput;
    private XMLEventFactory aXef;

    public MyFilter(PETransformerContext pContext, XMLEventReader pInput) {
      aContext = pContext;
      aInput = pInput;
      aXef = XMLEventFactory.newInstance();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Deletion is not supported on this read-only filter");
    }



    @Override
    public XMLEvent peek() throws XMLStreamException {
      if (! isPeekBufferEmpty()) {
        return peekFirst();
      }
      while(aInput.hasNext()) {
        XMLEvent event = aInput.nextEvent();
        if (event.isStartElement()) {
          return peekStartElement(event.asStartElement());
        } else if (event.isNamespace()){
          if (! Constants.MODIFY_NS.equals(((Namespace)event).getNamespaceURI())) {
            add(event);
            return event;
          }
        } else if (event.isCharacters()) {
          if (! isIgnorableWhiteSpace(event.asCharacters())) {
            add(event);
            return event;
          }
        } else {
          add(event);
          return event;
        }
      }
      return null;
    }

    private static boolean isIgnorableWhiteSpace(Characters pCharacters) {
      if (pCharacters.isIgnorableWhiteSpace()) {
        return true;
      }
      return XmlUtil.isXmlWhitespace(pCharacters.getData());
    }

    private XMLEvent peekStartElement(StartElement pElement) throws XMLStreamException {
      if (Constants.MODIFY_NS.toString().equals(pElement.getName().getNamespaceURI())) {
        String localname = pElement.getName().getLocalPart();

        final Map<String, String> attributes = parseAttributes(aInput, pElement);

        switch (localname) {
          case "attribute":
            add(getAttribute(attributes));
            readEndTag(pElement.getName());
            return peekFirst();
          case "element":
            processElement(attributes, false);
            readEndTag(pElement.getName());
            return peekFirst();
          case "value":
            processElement(attributes, true);
            readEndTag(pElement.getName());
            return peekFirst();
          default:
            throw new XMLStreamException("Unsupported element: "+pElement.getName());
        }
      } else {
        add(pElement);
        return pElement;
      }
    }

    private void readEndTag(QName pName) throws XMLStreamException {
      XMLEvent ev = aInput.nextTag();
      if (! (ev.isEndElement() && ev.asEndElement().getName().equals(pName))) {
        throw new XMLStreamException("Unexpected tag found ("+ev+")when expecting an end tag for "+pName);
      }
    }

    private void processElement(Map<String,String> pAttributes, boolean pHasDefault) throws XMLStreamException {
      String valueName = pAttributes.get("value");
      if (valueName==null) {
        if (pHasDefault) {
          addAll(aContext.resolveDefaultValue(aXef));
        } else {
          throw new XMLStreamException("This context does not allow for a missing value parameter");
        }
      } else {
        addAll(aContext.resolveElementValue(aXef, valueName));
      }
    }

    private static Map<String,String> parseAttributes(XMLEventReader pIn, StartElement pStartElement) throws XMLStreamException {
      TreeMap<String, String> result = new TreeMap<>();

      @SuppressWarnings("unchecked")
      Iterator<Attribute> attributes = pStartElement.getAttributes();
      while (attributes.hasNext()) {
        Attribute attribute = attributes.next();
        result.put(attribute.getName().getLocalPart(), attribute.getValue());
      }

      while(pIn.peek().isAttribute()) {
        Attribute attribute = (Attribute) pIn.nextEvent();
        result.put(attribute.getName().getLocalPart(), attribute.getValue());
      }
      return result;
    }

    private XMLEvent getAttribute(Map<String,String> pAttributes) throws XMLStreamException {
      String valueName = pAttributes.get("value");
      String paramName = pAttributes.get("name");

      if (valueName != null) {
        if (paramName==null) {
          paramName = aContext.resolveAttributeName(valueName);
        }
        String value = aContext.resolveAttributeValue(valueName);
        return aXef.createAttribute(paramName, value);
      } else {
        throw new MessagingFormatException("Missing parameter name");
      }
    }

    @Override
    public Object getProperty(String pName) throws IllegalArgumentException {
      return aInput.getProperty(pName);
    }

    @Override
    public void close() throws XMLStreamException {
      aInput.close();
      aInput = null;
      aContext = null;
      aXef = null;
      super.close();
    }

  }

  public interface PETransformerContext {

    List<XMLEvent> resolveElementValue(XMLEventFactory pXef, String pValueName) throws XMLStreamException;
    List<XMLEvent> resolveDefaultValue(XMLEventFactory pXef) throws XMLStreamException;
    String resolveAttributeValue(String pValueName) throws XMLStreamException;
    String resolveAttributeName(String pValueName);

  }

  public static abstract class AbstractDataContext implements PETransformerContext {

    protected abstract ProcessData getData(String pValueName);

    @Override
    public List<XMLEvent> resolveElementValue(XMLEventFactory pXef, String pValueName) throws XMLStreamException {
      ProcessData data = getData(pValueName);
      if (data==null) {
        throw new IllegalArgumentException("No value with name "+pValueName+" found");
      }
      return toEvents(data);
    }

    protected List<XMLEvent> toEvents(ProcessData data) throws XMLStreamException {
      List<XMLEvent> result = new ArrayList<>();
      DocumentFragment frag = data.getDocumentFragment();
      for(XMLEventReader dataReader = new NodeEventReader(frag);dataReader.hasNext();) {
        result.add(dataReader.nextEvent());
      }
      return result;
    }

    @Override
    public String resolveAttributeValue(String pValueName) throws XMLStreamException {
      ProcessData data = getData(pValueName);
      XMLEventReader dataReader = new NodeEventReader(data.getDocumentFragment());
      StringBuilder result = new StringBuilder();
      while (dataReader.hasNext()) {
        XMLEvent event = dataReader.nextEvent();
        switch (event.getEventType()) {
        case XMLEvent.ATTRIBUTE:
        case XMLEvent.DTD:
        case XMLEvent.START_ELEMENT:
        case XMLEvent.END_ELEMENT:
          throw new XMLStreamException("Unexpected node found while resolving attribute. Only CDATA allowed: "+event);
        case XMLEvent.CDATA:
        case XMLEvent.CHARACTERS:
          result.append(event.asCharacters().getData());
          break;
        case XMLEvent.START_DOCUMENT:
        case XMLEvent.END_DOCUMENT:
        case XMLEvent.COMMENT:
        case XMLEvent.PROCESSING_INSTRUCTION:
          break; // ignore
        default:
          throw new XMLStreamException("Unexpected node type: "+event);
        }
      }
      return result.toString();
    }

    @Override
    public String resolveAttributeName(String pValueName) {
      ProcessData data = getData(pValueName);
      return XmlUtil.toString(data.getDocumentFragment());
    }

  }

  public static class ProcessDataContext extends AbstractDataContext {

    private ProcessData[] aProcessData;
    private int aDefaultIdx;

    public ProcessDataContext(ProcessData... pProcessData) {
      aProcessData = pProcessData;
      aDefaultIdx = pProcessData.length==1 ? 0 : -1;
    }

    public ProcessDataContext(int pDefaultIdx, ProcessData... pProcessData) {
      assert pDefaultIdx>=-1 && pDefaultIdx<pProcessData.length;
      aProcessData = pProcessData;
      aDefaultIdx = pDefaultIdx;
    }

    @Override
    protected ProcessData getData(String pValueName) {
      for(ProcessData candidate: aProcessData) {
        if (pValueName.equals(candidate)) { return candidate; }
      }
      return null;
    }

    @Override
    public List<XMLEvent> resolveDefaultValue(XMLEventFactory pXef) throws XMLStreamException {
      return toEvents(aProcessData[aDefaultIdx]);
    }

  }

  private final PETransformerContext aContext;

  private PETransformer(PETransformerContext pContext) {
    aContext = pContext;
  }

  public static PETransformer create(ProcessData... pProcessData) {
    return new PETransformer(new ProcessDataContext(pProcessData));
  }

  public static PETransformer create(PETransformerContext pContext) {
    return new PETransformer(pContext);
  }

  public List<Node> transform(List<? extends Object> pContent) {
    try {
      Document document = null;
      ArrayList<Node> result = new ArrayList<>(pContent.size());
      for(Object obj: pContent) {
        if (obj instanceof CharSequence) {
          if (document == null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            document = dbf.newDocumentBuilder().newDocument();
          }
          result.add(document.createTextNode(obj.toString()));
        } else if (obj instanceof Node) {
          if (document==null) { document = ((Node) obj).getOwnerDocument(); }
          DocumentFragment v = transform((Node) obj);
          if (v!=null) {
            result.add(v);
          }
        } else if (obj instanceof JAXBElement<?>) {
          if (document == null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            document = dbf.newDocumentBuilder().newDocument();
          }
          JAXBElement<?> jbe = (JAXBElement<?>) obj;
          final DocumentFragment df = document.createDocumentFragment();
          DOMResult domResult = new DOMResult(df);
          JAXB.marshal(jbe, domResult);
          for(Node n = df.getFirstChild(); n!=null; n=n.getNextSibling()) {
            DocumentFragment v = transform(n);
            if (v!=null) {
              result.add(v);
            }
          }
        } else if (obj!=null) {
          throw new IllegalArgumentException("The node "+obj.toString()+" of type "+obj.getClass()+" is not understood");
        }
      }
      return result;
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  public DocumentFragment transform(Node pNode) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    Document document;
    try {
      document = dbf.newDocumentBuilder().newDocument();
      DocumentFragment fragment = document.createDocumentFragment();
      DOMResult result = new DOMResult(fragment);
      transform(new DOMSource(pNode), result);
      return fragment;
    } catch (ParserConfigurationException | XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  public void transform(Source source, final Result result) throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newInstance();
    final XMLOutputFactory xof = XMLOutputFactory.newInstance();
    final XMLEventReader xer = createFilter(xif.createXMLEventReader(Sources.toReader(source)));
    final XMLEventWriter xew = xof.createXMLEventWriter(result);
    xew.add(xer);
  }

  private XMLEventReader createFilter(XMLEventReader pInput) {
    return new MyFilter(aContext, pInput);
  }

}
