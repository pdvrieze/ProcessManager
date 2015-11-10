package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.*;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;


public class StartNode extends ProcessNode {

  private Set<ProcessNode> aSuccessors;

  public StartNode(final String id) {
    super(id);
  }

  public static ProcessNode fromXml(final Element element) {
    String id = null;
    final NamedNodeMap attrs = element.getAttributes();
    final int attrCount = attrs.getLength();
    for (int i = 0; i < attrCount; ++i) {
      final Attr attr = (Attr) attrs.item(i);
      if (XMLUtil.isNS(null, attr)) {
        if ("id".equals(attr.getName())) {
          id = attr.getValue();
        } else {
          GWT.log("Unsupported attribute in startnode: " + attr.getName(), null);
        }
      }
    }
    return new StartNode(id);
  }

  @Override
  public void resolvePredecessors(final Map<String, ProcessNode> map) {
    // start node has no predecessors
  }

  @Override
  public void ensureSuccessor(final ProcessNode node) {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ProcessNode>();
    }
    aSuccessors.add(node);
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
    return Collections.emptyList();
  }

}
