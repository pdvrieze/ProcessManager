package nl.adaptivity.xml;

import net.devrieze.util.StringUtil;
import nl.adaptivity.util.xml.SimpleNamespaceContext;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Created by pdvrieze on 15/11/15.
 */
public class AndroidXmlReader extends AbstractXmlReader {

  private static final EventType[] DELEGATE_TO_LOCAL;

  private static final int[] LOCAL_TO_DELEGATE;

  static {
    DELEGATE_TO_LOCAL = new EventType[11];
    DELEGATE_TO_LOCAL[XmlPullParser.CDSECT] = XmlStreaming.CDSECT;
    DELEGATE_TO_LOCAL[XmlPullParser.COMMENT] = XmlStreaming.COMMENT;
    DELEGATE_TO_LOCAL[XmlPullParser.DOCDECL] = XmlStreaming.DOCDECL;
    DELEGATE_TO_LOCAL[XmlPullParser.END_DOCUMENT] = XmlStreaming.END_DOCUMENT;
    DELEGATE_TO_LOCAL[XmlPullParser.END_TAG] = XmlStreaming.END_ELEMENT;
    DELEGATE_TO_LOCAL[XmlPullParser.ENTITY_REF] = XmlStreaming.ENTITY_REF;
    DELEGATE_TO_LOCAL[XmlPullParser.IGNORABLE_WHITESPACE] = XmlStreaming.IGNORABLE_WHITESPACE;
    DELEGATE_TO_LOCAL[XmlPullParser.PROCESSING_INSTRUCTION] = XmlStreaming.PROCESSING_INSTRUCTION;
    DELEGATE_TO_LOCAL[XmlPullParser.START_DOCUMENT] = XmlStreaming.START_DOCUMENT;
    DELEGATE_TO_LOCAL[XmlPullParser.START_TAG] = XmlStreaming.START_ELEMENT;
    DELEGATE_TO_LOCAL[XmlPullParser.TEXT] = XmlStreaming.TEXT;

    LOCAL_TO_DELEGATE = new int[12];
    LOCAL_TO_DELEGATE[XmlStreaming.CDSECT.ordinal()] = XmlPullParser.CDSECT;
    LOCAL_TO_DELEGATE[XmlStreaming.COMMENT.ordinal()] = XmlPullParser.COMMENT;
    LOCAL_TO_DELEGATE[XmlStreaming.DOCDECL.ordinal()] = XmlPullParser.DOCDECL;
    LOCAL_TO_DELEGATE[XmlStreaming.END_DOCUMENT.ordinal()] = XmlPullParser.END_DOCUMENT;
    LOCAL_TO_DELEGATE[XmlStreaming.END_ELEMENT.ordinal()] = XmlPullParser.END_TAG;
    LOCAL_TO_DELEGATE[XmlStreaming.ENTITY_REF.ordinal()] = XmlPullParser.ENTITY_REF;
    LOCAL_TO_DELEGATE[XmlStreaming.IGNORABLE_WHITESPACE.ordinal()] = XmlPullParser.IGNORABLE_WHITESPACE;
    LOCAL_TO_DELEGATE[XmlStreaming.PROCESSING_INSTRUCTION.ordinal()] = XmlPullParser.PROCESSING_INSTRUCTION;
    LOCAL_TO_DELEGATE[XmlStreaming.START_DOCUMENT.ordinal()] = XmlPullParser.START_DOCUMENT;
    LOCAL_TO_DELEGATE[XmlStreaming.START_ELEMENT.ordinal()] = XmlPullParser.START_TAG;
    LOCAL_TO_DELEGATE[XmlStreaming.TEXT.ordinal()] = XmlPullParser.TEXT;
    LOCAL_TO_DELEGATE[XmlStreaming.ATTRIBUTE.ordinal()] = Integer.MIN_VALUE;
  }

  final XmlPullParser mReader;

  private AndroidXmlReader() throws XmlPullParserException {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    factory.setNamespaceAware(true);
    mReader = factory.newPullParser();
  }

  public AndroidXmlReader(Reader in) throws XmlPullParserException {
    this();
    mReader.setInput(in);
  }

  public AndroidXmlReader(InputStream in, String encoding) throws XmlPullParserException {
    this();
    mReader.setInput(in, encoding);
  }

