/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.editor.android.BuildConfig;
import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;


/**
 * An android implementation of XmlWriter.
 * Created by pdvrieze on 15/11/15.
 */
public class AndroidXmlWriter extends AbstractXmlWriter {

  private final NamespaceHolder mNamespaceHolder = new NamespaceHolder();
  private final boolean mRepairNamespaces;
  private final XmlSerializer mWriter;

// Object Initialization
  public AndroidXmlWriter(final Writer writer) throws XmlPullParserException, IOException {
    this(writer, true);
  }

  public AndroidXmlWriter(final Writer writer, boolean repairNamespaces) throws XmlPullParserException, IOException {
    this(repairNamespaces);
    mWriter.setOutput(writer);
    initWriter(mWriter);
  }

  private AndroidXmlWriter(boolean repairNamespaces) throws XmlPullParserException {
    mRepairNamespaces = repairNamespaces;
    mWriter = new BetterXmlSerializer();
    initWriter(mWriter);
  }

  public AndroidXmlWriter(final OutputStream outputStream, String encoding) throws XmlPullParserException, IOException {
    this(outputStream, encoding, true);
  }

  public AndroidXmlWriter(final OutputStream outputStream, String encoding, boolean repairNamespaces) throws XmlPullParserException, IOException {
    this(repairNamespaces);
    mWriter.setOutput(outputStream, encoding);
    initWriter(mWriter);
  }

  private static void initWriter(final XmlSerializer writer) {
    try {
      writer.setPrefix(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public AndroidXmlWriter(final XmlSerializer serializer) {
    this(serializer, true);
  }

  public AndroidXmlWriter(final XmlSerializer serializer, boolean repairNamespaces) {
    mWriter = serializer;
    mRepairNamespaces = repairNamespaces;
    initWriter(mWriter);
  }

// Object Initialization end

  @Override
  public void flush() throws XmlException {
    try {
      mWriter.flush();
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void startTag(@Nullable final CharSequence namespace, @NonNull final CharSequence localName, @Nullable final CharSequence prefix) throws
          XmlException {
    String namespaceStr = StringUtil.toString(namespace.toString());
    try {
      if (namespace != null) {
        mWriter.setPrefix(prefix==null ? "" : prefix.toString(), namespaceStr);
      }
      mWriter.startTag(namespaceStr, StringUtil.toString(localName));
      mNamespaceHolder.incDepth();
      ensureNamespaceIfRepairing(namespace, prefix);
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  private void ensureNamespaceIfRepairing(final @NonNull CharSequence namespace, final @Nullable CharSequence prefix) throws XmlException {
    if (mRepairNamespaces && namespace!=null && namespace.length()>0 && prefix!=null) {
      // TODO fix more cases than missing namespaces with given prefix and uri
      if (! StringUtil.isEqual(mNamespaceHolder.getNamespaceUri(prefix), namespace)) {
        namespaceAttr(prefix, namespace);
      }
    }
  }

  @Override
  public void comment(final CharSequence text) throws XmlException {
    try {
      mWriter.comment(StringUtil.toString(text));
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void text(final CharSequence text) throws XmlException {
    try {
      mWriter.text(text.toString());
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void cdsect(final CharSequence text) throws XmlException {
    try {
      mWriter.cdsect(text.toString());
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void entityRef(final CharSequence text) throws XmlException {
    try {
      mWriter.entityRef(StringUtil.toString(text));
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void processingInstruction(final CharSequence text) throws XmlException {
    try {
      mWriter.processingInstruction(StringUtil.toString(text));
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void ignorableWhitespace(final CharSequence text) throws XmlException {
    try {
      mWriter.ignorableWhitespace(StringUtil.toString(text));
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void attribute(final CharSequence namespace, final CharSequence name, final CharSequence prefix, final CharSequence value) throws XmlException {
    try {
      String sNamespace = StringUtil.toString(namespace);
      String sPrefix = StringUtil.toString(prefix);
      if (sPrefix!=null && sNamespace!=null) { setPrefix(sPrefix, sNamespace); }
      mWriter.attribute(sNamespace, StringUtil.toString(name), StringUtil.toString(value));
      ensureNamespaceIfRepairing(sNamespace, sPrefix);
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void docdecl(final CharSequence text) throws XmlException {
    try {
      mWriter.docdecl(StringUtil.toString(text));
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @param version Unfortunately the serializer is forced to version 1.0
   */
  @Override
  public void startDocument(@org.jetbrains.annotations.Nullable final CharSequence version, @org.jetbrains.annotations.Nullable final CharSequence encoding, @org.jetbrains.annotations.Nullable final Boolean standalone) throws XmlException {
    try {
      mWriter.startDocument(StringUtil.toString(encoding), standalone);
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void endDocument() throws XmlException {
    if (BuildConfig.DEBUG && getDepth() != 0) throw new AssertionError();
    try {
      mWriter.endDocument();
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void endTag(@org.jetbrains.annotations.Nullable final CharSequence namespace, @NotNull final CharSequence localName, @org.jetbrains.annotations.Nullable final CharSequence prefix) throws XmlException {
    try {
      mWriter.endTag(StringUtil.toString(namespace), StringUtil.toString(localName));
      mNamespaceHolder.decDepth();
    } catch (IOException e) {
      throw new XmlException(e);
    }
  }

  @Override
  public void setPrefix(final CharSequence prefix, final CharSequence namespaceUri) throws XmlException {
    if (! StringUtil.isEqual(namespaceUri, getNamespaceUri(prefix))) {
      mNamespaceHolder.addPrefixToContext(prefix, namespaceUri);
      try {
        mWriter.setPrefix(StringUtil.toString(prefix), StringUtil.toString(namespaceUri));
      } catch (IOException e) {
        throw new XmlException(e);
      }
    }
  }

  @Override
  public void namespaceAttr(final CharSequence namespacePrefix, final CharSequence namespaceUri) throws XmlException {
    mNamespaceHolder.addPrefixToContext(namespacePrefix, namespaceUri);
    try {
      if (namespacePrefix!=null && namespacePrefix.length()>0) {
        mWriter.attribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, StringUtil.toString(namespacePrefix), StringUtil.toString(namespaceUri));
      } else {
        mWriter.attribute(XMLConstants.NULL_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, StringUtil.toString(namespaceUri));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return mNamespaceHolder.getNamespaceContext();
  }

  @org.jetbrains.annotations.Nullable
  @Override
  public CharSequence getNamespaceUri(@NotNull final CharSequence prefix) {
    return mNamespaceHolder.getNamespaceUri(prefix);
  }

  @org.jetbrains.annotations.Nullable
  @Override
  public CharSequence getPrefix(@org.jetbrains.annotations.Nullable final CharSequence namespaceUri) {
    return mNamespaceHolder.getPrefix(namespaceUri);
  }

  @Override
  public void close() throws XmlException {
    super.close();
    mNamespaceHolder.clear();
  }

  // Property accessors start
  @Override
  public int getDepth() {
    return mNamespaceHolder.getDepth();
  }
// Property acccessors end
}
