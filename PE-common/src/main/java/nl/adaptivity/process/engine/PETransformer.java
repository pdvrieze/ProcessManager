package nl.adaptivity.process.engine;

import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.AbstractBufferedEventReader;
import nl.adaptivity.util.xml.CombiningNamespaceContext;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull private PETransformerContext mContext;
    @Nullable private final NamespaceContext mNamespaceContext;
    @NotNull private XMLEventReader mEventInput;
    @NotNull private XMLEventFactory mXef;
    private final boolean mRemoveWhitespace;

    public MyFilter(final PETransformerContext context, final NamespaceContext namespaceContext, final XMLStreamReader input, final boolean removeWhitespace) throws
            XMLStreamException {
      this(context, namespaceContext, XMLInputFactory.newFactory().createXMLEventReader(input), removeWhitespace);
    }

    public MyFilter(@NotNull final PETransformerContext context, @Nullable final NamespaceContext namespaceContext, @NotNull final XMLEventReader input, final boolean removeWhitespace) {
      mContext = context;
      mNamespaceContext = namespaceContext;
      mEventInput = input;
      mXef = XMLEventFactory.newInstance();
      mRemoveWhitespace = removeWhitespace;
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
      while(mEventInput.hasNext()) {
        final XMLEvent event = mEventInput.nextEvent();
        if (event.isStartElement()) {
          peekStartElement(event.asStartElement());
          return peekFirst();
        } else if (event.isNamespace()){
          if (! Constants.MODIFY_NS_STR.equals(((Namespace)event).getNamespaceURI())) {
            add(event);
            return peekFirst();
          }
        } else if (event.isCharacters()) {
          if (isIgnorableWhiteSpace(event.asCharacters())) {
            if (mRemoveWhitespace) { continue; /* Continue with the loop, get another event */ }
            add(event);
            continue; // peek more, so that if we find
          }
          add(event);
          return peekFirst();
        } else {
          add(event);
          return peekFirst();
        }
      }
      return peekFirst();
    }

    private void peekStartElement(@NotNull final StartElement element) throws XMLStreamException {
      if (Constants.MODIFY_NS_STR.equals(element.getName().getNamespaceURI())) {
        final String localname = element.getName().getLocalPart();

        final Map<String, String> attributes = parseAttributes(mEventInput, element);

        switch (localname) {
          case "attribute":
            stripWhiteSpaceFromPeekBuffer();
            add(getAttribute(attributes));
            readEndTag(element.getName());
            return;
          case "element":
            processElement(element, attributes, false);
            readEndTag(element.getName());
            return;
          case "value":
            processElement(element, attributes, true);
            readEndTag(element.getName());
            return;
          default:
            throw new XMLStreamException("Unsupported element: "+element.getName());
        }
      } else {
        boolean filterAttributes = false;
        final List<Attribute> newAttrs = new ArrayList<>();
        for(@SuppressWarnings("unchecked") final Iterator<Attribute> it = element.getAttributes(); it.hasNext(); ) {
          final Attribute attr = it.next();
          if (attr.isNamespace() && Constants.MODIFY_NS_STR.equals(attr.getValue())) {
            filterAttributes=true;
          } else {
            newAttrs.add(attr);
          }
        }
        final List<Namespace> newNamespaces = new ArrayList<>();
        for(@SuppressWarnings("unchecked") final Iterator<Namespace> it = element.getNamespaces(); it.hasNext();) {
          final Namespace ns = it.next();
          if (Constants.MODIFY_NS_STR.equals(ns.getNamespaceURI())) {
            filterAttributes=true;
          } else {
            newNamespaces.add(ns);
          }
        }
        if (filterAttributes) {
          add(mXef.createStartElement(element.getName(), newAttrs.iterator(), newNamespaces.iterator()));
        } else {
          add(element);
        }
      }
    }

    private void readEndTag(final QName name) throws XMLStreamException {
      final XMLEvent ev = mEventInput.nextTag();
      if (! (ev.isEndElement() && ev.asEndElement().getName().equals(name))) {
        throw new XMLStreamException("Unexpected tag found ("+ev+")when expecting an end tag for "+name);
      }
    }

    private void processElement(@NotNull final StartElement event, @NotNull final Map<String,String> attributes, final boolean hasDefault) throws XMLStreamException {
      final String valueName = attributes.get("value");
      final String xpath = attributes.get("xpath");
      try {
        if (valueName == null) {
          if (hasDefault) {
            addAll(applyXpath(event.getNamespaceContext(), mContext.resolveDefaultValue(mXef), xpath));
          } else {
            throw new XMLStreamException("This context does not allow for a missing value parameter");
          }
        } else {
          addAll(applyXpath(event.getNamespaceContext(), mContext.resolveElementValue(mXef, valueName), xpath));
        }
      } catch (@NotNull XPathExpressionException|ParserConfigurationException e) {
        throw new XMLStreamException(e);
      }
    }

    @NotNull
    private Collection<? extends XMLEvent> applyXpath(final NamespaceContext namespaceContext, @NotNull final List<XMLEvent> pendingEvents, @Nullable final String xpathstr) throws
            XPathExpressionException, XMLStreamException, ParserConfigurationException {
      if (xpathstr==null || ".".equals(xpathstr)) {
        return pendingEvents;
      }
      // TODO add a function resolver
      final XPath rawPath = XPathFactory.newInstance().newXPath();
      // Do this better
      if (mNamespaceContext ==null) {
        rawPath.setNamespaceContext(namespaceContext);
      } else {
        rawPath.setNamespaceContext(new CombiningNamespaceContext(namespaceContext, mNamespaceContext));
      }
      final XPathExpression xpathexpr = rawPath.compile(xpathstr);
      final ArrayList<XMLEvent> result = new ArrayList<>();
      final XMLOutputFactory xof = XMLOutputFactory.newFactory();
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final DocumentFragment eventFragment = db.newDocument().createDocumentFragment();
      final DOMResult domResult= new DOMResult(eventFragment);
      final XMLEventWriter xew = xof.createXMLEventWriter(domResult);
      for(final XMLEvent event: pendingEvents) {
        xew.add(event);
      }
      xew.close();
      final NodeList applicationResult = (NodeList) xpathexpr.evaluate(eventFragment, XPathConstants.NODESET);
      if (applicationResult.getLength()>0) {
        result.addAll(toEvents(new ProcessData("--xpath result--", XmlUtil.nodeListToFragment(applicationResult))));
      }
      return result;
    }

    @NotNull
    private static Map<String,String> parseAttributes(@NotNull final XMLEventReader in, @NotNull final StartElement startElement) throws XMLStreamException {
      final TreeMap<String, String> result = new TreeMap<>();

      @SuppressWarnings("unchecked") final
      Iterator<Attribute> attributes = startElement.getAttributes();
      while (attributes.hasNext()) {
        final Attribute attribute = attributes.next();
        result.put(attribute.getName().getLocalPart(), attribute.getValue());
      }

      while(in.peek().isAttribute()) {
        final Attribute attribute = (Attribute) in.nextEvent();
        result.put(attribute.getName().getLocalPart(), attribute.getValue());
      }
      return result;
    }

    private XMLEvent getAttribute(@NotNull final Map<String,String> attributes) throws XMLStreamException {
      final String valueName = attributes.get("value");
      final String xpath = attributes.get("xpath");
      String paramName = attributes.get("name");

      if (valueName != null) {
        if (paramName==null) {
          paramName = mContext.resolveAttributeName(valueName);
        }
        final String value = mContext.resolveAttributeValue(valueName, xpath);
        return mXef.createAttribute(paramName, value);
      } else {
        throw new MessagingFormatException("Missing parameter name");
      }
    }

    @Override
    public Object getProperty(final String name) throws IllegalArgumentException {
      return mEventInput.getProperty(name);
    }

    @Override
    public void close() throws XMLStreamException {
      mEventInput.close();
      super.close();
    }

  }

  public interface PETransformerContext {

    @NotNull
    List<XMLEvent> resolveElementValue(XMLEventFactory xef, String valueName) throws XMLStreamException;
    List<XMLEvent> resolveDefaultValue(XMLEventFactory xef) throws XMLStreamException; //Just return DOM, not events (that then need to be dom-ified)
    @NotNull
    String resolveAttributeValue(String valueName, final String xpath) throws XMLStreamException;
    @NotNull
    String resolveAttributeName(String valueName);

  }

  public static abstract class AbstractDataContext implements PETransformerContext {

    @Nullable
    protected abstract ProcessData getData(String valueName);

    @NotNull
    @Override
    public List<XMLEvent> resolveElementValue(final XMLEventFactory xef, final String valueName) throws XMLStreamException {
      final ProcessData data = getData(valueName);
      if (data==null) {
        throw new IllegalArgumentException("No value with name "+valueName+" found");
      }
      return toEvents(data);
    }

    @NotNull
    @Override
    public String resolveAttributeValue(final String valueName, final String xpath) throws XMLStreamException {
      final ProcessData data = getData(valueName);
      if (data==null) {
        throw new IllegalArgumentException("No data value with name "+valueName+" found");
      }
      final XMLInputFactory xif = XMLInputFactory.newFactory();
      final XMLEventReader dataReader = XmlUtil.createXMLEventReader(xif, new StAXSource(XMLFragmentStreamReader.from(data.getContent())));
      final StringBuilder result = new StringBuilder();
      while (dataReader.hasNext()) {
        final XMLEvent event = dataReader.nextEvent();
        switch (event.getEventType()) {
        case XMLStreamConstants.ATTRIBUTE:
        case XMLStreamConstants.DTD:
        case XMLStreamConstants.START_ELEMENT:
        case XMLStreamConstants.END_ELEMENT:
          throw new XMLStreamException("Unexpected node found while resolving attribute. Only CDATA allowed: ("+event.getClass().getSimpleName()+") "+event.getEventType());
        case XMLStreamConstants.CDATA:
        case XMLStreamConstants.CHARACTERS: {
          final Characters characters = event.asCharacters();
          if (! isIgnorableWhiteSpace(characters)) {
            result.append(characters.getData());
          }
          break;
        }
        case XMLStreamConstants.START_DOCUMENT:
        case XMLStreamConstants.END_DOCUMENT:
        case XMLStreamConstants.COMMENT:
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
          break; // ignore
        default:
          throw new XMLStreamException("Unexpected node type: "+event);
        }
      }
      return result.toString();
    }

    @NotNull
    @Override
    public String resolveAttributeName(final String valueName) {
      final ProcessData data = getData(valueName);
      return new String(data.getContent().getContent());
    }

  }

  public static class ProcessDataContext extends AbstractDataContext {

    @Nullable private ProcessData[] aProcessData;
    private int aDefaultIdx;

    public ProcessDataContext(@Nullable final ProcessData... processData) {
      if (processData==null) {
        aProcessData= new ProcessData[0];
        aDefaultIdx=0;
      } else {
        aProcessData = processData;
        aDefaultIdx = processData.length==1 ? 0 : -1;
      }
    }

    public ProcessDataContext(final int defaultIdx, @NotNull final ProcessData... processData) {
      assert defaultIdx>=-1 && defaultIdx<processData.length;
      aProcessData = processData;
      aDefaultIdx = defaultIdx;
    }

    @Nullable
    @Override
    protected ProcessData getData(@NotNull final String valueName) {
      for(final ProcessData candidate: aProcessData) {
        if (valueName.equals(candidate.getName())) { return candidate; }
      }
      return null;
    }

    @NotNull
    @Override
    public List<XMLEvent> resolveDefaultValue(final XMLEventFactory xef) throws XMLStreamException {
      if (aProcessData.length==0 || aProcessData[aDefaultIdx]==null) { return Collections.emptyList(); }
      return toEvents(aProcessData[aDefaultIdx]);
    }

  }

  private final PETransformerContext aContext;
  private final NamespaceContext aNamespaceContext;
  private final boolean mRemoveWhitespace;

  private PETransformer(final PETransformerContext context, final NamespaceContext namespaceContext, final boolean removeWhitespace) {
    aContext = context;
    aNamespaceContext = namespaceContext;
    mRemoveWhitespace = removeWhitespace;
  }

  @NotNull
  public static PETransformer create(final NamespaceContext namespaceContext, final boolean removeWhitespace, final ProcessData... processData) {
    return new PETransformer(new ProcessDataContext(processData), namespaceContext, removeWhitespace);
  }

  @NotNull
  @Deprecated
  public static PETransformer create(final boolean removeWhitespace, final ProcessData... processData) {
    return create(null, removeWhitespace, processData);
  }

  @NotNull
  public static PETransformer create(final NamespaceContext namespaceContext, final ProcessData... processData) {
    return create(namespaceContext, true, processData);
  }

  @NotNull
  @Deprecated
  public static PETransformer create(final ProcessData... processData) {
    return create(null, true, processData);
  }

  @NotNull
  @Deprecated
  public static PETransformer create(final PETransformerContext context) {
    return create(context, true);
  }

  @NotNull
  public static PETransformer create(final NamespaceContext namespaceContext, final PETransformerContext context) {
    return create(namespaceContext, context, true);
  }

  @NotNull
  public static PETransformer create(final PETransformerContext context, final boolean removeWhitespace) {
    return create(null, context, removeWhitespace);
  }

  @NotNull
  public static PETransformer create(final NamespaceContext namespaceContext, final PETransformerContext context, final boolean removeWhitespace) {
    return new PETransformer(context, namespaceContext, removeWhitespace);
  }

  @NotNull
  public List<Node> transform(@NotNull final List<?> content) {
    try {
      Document document = null;
      final ArrayList<Node> result = new ArrayList<>(content.size());
      for(final Object obj: content) {
        if (obj instanceof CharSequence) {
          if (document == null) {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            document = dbf.newDocumentBuilder().newDocument();
          }
          result.add(document.createTextNode(obj.toString()));
        } else if (obj instanceof Node) {
          if (document==null) { document = ((Node) obj).getOwnerDocument(); }
          final DocumentFragment v = transform((Node) obj);
          if (v!=null) {
            result.add(v);
          }
        } else if (obj instanceof JAXBElement<?>) {
          if (document == null) {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            document = dbf.newDocumentBuilder().newDocument();
          }
          final JAXBElement<?> jbe = (JAXBElement<?>) obj;
          final DocumentFragment df = document.createDocumentFragment();
          final DOMResult domResult = new DOMResult(df);
          JAXB.marshal(jbe, domResult);
          for(Node n = df.getFirstChild(); n!=null; n=n.getNextSibling()) {
            final DocumentFragment v = transform(n);
            if (v!=null) {
              result.add(v);
            }
          }
        } else if (obj!=null) {
          throw new IllegalArgumentException("The node "+obj.toString()+" of type "+obj.getClass()+" is not understood");
        }
      }
      return result;
    } catch (@NotNull final ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  public DocumentFragment transform(final Node node) {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final Document document;
    try {
      document = dbf.newDocumentBuilder().newDocument();
      final DocumentFragment fragment = document.createDocumentFragment();
      final DOMResult result = new DOMResult(fragment);
      transform(new DOMSource(node), result);
      return fragment;
    } catch (@NotNull ParserConfigurationException | XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  public void transform(final Source source, final Result result) throws XMLStreamException {
    final XMLEventReader xer = createFilter(XmlUtil.createXMLEventReader(XMLInputFactory.newInstance(), source));
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    final XMLEventWriter xew = XmlUtil.createXMLEventWriter(xof, result);
    xew.add(xer);
  }

  public void transform(final XMLStreamReader source, @NotNull final XMLStreamWriter result) throws XMLStreamException {
    final XMLEventReader xer = createFilter(source);
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    final XMLEventWriter xew = XmlUtil.createXMLEventWriter(xof, new StAXResult(result));
    xew.add(xer);
  }

  @NotNull
  public XMLEventReader createFilter(final XMLStreamReader input) throws XMLStreamException {
    return new MyFilter(aContext, aNamespaceContext, input, mRemoveWhitespace);
  }

  @NotNull
  public XMLEventReader createFilter(final XMLEventReader input) throws XMLStreamException {
    return new MyFilter(aContext, aNamespaceContext, input, mRemoveWhitespace);
  }

  @NotNull
  protected static List<XMLEvent> toEvents(@NotNull final ProcessData data) throws XMLStreamException {
    final List<XMLEvent> result = new ArrayList<>();

    final XMLStreamReader frag = data.getContentStream();
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    for(final XMLEventReader dataReader = XmlUtil.filterSubstream(XmlUtil.createXMLEventReader(xif, new StAXSource(frag))); dataReader.hasNext();) {
      result.add(dataReader.nextEvent());
    }
    return result;
  }

  static boolean isIgnorableWhiteSpace(@NotNull final Characters characters) {
    if (characters.isIgnorableWhiteSpace()) {
      return true;
    }
    return XmlUtil.isXmlWhitespace(characters.getData());
  }
}