  @Override
  public EventType getEventType() throws XmlException {
    try {
      return DELEGATE_TO_LOCAL[mReader.getEventType()];
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public String getAttributeValue(final CharSequence namespace, final CharSequence name) {
    return mReader.getAttributeValue(StringUtil.toString(namespace), StringUtil.toString(name));
  }

  @Override
  public boolean isWhitespace() throws XmlException {
    try {
      return mReader.isWhitespace();
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public boolean hasNext() throws XmlException {
    // TODO make this more robust (if needed)
    return getEventType()!=XmlStreaming.END_DOCUMENT;
  }

  @Override
  public EventType next() throws XmlException {
    try {
      return DELEGATE_TO_LOCAL[mReader.nextToken()];
    } catch (XmlPullParserException | IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public EventType nextTag() throws XmlException {
    try {
      return DELEGATE_TO_LOCAL[mReader.nextTag()];
    } catch (XmlPullParserException | IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void require(final EventType type, final CharSequence namespace, final CharSequence name) throws XmlException {
    try {
      mReader.require(LOCAL_TO_DELEGATE[type.ordinal()], StringUtil.toString(namespace), StringUtil.toString(name));
    } catch (XmlPullParserException | IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public int getDepth() {
    return mReader.getDepth();
  }

  @Override
  public String getText() {
    return mReader.getText();
  }

  @Override
  public String getLocalName() {
    return mReader.getName();
  }

  @Override
  public String getNamespaceUri() {
    return mReader.getNamespace();
  }

  @Override
  public String getPrefix() {
    return mReader.getPrefix();
  }

  @Override
  public int getAttributeCount() {
    return mReader.getAttributeCount();
  }

  @Override
  public String getAttributeLocalName(final int index) {
    return mReader.getAttributeName(index);
  }

  @Override
  public String getAttributePrefix(final int index) {
    return mReader.getAttributePrefix(index);
  }

  @Override
  public String getAttributeValue(final int index) {
    return mReader.getAttributeValue(index);
  }

  @Override
  public String getAttributeNamespace(final int index) {
    return mReader.getAttributeNamespace(index);
  }

  @Override
  public int getNamespaceStart() throws XmlException {
    require(XmlStreaming.START_ELEMENT, null, null);
    try {
      return mReader.getNamespaceCount(mReader.getDepth()-1);
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public int getNamespaceEnd() throws XmlException {
    require(XmlStreaming.START_ELEMENT, null, null);
    try {
      return mReader.getNamespaceCount(mReader.getDepth());
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public String getNamespaceUri(final int pos) throws XmlException {
    try {
      return mReader.getNamespaceUri(pos);
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public String getNamespacePrefix(final int pos) throws XmlException {
    try {
      return mReader.getNamespacePrefix(pos);
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public String getNamespaceUri(final CharSequence prefix) throws XmlException {
    try {
      for(int i = mReader.getNamespaceCount(mReader.getDepth()); i>=0; --i) {
        if (StringUtil.isEqual(prefix, mReader.getNamespacePrefix(i))) {
          return mReader.getNamespaceUri(i);
        }
      }
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
    if (prefix==null || prefix.length()==0) {
      return XMLConstants.NULL_NS_URI;
    }
    return null;
  }

  @Override
  public String getNamespacePrefix(final CharSequence namespaceUri) throws XmlException {
    if (namespaceUri==null || namespaceUri.length()==0) {
      return XMLConstants.DEFAULT_NS_PREFIX;
    }
    try {
      for(int i = mReader.getNamespaceCount(mReader.getDepth()); i>=0; --i) {
        if (StringUtil.isEqual(namespaceUri, mReader.getNamespaceUri(i))) {
          return mReader.getNamespacePrefix(i);
        }
      }
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
    return null;
  }

  @Override
  public String getLocationInfo() {
    return new StringBuilder(Integer.toString(mReader.getLineNumber())).append(':').append(Integer.toString(mReader.getColumnNumber())).toString();
  }

  @Override
  public Boolean getStandalone() {
    return (Boolean) mReader.getProperty("xmldecl-standalone");
  }

  @Override
  public String getEncoding() {
    return mReader.getInputEncoding();
  }

  @Override
  public CharSequence getVersion() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * This method creates a new immutable context, so keeping the context around is valid. For
   * reduced perfomance overhead use {@link #getNamespacePrefix(CharSequence)} and {@link #getNamespaceUri(CharSequence)}
   * for lookups.
   */
  @Override
  public NamespaceContext getNamespaceContext() throws XmlException {
    try {
      int nsCount = mReader.getNamespaceCount(mReader.getDepth());
      String[] prefixes = new String[nsCount];
      String[] uris = new String[nsCount];
      for(int i=0; i<nsCount; ++i) {
        prefixes[i] = mReader.getNamespacePrefix(i);
        uris[i] = mReader.getNamespaceUri(i);
      }
      return new SimpleNamespaceContext(prefixes, uris);
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void close() throws XmlException {
    /* Does nothing in this implementation */
  }
}
