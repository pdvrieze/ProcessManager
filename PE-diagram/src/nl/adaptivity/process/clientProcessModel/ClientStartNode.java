package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.XmlImportType;
import nl.adaptivity.process.processModel.engine.ProcessNodeSet;

public class ClientStartNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements StartNode<T> {

  private Set<T> aSuccessors;
  public ClientStartNode() {
    super();
  }

  public ClientStartNode(final String pId) {
    super(pId);
  }

  private void resolvePredecessors(final Map<String, T> pMap) {
    // start node has no predecessors
  }

  private void ensureSuccessor(final T pNode) {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<T>();
    }
    aSuccessors.add(pNode);
  }

  @Override
  public List<XmlImportType> getImports() {
    return super.getImports();
  }

  @Override
  public Set<T> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<T>();
    }
    return aSuccessors;
  }

  @Override
  public Set<T> getPredecessors() {
    return Collections.emptySet();
  }

  @Override
  public void setPredecessors(Collection<? extends T> pPredecessors) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  protected void setPredecessor(T pPredecessor) {
    throw new UnsupportedOperationException("Start nodes have no predecessors");
  }

  @Override
  public boolean isPredecessorOf(T pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  protected void setSuccessor(T pSuccessor) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet<T>(1);
    } else {
      aSuccessors.clear();
    }
    aSuccessors.add(pSuccessor);
  }

  @Override
  public void addSuccessor(T pNode) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet<T>(1);
    }
    aSuccessors.add(pNode);
  }

}
