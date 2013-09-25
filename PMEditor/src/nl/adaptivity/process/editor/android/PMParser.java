package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import static org.xmlpull.v1.XmlPullParser.*;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.engine.ActivityImpl;
import nl.adaptivity.process.processModel.engine.EndNodeImpl;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;
import android.util.Log;


public class PMParser {


  private static class RefNode extends ProcessNodeImpl {

    private static final long serialVersionUID = 6250389309360094282L;
    final String aRef;

    public RefNode(String pRef) {
      aRef = pRef;
    }

    @Override
    public boolean condition(IProcessNodeInstance<?> pInstance) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T, U extends IProcessNodeInstance<U>> boolean provideTask(IMessageService<T, U> pMessageService, U pInstance) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T, U extends IProcessNodeInstance<U>> boolean takeTask(IMessageService<T, U> pMessageService, U pInstance) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T, U extends IProcessNodeInstance<U>> boolean startTask(IMessageService<T, U> pMessageService, U pInstance) {
      throw new UnsupportedOperationException("Not implemented");
    }

  }

  public static final String NS_PROCESSMODEL="http://adaptivity.nl/ProcessEngine/";

  static ProcessModel parseProcessModel(InputStream pIn) {
    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      XmlPullParser in = factory.newPullParser();
      in.setInput(pIn, "utf-8");

      if(in.nextTag()==START_TAG && NS_PROCESSMODEL.equals(in.getNamespace()) && "processModel".equals(in.getName())){
        String modelName = in.getAttributeValue(null, "name");
        Map<String, ProcessNode> nodes = new HashMap<String, ProcessNode>();
        for(int type = in.nextTag(); type!=END_TAG; type = in.nextTag()) {

          ProcessNode node = parseNode(in, nodes);
          if (node.getId()!=null) {
            nodes.put(node.getId(), node);
          }
        }
        final ProcessModelImpl result = new ProcessModelImpl(getEndNodes(nodes));
        result.setName(modelName);
        return result;

      } else {
        return null;
      }
    } catch (Exception e) {
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
  }

  private static Collection<? extends EndNode> getEndNodes(Map<String, ProcessNode> pNodes) {
    List<EndNode> result = new ArrayList<EndNode>();
    for(ProcessNode node: pNodes.values()) {
      resolveRefs(node, pNodes);
      if (node instanceof EndNode) {
        result.add((EndNode) node);
      }
    }
    return result;
  }

  private static void resolveRefs(ProcessNode pNode, Map<String, ProcessNode> pNodes) {
    List<ProcessNode> preds = new ArrayList<ProcessNode>();
    boolean changed = false;
    for(ProcessNode pred: pNode.getPredecessors()) {
      if (pred instanceof RefNode) {
        String ref = ((RefNode)pred).aRef;
        if (pNode.getPredecessors().size()==1) {
          pNode.setPredecessors(Collections.singleton(pNodes.get(ref)));
          return;
        } else {
          preds.add(pNodes.get(ref));
          changed = true;
        }
      } else {
        preds.add(pred);
      }
    }
    if (changed) {
      pNode.setPredecessors(preds);;
    }
  }

  private static ProcessNode parseNode(XmlPullParser pIn, Map<String, ProcessNode> pNodes) throws XmlPullParserException, IOException {
    if (!NS_PROCESSMODEL.equals(pIn.getNamespace())) {
      throw new IllegalArgumentException("Invalid process model");
    }
    if ("start".equals(pIn.getName())) {
      return parseStart(pIn, pNodes);
    } else if ("activity".equals(pIn.getName())) {
      return parseActivity(pIn, pNodes);
    } else if ("join".equals(pIn.getName())) {
      return parseJoin(pIn, pNodes);
    } else if ("end".equals(pIn.getName())) {
      return parseEnd(pIn, pNodes);
    }
    throw new UnsupportedOperationException("Unsupported tag");
  }

  private static ProcessNode parseStart(XmlPullParser pIn, Map<String, ProcessNode> pNodes) throws XmlPullParserException, IOException {
    StartNodeImpl result = new StartNodeImpl();
    parseCommon(pIn, pNodes, result);
    if (pIn.nextTag()!=END_TAG) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static ProcessNode parseActivity(XmlPullParser pIn, Map<String, ProcessNode> pNodes) throws XmlPullParserException, IOException {
    ActivityImpl result = new ActivityImpl();
    parseCommon(pIn, pNodes, result);
    String name = pIn.getAttributeValue(null, "name");
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

  private static ProcessNode parseJoin(XmlPullParser pIn, Map<String, ProcessNode> pNodes) throws XmlPullParserException, IOException {
    JoinImpl result = new JoinImpl();
    parseCommon(pIn, pNodes, result);
    parseJoinAttrs(pIn, result);
    List<ProcessNode> predecessors = new ArrayList<ProcessNode>();

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
      predecessors.add(getPredecessor(name.toString(), pNodes));
    }

    return result;
  }

  private static void parseJoinAttrs(XmlPullParser pIn, Join pNode) {
    for(int i=0; i< pIn.getAttributeCount();++i) {
      if (pIn.getAttributeNamespace(i)==null) {
        final String aname = pIn.getAttributeName(i);
        if ("min".equals(aname)) {
          pNode.setMin(Integer.parseInt(pIn.getAttributeValue(i)));
        } else if ("max".equals(aname)) {
          pNode.setMax(Integer.parseInt(pIn.getAttributeValue(i)));
        }
      }
    }
  }

  private static ProcessNode parseEnd(XmlPullParser pIn, Map<String, ProcessNode> pNodes) throws XmlPullParserException, IOException {
    EndNodeImpl result = new EndNodeImpl();
    parseCommon(pIn, pNodes, result);
    if (pIn.nextTag()!=END_TAG) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static void parseCommon(XmlPullParser pIn, Map<String, ProcessNode> pNodes, ProcessNodeImpl pNode) {
    for(int i=0; i< pIn.getAttributeCount();++i) {
      if (pIn.getAttributeNamespace(i)==null) {
        final String aname = pIn.getAttributeName(i);
        if ("x".equals(aname)) {
          pNode.setX(Double.parseDouble(pIn.getAttributeValue(i)));
        } else if ("y".equals(aname)) {
          pNode.setY(Double.parseDouble(pIn.getAttributeValue(i)));
        } else if ("id".equals(aname)) {
          pNode.setId(pIn.getAttributeValue(i));
        } else if ("predecessor".equals(aname)) {
          pNode.setPredecessors(getPredecessors(pIn.getAttributeValue(i),pNodes));
        }
      }
    }
  }

  private static Collection<? extends ProcessNode> getPredecessors(String pName, Map<String, ProcessNode> pNodes) {
    final ProcessNode predecessor = getPredecessor(pName, pNodes);
    return predecessor==null ? null : Collections.singleton(predecessor);
  }

  private static ProcessNode getPredecessor(String pName, Map<String, ProcessNode> pNodes) {
    ProcessNode val = pNodes.get(pName);
    if (val==null) {
      val = new RefNode(pName);
    }
    return val;
  }

}
