package nl.adaptivity.process.editor.android;

import android.support.v4.util.ArrayMap;
import android.util.Log;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.clientProcessModel.ClientMessage;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.SimpleNamespaceContext;
import nl.adaptivity.xml.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import java.io.*;
import java.util.*;

import static nl.adaptivity.xml.XmlStreaming.*;

public class PMParser {

  public static final String MIME_TYPE="application/x-processmodel";

  public static final String NS_PROCESSMODEL="http://adaptivity.nl/ProcessEngine/";

  public static void serializeProcessModel(OutputStream out, ClientProcessModel<?> processModel) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer(out);
    serializeProcessModel(serializer, processModel);
  }

  public static void serializeProcessModel(Writer out, ClientProcessModel<?> processModel) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer(out);
    serializeProcessModel(serializer, processModel);
  }

  private static XmlSerializer getSerializer() throws XmlPullParserException {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    factory.setNamespaceAware(true);
    final XmlSerializer serializer = factory.newSerializer();
    serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", false);
    return serializer;
  }

  static XmlSerializer getSerializer(OutputStream out) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(out, "UTF-8");
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  static XmlSerializer getSerializer(Writer out) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(out);
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  private static void serializeProcessModel(XmlSerializer serializer, ClientProcessModel<?> processModel) {
    try {
      serializer.startDocument(null, null);
      serializer.ignorableWhitespace("\n");
      processModel.serialize(new AndroidXmlWriter(serializer));
      serializer.endDocument();
    } catch (IllegalArgumentException | IllegalStateException | IOException | XmlException e) {
      throw new RuntimeException(e);
    }
  }

  public static DrawableProcessModel parseProcessModel(Reader in, LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    XmlReader parser;
    try {
      parser = new AndroidXmlReader(in);
    } catch (Exception e){
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
    return parseProcessModel(parser, simpleLayoutAlgorithm, advancedAlgorithm);
  }

  public static DrawableProcessModel parseProcessModel(InputStream in, LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    XmlReader parser;
    try {
      parser = new AndroidXmlReader(in, "UTF-8");
    } catch (Exception e){
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
    return parseProcessModel(parser, simpleLayoutAlgorithm, advancedAlgorithm);
  }

  public static DrawableProcessModel parseProcessModel(XmlReader in, LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    try {

      if(in.nextTag()== START_ELEMENT && StringUtil.isEqual(NS_PROCESSMODEL, in.getNamespaceUri()) && StringUtil.isEqual("processModel", in.getLocalName())){
        ArrayList<DrawableProcessNode> modelElems = new ArrayList<>();
        String modelName = StringUtil.toString(in.getAttributeValue(XMLConstants.NULL_NS_URI, "name"));
        String uuid = StringUtil.toString(in.getAttributeValue(XMLConstants.NULL_NS_URI, "uuid"));
        String owner = StringUtil.toString(in.getAttributeValue(XMLConstants.NULL_NS_URI, "owner"));
        Map<String, DrawableProcessNode> nodeMap = new HashMap<>();
        for(EventType type = in.nextTag(); type!= END_ELEMENT; type = in.nextTag()) {

          DrawableProcessNode node = parseNode(in, nodeMap, modelElems);
          modelElems.add(node);
          if (node.getId()!=null) {
            nodeMap.put(node.getId(), node);
          }
        }
        // Use list indexing as resolveRefs may add elements to the list.
        // We will need to still check those
        boolean noPos = false;
        for(int i=0; i< modelElems.size(); ++i) {
          final DrawableProcessNode elem = modelElems.get(i);
          resolveRefs(elem, nodeMap, modelElems);
          addId(elem, nodeMap);
          noPos|=Double.isNaN(elem.getX())||Double.isNaN(elem.getY());
        }
        final DrawableProcessModel drawableProcessModel = new DrawableProcessModel(uuid==null? null: UUID.fromString(uuid), modelName, modelElems, noPos ? advancedAlgorithm : simpleLayoutAlgorithm);
        if (owner!=null) { drawableProcessModel.setOwner(owner); }
        return drawableProcessModel;

      } else {
        return null;
      }
    } catch (Exception e) {
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
  }

  private static void addId(final DrawableProcessNode elem, final Map<String, DrawableProcessNode> nodeMap) {
    int counter = 1;
    String baseId = elem.getIdBase();
    if (elem.getId()!=null && elem.getId().length()==0) {
      elem.setId(null);
    }
    while (elem.getId()==null) {
      String candidateId = baseId+Integer.toString(counter);
      if (! nodeMap.containsKey(candidateId)) {
        elem.setId(candidateId);
      }
    }
  }

  private static void resolveRefs(DrawableProcessNode node, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) {
    for(Identifiable predid: node.getPredecessors()) {
      // It is a temporary predecessor
      if (! (predid instanceof DrawableProcessNode)) {
        // First remove the link with the temporary
        node.removePredecessor(predid);
        // Get the node that should replace the temporary
        DrawableProcessNode realNode = nodes.get(predid.getId());
        // Add the node as successor to the real predecessor
        addAsSuccessor(realNode, node, modelElems);
      }
    }
  }

  private static DrawableProcessNode parseNode(XmlReader in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws
          XmlException {
    if (!NS_PROCESSMODEL.equals(in.getNamespaceUri())) {
      throw new IllegalArgumentException("Invalid process model");
    }
    if ("start".equals(in.getLocalName())) {
      return parseStart(in, nodes, modelElems);
    } else if ("activity".equals(in.getLocalName())) {
      return parseActivity(in, nodes, modelElems);
    } else if ("split".equals(in.getLocalName())) {
      return parseSplit(in, nodes, modelElems);
    } else if ("join".equals(in.getLocalName())) {
      return parseJoin(in, nodes, modelElems);
    } else if ("end".equals(in.getLocalName())) {
      return parseEnd(in, nodes, modelElems);
    }
    throw new UnsupportedOperationException("Unsupported tag");
  }

  private static DrawableProcessNode parseStart(XmlReader in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws
          XmlException {
    DrawableStartNode result = new DrawableStartNode(false);
    parseCommon(in, nodes, modelElems, result);
    if (in.nextTag()!= END_ELEMENT) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static DrawableProcessNode parseActivity(XmlReader in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws XmlException {
    DrawableActivity result = new DrawableActivity((DrawableProcessModel) null, false);
    parseCommon(in, nodes, modelElems, result);
    String name = trimWS(in.getAttributeValue(XMLConstants.NULL_NS_URI, "name"));
    if (name!=null && name.length()>0) {
      result.setName(name);
    }
    for(EventType type = in.nextTag(); type!= END_ELEMENT; type = in.nextTag()) {
      switch (type) {
      case START_ELEMENT:
        if (NS_PROCESSMODEL.equals(in.getNamespaceUri())) {
          if ("message".equals(in.getLocalName())) {
            result.setMessage(parseMessage(in));
          } else {
            parseUnknownTag(in);
          }
        } else {
          parseUnknownTag(in);
        }
        break;
      default:
          // ignore
      }
    }
    return result;
  }

  private static IXmlMessage parseMessage(XmlReader in) throws XmlException {
    ClientMessage result = new ClientMessage();
    String endpoint = StringUtil.toString(in.getAttributeValue(XMLConstants.NULL_NS_URI, "endpoint"));
    String operation = StringUtil.toString(in.getAttributeValue(XMLConstants.NULL_NS_URI, "operation"));
    String url = StringUtil.toString(in.getAttributeValue(XMLConstants.NULL_NS_URI, "url"));
    String method = StringUtil.toString(in.getAttributeValue(XMLConstants.NULL_NS_URI, "method"));
    String type = StringUtil.toString(in.getAttributeValue(XMLConstants.NULL_NS_URI, "type"));
    CharSequence serviceNS = in.getAttributeValue(XMLConstants.NULL_NS_URI, "serviceNS");
    CharSequence serviceName = in.getAttributeValue(XMLConstants.NULL_NS_URI, "serviceName");
    result.setEndpoint(endpoint);
    result.setOperation(operation);
    result.setUrl(url);
    result.setMethod(method);
    result.setType(type);
    if(serviceName!=null) {
      result.setServiceNS(StringUtil.toString(serviceNS));
      result.setServiceName(StringUtil.toString(serviceName));
    }

    CharArrayWriter caw = new CharArrayWriter();

    XmlWriter serializer = null;
    try {
      serializer = new AndroidXmlWriter(caw);
    } catch (XmlPullParserException | IOException e) {
      throw new XmlException(e);
    }

    Map<String, String> namespaces = new ArrayMap<>();
    int nsStart = in.getNamespaceStart();
    parseChildren(namespaces, in, serializer, nsStart);
    serializer.flush();
    // TODO fix this as it does not do fragments properly
    result.setContent(new CompactFragment(new SimpleNamespaceContext(namespaces), caw.toCharArray()));
    return result;
  }

  private static void parseChildren(final Map<String, String> namespaces, final XmlReader in, final XmlWriter serializer, final int nsStart) {
    EventType tagtype;
    try {
      while ((tagtype=in.next())!= END_ELEMENT) {
        switch (tagtype) {
          case COMMENT:
            serializer.comment(in.getText()); break;
          case TEXT:
            serializer.text(in.getText()); break;
          case CDSECT:
            serializer.cdsect(in.getText()); break;
          case START_ELEMENT: {
            addUndefinedNamespaces(namespaces, in, serializer, nsStart);
            serializer.startTag(in.getNamespaceUri(), in.getLocalName(), in.getPrefix());
            parseChildren(namespaces, in, serializer, nsStart);
            break;
          }
          default: {
            // ignore text, it
          }
        }
      }
    } catch (XmlException e) {
      Log.e(PMParser.class.getSimpleName(), "Error parsing activity body", e);
    }
  }

  private static void addUndefinedNamespaces(final Map<String, String> target, final XmlReader in, final XmlWriter out, final int nsStart) throws XmlException {
    CharSequence namespace = in.getNamespaceUri();
    CharSequence prefix = in.getPrefix();
    addUndefinedNamespace(target, prefix, namespace, in, out, nsStart);

    int attributeCount = in.getAttributeCount();
    for(int i=0; i<attributeCount; ++i) {
      if (! XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(in.getAttributeNamespace(i))) {
        addUndefinedNamespace(target, in.getAttributePrefix(i), in.getAttributeNamespace(i), in, out, nsStart);
      }
    }
  }

  private static void addUndefinedNamespace(final Map<String, String> target, final CharSequence prefix, final CharSequence namespace, final XmlReader in, final XmlWriter out, final int nsStart) throws XmlException {
    if (namespace!=null && prefix!=null && namespace.length()>0) {
      if (! isPrefixDefinedInFragment(in, prefix, namespace, nsStart)) {
        target.put(prefix.toString(), namespace.toString());
      }
    }
  }

  private static boolean isPrefixDefinedInFragment(final XmlReader in, final CharSequence prefix, final CharSequence namespace, final int nsStart) throws
          XmlException {
    int nsEnd = in.getNamespaceEnd();
    for(int i = nsStart; i<nsEnd; ++i) {
      CharSequence defPrefix = in.getNamespacePrefix(i);
      CharSequence defNs = in.getNamespaceUri(i);
      if (StringUtil.isEqual(prefix,defPrefix) && StringUtil.isEqual(namespace,defNs)) {
        return true;
      }
    }
    return false;
  }

  private static QName toQName(XmlPullParser in, String value) {
    if (value==null) { return null; }
    int i = value.indexOf(':');
    if (i>0) {
      String prefix = value.substring(0, i);
      String namespace = in.getNamespace(prefix);
      String localname = value.substring(i+1);
      return new QName(namespace, localname, prefix);
    } else {
      String namespace = in.getNamespace("");
      return new QName(namespace, value);
    }
  }

  private static void parseUnknownTag(XmlReader in) throws XmlException {
    for(EventType type = in.next(); type!= END_ELEMENT; type = in.next()) {
      switch (type) {
      case START_ELEMENT:
        parseUnknownTag(in);
        break;
      default:
          // ignore
      }
    }
  }

  private static DrawableProcessNode parseJoin(XmlReader in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws
          XmlException {
    DrawableJoin result = new DrawableJoin(false);
    parseCommon(in, nodes, modelElems, result);
    parseJoinSplitAttrs(in, result);
    List<Identifiable> predecessors = new ArrayList<>();

    for(EventType type = in.nextTag(); type!= END_ELEMENT; type = in.nextTag()) {
      if (! (StringUtil.isEqual(NS_PROCESSMODEL,in.getNamespaceUri()) && StringUtil.isEqual("predecessor", in.getLocalName()))) {
        throw new IllegalArgumentException("Invalid process model");
      }
      StringBuilder name = new StringBuilder();
      type = in.next();
      while (type!= END_ELEMENT) {
        if (type==TEXT) {
          name.append(in.getText());
        } else if (type== START_ELEMENT) {
          throw new IllegalArgumentException("Invalid process model");
        }
        type=in.next();
      }
      predecessors.add(getPredecessor(trimWS(name), nodes, modelElems));
    }
    result.setPredecessors(predecessors);

    return result;
  }

  private static String trimWS(CharSequence str) {
    if (str==null) { return null; }
    int start, end;
    for(start=0;start<str.length()&&isXMLWS(str.charAt(start));++start) {/*no body*/}
    for(end=str.length()-1;end>=start&& isXMLWS(str.charAt(end));--end) {/*no body*/}
    return str.subSequence(start, end+1).toString();
  }

  private static boolean isXMLWS(int codepoint) {
    return codepoint<=0x20 && (codepoint==0x20||codepoint==0x9||codepoint==0xD||codepoint==0xA);
  }

  private static DrawableProcessNode parseSplit(XmlReader in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws XmlException {
    DrawableSplit result = new DrawableSplit();
    parseCommon(in, nodes, modelElems, result);
    parseJoinSplitAttrs(in, result);
    for(EventType type = in.next(); type!= END_ELEMENT; type = in.next()) {
      switch (type) {
      case START_ELEMENT:
        parseUnknownTag(in);
        break;
      default:
          // ignore
      }
    }

    return result;
  }

  private static void parseJoinSplitAttrs(XmlReader in, DrawableJoinSplit node) throws XmlException {
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

  private static DrawableProcessNode parseEnd(XmlReader in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws
          XmlException {
    DrawableEndNode result = new DrawableEndNode();
    parseCommon(in, nodes, modelElems, result);
    if (in.nextTag()!= END_ELEMENT) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static void parseCommon(XmlReader in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems, DrawableProcessNode node) throws
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

  private static void addPredecessor(DrawableProcessNode node, String name, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) {
    Identifiable predecessor = getPredecessor(name, nodes, modelElems);
    if (predecessor instanceof DrawableProcessNode) {
      addAsSuccessor((DrawableProcessNode) predecessor, node, modelElems);
    }
  }

  private static Identifiable getPredecessor(String name, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) {
    DrawableProcessNode val = nodes.get(name);
    if (val==null) {
      return new Identifier(name);
    } else { // there already is a node
      // Allow temporary references to collect as many successors as desired, it might be a split.
      if ((val.getSuccessors().size()<((DrawableProcessNode)val).getMaxSuccessorCount())) {
        return val;
      } else {
        // There is no suitable successor
        return introduceSplit((DrawableProcessNode)val, modelElems);
      }
    }
  }

  private static DrawableSplit introduceSplit(DrawableProcessNode predecessor, List<DrawableProcessNode> modelElems) {
    for(Identifiable successor:predecessor.getSuccessors()) {
      if (successor instanceof DrawableSplit) {
        return (DrawableSplit) successor;
      }
    }
    DrawableSplit newSplit = new DrawableSplit();

    ArrayList<Identifiable> successors = new ArrayList<>(predecessor.getSuccessors());
    for(Identifiable successorId: successors) {
      DrawableProcessNode successor = (DrawableProcessNode) successorId;
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

  private static void addAsSuccessor(DrawableProcessNode predecessor, DrawableProcessNode successor, List<DrawableProcessNode> modelElems) {
    if (predecessor.getSuccessors().size()<predecessor.getMaxSuccessorCount()) {
      predecessor.addSuccessor(successor);
    } else {
      DrawableSplit newSplit = introduceSplit(predecessor, modelElems);
      newSplit.addSuccessor(successor);
      successor.addPredecessor(newSplit);
    }

  }

}
