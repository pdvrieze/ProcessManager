package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.Split;


public class ClientSplitNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Split<T> {

  public ClientSplitNode() {
    super();
  }

  public ClientSplitNode(String pId) {
    super(pId);
  }

  protected ClientSplitNode(ClientJoinSplit<T> pOrig) {
    super(pOrig);
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void serialize(SerializerAdapter pOut) {
    serializeSplit(pOut);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> pVisitor) {
    return pVisitor.visitSplit(this);
  }

}
