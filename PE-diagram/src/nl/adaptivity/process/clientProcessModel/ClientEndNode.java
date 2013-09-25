package nl.adaptivity.process.clientProcessModel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.XmlExportType;


public class ClientEndNode extends ClientProcessNode implements EndNode {

  private ProcessNode aPredecessor;

  @Override
  public Set<ProcessNode> getSuccessors() {
    return Collections.emptySet();
  }

  @Override
  public void setSuccessor(ProcessNode pNode) {
    throw new UnsupportedOperationException("end nodes never have successors");
  }

  @Override
  public boolean isPredecessorOf(ProcessNode pNode) {
    // TODO Auto-generated method stub
    // return false;
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public List<XmlExportType> getExports() {
    return super.getExports();
  }

  @Override
  public void setExports(Collection<? extends XmlExportType> pExports) {
    super.setExports(pExports);
  }

  @Override
  public Set<ProcessNode> getPredecessors() {
    return Collections.singleton(aPredecessor);
  }

  @Override
  public ProcessNode getPredecessor() {
    return aPredecessor;
  }

  @Override
  public void setPredecessor(ProcessNode pPredecessor) {
    aPredecessor = pPredecessor;
  }

}
