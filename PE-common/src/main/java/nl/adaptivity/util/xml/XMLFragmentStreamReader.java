package nl.adaptivity.util.xml;

import nl.adaptivity.util.CombiningReader;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import java.io.CharArrayReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * This streamreader allows for reading document fragments. It does so by wrapping the reader into a pair of wrapper elements, and then ignoring those on reading.
 *
 * Created by pdvrieze on 04/11/15.
 */
public class XMLFragmentStreamReader implements XMLStreamReader {

  private static class FragmentNamespaceContext extends SimpleNamespaceContext {

    private final FragmentNamespaceContext aParent;

    public FragmentNamespaceContext(FragmentNamespaceContext parent, String[] prefixes, String[] namespaces) {
      super(prefixes, namespaces);
      aParent = parent;
    }

    @Override
    public String getNamespaceURI(final String prefix) {
      String namespaceURI = super.getNamespaceURI(prefix);
      if (namespaceURI==null && aParent!=null) {
        return aParent.getNamespaceURI(prefix);
      }
      return namespaceURI;
    }

    @Override
    public String getPrefix(final String namespaceURI) {

      String prefix = super.getPrefix(namespaceURI);
      if (prefix==null && aParent!=null) { return aParent.getPrefix(namespaceURI); }
      return prefix;

    }

    @Override
    public Iterator<String> getPrefixes(final String namespaceURI) {
      if (aParent==null) { return super.getPrefixes(namespaceURI); }
      Set<String> prefixes = new HashSet<>();

      for(Iterator<String> it = super.getPrefixes(namespaceURI); it.hasNext();) {
        prefixes.add(it.next());
      }

      for(Iterator<String> it = aParent.getPrefixes(namespaceURI); it.hasNext();) {
        String prefix = it.next();
        String localNamespaceUri = getLocalNamespaceUri(prefix);
        if (localNamespaceUri==null) {
          prefixes.add(prefix);
        }
      }

      return prefixes.iterator();
    }

    private String getLocalNamespaceUri(final String pPrefix) {
      for(int i=size()-1; i>=0; --i) {
        if (pPrefix.equals(getPrefix(i))) {
          return getNamespaceURI(i);
        }
      }
      return null;
    }
  }

  private static final String WRAPPERPPREFIX = "SDFKLJDSF";
  private static final String WRAPPERNAMESPACE = "http://wrapperns";
  private final XMLStreamReader delegate;
  private FragmentNamespaceContext localNamespaceContext;

  public XMLFragmentStreamReader(final XMLInputFactory pXif, final Reader pIn, final Iterable<Namespace> pWrapperNamespaceContext) throws XMLStreamException {
    StringBuilder wrapperBuilder = new StringBuilder();
    wrapperBuilder.append("<" + WRAPPERPPREFIX + ":wrapper xmlns:" + WRAPPERPPREFIX + "=\"" + WRAPPERNAMESPACE+'"');
    for(Namespace ns:pWrapperNamespaceContext) {
      String prefix = ns.getPrefix();
      String uri = ns.getNamespaceURI();
      if (prefix==null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
        wrapperBuilder.append(" xmlns");
      } else {
        wrapperBuilder.append(" xmlns:").append(prefix);
      }
      wrapperBuilder.append("=\"").append(XmlUtil.xmlEncode(uri)).append('"');
    }
    wrapperBuilder.append(" >");

    String wrapper = wrapperBuilder.toString();
    Reader actualInput = new CombiningReader(new StringReader(wrapper), pIn, new StringReader("</" + WRAPPERPPREFIX + ":wrapper>"));
    delegate = pXif.createXMLStreamReader(actualInput);
    localNamespaceContext = new FragmentNamespaceContext(null, new String[0], new String[0]);
  }

  public static XMLFragmentStreamReader from (XMLInputFactory xif, Reader in, Iterable<Namespace> pNamespaceContext) throws XMLStreamException {
    return new XMLFragmentStreamReader(xif, in, pNamespaceContext);
  }

  public static XMLFragmentStreamReader from (XMLInputFactory xif, Reader in) throws XMLStreamException {
    return new XMLFragmentStreamReader(xif, in, Collections.<Namespace>emptyList());
  }

  public static XMLFragmentStreamReader from(CompactFragment fragment) throws XMLStreamException {
    return new XMLFragmentStreamReader(XMLInputFactory.newFactory(), new CharArrayReader(fragment.getContent()), fragment.getNamespaces());
  }

  @Override
  public Object getProperty(final String name) throws IllegalArgumentException {
    return delegate.getProperty(name);
  }

