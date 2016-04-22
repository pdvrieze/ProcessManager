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

import org.xmlpull.v1.XmlSerializer;

import javax.xml.XMLConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class BetterXmlSerializer implements XmlSerializer{

  //    static final String UNDEFINED = ":";

  private Writer writer;

  private boolean pending;
  private int auto;
  private int depth;

  private String[] elementStack = new String[12];
  //nsp/prefix/name
  private int[] nspCounts = new int[4];
  private String[] nspStack = new String[10];
  private boolean[] nspWritten = new boolean[5];
  //prefix/nsp; both empty are ""
  private boolean[] indent = new boolean[4];
  private boolean unicode;
  private String encoding;
  private boolean escapeAggressive = false;

  private final void check(boolean close) throws IOException {
    if (!pending)
      return;

    depth++;
    pending = false;

    if (indent.length <= depth) {
      boolean[] hlp = new boolean[depth + 4];
      System.arraycopy(indent, 0, hlp, 0, depth);
      indent = hlp;
    }
    indent[depth] = indent[depth - 1];

    if (nspCounts.length <= depth + 1) {
      int[] hlp = new int[depth + 8];
      System.arraycopy(nspCounts, 0, hlp, 0, depth + 1);
      nspCounts = hlp;
    }

    nspCounts[depth + 1] = nspCounts[depth];
    //   nspCounts[depth + 2] = nspCounts[depth];

    writer.write(close ? " />" : ">");
  }

  private final void writeEscaped(String s, int quot)
          throws IOException {

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '&' :
        writer.write("&amp;");
        break;
        case '>' :
        writer.write("&gt;");
        break;
        case '<' :
        writer.write("&lt;");
        break;
        case '"' :
        case '\'' :
        if (c == quot) {
          writer.write(
                    c == '"' ? "&quot;" : "&apos;");
          break;
        }
        case '\n':
        case '\r':
        case '\t':
          if(escapeAggressive && quot != -1) {
            writer.write("&#"+((int) c)+';');
          } else {
            writer.write(c);
          }
          break;
        default :
          //if(c < ' ')
          //	throw new IllegalArgumentException("Illegal control code:"+((int) c));
          if (escapeAggressive && (c<' ' || c=='@' || (c>127 && ! unicode))) {
            writer.write("&#" + ((int) c) + ";");
          } else {
            writer.write(c);
          }
      }
    }
  }

    /*
    	private final void writeIndent() throws IOException {
    		writer.write("\r\n");
    		for (int i = 0; i < depth; i++)
    			writer.write(' ');
    	}*/

  public void docdecl(String dd) throws IOException {
    writer.write("<!DOCTYPE");
    writer.write(dd);
    writer.write(">");
  }

  public void endDocument() throws IOException {
    while (depth > 0) {
      endTag(
              elementStack[depth * 3 - 3],
              elementStack[depth * 3 - 1]);
    }
    flush();
  }

  public void entityRef(String name) throws IOException {
    check(false);
    writer.write('&');
    writer.write(name);
    writer.write(';');
  }

  public boolean getFeature(String name) {
    //return false;
    return (
            "http://xmlpull.org/v1/doc/features.html#indent-output"
                    .equals(
                            name))
            ? indent[depth]
            : false;
  }

  public String getPrefix(String namespace, boolean create) {
    try {
      return getPrefix(namespace, false, create);
    }
    catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
  }

  private final String getPrefix(String namespace, boolean includeDefault, boolean create)
          throws IOException {

    for (int i = nspCounts[depth + 1] * 2 - 2;
         i >= 0;
         i -= 2) {
      if (nspStack[i + 1].equals(namespace)
              && (includeDefault || !nspStack[i].equals(""))) {
        String candidate = nspStack[i];
        for (int j = i + 2;
             j < nspCounts[depth + 1] * 2;
             j++) {
          if (nspStack[j].equals(candidate)) {
            candidate = null;
            break;
          }
        }
        if (candidate != null) {
          return candidate;
        }
      }
    }

    if (!create)
      return null;

    String prefix;

    if ("".equals(namespace))
      prefix = "";
    else {
      do {
        prefix = "n" + (auto++);
        for (int i = nspCounts[depth + 1] * 2 - 2;
             i >= 0;
             i -= 2) {
          if (prefix.equals(nspStack[i])) {
            prefix = null;
            break;
          }
        }
      }
      while (prefix == null);
    }

    boolean p = pending;
    pending = false;
    setPrefix(prefix, namespace);
    pending = p;
    return prefix;
  }

  public Object getProperty(String name) {
    throw new RuntimeException("Unsupported property");
  }

  public void ignorableWhitespace(String s)
          throws IOException {
    text(s);
  }

  public void setFeature(String name, boolean value) {
    if ("http://xmlpull.org/v1/doc/features.html#indent-output"
            .equals(name)) {
      indent[depth] = value;
    }
    else
      throw new RuntimeException("Unsupported Feature");
  }

  public void setProperty(String name, Object value) {
    throw new RuntimeException(
            "Unsupported Property:" + value);
  }

  public void setPrefix(String prefix, String namespace) throws IOException {

    if (prefix == null)
      prefix = "";
    if (namespace == null)
      namespace = "";

    final int depth = this.depth +1;

    for (int i = nspCounts[depth] * 2 - 2;
         i >= 0;
         i -= 2) {
      if (nspStack[i + 1].equals(namespace) && nspStack[i].equals(prefix)) {
        // bail out if already defined
        return;
      }
    }


    int pos = (nspCounts[depth]++) << 1;

    addSpaceToNspStack();

    nspStack[pos++] = prefix;
    nspStack[pos] = namespace;
    nspWritten[nspCounts[depth]-1] = false;
  }

  private void addSpaceToNspStack() {
    int nspCount = nspCounts[pending ? depth + 1 : depth];
    int pos = nspCount << 1;
    if (nspStack.length < pos + 2) {
      {
        String[] hlp = new String[nspStack.length + 16];
        System.arraycopy(nspStack, 0, hlp, 0, pos);
        nspStack = hlp;
      }
      {
        boolean[] help = new boolean[nspWritten.length + 8];
        System.arraycopy(nspWritten, 0, help, 0, nspCount);
        nspWritten = help;
      }
    }
  }

  public void setOutput(Writer writer) {
    this.writer = writer;

    // elementStack = new String[12]; //nsp/prefix/name
    //nspCounts = new int[4];
    //nspStack = new String[8]; //prefix/nsp
    //indent = new boolean[4];

    nspCounts[0] = 3;
    nspCounts[1] = 3;
    nspStack[0] = "";
    nspStack[1] = "";
    nspStack[2] = "xml";
    nspStack[3] = "http://www.w3.org/XML/1998/namespace";
    nspStack[4] = "xmlns";
    nspStack[5] = "http://www.w3.org/2000/xmlns/";
    pending = false;
    auto = 0;
    depth = 0;

    unicode = false;
  }

  public void setOutput(OutputStream os, String encoding)
          throws IOException {
    if (os == null)
      throw new IllegalArgumentException();
    setOutput(
            encoding == null
                    ? new OutputStreamWriter(os)
                    : new OutputStreamWriter(os, encoding));
    this.encoding = encoding;
    if (encoding != null
            && encoding.toLowerCase().startsWith("utf"))
      unicode = true;
  }

  public void startDocument(
          String encoding,
          Boolean standalone)
          throws IOException {
    writer.write("<?xml version='1.0' ");

    if (encoding != null) {
      this.encoding = encoding;
      if (encoding.toLowerCase().startsWith("utf"))
        unicode = true;
    }

    if (this.encoding != null) {
      writer.write("encoding='");
      writer.write(this.encoding);
      writer.write("' ");
    }

    if (standalone != null) {
      writer.write("standalone='");
      writer.write(
              standalone.booleanValue() ? "yes" : "no");
      writer.write("' ");
    }
    writer.write("?>");
  }

  public BetterXmlSerializer startTag(String namespace, String name)
          throws IOException {
    check(false);

    //        if (namespace == null)
    //            namespace = "";

    if (indent[depth]) {
      writer.write("\r\n");
      for (int i = 0; i < depth; i++)
        writer.write("  ");
    }

    int esp = depth * 3;

    if (elementStack.length < esp + 3) {
      String[] hlp = new String[elementStack.length + 12];
      System.arraycopy(elementStack, 0, hlp, 0, esp);
      elementStack = hlp;
    }

    String prefix =
            namespace == null
                    ? ""
                    : getPrefix(namespace, true, true);

    if (namespace.isEmpty()) {
      for (int i = nspCounts[depth];
           i < nspCounts[depth + 1];
           i++) {
        if ("".equals(nspStack[i * 2]) && !"".equals(nspStack[i * 2 + 1])) {
          throw new IllegalStateException("Cannot set default namespace for elements in no namespace");
        }
      }
    }

    elementStack[esp++] = namespace;
    elementStack[esp++] = prefix;
    elementStack[esp] = name;

    writer.write('<');
    if (prefix.length()>0) {
      writer.write(prefix);
      writer.write(':');
    }

    writer.write(name);

    pending = true;

    return this;
  }

  public BetterXmlSerializer attribute(
          String namespace,
          String name,
          String value)
          throws IOException {
    if (!pending)
      throw new IllegalStateException("illegal position for attribute");

    //        int cnt = nspCounts[depth];

    if (namespace == null) {
      namespace = "";
    } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace)) {
      return namespace(name, value); // If it is a namespace attribute, just go there.
    } else if (XMLConstants.NULL_NS_URI.equals(namespace) && XMLConstants.XMLNS_ATTRIBUTE.equals(name)) {
      return namespace("", value); // If it is a namespace attribute, just go there.
    }

    //		depth--;
    //		pending = false;

    String prefix =
            "".equals(namespace)
                    ? ""
                    : getPrefix(namespace, false, true);

    //		pending = true;
    //		depth++;

        /*        if (cnt != nspCounts[depth]) {
                    writer.write(' ');
                    writer.write("xmlns");
                    if (nspStack[cnt * 2] != null) {
                        writer.write(':');
                        writer.write(nspStack[cnt * 2]);
                    }
                    writer.write("=\"");
                    writeEscaped(nspStack[cnt * 2 + 1], '"');
                    writer.write('"');
                }
                */

    writer.write(' ');
    if (!"".equals(prefix)) {
      writer.write(prefix);
      writer.write(':');
    }
    writer.write(name);
    writer.write('=');
    char q = value.indexOf('"') == -1 ? '"' : '\'';
    writer.write(q);
    writeEscaped(value, q);
    writer.write(q);

    return this;
  }

  public BetterXmlSerializer namespace(
          String prefix,
          String namespace)
          throws IOException {
    if (!pending)
      throw new IllegalStateException("illegal position for attribute");

    boolean wasSet = false;
    for (int i = nspCounts[depth];
         i < nspCounts[depth+1];
         i++) {
      if (prefix.equals(nspStack[i * 2])) {
        if (! nspStack[i * 2 + 1].equals(namespace)) { // If we find the prefix redefined within the element, bail out
          throw new IllegalArgumentException("Attempting to bind prefix to conflicting values in one element");
        } if (nspWritten[i]) {
          // otherwise just ignore the request.
          return this;
        }
        nspWritten[i] = true;
        wasSet = true;
        break;
      }
    }

    if (! wasSet) { // Don't use setPrefix as we know it isn't there
      addSpaceToNspStack();
      int pos = (nspCounts[depth+1]++)<<1;
      nspStack[pos] = prefix;
      nspStack[pos+1] = namespace;
      nspWritten[pos>>1] = true;
    }

    if (namespace == null)
      namespace = "";

    writer.write(' ');
    writer.write(XMLConstants.XMLNS_ATTRIBUTE);
    if (prefix.length()>0) {
      writer.write(':');
      writer.write(prefix);
    }
    writer.write('=');
    char q = namespace.indexOf('"') == -1 ? '"' : '\'';
    writer.write(q);
    writeEscaped(namespace, q);
    writer.write(q);

    return this;
  }

  public void flush() throws IOException {
    check(false);
    writer.flush();
  }
  /*
    public void close() throws IOException {
      check();
      writer.close();
    }
  */
  public BetterXmlSerializer endTag(String namespace, String name)
          throws IOException {

    if (!pending)
      depth--;
    //        if (namespace == null)
    //          namespace = "";

    if ((namespace == null
            && elementStack[depth * 3] != null)
            || (namespace != null
            && !namespace.equals(elementStack[depth * 3]))
            || !elementStack[depth * 3 + 2].equals(name))
      throw new IllegalArgumentException("</{"+namespace+"}"+name+"> does not match start");

    if (pending) {
      check(true);
      depth--;
    }
    else {
      if (indent[depth + 1]) {
        writer.write("\r\n");
        for (int i = 0; i < depth; i++)
          writer.write("  ");
      }

      writer.write("</");
      String prefix = elementStack[depth * 3 + 1];
      if (!"".equals(prefix)) {
        writer.write(prefix);
        writer.write(':');
      }
      writer.write(name);
      writer.write('>');
    }

    nspCounts[depth + 1] = nspCounts[depth];
    return this;
  }

  public String getNamespace() {
    return getDepth() == 0 ? null : elementStack[getDepth() * 3 - 3];
  }

  public String getName() {
    return getDepth() == 0 ? null : elementStack[getDepth() * 3 - 1];
  }

  public int getDepth() {
    return pending ? depth + 1 : depth;
  }

  public BetterXmlSerializer text(String text) throws IOException {
    check(false);
    indent[depth] = false;
    writeEscaped(text, -1);
    return this;
  }

  public BetterXmlSerializer text(char[] text, int start, int len)
          throws IOException {
    text(new String(text, start, len));
    return this;
  }

  public void cdsect(String data) throws IOException {
    check(false);
    writer.write("<![CDATA[");
    writer.write(data);
    writer.write("]]>");
  }

  public void comment(String comment) throws IOException {
    check(false);
    writer.write("<!--");
    writer.write(comment);
    writer.write("-->");
  }

  public void processingInstruction(String pi)
          throws IOException {
    check(false);
    writer.write("<?");
    writer.write(pi);
    writer.write("?>");
  }
}
