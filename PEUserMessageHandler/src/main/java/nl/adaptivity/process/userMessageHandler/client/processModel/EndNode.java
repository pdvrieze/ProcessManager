/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.*;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;


public class EndNode extends ProcessNode {

  private String mPredecessorId;

  private List<Element> mExports;

  private ProcessNode mPredecessor;

  public EndNode(final String id, final String predecessorName) {
    super(id);
    mPredecessorId = predecessorName;
  }

  public static EndNode fromXml(final Element node) {
    String id = null;
    String predecessor = null;
    {
      final NamedNodeMap attrs = node.getAttributes();
      final int attrCount = attrs.getLength();
      for (int i = 0; i < attrCount; ++i) {
        final Attr attr = (Attr) attrs.item(i);
        if (XMLUtil.isNS(null, attr)) {
          if ("id".equals(attr.getName())) {
            id = attr.getValue();
          } else if ("predecessor".equals(attr.getName())) {
            predecessor = attr.getValue();
          } else {
            GWT.log("Unsupported attribute in endnode " + attr.getName(), null);
          }
        }
      }
    }

    final EndNode result = new EndNode(id, predecessor);

    final List<Element> exports = new ArrayList<Element>();

    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (XMLUtil.isNS(ProcessModel.PROCESSMODEL_NS, child)) {

        if (XMLUtil.isLocalPart("export", child)) {
          exports.add(exportFromXml((Element) child));
        } else {
          GWT.log("Unexpected subtag " + child, null);
        }
      } else {
        GWT.log("Unexpected subtag " + child, null);
      }
    }
    result.setExports(exports);

    return result;
  }

  private void setExports(final List<Element> exports) {
    mExports = exports;
  }

  private static Element exportFromXml(final Element child) {
    return child;
  }

  @Override
  public void resolvePredecessors(final Map<String, ProcessNode> map) {
    final ProcessNode predecessor = map.get(mPredecessorId);
    if (predecessor != null) {
      mPredecessorId = predecessor.getId();
      mPredecessor = predecessor;
      mPredecessor.ensureSuccessor(this);
    }
  }

  @Override
  public void ensureSuccessor(final ProcessNode node) {
    throw new UnsupportedOperationException("end nodes never have successors");
  }

  @Override
  public Collection<ProcessNode> getSuccessors() {
    return new ArrayList<ProcessNode>(0);
  }

  @Override
  public Collection<ProcessNode> getPredecessors() {
    return Collections.singletonList(mPredecessor);
  }

}