  @Override
  public int next() throws XMLStreamException {
    int result = delegate.next();
    switch (result) {
      case XMLStreamConstants.START_DOCUMENT:
      case XMLStreamConstants.END_DOCUMENT:
      case XMLStreamConstants.PROCESSING_INSTRUCTION:
      case XMLStreamConstants.DTD:
        return next();
      case XMLStreamConstants.START_ELEMENT:
        if (WRAPPERNAMESPACE.equals(delegate.getNamespaceURI())) { return delegate.next(); }
        extendNamespace();
        break;
      case XMLStreamConstants.END_ELEMENT:
        if (WRAPPERNAMESPACE.equals(delegate.getNamespaceURI())) { return delegate.next(); }
        localNamespaceContext = localNamespaceContext.aParent;
        break;
    }
    return result;
  }

  private void extendNamespace() {
    int nscount = delegate.getNamespaceCount();
    String[] prefixes = new String[nscount];
    String[] namespaces = new String[nscount];
    for(int i=nscount-1; i>=0; --i) {
      prefixes[i] = delegate.getNamespacePrefix(i);
      namespaces[i] = delegate.getNamespaceURI(i);
    }
    localNamespaceContext = new FragmentNamespaceContext(localNamespaceContext, prefixes, namespaces);
  }

  @Override
  public void require(final int type, final String namespaceURI, final String localName) throws XMLStreamException {
    delegate.require(type, namespaceURI, localName);
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return delegate.getElementText();
  }

  @Override
  public int nextTag() throws XMLStreamException {
    return delegate.nextTag();
  }

  @Override
  public boolean hasNext() throws XMLStreamException {
    return delegate.hasNext();
  }

  @Override
  public void close() throws XMLStreamException {
    delegate.close();
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    return delegate.getNamespaceURI(prefix);
  }

  @Override
  public boolean isStartElement() {
    return delegate.isStartElement();
  }

  @Override
  public boolean isEndElement() {
    return delegate.isEndElement();
  }

  @Override
  public boolean isCharacters() {
    return delegate.isCharacters();
  }

  @Override
  public boolean isWhiteSpace() {
    return delegate.isWhiteSpace();
  }

  @Override
  public String getAttributeValue(final String namespaceURI, final String localName) {
    return delegate.getAttributeValue(namespaceURI, localName);
  }

  @Override
  public int getAttributeCount() {
    return delegate.getAttributeCount();
  }

  @Override
  public QName getAttributeName(final int index) {
    return delegate.getAttributeName(index);
  }

  @Override
  public String getAttributeNamespace(final int index) {
    return delegate.getAttributeNamespace(index);
  }

  @Override
  public String getAttributeLocalName(final int index) {
    return delegate.getAttributeLocalName(index);
  }

  @Override
  public String getAttributePrefix(final int index) {
    return delegate.getAttributePrefix(index);
  }

  @Override
  public String getAttributeType(final int index) {
    return delegate.getAttributeType(index);
  }

  @Override
  public String getAttributeValue(final int index) {
    return delegate.getAttributeValue(index);
  }

  @Override
  public boolean isAttributeSpecified(final int index) {
    return delegate.isAttributeSpecified(index);
  }

  @Override
  public int getNamespaceCount() {
    return delegate.getNamespaceCount();
  }

  @Override
  public String getNamespacePrefix(final int index) {
    return delegate.getNamespacePrefix(index);
  }

  @Override
  public String getNamespaceURI(final int index) {
    return delegate.getNamespaceURI(index);
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return localNamespaceContext;
  }

  @Override
  public int getEventType() {
    return delegate.getEventType();
  }

  @Override
  public String getText() {
    return delegate.getText();
  }

  @Override
  public char[] getTextCharacters() {
    return delegate.getTextCharacters();
  }

  @Override
  public int getTextCharacters(final int sourceStart, final char[] target, final int targetStart, final int length) throws
          XMLStreamException {
    return delegate.getTextCharacters(sourceStart, target, targetStart, length);
  }

  @Override
  public int getTextStart() {
    return delegate.getTextStart();
  }

  @Override
  public int getTextLength() {
    return delegate.getTextLength();
  }

  @Override
  public String getEncoding() {
    return delegate.getEncoding();
  }

  @Override
  public boolean hasText() {
    return delegate.hasText();
  }

  @Override
  public Location getLocation() {
    return delegate.getLocation();
  }

  @Override
  public QName getName() {
    return delegate.getName();
  }

  @Override
  public String getLocalName() {
    return delegate.getLocalName();
  }

  @Override
  public boolean hasName() {
    return delegate.hasName();
  }

  @Override
  public String getNamespaceURI() {
    return delegate.getNamespaceURI();
  }

  @Override
  public String getPrefix() {
    return delegate.getPrefix();
  }

  @Override
  public String getVersion() {
    return delegate.getVersion();
  }

  @Override
  public boolean isStandalone() {
    return delegate.isStandalone();
  }

  @Override
  public boolean standaloneSet() {
    return delegate.standaloneSet();
  }

  @Override
  public String getCharacterEncodingScheme() {
    return delegate.getCharacterEncodingScheme();
  }

  @Override
  public String getPITarget() {
    return delegate.getPITarget();
  }

  @Override
  public String getPIData() {
    return delegate.getPIData();
  }
}
