package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.*;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;


public class ActivityNode extends ProcessNode {

  private String aName;
  private String aPredecessorId;
  private String aOperation;
  private String aServiceNS;
  private String aServiceName;
  private String aEndpoint;
  private List<Element> aImports;
  private List<Element> aExports;
  private String aCondition;
  private ProcessNode aPredecessor;
  private Set<ProcessNode> aSuccessors;

  public ActivityNode(String pId, String pName, String pPredecessor) {
    super(pId);
    aName = pName;
    aPredecessorId = pPredecessor;
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

    List<Element> imports = new ArrayList<Element>();
    List<Element> exports = new ArrayList<Element>();
    Node message;

    for (Node child = pNode.getFirstChild(); child!=null; child = child.getNextSibling()) {
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

  private static String conditionFromXml(Element pElem) {
    return XMLUtil.getTextChildren(pElem);
  }

  private static Element importFromXml(Element pChild) {
    return pChild;
  }

  private static Element exportFromXml(Element pChild) {
    return pChild;
  }

  private void setExports(List<Element> pExports) {
    aExports = pExports;
  }

  private void setImports(List<Element> pImports) {
    aImports = pImports;
  }

  private void setCondition(String pCondition) {
    aCondition = pCondition;
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

  @Override
  public void resolvePredecessors(Map<String, ProcessNode> pMap) {
    ProcessNode predecessor = pMap.get(aPredecessorId);
    if (predecessor !=null) {
      aPredecessorId = predecessor.getId();
      aPredecessor = predecessor;
      aPredecessor.ensureSuccessor(this);
    }
  }

  @Override
  public void ensureSuccessor(ProcessNode pNode) {
    if (aSuccessors==null) { aSuccessors = new HashSet<ProcessNode>(); }
    aSuccessors.add(pNode);
  }

  @Override
  public Collection<ProcessNode> getSuccessors() {
    if (aSuccessors==null) { aSuccessors = new HashSet<ProcessNode>(); }
    return aSuccessors;
  }
}
