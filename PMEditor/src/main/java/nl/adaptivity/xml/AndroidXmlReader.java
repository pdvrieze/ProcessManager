package nl.adaptivity.xml;

import net.devrieze.util.StringUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Created by pdvrieze on 15/11/15.
 */
public class AndroidXmlReader implements XmlReader {

  private static final int[] DELEGATE_TO_LOCAL;

  private static final int[] LOCAL_TO_DELEGATE;

  static {
    DELEGATE_TO_LOCAL = new int[11];
    DELEGATE_TO_LOCAL[XmlPullParser.CDSECT] = XmlReadingConstants.CDSECT;
    DELEGATE_TO_LOCAL[XmlPullParser.COMMENT] = XmlReadingConstants.COMMENT;
    DELEGATE_TO_LOCAL[XmlPullParser.DOCDECL] = XmlReadingConstants.DOCDECL;
    DELEGATE_TO_LOCAL[XmlPullParser.END_DOCUMENT] = XmlReadingConstants.END_DOCUMENT;
    DELEGATE_TO_LOCAL[XmlPullParser.END_TAG] = XmlReadingConstants.END_TAG;
    DELEGATE_TO_LOCAL[XmlPullParser.ENTITY_REF] = XmlReadingConstants.ENTITY_REF;
    DELEGATE_TO_LOCAL[XmlPullParser.IGNORABLE_WHITESPACE] = XmlReadingConstants.IGNORABLE_WHITESPACE;
    DELEGATE_TO_LOCAL[XmlPullParser.PROCESSING_INSTRUCTION] = XmlReadingConstants.PROCESSING_INSTRUCTION;
    DELEGATE_TO_LOCAL[XmlPullParser.START_DOCUMENT] = XmlReadingConstants.START_DOCUMENT;
    DELEGATE_TO_LOCAL[XmlPullParser.START_TAG] = XmlReadingConstants.START_TAG;
    DELEGATE_TO_LOCAL[XmlPullParser.TEXT] = XmlReadingConstants.TEXT;

    LOCAL_TO_DELEGATE = new int[11];
    LOCAL_TO_DELEGATE[XmlReadingConstants.CDSECT] = XmlPullParser.CDSECT;
    LOCAL_TO_DELEGATE[XmlReadingConstants.COMMENT] = XmlPullParser.COMMENT;
    LOCAL_TO_DELEGATE[XmlReadingConstants.DOCDECL] = XmlPullParser.DOCDECL;
    LOCAL_TO_DELEGATE[XmlReadingConstants.END_DOCUMENT] = XmlPullParser.END_DOCUMENT;
    LOCAL_TO_DELEGATE[XmlReadingConstants.END_TAG] = XmlPullParser.END_TAG;
    LOCAL_TO_DELEGATE[XmlReadingConstants.ENTITY_REF] = XmlPullParser.ENTITY_REF;
    LOCAL_TO_DELEGATE[XmlReadingConstants.IGNORABLE_WHITESPACE] = XmlPullParser.IGNORABLE_WHITESPACE;
    LOCAL_TO_DELEGATE[XmlReadingConstants.PROCESSING_INSTRUCTION] = XmlPullParser.PROCESSING_INSTRUCTION;
    LOCAL_TO_DELEGATE[XmlReadingConstants.START_DOCUMENT] = XmlPullParser.START_DOCUMENT;
    LOCAL_TO_DELEGATE[XmlReadingConstants.START_TAG] = XmlPullParser.START_TAG;
    LOCAL_TO_DELEGATE[XmlReadingConstants.TEXT] = XmlPullParser.TEXT;
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
  public int getEventType() throws XmlException {
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
  public int next() throws XmlException {
    try {
      return DELEGATE_TO_LOCAL[mReader.nextToken()];
    } catch (XmlPullParserException | IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public int nextTag() throws XmlException {
    try {
      return DELEGATE_TO_LOCAL[mReader.nextTag()];
    } catch (XmlPullParserException | IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void require(final int type, final CharSequence namespace, final CharSequence name) throws XmlException {
    try {
      mReader.require(LOCAL_TO_DELEGATE[type], StringUtil.toString(namespace), StringUtil.toString(name));
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
  public String getNamespace() {
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
  public CharSequence getAttributeLocalName(final int index) {
    return mReader.getAttributeName(index);
  }

  @Override
  public CharSequence getAttributePrefix(final int index) {
    return mReader.getAttributePrefix(index);
  }

  @Override
  public CharSequence getAttributeValue(final int index) {
    return mReader.getAttributeValue(index);
  }

  @Override
  public CharSequence getAttributeNamespace(final int index) {
    return mReader.getAttributeNamespace(index);
  }

  @Override
  public int getNamespaceStart() throws XmlException {
    require(XmlReadingConstants.START_TAG, null, null);
    try {
      return mReader.getNamespaceCount(mReader.getDepth()-1);
    } catch (XmlPullParserException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public int getNamespaceEnd() throws XmlException {
    require(XmlReadingConstants.START_TAG, null, null);
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
}
