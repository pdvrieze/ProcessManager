/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CombiningNamespaceContext;
import nl.adaptivity.util.DomUtil;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.XmlEvent.EndElementEvent;
import nl.adaptivity.xml.XmlEvent.StartElementEvent;
import nl.adaptivity.xml.XmlEvent.TextEvent;
import nl.adaptivity.xml.EventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.*;

import java.util.*;


public class PETransformer {


  public static class MyFilter extends JvmXmlBufferedReader {

    @NotNull private PETransformerContext mContext;
    @Nullable private final NamespaceContext mNamespaceContext;
    private final boolean mRemoveWhitespace;

    public MyFilter(@NotNull final PETransformerContext context, @Nullable final NamespaceContext namespaceContext, @NotNull final XmlReader delegate, final boolean removeWhitespace) {
      super(delegate);
      mContext = context;
      mNamespaceContext = namespaceContext;
      mRemoveWhitespace = removeWhitespace;
    }


    @NotNull
    @Override
    public List<XmlEvent> doPeek() {
      final List<XmlEvent> results = new ArrayList<>(1);

      doPeek(results);
      return results;
    }

    @Nullable
    private void doPeek(final List<XmlEvent> results) {
      final List<XmlEvent> events = super.doPeek();

      for(XmlEvent event:events) {
        switch (event.getEventType()) {
          case START_ELEMENT:
            peekStartElement(results, (StartElementEvent) event);
            break;
          case TEXT: {
            TextEvent text = (TextEvent) event;
            if (!isIgnorableWhiteSpace(text)) {
              results.add(event);
              break;
            }
            // fall through if ignorable
          }
          case IGNORABLE_WHITESPACE: {
            if (! mRemoveWhitespace) {
              results.add(event);
            }
            doPeek(results); //peek again, as sometimes whitespace needs to be stripped to add attributes
            break;
          } // fall through if not whitespace
          default:
            results.add(event);

        }
      }
    }


    private static void stripWhiteSpaceFromPeekBuffer(final List<XmlEvent> results) {
      XmlEvent peekLast;
      while(results.size()>0 && (peekLast = results.get(results.size()-1)) instanceof TextEvent && XmlUtil.isXmlWhitespace(((TextEvent) peekLast)
                                                                                                                                     .getText())) {
        results.remove(results.size()-1);
      }
    }

    private void peekStartElement(final List<XmlEvent> results, @NotNull final StartElementEvent element) {
      if (Constants.MODIFY_NS_STR.equals(element.getNamespaceUri())) {
        final String localname = StringUtil.toString(element.getLocalName());

        final Map<String, CharSequence> attributes = parseAttributes(element);

        switch (localname) {
          case "attribute":
            stripWhiteSpaceFromPeekBuffer(results);
            results.add(getAttribute(attributes));
            readEndTag(element);
            return;
          case "element":
            processElement(results, element, attributes, false);
            readEndTag(element);
            return;
          case "value":
            processElement(results, element, attributes, true);
            readEndTag(element);
            return;
          default:
              (new XmlException("Unsupported element: {" + element.getNamespaceUri() + '}' + element.getLocalName())).doThrow();
        }
      } else {
        boolean filterAttributes = false;
        final List<XmlEvent.Attribute> newAttrs = new ArrayList<>();
        for(final XmlEvent.Attribute attr : element.getAttributes()) {
          if (attr.hasNamespaceUri() && StringUtil.isEqual(Constants.MODIFY_NS_STR, attr.getValue())) {
            filterAttributes=true;
          } else {
            newAttrs.add(attr);
          }
        }
        final List<Namespace> newNamespaces = new ArrayList<>();
        for(final Namespace ns : element.getNamespaceDecls()) {
          if (Constants.MODIFY_NS_STR.equals(ns.getNamespaceURI())) {
            filterAttributes=true;
          } else {
            newNamespaces.add(ns);
          }
        }
        if (filterAttributes) {
          results.add(new StartElementEvent(element.getLocationInfo(), element.getNamespaceUri(), element.getLocalName(), element.getPrefix(),
                                    newAttrs.toArray(new XmlEvent.Attribute[newAttrs.size()]),
                                    newNamespaces.toArray(new Namespace[newNamespaces.size()])));
        } else {
          results.add(element);
        }
      }
    }

