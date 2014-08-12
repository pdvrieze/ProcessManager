package nl.adaptivity.process.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import nl.adaptivity.messaging.HttpResponseException;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.XmlUtil;


public class PETransformer {




  private static class MyFilter implements XMLEventReader {

    private PETransformerContext aContext;
    private XMLEventReader aInput;
    private ArrayDeque<XMLEvent> aPeekBuffer;

    public MyFilter(PETransformerContext pContext, XMLEventReader pInput) {
      aContext = pContext;
      aInput = pInput;
      aPeekBuffer = new ArrayDeque<>();
    }

    @Override
    public Object next() {
      try {
        return nextEvent();
      } catch (XMLStreamException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Deletion is not supported on this read-only filter");
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
      if (! aPeekBuffer.isEmpty()) {
        return aPeekBuffer.removeFirst();
      }
      if (! hasNext()) { throw new NoSuchElementException(); }
      peek();
      return aPeekBuffer.removeFirst();
    }

    @Override
    public boolean hasNext() {
      if (! aPeekBuffer.isEmpty()) { return true; }
      if (! aInput.hasNext()) { return false; }
      try {
        return peek()!=null;
      } catch (XMLStreamException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
      if (! aPeekBuffer.isEmpty()) {
        return aPeekBuffer.getFirst();
      }
      if (! aInput.hasNext()) { return null; }
      XMLEvent event = aInput.nextEvent();
      if (event.isStartElement()) {
        return peekStartElement(event.asStartElement());
      } else if (event.isNamespace()){
        if (! Constants.MODIFY_NS.equals(((Namespace)event).getNamespaceURI())) {
          aPeekBuffer.add(event);
        }
      } else {
        aPeekBuffer.add(event);
        return event;
      }
    }

    private XMLEvent peekStartElement(StartElement pAsStartElement) {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getElementText() throws XMLStreamException {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Object getProperty(String pName) throws IllegalArgumentException {
      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void close() throws XMLStreamException {
      // TODO Auto-generated method stub
      //
      throw new UnsupportedOperationException("Not yet implemented");
    }

  }

  public interface PETransformerContext {

    List<XMLEvent> resolveElementValue(XMLEventFactory pXef, String pValueName);
    String resolveAttributeValue(String pValueName);
    String resolveAttributeName(String pValueName);

  }

  public static class ProcessDataContext implements PETransformerContext {

    private ProcessData[] aProcessData;

    public ProcessDataContext(ProcessData[] pProcessData) {
      aProcessData = pProcessData;
    }

    @Override
    public List<XMLEvent> resolveElementValue(XMLEventFactory pXef, String pValueName) {
      ProcessData data = getData(pValueName);
      List<XMLEvent> result = new ArrayList<>();
      NodeList nl = data.getNodeListValue();
      for(int i=0; i<nl.getLength(); ++i) {
        pXef;
      }
      XMLInputFactory xef = XMLInputFactory.newInstance();
      xef.

      return result;
    }

    private ProcessData getData(String pValueName) {
      for(ProcessData candidate: aProcessData) {
        if (pValueName.equals(candidate)) { return candidate; }
      }
      return null;
    }

    @Override
    public String resolveAttributeValue(String pValueName) {
      ProcessData data = getData(pValueName);

      // TODO Auto-generated method stub
      // return null;
      throw new UnsupportedO perationException("Not yet implemented");
    }

    @Override
    public String resolveAttributeName(String pValueName) {
      ProcessData data = getData(pValueName);
      return XmlUtil.toString(data.getNodeListValue());
    }

  }

  private final PETransformerContext aContext;

  public PETransformer(PETransformerContext pContext) {
    aContext = pContext;
  }

  public static PETransformer create(ProcessData... pProcessData) {
    return new PETransformer(new ProcessDataContext(pProcessData));
  }

  public List<Node> transform(List<? extends Node> pContent) {
    ArrayList<Node> result = new ArrayList<>(pContent.size());
    for(Node node: pContent) {
      Node v = transform(node);
      if (v!=null) {
        result.add(v);
      }
    }
    return result;
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
      out.add(pContext.resolveElementValue(pXef, valueName));
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
