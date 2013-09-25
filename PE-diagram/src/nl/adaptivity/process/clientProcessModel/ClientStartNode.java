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

public class ClientStartNode extends ClientProcessNode implements StartNode {

  private Set<ProcessNode> aSuccessors;
  private List<XmlImportType> aImports;

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
  public boolean isPredecessorOf(ProcessNode pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public List<XmlImportType> getImports() {
    return aImports;
  }

  @Override
  public void setSuccessors(Collection<? extends ProcessNode> pSuccessors) {
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  protected void setPredecessor(ProcessNode pPredecessor) {
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  protected void setSuccessor(ProcessNode pSuccessor) {
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");
  }

}
