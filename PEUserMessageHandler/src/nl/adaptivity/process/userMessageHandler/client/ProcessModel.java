package nl.adaptivity.process.userMessageHandler.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;


public class ProcessModel {

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";

  public static ProcessModel fromXml(Document pParse) {
    Element root = pParse.getDocumentElement();
    if (! XMLUtil.isTag(PROCESSMODEL_NS, "processModel", root)) {
      return null;
    }
    Node child = root.getFirstChild();
    Map<String, ProcessNode> map = new HashMap<String, ProcessNode>();
    List<ProcessNode> nodes = new ArrayList<ProcessNode>();

    while (child!=null) {
      if (XMLUtil.isNS(PROCESSMODEL_NS, child)) {

        ProcessNode node = null;
        if (XMLUtil.isLocalPart("start", child)) {
          node = StartNode.fromXml((Element)child);
        } else if (XMLUtil.isLocalPart("activity", child)) {
          node = ActivityNode.fromXml((Element)child);
        }
        String id = node.getId();
        if (id!=null && id.length()>0) {
          map.put(id, node);
        }
        nodes.add(node);


      }
    }

    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");

  }

}
