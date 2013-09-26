package nl.adaptivity.process.clientProcessModel;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.devrieze.util.CollectionUtil;

import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;


public class ClientProcessModel<T extends IClientProcessNode<T>> implements ProcessModel<T>{

  static final String PROCESSMODEL_NS = "http://adaptivity.nl/ProcessEngine/";

  private final String aName;

  private List<T> aNodes;

  public ClientProcessModel(final String pName, final Collection<? extends T> pNodes) {
    aName = pName;
    setNodes(pNodes);
  }

  public void setNodes(final Collection<? extends T> nodes) {
    aNodes = CollectionUtil.copy(nodes);
  }

  public void layout() {
    double lowestY = 30;
    for (final T node : aNodes) {
      if (node.getX()==Double.NaN || node.getY()==Double.NaN) {
        lowestY = node.layout(30, lowestY, null, true);
        lowestY += 45;
      }
    }
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    for (final T node : aNodes) {
      minX = Math.min(node.getX(), minX);
      minY = Math.min(node.getY(), minY);
    }
    final double offsetX = 30 - minX;
    final double offsetY = 30 - minY;

    for (final T node : aNodes) {
      node.setX(node.getX()+offsetX);
      node.setY(node.getY()+offsetY);
    }
  }

  @Override
  public int getEndNodeCount() {
    // TODO Auto-generated method stub
    // return 0;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public IProcessModelRef<T> getRef() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public T getNode(String pNodeId) {
    for(T n: getModelNodes()) {
      if (pNodeId.equals(n.getId())) {
        return n;
      }
    }
    return null;
  }

  @Override
  public Collection<? extends T> getModelNodes() {
    if (aNodes == null) {
      aNodes = new ArrayList<T>(0);
    }
    return aNodes;
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
  public Collection<? extends ClientStartNode<T>> getStartNodes() {
    List<ClientStartNode<T>> result = new ArrayList<ClientStartNode<T>>();
    for(T n:getModelNodes()) {
      if (n instanceof ClientStartNode) {
        result.add((ClientStartNode<T>) n);
      }
    }
    return result;
  }

}
