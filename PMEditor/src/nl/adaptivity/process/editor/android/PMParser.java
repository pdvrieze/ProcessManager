package nl.adaptivity.process.editor.android;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.clientProcessModel.ClientProcessNode;
import nl.adaptivity.process.clientProcessModel.SerializerAdapter;
import nl.adaptivity.process.diagram.DrawableActivity;
import nl.adaptivity.process.diagram.DrawableEndNode;
import nl.adaptivity.process.diagram.DrawableJoin;
import nl.adaptivity.process.diagram.DrawableJoinSplit;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.DrawableSplit;
import nl.adaptivity.process.diagram.DrawableStartNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;

public class PMParser {

  public static final String MIME_TYPE="application/x-processmodel";

  public static class XmlSerializerAdapter implements SerializerAdapter {

    private final XmlSerializer mSerializer;

    public XmlSerializerAdapter(XmlSerializer pSerializer) {
      mSerializer = pSerializer;
    }

    @Override
    public void addNamespace(String pPrefix, String pNamespace) {
      // TODO maybe record pending namespaces and only add them on startTag
      try {
        mSerializer.setPrefix(pPrefix, pNamespace);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void startTag(String pNamespace, String pName) {
      try {
        mSerializer.startTag(pNamespace, pName);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void endTag(String pNamespace, String pName) {
      try {
        mSerializer.endTag(pNamespace, pName);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void addAttribute(String pName, String pValue) {
      try {
        mSerializer.attribute(null, pName, pValue);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void text(String pString) {
      try {
        mSerializer.text(pString);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void ignorableWhitespace(String pString) {
      try {
        mSerializer.ignorableWhitespace(pString);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }


  }

  private static final class RefNode extends ClientProcessNode<DrawableProcessNode> implements DrawableProcessNode {

    final String aRef;

    public RefNode(String pRef) {
      super((ClientProcessModel<DrawableProcessNode>) null);
      aRef = pRef;
    }

    @Override
	public RefNode clone() {
    	return new RefNode(aRef);
    }

    @Override
    public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pArg0, Rectangle pArg1) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Rectangle getBounds() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void move(double pX, double pY) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setPos(double pLeft, double pTop) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Drawable getItemAt(double pX, double pY) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getState() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setState(int pState) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void serialize(SerializerAdapter pOut) {
      // Don't serialize temps
    }

    @Override
    public String getId() {
      return aRef;
    }

  }

  public static final String NS_PROCESSMODEL="http://adaptivity.nl/ProcessEngine/";

  static void serializeProcessModel(OutputStream pOut, ClientProcessModel<?> pProcessModel) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer(pOut);
    serializeProcessModel(serializer, pProcessModel);
  }

  static void serializeProcessModel(Writer pOut, ClientProcessModel<?> pProcessModel) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer(pOut);
    serializeProcessModel(serializer, pProcessModel);
  }

  private static XmlSerializer getSerializer() throws XmlPullParserException {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newSerializer();
  }

  public static XmlSerializer getSerializer(OutputStream pOut) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(pOut, "UTF-8");
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  public static XmlSerializer getSerializer(Writer pOut) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(pOut);
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  private static void serializeProcessModel(XmlSerializer pSerializer, ClientProcessModel<?> pProcessModel) {
    try {
      pSerializer.startDocument(null, null);
      pSerializer.ignorableWhitespace("\n");
      pProcessModel.serialize(new XmlSerializerAdapter(pSerializer));
      pSerializer.endDocument();
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  static DrawableProcessModel parseProcessModel(InputStream pIn, LayoutAlgorithm<DrawableProcessNode> pLayoutAlgorithm) {
    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      XmlPullParser in = factory.newPullParser();
      in.setInput(pIn, "utf-8");

      if(in.nextTag()==START_TAG && NS_PROCESSMODEL.equals(in.getNamespace()) && "processModel".equals(in.getName())){
        ArrayList<DrawableProcessNode> modelElems = new ArrayList<>();
        String modelName = in.getAttributeValue(XMLConstants.NULL_NS_URI, "name");
        Map<String, DrawableProcessNode> nodeMap = new HashMap<>();
        for(int type = in.nextTag(); type!=END_TAG; type = in.nextTag()) {

          DrawableProcessNode node = parseNode(in, nodeMap, modelElems);
          modelElems.add(node);
          if (node.getId()!=null) {
            nodeMap.put(node.getId(), node);
          }
        }
        // Use list indexing as resolveRefs may add elements to the list.
        // We will need to still check those
        for(int i=0; i< modelElems.size(); ++i) {
          resolveRefs(modelElems.get(i), nodeMap, modelElems);
        }
        return new DrawableProcessModel(modelName, modelElems, pLayoutAlgorithm);

      } else {
        return null;
      }
    } catch (Exception e) {
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
  }

  private static void resolveRefs(DrawableProcessNode pNode, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) {
    for(DrawableProcessNode pred: pNode.getPredecessors()) {
      // It is a temporary predecessor
      if (pred instanceof RefNode) {
        // First remove the link with the temporary
        pNode.removePredecessor(pred);
        // Get the id we need
        String ref = ((RefNode)pred).aRef;
        // Get the node that should replace the temporary
        DrawableProcessNode realNode = pNodes.get(ref);
        // Add the node as successor to the real predecessor
        addAsSuccessor(realNode, pNode, pModelElems);
      }
    }
  }

  private static DrawableProcessNode parseNode(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) throws XmlPullParserException, IOException {
    if (!NS_PROCESSMODEL.equals(pIn.getNamespace())) {
      throw new IllegalArgumentException("Invalid process model");
    }
    if ("start".equals(pIn.getName())) {
      return parseStart(pIn, pNodes, pModelElems);
    } else if ("activity".equals(pIn.getName())) {
      return parseActivity(pIn, pNodes, pModelElems);
    } else if ("split".equals(pIn.getName())) {
      return parseSplit(pIn, pNodes, pModelElems);
    } else if ("join".equals(pIn.getName())) {
      return parseJoin(pIn, pNodes, pModelElems);
    } else if ("end".equals(pIn.getName())) {
      return parseEnd(pIn, pNodes, pModelElems);
    }
    throw new UnsupportedOperationException("Unsupported tag");
  }

  private static DrawableProcessNode parseStart(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) throws XmlPullParserException, IOException {
    DrawableStartNode result = new DrawableStartNode((ClientProcessModel<DrawableProcessNode>) null);
    parseCommon(pIn, pNodes, pModelElems, result);
    if (pIn.nextTag()!=END_TAG) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static DrawableProcessNode parseActivity(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) throws XmlPullParserException, IOException {
    DrawableActivity result = new DrawableActivity((ClientProcessModel<DrawableProcessNode>) null);
    parseCommon(pIn, pNodes, pModelElems, result);
    String name = pIn.getAttributeValue(XMLConstants.NULL_NS_URI, "name");
    if (name!=null && name.length()>0) {
      result.setName(name);
    }
    for(int type = pIn.next(); type!=END_TAG; type = pIn.next()) {
      switch (type) {
      case START_TAG:
        parseUnknownTag(pIn);
        break;
      default:
          // ignore
      }
    }
    return result;
  }

  private static void parseUnknownTag(XmlPullParser pIn) throws XmlPullParserException, IOException {
    for(int type = pIn.next(); type!=END_TAG; type = pIn.next()) {
      switch (type) {
      case START_TAG:
        parseUnknownTag(pIn);
        break;
      default:
          // ignore
      }
    }
  }

  private static DrawableProcessNode parseJoin(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) throws XmlPullParserException, IOException {
    DrawableJoin result = new DrawableJoin((ClientProcessModel<DrawableProcessNode>) null);
    parseCommon(pIn, pNodes, pModelElems, result);
    parseJoinSplitAttrs(pIn, result);
    List<DrawableProcessNode> predecessors = new ArrayList<>();

    for(int type = pIn.nextTag(); type!=END_TAG; type = pIn.nextTag()) {
      if (! (NS_PROCESSMODEL.equals(pIn.getNamespace()) && "predecessor".equals(pIn.getName()))) {
        throw new IllegalArgumentException("Invalid process model");
      }
      StringBuilder name = new StringBuilder();
      type = pIn.next();
      while (type!=END_TAG) {
        if (type==TEXT) {
          name.append(pIn.getText());
        } else if (type==START_TAG) {
          throw new IllegalArgumentException("Invalid process model");
        }
        type=pIn.next();
      }
      predecessors.add(getPredecessor(name.toString(), pNodes, pModelElems));
    }
    result.setPredecessors(predecessors);

    return result;
  }

  private static DrawableProcessNode parseSplit(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) throws XmlPullParserException, IOException {
    DrawableSplit result = new DrawableSplit((ClientProcessModel<DrawableProcessNode>) null);
    parseCommon(pIn, pNodes, pModelElems, result);
    parseJoinSplitAttrs(pIn, result);
    for(int type = pIn.next(); type!=END_TAG; type = pIn.next()) {
      switch (type) {
      case START_TAG:
        parseUnknownTag(pIn);
        break;
      default:
          // ignore
      }
    }

    return result;
  }

  private static void parseJoinSplitAttrs(XmlPullParser pIn, DrawableJoinSplit pNode) {
    for(int i=0; i< pIn.getAttributeCount();++i) {
      if (XMLConstants.NULL_NS_URI.equals(pIn.getAttributeNamespace(i))) {
        final String aname = pIn.getAttributeName(i);
        if ("min".equals(aname)) {
          pNode.setMin(Integer.parseInt(pIn.getAttributeValue(i)));
        } else if ("max".equals(aname)) {
          pNode.setMax(Integer.parseInt(pIn.getAttributeValue(i)));
        }
      }
    }
  }

  private static DrawableProcessNode parseEnd(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) throws XmlPullParserException, IOException {
    DrawableEndNode result = new DrawableEndNode((ClientProcessModel<DrawableProcessNode>) null);
    parseCommon(pIn, pNodes, pModelElems, result);
    if (pIn.nextTag()!=END_TAG) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static void parseCommon(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems, DrawableProcessNode pNode) {
    for(int i=0; i< pIn.getAttributeCount();++i) {
      if (XMLConstants.NULL_NS_URI.equals(pIn.getAttributeNamespace(i))) {
        final String aname = pIn.getAttributeName(i);
        if ("x".equals(aname)) {
          pNode.setX(Double.parseDouble(pIn.getAttributeValue(i)));
        } else if ("y".equals(aname)) {
          pNode.setY(Double.parseDouble(pIn.getAttributeValue(i)));
        } else if ("id".equals(aname)) {
          pNode.setId(pIn.getAttributeValue(i));
        } else if ("label".equals(aname)) {
          pNode.setLabel(pIn.getAttributeValue(i));
        } else if ("name".equals(aname)) {
          if (pNode.getLabel()==null) {
            pNode.setLabel(pIn.getAttributeValue(i));
          }
        } else if ("predecessor".equals(aname)) {
          addPredecessor(pNode, pIn.getAttributeValue(i), pNodes, pModelElems);
//          pNode.setPredecessors(getPredecessors(pIn.getAttributeValue(i),pNodes, pModelElems));
        }
      }
    }
  }

  private static void addPredecessor(DrawableProcessNode pNode, String pName, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) {
    DrawableProcessNode predecessor = getPredecessor(pName, pNodes, pModelElems);
    addAsSuccessor(predecessor, pNode, pModelElems);
  }

  private static DrawableProcessNode getPredecessor(String pName, Map<String, DrawableProcessNode> pNodes, List<DrawableProcessNode> pModelElems) {
    DrawableProcessNode val = pNodes.get(pName);
    if (val==null) {
      val = new RefNode(pName);
    } else { // there already is a node
      // Allow temporary references to collect as many successors as desired, it might be a split.
      if ((val instanceof RefNode)|| (val.getSuccessors().size()<val.getMaxSuccessorCount())) {
        return val;
      } else {
        // There is no suitable successor
        return introduceSplit(val, pModelElems);
      }
    }
    return val;
  }

  private static DrawableSplit introduceSplit(DrawableProcessNode predecessor, List<DrawableProcessNode> pModelElems) {
    for(DrawableProcessNode successor:predecessor.getSuccessors()) {
      if (successor instanceof DrawableSplit) {
        return (DrawableSplit) successor;
      }
    }

    DrawableSplit newSplit = new DrawableSplit((ClientProcessModel<DrawableProcessNode>) null);
    ArrayList<DrawableProcessNode> successors = new ArrayList<>(predecessor.getSuccessors());
    for(DrawableProcessNode successor: successors) {
      predecessor.removeSuccessor(successor);
      successor.removePredecessor(predecessor);
      newSplit.addSuccessor(successor);
      successor.addPredecessor(newSplit);
    }
    predecessor.addSuccessor(newSplit);
    newSplit.addPredecessor(predecessor);
    pModelElems.add(newSplit);
    return newSplit;
  }

  private static void addAsSuccessor(DrawableProcessNode predecessor, DrawableProcessNode successor, List<DrawableProcessNode> pModelElems) {
    if (predecessor.getSuccessors().size()<predecessor.getMaxSuccessorCount()) {
      predecessor.addSuccessor(successor);
      successor.addPredecessor(predecessor);
    } else {
      DrawableSplit newSplit = introduceSplit(predecessor, pModelElems);
      newSplit.addSuccessor(successor);
      successor.addPredecessor(newSplit);
    }

  }

}
