package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;


public class ProcessModel {

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";

  private final String aName;

  private List<ProcessNode> aNodes;

  public ProcessModel(final String pName, final List<ProcessNode> pNodes) {
    aName = pName;
    setNodes(pNodes);
  }

  public static ProcessModel fromXml(final Document pParse) {
    final Element root = pParse.getDocumentElement();
    if (!XMLUtil.isTag(PROCESSMODEL_NS, "processModel", root)) {
      return null;
    }

    String name = null;

    {
      final NamedNodeMap attrs = root.getAttributes();
      final int attrCount = attrs.getLength();
      for (int i = 0; i < attrCount; ++i) {
        final Attr attr = (Attr) attrs.item(i);
        if (XMLUtil.isNS(null, attr)) {
          if ("name".equals(attr.getName())) {
            name = attr.getValue();
          } else {
            GWT.log("Unsupported attribute in processModel " + attr.getName(), null);
          }
        }
      }
    }


    final Map<String, ProcessNode> map = new HashMap<String, ProcessNode>();
    final List<ProcessNode> nodes = new ArrayList<ProcessNode>();

    for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (XMLUtil.isNS(PROCESSMODEL_NS, child)) {

        ProcessNode node = null;
        if (XMLUtil.isLocalPart("start", child)) {
          node = StartNode.fromXml((Element) child);
        } else if (XMLUtil.isLocalPart("activity", child)) {
          node = ActivityNode.fromXml((Element) child);
        } else if (XMLUtil.isLocalPart("end", child)) {
          node = EndNode.fromXml((Element) child);
        } else if (XMLUtil.isLocalPart("join", child)) {
          node = JoinNode.fromXml((Element) child);
        }
        final String id = node==null ? null : node.getId();
        if ((id != null) && (id.length() > 0)) {
          map.put(id, node);
        }
        nodes.add(node);


      }
    }

    for (final ProcessNode node : nodes) {
      node.resolvePredecessors(map);
    }

    return new ProcessModel(name, nodes);
  }

  public void setNodes(final List<ProcessNode> nodes) {
    aNodes = nodes;
  }

  public List<ProcessNode> getNodes() {
    if (aNodes == null) {
      aNodes = new ArrayList<ProcessNode>(0);
    }
    return aNodes;
  }

  public void layout() {
    for (final ProcessNode node : aNodes) {
      node.unsetPos();
    }
    int lowestY = 30;
    for (final ProcessNode node : aNodes) {
      if (!node.hasPos()) {
        lowestY = node.layout(30, lowestY, null, true);
        lowestY += 45;
      }
    }
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    for (final ProcessNode node : aNodes) {
      minX = Math.min(node.getX(), minX);
      minY = Math.min(node.getY(), minY);
    }
    final int offsetX = 30 - minX;
    final int offsetY = 30 - minY;

    for (final ProcessNode node : aNodes) {
      node.offset(offsetX, offsetY);
    }
  }

}
