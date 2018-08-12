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

package nl.adaptivity.xml;

import nl.adaptivity.xmlutil.XmlWriter;
import nl.adaptivity.xmlutil.util.XmlDelegatingWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A writer for debugging that writes all events to stdout as well
 * Created by pdvrieze on 05/12/15.
 */
public class DebugWriter extends XmlDelegatingWriter {

    private static final String TAG = "DEBUGWRITER: ";

    public DebugWriter(final XmlWriter delegate) {
        super(delegate);
    }

    @Override
    public void startTag(@Nullable final String namespace, @NotNull final String localName, @Nullable final String prefix) {
        System.out.println(TAG + "startTag(namespace='"+namespace+"', localName='"+localName+"', prefix='"+prefix+"')");
        super.startTag(namespace, localName, prefix);
    }

    @Override
    public void endTag(@Nullable final String namespace, @NotNull final String localName, @Nullable final String prefix) {
        System.out.println(TAG + "endTag(namespace='"+namespace+"', localName='"+localName+"', prefix='"+prefix+"')");
        super.endTag(namespace, localName, prefix);
    }

    @Override
    public void attribute(@Nullable final String namespace, @NotNull final String name, @Nullable final String prefix, @NotNull final String value) {
        System.out.println(TAG + "  attribute(namespace='"+namespace+"', name='"+name+"', prefix='"+prefix+"', value='"+value+"')");
        super.attribute(namespace, name, prefix, value);
    }

    @Override
    public void namespaceAttr(final String namespacePrefix, final String namespaceUri) {
        System.out.println(TAG + "  namespaceAttr(namespacePrefix='"+namespacePrefix+"', namespaceUri='"+namespaceUri+"')");
        super.namespaceAttr(namespacePrefix, namespaceUri);
    }

    @Override
    public void text(final String text) {
        System.out.println(TAG + "--text('"+text+"')");
        super.text(text);
    }

    @Override
    public void ignorableWhitespace(final String text) {
        System.out.println(TAG + "  ignorableWhitespace()");
        super.ignorableWhitespace(text);
    }

    @Override
    public void startDocument(@Nullable final String version, @Nullable final String encoding, @Nullable final Boolean standalone) {
        System.out.println(TAG + "startDocument()");
        super.startDocument(version, encoding, standalone);
    }

    @Override
    public void comment(final String text) {
        System.out.println(TAG + "comment('"+text+"')");
        super.comment(text);
    }

    @Override
    public void processingInstruction(final String text) {
        System.out.println(TAG + "processingInstruction('"+text+"')");
        super.processingInstruction(text);
    }

    @Override
    public void close() {
        System.out.println(TAG + "close()");
        super.close();
    }

    @Override
    public void flush() {
        System.out.println(TAG + "flush()");
        super.flush();
    }

    @Override
    public void endDocument() {
        System.out.println(TAG + "endDocument()");
        super.endDocument();
    }
}