    private void readEndTag(final StartElementEvent name) {
      while(true) {
        List<XmlEvent> elems = super.doPeek();
        for(XmlEvent elem: elems) {
          switch (elem.getEventType()) {
            case IGNORABLE_WHITESPACE:
            case COMMENT:
              break;
            case TEXT:
              if (XmlUtil.isXmlWhitespace(((TextEvent) elem).getText())) {
                break;
              }
            default:
              if (! (elem.getEventType()== EventType.END_ELEMENT && name.isEqualNames((EndElementEvent) elem))) {
                new XmlException("Unexpected tag found ("+elem+")when expecting an end tag for "+name).doThrow();
              }
              return;
          }
        }
      }
    }

    private void processElement(final List<XmlEvent> results, @NotNull final StartElementEvent event, @NotNull final Map<String, CharSequence> attributes, final boolean hasDefault) {
      final CharSequence valueName = attributes.get("value");
      final CharSequence xpath = attributes.get("xpath");
      try {
        if (valueName == null) {
          if (hasDefault) {
            addAllRegular(results, applyXpath(event.getNamespaceContext(), mContext.resolveDefaultValue(), xpath));
          } else {
            new XmlException("This context does not allow for a missing value parameter").doThrow();
          }
        } else {
          addAllRegular(results, applyXpath(event.getNamespaceContext(), mContext.resolveElementValue(valueName), xpath));
        }
      } catch (XPathExpressionException | ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }

    private static void addAllRegular(List<XmlEvent> target, Iterable<? extends XmlEvent> source) {
      for(XmlEvent event:source) {
        if (!event.isIgnorable()) {
          target.add(event);
        }
      }
    }

    @NotNull
    private Collection<? extends XmlEvent> applyXpath(final NamespaceContext namespaceContext, @NotNull final List<XmlEvent> pendingEvents, @Nullable final CharSequence xpath) throws
            XPathExpressionException, ParserConfigurationException {
      String xpathstr = StringUtil.toString(xpath);
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
      final ArrayList<XmlEvent> result = new ArrayList<>();
      final XMLOutputFactory xof = XMLOutputFactory.newFactory();
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final DocumentFragment eventFragment = db.newDocument().createDocumentFragment();
      final DOMResult domResult= new DOMResult(eventFragment);

      final XmlWriter writer = XmlStreaming.newWriter(domResult);
      for(final XmlEvent event: pendingEvents) {
        event.writeTo(writer);
      }
      writer.close();
      final NodeList applicationResult = (NodeList) xpathexpr.evaluate(eventFragment, XPathConstants.NODESET);
      if (applicationResult.getLength()>0) {
        result.addAll(toEvents(new ProcessData("--xpath result--", DomUtil.nodeListToFragment(applicationResult))));
      }
      return result;
    }

    @NotNull
    private static Map<String,CharSequence> parseAttributes(@NotNull final StartElementEvent startElement) {
      final TreeMap<String, CharSequence> result = new TreeMap<>();

      for(XmlEvent.Attribute attribute: startElement.getAttributes()) {
        result.put(StringUtil.toString(attribute.getLocalName()), attribute.getValue());
      }
      return result;
    }

    private XmlEvent getAttribute(@NotNull final Map<String, CharSequence> attributes) {
      final String valueName = StringUtil.toString(attributes.get("value"));
      final CharSequence xpath = attributes.get("xpath");
      CharSequence paramName = attributes.get("name");

      if (valueName != null) {
        if (paramName==null) {
          paramName = mContext.resolveAttributeName(valueName);
        }
        final String value = mContext.resolveAttributeValue(valueName, StringUtil.toString(xpath));
        return new XmlEvent.Attribute(null, XMLConstants.NULL_NS_URI, paramName, XMLConstants.DEFAULT_NS_PREFIX, value);
      } else {
        throw new MessagingFormatException("Missing parameter name");
      }
    }
  }


  public interface PETransformerContext {
    @NotNull
    List<XmlEvent> resolveElementValue(CharSequence valueName);
    @NotNull
    List<XmlEvent> resolveDefaultValue(); //Just return DOM, not events (that then need to be dom-ified)
    @NotNull
    String resolveAttributeValue(String valueName, final String xpath);
    @NotNull
    String resolveAttributeName(String valueName);

  }

  public static abstract class AbstractDataContext implements PETransformerContext {

    @Nullable
    protected abstract ProcessData getData(String valueName);

    @Override
    @NotNull
    public List<XmlEvent> resolveElementValue(final CharSequence valueName) {
      final ProcessData data = getData(StringUtil.toString(valueName));
      if (data==null) {
        throw new IllegalArgumentException("No value with name "+valueName+" found");
      }
      return toEvents(data);
    }

