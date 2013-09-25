package nl.adaptivity.process.clientProcessModel;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRef;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;


public class ClientProcessModel implements ProcessModel{

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";

  private final String aName;

  private List<ProcessNode> aNodes;

  public ClientProcessModel(final String pName, final Collection<? extends ProcessNode> pNodes) {
    aName = pName;
    setNodes(pNodes);
  }

  public void setNodes(final Collection<? extends ProcessNode> nodes) {
    aNodes = CollectionUtil.copy(nodes);
  }

  public List<ProcessNode> getNodes() {
    if (aNodes == null) {
      aNodes = new ArrayList<ProcessNode>(0);
    }
    return aNodes;
  }
//
//  public void layout() {
//    for (final ProcessNode node : aNodes) {
//      node.unsetPos();
//    }
//    int lowestY = 30;
//    for (final ClientProcessNode node : aNodes) {
//      if (!node.hasPos()) {
//        lowestY = node.layout(30, lowestY, null, true);
//        lowestY += 45;
//      }
//    }
//    int minX = Integer.MAX_VALUE;
//    int minY = Integer.MAX_VALUE;
//    for (final ClientProcessNode node : aNodes) {
//      minX = Math.min(node.getX(), minX);
//      minY = Math.min(node.getY(), minY);
//    }
//    final int offsetX = 30 - minX;
//    final int offsetY = 30 - minY;
//
//    for (final ProcessNode node : aNodes) {
//      node.offset(offsetX, offsetY);
//    }
//  }

  @Override
  public int getEndNodeCount() {
    // TODO Auto-generated method stub
    // return 0;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public ProcessModelRef getRef() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public ProcessNode getNode(String pNodeId) {
    for(ProcessNode n: getNodes()) {
      if (pNodeId.equals(n.getId())) {
        return n;
      }
    }
    return null;
  }

  @Override
  public Collection<? extends ProcessNode> getModelNodes() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public String getName() {
    return aName;
  }

  @Override
  public Principal getOwner() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Set<String> getRoles() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Collection<StartNode> getStartNodes() {
    List<StartNode> result = new ArrayList<StartNode>();
    for(ProcessNode n:getNodes()) {
      if (n instanceof StartNode) {
        result.add((StartNode) n);
      }
    }
    return result;
  }

}
