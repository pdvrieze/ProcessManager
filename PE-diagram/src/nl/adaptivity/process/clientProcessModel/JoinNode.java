package nl.adaptivity.process.clientProcessModel;

import java.util.*;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;


public class JoinNode extends ProcessNode {

  private final Collection<String> aPredecessorNames;

  private Set<ProcessNode> aPredecessors;

  private Set<ProcessNode> aSuccessors;

  private final String aMin;

  private final String aMax;

  public JoinNode(final String pId, final Collection<String> pPredecessorNames, final String pMin, final String pMax) {
    super(pId);
    aPredecessorNames = pPredecessorNames;
    aMin = pMin;
    aMax = pMax;
  }

  public static JoinNode fromXml(final Element pNode) {
    String id = null;
    String min = null;
    String max = null;
    {
      final NamedNodeMap attrs = pNode.getAttributes();
      final int attrCount = attrs.getLength();
      for (int i = 0; i < attrCount; ++i) {
        final Attr attr = (Attr) attrs.item(i);
        if (XMLUtil.isNS(null, attr)) {
          if ("id".equals(attr.getName())) {
            id = attr.getValue();
          } else if ("min".equals(attr.getName())) {
            min = attr.getValue();
          } else if ("max".equals(attr.getName())) {
            max = attr.getValue();
          } else {
            GWT.log("Unsupported attribute in joinnode " + attr.getName(), null);
          }
        }
      }
    }


    final List<String> predecessors = new ArrayList<String>();

    for (Node child = pNode.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (XMLUtil.isNS(ProcessModel.PROCESSMODEL_NS, child)) {

        if (XMLUtil.isLocalPart("predecessor", child)) {
          predecessors.add(XMLUtil.getTextChildren(child));
        } else {
          GWT.log("Unexpected subtag " + child, null);
        }
      } else {
        GWT.log("Unexpected subtag " + child, null);
      }
    }
    final JoinNode result = new JoinNode(id, predecessors, min, max);

    return result;
  }

  @Override
  public void resolvePredecessors(final Map<String, ProcessNode> pMap) {
    if (aPredecessors == null) {
      aPredecessors = new LinkedHashSet<ProcessNode>();
    } else {
      aPredecessors.clear();
    }
    for (final String predecessorId : aPredecessorNames) {

      final ProcessNode predecessor = pMap.get(predecessorId);
      if (predecessor != null) {
        aPredecessors.add(predecessor);
        predecessor.ensureSuccessor(this);
      }
    }
  }

  @Override
  public void ensureSuccessor(final ProcessNode pNode) {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ProcessNode>();
    }
    aSuccessors.add(pNode);
  }

  @Override
  public Collection<ProcessNode> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ProcessNode>();
    }
    return aSuccessors;
  }

  @Override
  public Collection<ProcessNode> getPredecessors() {
    return aPredecessors;
  }

}
