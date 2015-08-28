package nl.adaptivity.process.engine;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.util.xml.AbstractBufferedEventReader;
import nl.adaptivity.util.xml.CombiningNamespaceContext;
import nl.adaptivity.util.xml.NodeEventReader;
import nl.adaptivity.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.xpath.*;

import java.util.*;


public class PETransformer {





  public static class MyFilter extends AbstractBufferedEventReader {

    private PETransformerContext aContext;
    private final NamespaceContext aNamespaceContext;
    private XMLStreamReader aStreamInput;
    private XMLEventReader aEventInput;
    private XMLEventFactory aXef;
    private boolean mRemoveWhitespace;

    public MyFilter(PETransformerContext pContext, NamespaceContext pNamespaceContext, XMLStreamReader pInput, boolean pRemoveWhitespace) throws
            XMLStreamException {
      aContext = pContext;
      aNamespaceContext = pNamespaceContext;
      aStreamInput = pInput;
      aEventInput = XMLInputFactory.newFactory().createXMLEventReader(aStreamInput);
      aXef = XMLEventFactory.newInstance();
      mRemoveWhitespace = pRemoveWhitespace;
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
      while(aEventInput.hasNext()) {
        XMLEvent event = aEventInput.nextEvent();
        if (event.isStartElement()) {
          return peekStartElement(event.asStartElement());
        } else if (event.isNamespace()){
          if (! Constants.MODIFY_NS_STR.equals(((Namespace)event).getNamespaceURI())) {
            add(event);
            return event;
          }
        } else if (event.isCharacters()) {
          if (! (mRemoveWhitespace && isIgnorableWhiteSpace(event.asCharacters()))) {
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
      if (Constants.MODIFY_NS_STR.toString().equals(pElement.getName().getNamespaceURI())) {
        String localname = pElement.getName().getLocalPart();

        final Map<String, String> attributes = parseAttributes(aEventInput, pElement);

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
        boolean filterAttributes = false;
        List<Attribute> newAttrs = new ArrayList<>();
        for(Iterator<Attribute> it = pElement.getAttributes(); it.hasNext(); ) {
          Attribute attr = it.next();
          if (attr.isNamespace() && Constants.MODIFY_NS_STR.equals(attr.getValue())) {
            filterAttributes=true;
          } else {
            newAttrs.add(attr);
          }
        }
        List<Namespace> newNamespaces = new ArrayList<>();
        for(Iterator<Namespace> it = pElement.getNamespaces(); it.hasNext();) {
          Namespace ns = it.next();
          if (Constants.MODIFY_NS_STR.equals(ns.getNamespaceURI())) {
            filterAttributes=true;
          } else {
            newNamespaces.add(ns);
          }
        }
        if (filterAttributes) {
          add(aXef.createStartElement(pElement.getName(), newAttrs.iterator(), newNamespaces.iterator()));
        } else {
          add(pElement);
        }
        return pElement;
      }
    }

    private void readEndTag(QName pName) throws XMLStreamException {
      XMLEvent ev = aEventInput.nextTag();
      if (! (ev.isEndElement() && ev.asEndElement().getName().equals(pName))) {
        throw new XMLStreamException("Unexpected tag found ("+ev+")when expecting an end tag for "+pName);
      }
    }

    private void processElement(Map<String,String> pAttributes, boolean pHasDefault) throws XMLStreamException {
      String valueName = pAttributes.get("value");
      String xpath = pAttributes.get("xpath");
      try {
        if (valueName == null) {
          if (pHasDefault) {
            addAll(applyXpath(aContext.resolveDefaultValue(aXef), xpath));
          } else {
            throw new XMLStreamException("This context does not allow for a missing value parameter");
          }
        } else {
          addAll(applyXpath(aContext.resolveElementValue(aXef, valueName), xpath));
        }
      } catch (XPathExpressionException|ParserConfigurationException e) {
        throw new XMLStreamException(e);
      }
    }

    private Collection<? extends XMLEvent> applyXpath(final List<XMLEvent> pXMLEvents, final String pXpath) throws
            XPathExpressionException, XMLStreamException, ParserConfigurationException {
      if (pXpath==null || ".".equals(pXpath)) {
        return pXMLEvents;
      }
      // TODO add a function resolver
      XPath rawPath = XPathFactory.newInstance().newXPath();
      // Do this better
      if (aNamespaceContext==null) {
        rawPath.setNamespaceContext(aStreamInput.getNamespaceContext());
      } else {
        rawPath.setNamespaceContext(new CombiningNamespaceContext(aStreamInput.getNamespaceContext(), aNamespaceContext));
      }
      XPathExpression xpath = rawPath.compile(pXpath);
      ArrayList<XMLEvent> result = new ArrayList<>();
      XMLOutputFactory xof = XMLOutputFactory.newFactory();
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      DocumentFragment eventFragment = db.newDocument().createDocumentFragment();
      DOMResult domResult= new DOMResult(eventFragment);
      XMLEventWriter xew = xof.createXMLEventWriter(domResult);
      for(XMLEvent event: pXMLEvents) {
        xew.add(event);
      }
      xew.close();
      NodeList applicationResult = (NodeList) xpath.evaluate(eventFragment, XPathConstants.NODESET);
      if (applicationResult.getLength()>0) {
        result.addAll(toEvents(new ProcessData("--xpath result--", applicationResult)));
      }
      return result;
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
      String xpath = pAttributes.get("xpath");
      String paramName = pAttributes.get("name");

      if (valueName != null) {
        if (paramName==null) {
          paramName = aContext.resolveAttributeName(valueName);
        }
        String value = aContext.resolveAttributeValue(valueName, xpath);
        return aXef.createAttribute(paramName, value);
      } else {
        throw new MessagingFormatException("Missing parameter name");
      }
    }

    @Override
    public Object getProperty(String pName) throws IllegalArgumentException {
      return aStreamInput.getProperty(pName);
    }

    @Override
    public void close() throws XMLStreamException {
      aEventInput.close();
      aStreamInput.close();
      aEventInput = null;
      aStreamInput = null;
      aContext = null;
      aXef = null;
      super.close();
    }

  }

  public interface PETransformerContext {

    List<XMLEvent> resolveElementValue(XMLEventFactory pXef, String pValueName) throws XMLStreamException;
    List<XMLEvent> resolveDefaultValue(XMLEventFactory pXef) throws XMLStreamException; //Just return DOM, not events (that then need to be dom-ified)
    String resolveAttributeValue(String pValueName, final String pXpath) throws XMLStreamException;
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

    @Override
    public String resolveAttributeValue(String pValueName, final String pXpath) throws XMLStreamException {
      ProcessData data = getData(pValueName);
      if (data==null) {
        throw new IllegalArgumentException("No data value with name "+pValueName+" found");
      }
      XMLEventReader dataReader = new NodeEventReader(data.getDocumentFragment());
      StringBuilder result = new StringBuilder();
      while (dataReader.hasNext()) {
        XMLEvent event = dataReader.nextEvent();
        switch (event.getEventType()) {
        case XMLEvent.ATTRIBUTE:
        case XMLEvent.DTD:
        case XMLEvent.START_ELEMENT:
        case XMLEvent.END_ELEMENT:
          throw new XMLStreamException("Unexpected node found while resolving attribute. Only CDATA allowed: ("+event.getClass().getSimpleName()+") "+event.getEventType());
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
      if (pProcessData==null) {
        aProcessData= new ProcessData[0];
        aDefaultIdx=0;
      } else {
        aProcessData = pProcessData;
        aDefaultIdx = pProcessData.length==1 ? 0 : -1;
      }
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
      if (aProcessData.length==0 || aProcessData[aDefaultIdx]==null) { return Collections.emptyList(); }
      return toEvents(aProcessData[aDefaultIdx]);
    }

  }

  private final PETransformerContext aContext;
  private final NamespaceContext aNamespaceContext;
  private final boolean mRemoveWhitespace;

  private PETransformer(PETransformerContext pContext, NamespaceContext pNamespaceContext, boolean pRemoveWhitespace) {
    aContext = pContext;
    aNamespaceContext = pNamespaceContext;
    mRemoveWhitespace = pRemoveWhitespace;
  }

  public static PETransformer create(NamespaceContext pNamespaceContext, boolean pRemoveWhitespace, ProcessData... pProcessData) {
    return new PETransformer(new ProcessDataContext(pProcessData), pNamespaceContext, pRemoveWhitespace);
  }

  @Deprecated
  public static PETransformer create(boolean pRemoveWhitespace, ProcessData... pProcessData) {
    return create(null, pRemoveWhitespace, pProcessData);
  }

  public static PETransformer create(NamespaceContext pNamespaceContext, ProcessData... pProcessData) {
    return create(pNamespaceContext, true, pProcessData);
  }

  @Deprecated
  public static PETransformer create(ProcessData... pProcessData) {
    return create(null, true, pProcessData);
  }

  @Deprecated
  public static PETransformer create(PETransformerContext pContext) {
    return create(pContext, true);
  }

  public static PETransformer create(NamespaceContext pNamespaceContext, PETransformerContext pContext) {
    return create(pContext, true);
  }

  public static PETransformer create(PETransformerContext pContext, boolean pRemoveWhitespace) {
    return create(null, pContext, pRemoveWhitespace);
  }

  public static PETransformer create(NamespaceContext pNamespaceContext, PETransformerContext pContext, boolean pRemoveWhitespace) {
    return new PETransformer(pContext, pNamespaceContext, pRemoveWhitespace);
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
    final XMLEventReader xer;
    if (source instanceof StAXSource) {
      xer = createFilter(((StAXSource) source).getXMLStreamReader());
    } else {
      final XMLInputFactory xif = XMLInputFactory.newInstance();
      xer = createFilter(xif.createXMLStreamReader(Sources.toReader(source)));
    }

    final XMLEventWriter xew;
    if (result instanceof StAXResult) {
      xew = ((StAXResult) result).getXMLEventWriter();
    } else {
      final XMLOutputFactory xof = XMLOutputFactory.newInstance();
      xew = xof.createXMLEventWriter(result);
    }
    xew.add(xer);
  }

  private XMLEventReader createFilter(XMLStreamReader pInput) throws XMLStreamException {
    return new MyFilter(aContext, aNamespaceContext, pInput, mRemoveWhitespace);
  }

  protected static List<XMLEvent> toEvents(ProcessData data) throws XMLStreamException {
    List<XMLEvent> result = new ArrayList<>();
    DocumentFragment frag = data.getDocumentFragment();
    for(XMLEventReader dataReader = new NodeEventReader(frag);dataReader.hasNext();) {
      result.add(dataReader.nextEvent());
    }
    return result;
  }

}
