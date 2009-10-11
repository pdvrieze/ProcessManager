package nl.adaptivity.process.userMessageHandler.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;


public class ActivityNode extends ProcessNode {

  private String aName;
  private String aPredecessorName;
  private String aOperation;
  private String aServiceNS;
  private String aServiceName;
  private String aEndpoint;
  private List<String> aImports;
  private List<String> aExports;

  public ActivityNode(String pId, String pName, String pPredecessor) {
    super(pId);
    aName = pName;
    aPredecessorName = pPredecessor;
  }

  public static ActivityNode fromXml(Element pNode) {
    String id = null;
    String name = null;
    String predecessor = null;
    {
      NamedNodeMap attrs = pNode.getAttributes();
      int attrCount = attrs.getLength();
      for (int i=0; i<attrCount; ++i) {
        Attr attr = (Attr) attrs.item(i);
        if (XMLUtil.isNS(null, attr)) {
          if ("id".equals(attr.getName())) {
            id = attr.getValue();
          } else if ("predecessor".equals(attr.getName())) {
            predecessor=attr.getValue();
          } else if ("name".equals(attr.getName())) {
            name=attr.getValue();
          } else {
            GWT.log("Unsupported attribute in startnode "+attr.toString(), null);
          }
        }
      }
    }

    ActivityNode result = new ActivityNode(id, name, predecessor);

    Node child = pNode.getFirstChild();
    Map<String, ProcessNode> map = new HashMap<String, ProcessNode>();
    List<ProcessNode> nodes = new ArrayList<ProcessNode>();

    List<String> imports = new ArrayList<String>();
    List<String> exports = new ArrayList<String>();
    Node message;
    String condition;
    String serviceNS;
    String serviceName;
    String endpoint;
    String operation;

    while (child!=null) {
      if (XMLUtil.isNS(ProcessModel.PROCESSMODEL_NS, child)) {

        if (XMLUtil.isLocalPart("import", child)) {
          imports.add(importFromXml((Element)child));
        } else if (XMLUtil.isLocalPart("export", child)) {
          exports.add(exportFromXml((Element)child));
        } else if (XMLUtil.isLocalPart("message", child)) {
          {
            NamedNodeMap attrs = child.getAttributes();
            int attrCnt = attrs.getLength();
            for(int i = 0; i<attrCnt; ++i) {
              Attr attr = (Attr) attrs.item(i);
              if (XMLUtil.isNS(null, attr)) {
                if ("serviceNS".equals(attr.getName())) {
                  result.setServiceNS(attr.getValue());
                } else if ("serviceName".equals(attr.getName())) {
                  result.setServiceName(attr.getValue());
                } else if ("endpoint".equals(attr.getName())) {
                  result.setEndpoint(attr.getValue());
                } else if ("operation".equals(attr.getName())) {
                  result.setOperation(attr.getValue());
                } else {
                  GWT.log("Unsupported attribute " + attr, null);
                }
              } else {
                GWT.log("Unsupported attribute " + attr, null);
              }
            }
          }


          message = ((Element)child).getFirstChild();
          if (message.getNextSibling()!=null) {
            GWT.log("Too many children to activity message", null);
          }
        } else if (XMLUtil.isLocalPart("condition", child)) {
          result.setCondition(conditionFromXml((Element)child));
        }
      }
    }
    result.setImports(imports);
    result.setExports(exports);

    return result;
  }

  private void setExports(List<String> pExports) {
    aExports = pExports;
  }

  private void setImports(List<String> pImports) {
    aImports = pImports;
  }

  private void setOperation(String pValue) {
    aOperation = pValue;
  }

  private void setEndpoint(String pValue) {
    aEndpoint = pValue;
  }

  private void setServiceName(String pValue) {
    aServiceName = pValue;
  }

  private void setServiceNS(String pValue) {
    aServiceNS = pValue;
  }

}
