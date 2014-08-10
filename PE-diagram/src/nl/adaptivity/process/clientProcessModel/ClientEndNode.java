package nl.adaptivity.process.clientProcessModel;

import static nl.adaptivity.process.clientProcessModel.ClientProcessModel.NS_PM;

import java.util.Collection;
import java.util.List;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public class ClientEndNode<T extends IClientProcessNode<T>> extends ClientProcessNode<T> implements EndNode<T> {

  public ClientEndNode() {
    super();
  }

  public ClientEndNode(String pId) {
    super(pId);
  }

  protected ClientEndNode(ClientEndNode<T> pOrig) {
    super(pOrig);
  }

  @Override
  public int getMaxSuccessorCount() {
    return 0;
  }

  @Override
  public List<IXmlDefineType> getDefines() {
    return super.getDefines();
  }

  @Override
  public void setExports(Collection<? extends IXmlDefineType> pExports) {
    super.setExports(pExports);
  }

  @Override
  public T getPredecessor() {
    ProcessNodeSet<T> list = getPredecessors();
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public void setPredecessor(T pPredecessor) {
    T previous = getPredecessor();
    if (previous==null) {
      removePredecessor(previous);
    }
    addPredecessor(pPredecessor);
  }

  @Override
  public void serialize(SerializerAdapter pOut) {
    pOut.startTag(NS_PM, "end", true);
    serializeCommonAttrs(pOut);
    serializeCommonChildren(pOut);
    pOut.endTag(NS_PM, "end", true);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitEndNode(this);
  }

}
