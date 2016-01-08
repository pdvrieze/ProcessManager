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


public class StartNode extends ProcessNode {

  private Set<ProcessNode> mSuccessors;

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
    if (mSuccessors == null) {
      mSuccessors = new LinkedHashSet<ProcessNode>();
    }
    mSuccessors.add(node);
  }

  @Override
  public Collection<ProcessNode> getSuccessors() {
    if (mSuccessors == null) {
      mSuccessors = new LinkedHashSet<ProcessNode>();
    }
    return mSuccessors;
  }

  @Override
  public Collection<ProcessNode> getPredecessors() {
    return Collections.emptyList();
  }

}
