package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.Split;


public class ClientSplitNode<T extends IClientProcessNode<T>> extends ClientJoinSplit<T> implements Split<T> {

  public ClientSplitNode() {
    super();
  }

  public ClientSplitNode(String id) {
    super(id);
  }

  protected ClientSplitNode(ClientJoinSplit<T> orig) {
    super(orig);
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void serialize(SerializerAdapter out) {
    serializeSplit(out);
  }

  @Override
  public <R> R visit(ProcessNode.Visitor<R> visitor) {
    return visitor.visitSplit(this);
  }

}
