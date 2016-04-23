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

package nl.adaptivity.process.editor.android;

import android.util.Log;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.clientProcessModel.ClientProcessNode;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.xml.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import nl.adaptivity.xml.XmlStreaming.EventType;
import nl.adaptivity.xml.XmlStreaming;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import java.io.*;
import java.util.*;

import static nl.adaptivity.xml.XmlStreaming.*;

public final class PMParser {

  public static final String MIME_TYPE="application/x-processmodel";

  public static final String NS_PROCESSMODEL="http://adaptivity.nl/ProcessEngine/";

  public static void serializeProcessModel(final OutputStream out, final ClientProcessModel<?, ?> processModel) throws XmlPullParserException, IOException, XmlException {
    final XmlSerializer serializer = getSerializer(out);
    final AndroidXmlWriter writer = new AndroidXmlWriter(serializer);
    processModel.serialize(writer);
    writer.close();
  }

  public static void serializeProcessModel(final Writer out, final ClientProcessModel<?, ?> processModel) throws XmlPullParserException, IOException, XmlException {
    final XmlSerializer serializer = getSerializer(out);
    final AndroidXmlWriter writer = new AndroidXmlWriter(serializer);
    processModel.serialize(writer);
    writer.close();
  }

  private static BetterXmlSerializer getSerializer() throws XmlPullParserException {
    final XmlSerializer serializer = new BetterXmlSerializer();
    serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", false);
    return (BetterXmlSerializer) serializer;
  }