    @NotNull
    @Override
    public String resolveAttributeValue(final String valueName, final String xpath) {
      final ProcessData data = getData(valueName);
      if (data==null) {
        throw new IllegalArgumentException("No data value with name "+valueName+" found");
      }
      final XmlReader dataReader = data.getContentStream();
      final StringBuilder result = new StringBuilder();
      try {
        while (dataReader.hasNext()) {
          final EventType event = dataReader.next();
          switch (event) {
            case ATTRIBUTE:
              case DOCDECL:
            case START_ELEMENT:
              throw new XmlException("Unexpected node found while resolving attribute. Only CDATA allowed: ("+event.getClass().getSimpleName()+") "+event);
            case CDSECT:
            case TEXT: {
                if (! isIgnorableWhiteSpace(dataReader)) {
                    result.append(dataReader.getText());
                }
                break;
            }
            case START_DOCUMENT:
            case END_DOCUMENT:
            case COMMENT:
            case PROCESSING_INSTRUCTION:
              break; // ignore
            case END_ELEMENT: // finished the element
              return result.toString();
            default:
              throw new XmlException("Unexpected node type: "+event);
          }
        }
      } catch (XmlException e) {
        new XmlException("Failure to parse data (name="+valueName+", value="+data.getContent().getContentString()+")", e).doThrow();
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

    @Nullable private ProcessData[] mProcessData;
    private int mDefaultIdx;

    public ProcessDataContext(@Nullable final ProcessData... processData) {
      if (processData==null) {
        mProcessData= new ProcessData[0];
        mDefaultIdx=0;
      } else {
        mProcessData = processData;
        mDefaultIdx = processData.length==1 ? 0 : -1;
      }
    }

    public ProcessDataContext(final int defaultIdx, @NotNull final ProcessData... processData) {
      assert defaultIdx>=-1 && defaultIdx<processData.length;
      mProcessData = processData;
      mDefaultIdx = defaultIdx;
    }

    @Nullable
    @Override
    protected ProcessData getData(@NotNull final String valueName) {
      for(final ProcessData candidate: mProcessData) {
        if (StringUtil.isEqual(valueName, candidate.getName())) { return candidate; }
      }
      return null;
    }

    @NotNull
    @Override
    public List<XmlEvent> resolveDefaultValue() {
      if (mProcessData.length==0 || mProcessData[mDefaultIdx]==null) { return Collections.emptyList(); }
      return toEvents(mProcessData[mDefaultIdx]);
    }

  }

  private final PETransformerContext mContext;
  private final NamespaceContext mNamespaceContext;
  private final boolean mRemoveWhitespace;

  private PETransformer(final PETransformerContext context, final NamespaceContext namespaceContext, final boolean removeWhitespace) {
    mContext = context;
    mNamespaceContext = namespaceContext;
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
      new XmlException(e).doThrow();
      return null;
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
    } catch (@NotNull ParserConfigurationException e) {
      new XmlException(e).doThrow();
      return null;
    }
  }

  public void transform(final Source source, final Result result) {
    transform(XmlStreaming.newReader(source), XmlStreaming.newWriter(result, true));
  }

  public void transform(final XmlReader in, @NotNull final XmlWriter out) {
    final XmlReader filteredIn = createFilter(in);
    while (filteredIn.hasNext()) {
      filteredIn.next(); // Don't forget to move to next element as well.
      XmlReaderUtil.writeCurrent(filteredIn, out);
    }
  }

  public XmlReader createFilter(final XmlReader input) {
    return new MyFilter(mContext, mNamespaceContext, input, mRemoveWhitespace);
  }

  @NotNull
  protected static List<XmlEvent> toEvents(@NotNull final ProcessData data) {
    final List<XmlEvent> result = new ArrayList<>();

    final XmlReader frag = data.getContentStream();
    while (frag.hasNext()) {
      frag.next();
      result.add(XmlEvent.Companion.from(frag));
    }
    return result;
  }

  static boolean isIgnorableWhiteSpace(@NotNull final TextEvent characters) {
    if (characters.getEventType() == EventType.IGNORABLE_WHITESPACE) {
      return true;
    }
    return XmlUtil.isXmlWhitespace(characters.getText());
  }

  static boolean isIgnorableWhiteSpace(@NotNull final XmlReader characters) {
    if (characters.getEventType()== EventType.IGNORABLE_WHITESPACE) {
      return true;
    }
    return XmlUtil.isXmlWhitespace(characters.getText());
  }
}
