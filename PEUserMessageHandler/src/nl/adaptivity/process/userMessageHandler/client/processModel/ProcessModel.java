package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.*;


public class ProcessModel {

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";
  private String aName;
  private List<ProcessNode> aNodes;

  public ProcessModel(String pName, List<ProcessNode> pNodes) {
    aName = pName;
    setNodes(pNodes);
  }

  public static ProcessModel fromXml(Document pParse) {
    Element root = pParse.getDocumentElement();
    if (! XMLUtil.isTag(PROCESSMODEL_NS, "processModel", root)) {
      return null;
    }

    String name = null;

    {
      NamedNodeMap attrs = root.getAttributes();
      int attrCount = attrs.getLength();
      for (int i=0; i<attrCount; ++i) {
        Attr attr = (Attr) attrs.item(i);
        if (XMLUtil.isNS(null, attr)) {
          if ("name".equals(attr.getName())) {
            name=attr.getValue();
          } else {
            GWT.log("Unsupported attribute in startnode "+attr.toString(), null);
          }
        }
      }
    }


    Map<String, ProcessNode> map = new HashMap<String, ProcessNode>();
    List<ProcessNode> nodes = new ArrayList<ProcessNode>();

    for (Node child = root.getFirstChild(); child!=null; child = child.getNextSibling()) {
      if (XMLUtil.isNS(PROCESSMODEL_NS, child)) {

        ProcessNode node = null;
        if (XMLUtil.isLocalPart("start", child)) {
          node = StartNode.fromXml((Element)child);
        } else if (XMLUtil.isLocalPart("activity", child)) {
          node = ActivityNode.fromXml((Element)child);
        } else if (XMLUtil.isLocalPart("end", child)) {
          node = EndNode.fromXml((Element) child);
        } else if (XMLUtil.isLocalPart("join", child)) {
          node = JoinNode.fromXml((Element) child);
        }
        String id = node.getId();
        if (id!=null && id.length()>0) {
          map.put(id, node);
        }
        nodes.add(node);


      }
    }

    for(ProcessNode node: nodes) {
      node.resolvePredecessors(map);
    }

    return new ProcessModel(name, nodes);
  }

  public void setNodes(List<ProcessNode> nodes) {
    aNodes = nodes;
  }

  public List<ProcessNode> getNodes() {
    if (aNodes ==null) {
      aNodes = new ArrayList<ProcessNode>(0);
    }
    return aNodes;
  }

}