  public static XmlSerializer getSerializer(final OutputStream out) throws XmlPullParserException, IOException {
    final XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(out, "UTF-8");
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  public static XmlSerializer getSerializer(final Writer out) throws XmlPullParserException, IOException {
    final XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(out);
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  private static <T extends ClientProcessNode<T,M>, M extends ClientProcessModel<T,M>> void serializeProcessModel(final XmlSerializer serializer, final DrawableProcessModel processModel) {
    try {
      serializer.startDocument(null, null);
      serializer.ignorableWhitespace("\n");
      processModel.serialize(new AndroidXmlWriter(serializer));
      serializer.endDocument();
    } catch (IllegalArgumentException | IllegalStateException | IOException | XmlException e) {
      throw new RuntimeException(e);
    }
  }

  public static DrawableProcessModel parseProcessModel(final Reader in, final LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, final LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    try {
      return parseProcessModel(new AndroidXmlReader(in), simpleLayoutAlgorithm, advancedAlgorithm);
    } catch (Exception e){
      Log.e(PMEditor.class.getSimpleName(), e.getMessage(), e);
      return null;
    }
  }

  public static DrawableProcessModel parseProcessModel(final InputStream in, final LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, final LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    try {
      return parseProcessModel(new AndroidXmlReader(in, "UTF-8"), simpleLayoutAlgorithm, advancedAlgorithm);
    } catch (Exception e){
      Log.e(PMEditor.class.getSimpleName(), e.getMessage(), e);
      return null;
    }
  }

  public static DrawableProcessModel parseProcessModel(final XmlReader in, final LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, final LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) throws XmlException {
    final DrawableProcessModel result = DrawableProcessModel.deserialize(in);
    if (result.hasUnpositioned()) {
      result.setLayoutAlgorithm(advancedAlgorithm);
      result.layout();
    } else {
      result.setLayoutAlgorithm(simpleLayoutAlgorithm);
    }
    return result;
  }

  private static QName toQName(final XmlPullParser in, final String value) {
    if (value==null) { return null; }
    final int i = value.indexOf(':');
    if (i>0) {
      final String prefix = value.substring(0, i);
      final String namespace = in.getNamespace(prefix);
      final String localname = value.substring(i + 1);
      return new QName(namespace, localname, prefix);
    } else {
      final String namespace = in.getNamespace("");
      return new QName(namespace, value);
    }
  }

  private static void parseUnknownTag(final XmlReader in) throws XmlException {
    for(XmlStreaming.EventType type = in.next(); type!= END_ELEMENT; type = in.next()) {
      switch (type) {
      case START_ELEMENT:
        parseUnknownTag(in);
        break;
      default:
          // ignore
      }
    }
  }

  private static String trimWS(final CharSequence str) {
    if (str==null) { return null; }
    int start, end;
    for(start=0;start<str.length()&&isXMLWS(str.charAt(start));++start) {/*no body*/}
    for(end=str.length()-1;end>=start&& isXMLWS(str.charAt(end));--end) {/*no body*/}
    return str.subSequence(start, end+1).toString();
  }

  private static boolean isXMLWS(final int codepoint) {
    return codepoint<=0x20 && (codepoint==0x20||codepoint==0x9||codepoint==0xD||codepoint==0xA);
  }

  private static void parseJoinSplitAttrs(final XmlReader in, final DrawableJoinSplit node) throws XmlException {
    for(int i=0; i< in.getAttributeCount();++i) {
      if (XMLConstants.NULL_NS_URI.equals(in.getAttributeNamespace(i))) {
        final CharSequence aname = in.getAttributeLocalName(i);
        if (StringUtil.isEqual("min",aname)) {
          node.setMin(Integer.parseInt(in.getAttributeValue(i).toString()));
        } else if ("max".equals(aname)) {
          node.setMax(Integer.parseInt(in.getAttributeValue(i).toString()));
        }
      }
    }
  }

  private static void parseCommon(final XmlReader in, final Map<String, DrawableProcessNode> nodes, final List<DrawableProcessNode> modelElems, final DrawableProcessNode node) throws
          XmlException {
    for(int i=0; i< in.getAttributeCount();++i) {
      if (XMLConstants.NULL_NS_URI.equals(in.getAttributeNamespace(i))) {
        final CharSequence aname = in.getAttributeLocalName(i);
        if ("x".equals(aname)) {
          node.setX(Double.parseDouble(in.getAttributeValue(i).toString()));
        } else if ("y".equals(aname)) {
          node.setY(Double.parseDouble(in.getAttributeValue(i).toString()));
        } else if ("id".equals(aname)) {
          node.setId(trimWS(in.getAttributeValue(i)));
        } else if ("label".equals(aname)) {
          node.setLabel(in.getAttributeValue(i).toString());
        } else if ("name".equals(aname)) {
          if (node.getLabel()==null) {
            node.setLabel(in.getAttributeValue(i).toString());
          }
        } else if ("predecessor".equals(aname)) {
          addPredecessor(node, trimWS(in.getAttributeValue(i)), nodes, modelElems);
//          pNode.setPredecessors(getPredecessors(pIn.getAttributeValue(i),pNodes, pModelElems));
        }
      }
    }
  }

  private static void addPredecessor(final DrawableProcessNode node, final String name, final Map<String, DrawableProcessNode> nodes, final List<DrawableProcessNode> modelElems) {
    final Identifiable predecessor = getPredecessor(name, nodes, modelElems);
    if (predecessor instanceof DrawableProcessNode) {
      addAsSuccessor((DrawableProcessNode) predecessor, node, modelElems);
    }
  }

  private static Identifiable getPredecessor(final String name, final Map<String, DrawableProcessNode> nodes, final List<DrawableProcessNode> modelElems) {
    final DrawableProcessNode val = nodes.get(name);
    if (val==null) {
      return new Identifier(name);
    } else { // there already is a node
      // Allow temporary references to collect as many successors as desired, it might be a split.
      if ((val.getSuccessors().size() < val.getMaxSuccessorCount())) {
        return val;
      } else {
        // There is no suitable successor
        return introduceSplit(val, modelElems);
      }
    }
  }

  private static DrawableSplit introduceSplit(final DrawableProcessNode predecessor, final List<DrawableProcessNode> modelElems) {
    for(final Identifiable successor:predecessor.getSuccessors()) {
      if (successor instanceof DrawableSplit) {
        return (DrawableSplit) successor;
      }
    }
    final DrawableSplit newSplit = new DrawableSplit((DrawableProcessModel) null);

    final ArrayList<Identifiable> successors = new ArrayList<>(predecessor.getSuccessors());
    for(final Identifiable successorId: successors) {
      final DrawableProcessNode successor = (DrawableProcessNode) successorId;
      predecessor.removeSuccessor(successor);
      successor.removePredecessor(predecessor);
      newSplit.addSuccessor(successor);
      successor.addPredecessor(newSplit);
    }
    predecessor.addSuccessor(newSplit);
    newSplit.addPredecessor(predecessor);
    modelElems.add(newSplit);
    return newSplit;
  }

  private static void addAsSuccessor(final DrawableProcessNode predecessor, final DrawableProcessNode successor, final List<DrawableProcessNode> modelElems) {
    if (predecessor.getSuccessors().size()<predecessor.getMaxSuccessorCount()) {
      predecessor.addSuccessor(successor);
    } else {
      final DrawableSplit newSplit = introduceSplit(predecessor, modelElems);
      newSplit.addSuccessor(successor);
      successor.addPredecessor(newSplit);
    }

  }

}
