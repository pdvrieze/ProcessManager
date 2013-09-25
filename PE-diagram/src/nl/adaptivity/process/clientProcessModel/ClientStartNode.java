package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.processModel.XmlImportType;
import nl.adaptivity.process.processModel.engine.ProcessNodeSet;

public class ClientStartNode extends ClientProcessNode implements StartNode {

  private Set<ProcessNode> aSuccessors;
  public ClientStartNode() {
    super();
  }

  public ClientStartNode(final String pId) {
    super(pId);
  }

  private void resolvePredecessors(final Map<String, ProcessNode> pMap) {
    // start node has no predecessors
  }

  private void ensureSuccessor(final ProcessNode pNode) {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ProcessNode>();
    }
    aSuccessors.add(pNode);
  }

  @Override
  public List<XmlImportType> getImports() {
    return super.getImports();
  }

  @Override
  public Set<ProcessNode> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ProcessNode>();
    }
    return aSuccessors;
  }

  @Override
  public Set<ProcessNode> getPredecessors() {
    return Collections.emptySet();
  }

  @Override
  public void setPredecessors(Collection<? extends ProcessNode> pPredecessors) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  protected void setPredecessor(ProcessNode pPredecessor) {
    throw new UnsupportedOperationException("Start nodes have no predecessors");
  }

  @Override
  public boolean isPredecessorOf(ProcessNode pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  protected void setSuccessor(ProcessNode pSuccessor) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet(1);
    } else {
      aSuccessors.clear();
    }
    aSuccessors.add(pSuccessor);
  }

  @Override
  public void addSuccessor(ProcessNode pNode) {
    if (aSuccessors==null) {
      aSuccessors = new ProcessNodeSet(1);
    }
    aSuccessors.add(pNode);
  }

}
