package nl.adaptivity.process.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
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

import nl.adaptivity.messaging.HttpResponseException;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.AbstractBufferedEventReader;
import nl.adaptivity.util.xml.NodeEventReader;
import nl.adaptivity.util.xml.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


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
          if (! event.asCharacters().isIgnorableWhiteSpace()) {
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

    private XMLEvent peekStartElement(StartElement pElement) throws XMLStreamException {
      if (Constants.MODIFY_NS.equals(pElement.getName().getNamespaceURI())) {
        String localname = pElement.getName().getLocalPart();

        final Map<String, String> attributes = parseAttributes(aInput, pElement);

        switch (localname) {
          case "attribute":
            add(getAttribute(attributes));
            return peekFirst();
          case "element":
          case "value":
            processElement(attributes);
            return peekFirst();
          default:
            throw new XMLStreamException("Unsupported element: "+pElement.getName());
        }
      } else {
        add(pElement);
        return pElement;
      }
    }

    private void processElement(Map<String,String> pAttributes) throws XMLStreamException {
      String valueName = pAttributes.get("value");

      addAll(aContext.resolveElementValue(aXef, valueName));
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
    String resolveAttributeValue(String pValueName) throws XMLStreamException;
    String resolveAttributeName(String pValueName);

  }

  public static abstract class AbstractDataContext implements PETransformerContext {

    protected abstract ProcessData getData(String pValueName);

    @Override
    public List<XMLEvent> resolveElementValue(XMLEventFactory pXef, String pValueName) throws XMLStreamException {
      ProcessData data = getData(pValueName);
      List<XMLEvent> result = new ArrayList<>();
      NodeList nl = data.getNodeListValue();
      for(XMLEventReader dataReader = new NodeEventReader(nl);dataReader.hasNext();) {
        result.add(dataReader.nextEvent());
      }
      return result;
    }

    @Override
    public String resolveAttributeValue(String pValueName) throws XMLStreamException {
      ProcessData data = getData(pValueName);
      XMLEventReader dataReader = new NodeEventReader(data.getNodeListValue());
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
      return XmlUtil.toString(data.getNodeListValue());
    }

  }

  public static class ProcessDataContext extends AbstractDataContext {

    private ProcessData[] aProcessData;

    public ProcessDataContext(ProcessData[] pProcessData) {
      aProcessData = pProcessData;
    }

    @Override
    protected ProcessData getData(String pValueName) {
      for(ProcessData candidate: aProcessData) {
        if (pValueName.equals(candidate)) { return candidate; }
      }
      return null;
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
          Node v = transform((Node) obj);
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
            Node v = transform(n);
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

  public Node transform(Node pNode) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    Document document;
    try {
      document = dbf.newDocumentBuilder().newDocument();
      DOMResult result = new DOMResult(document);
      transform(new DOMSource(pNode), result);
      return document.getDocumentElement();
    } catch (ParserConfigurationException | XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  public void transform2(Source source, final Result result) throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newInstance();
    final XMLOutputFactory xof = XMLOutputFactory.newInstance();
    final XMLEventReader xer = createFilter(xif.createXMLEventReader(Sources.toReader(source)));
    final XMLEventWriter xew = xof.createXMLEventWriter(result);
    xew.add(xer);
  }

  private XMLEventReader createFilter(XMLEventReader pInput) {
    return new MyFilter(aContext, pInput);
  }

  public void transform(Source source, final Result result) throws XMLStreamException {
    final XMLInputFactory xif = XMLInputFactory.newInstance();
    final XMLOutputFactory xof = XMLOutputFactory.newInstance();
    // Use a reader as a DOMSource is not directly supported by stax for some stupid reason.
    final XMLEventReader xer = xif.createXMLEventReader(Sources.toReader(source));
    final XMLEventWriter xew = xof.createXMLEventWriter(result);

    final XMLEventFactory xef = XMLEventFactory.newInstance();

    while (xer.hasNext()) {
      final XMLEvent event = xer.nextEvent();
      if (event.isStartElement()) {
        final StartElement se = event.asStartElement();
        final QName eName = se.getName();
        if (Constants.MODIFY_NS.toString().equals(eName.getNamespaceURI())) {
          @SuppressWarnings("unchecked")
          final Iterator<Attribute> attributes = se.getAttributes();
          if (eName.getLocalPart().equals("attribute")) {
            writeAttribute(aContext, xef, xer, attributes, xew);
          } else if (eName.getLocalPart().equals("element")) {
            writeElement(aContext, xef, xer, attributes, xew);
          } else {
            throw new HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsupported activity modifier");
          }
        } else {
          xew.add(se);
        }
      } else {
        if (event.isCharacters()) {
          final Characters c = event.asCharacters();
          final String charData = c.getData();
          int i;
          for (i = 0; i < charData.length(); ++i) {
            if (!Character.isWhitespace(charData.charAt(i))) {
              break;
            }
          }
          if (i == charData.length()) {
            continue; // ignore it, and go to next event
          }
        }

        if (event instanceof Namespace) {

          final Namespace ns = (Namespace) event;
          if (!ns.getNamespaceURI().equals(Constants.MODIFY_NS)) {
            xew.add(event);
          }
        } else {
          try {
            xew.add(event);
          } catch (final IllegalStateException e) {
            final StringBuilder errorMessage = new StringBuilder("Error adding event: ");
            errorMessage.append(event.toString()).append(' ');
            if (event.isStartElement()) {
              errorMessage.append('<').append(event.asStartElement().getName()).append('>');
            } else if (event.isEndElement()) {
              errorMessage.append("</").append(event.asEndElement().getName()).append('>');
            }
            getLogger().log(Level.WARNING, errorMessage.toString(), e);

            throw e;
          }
        }
      }
    }
  }

  private static Logger getLogger() {
    Logger logger = Logger.getLogger(PETransformer.class.getName());
    return logger;
  }

  private static void writeElement(PETransformerContext pContext, XMLEventFactory pXef, final XMLEventReader in, final Iterator<Attribute> pAttributes, final XMLEventWriter out) throws XMLStreamException {
    String valueName = null;
    {
      while (pAttributes.hasNext()) {
        final Attribute attr = pAttributes.next();
        final String attrName = attr.getName().getLocalPart();
        if ("value".equals(attrName)) {
          valueName = attr.getValue();
        }
      }
    }
    {
      final XMLEvent ev = in.nextEvent();

      while (!ev.isEndElement()) {
        if (ev.isStartElement()) {
          throw new MessagingFormatException("Violation of schema");
        }
        if (ev.isAttribute()) {
          final Attribute attr = (Attribute) ev;
          final String attrName = attr.getName().getLocalPart();
          if ("value".equals(attrName)) {
            valueName = attr.getValue();
          }
        }
      }
    }
    if (valueName != null) {
      for(XMLEvent value: pContext.resolveElementValue(pXef, valueName)){
        out.add(value);
      }
    } else {
      throw new MessagingFormatException("Missing parameter name");
    }

  }

  private static void writeAttribute(PETransformerContext pContext, XMLEventFactory xef, final XMLEventReader in, final Iterator<Attribute> pAttributes, final XMLEventWriter out) throws XMLStreamException {
    String valueName = null;
    String paramName = null;
    {
      while (pAttributes.hasNext()) {
        final Attribute attr = pAttributes.next();
        final String attrName = attr.getName().getLocalPart();
        if ("value".equals(attrName)) {
          valueName = attr.getValue();
        } else if ("name".equals(attrName)) {
          paramName = attr.getValue();
        }
      }
    }
    {
      final XMLEvent ev = in.nextEvent();

      while (!ev.isEndElement()) {
        if (ev.isStartElement()) {
          throw new MessagingFormatException("Violation of schema");
        }
        if (ev.isAttribute()) {
          final Attribute attr = (Attribute) ev;
          final String attrName = attr.getName().getLocalPart();
          if ("value".equals(attrName)) {
            valueName = attr.getValue();
          } else if ("name".equals(attrName)) {
            paramName = attr.getValue();
          }
        }
      }
    }
    if (valueName != null) {
      if (paramName==null) {
        paramName = pContext.resolveAttributeName(valueName);
      }
      String value = pContext.resolveAttributeValue(valueName);
      out.add(xef.createAttribute(paramName, value));
    } else {
      throw new MessagingFormatException("Missing parameter name");
    }

  }

}
